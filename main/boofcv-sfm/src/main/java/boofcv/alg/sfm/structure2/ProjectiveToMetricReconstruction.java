/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.sfm.structure2;

import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Upgrades a projective reconstruction from N-views into a metric reconstruction. Following the steps listed below:
 * <ol>
 *     <li>Dual quadratic self calibration via projective camera matrices</li>
 *     <li>Create features from set of projective observation inliers and triangulation in metric space</li>
 *     <li>Remove feature observations which are inconsistent with a feature's 3D location</li>
 *     <li>Sparse Bundle Adjustment</li>
 * </ol>
 *
 * Output: Most of the results are written to the provided {@link SceneWorkingGraph}. The one exception is the
 * bundle adjustment refined camera. To get those parameters call {@link #getRefinedCamera(String)}.
 *
 * <p>WARNING: This approach primarily acts as a warning to others for what not to do. The projective
 * reconstructions have a serious issue where the scale rapidly converges towards extreme small values
 * the farther you are from the seed. The initial idea was that the metric reconstruction would work better
 * with more views but that didn't appear to be the case either since projective to metric is very sensitive
 * to noise. Upon reviewing literature again it seems everyone does the projective to metric elevation
 * from a small number of views (2 to 3 is typical) instead of an arbitrary large number.</p>
 *
 * @author Peter Abeles
 */
public class ProjectiveToMetricReconstruction implements VerbosePrint {

	// Some configurations for this class. Used to adjust how it refines
	@Getter ConfigProjectiveToMetric config;

	// The bundle adjustment algorithm
	@Getter BundleAdjustment<SceneStructureMetric> bundleAdjustment;
	// Bundle adjustment data structure and tuning parameters
	final SceneStructureMetric structure = new SceneStructureMetric(false);
	final SceneObservations observations = new SceneObservations();

	// If not null then verbose output
	private PrintStream verbose;

	// Used to find the 3D location of a feature after the view has been upgraded to metric
	@Getter TriangulateNViewsMetric triangulator;

	protected @Getter SelfCalibrationLinearDualQuadratic selfCalib;

	// convenience references to input
	SceneWorkingGraph graph;
	LookupSimilarImages db;

	//------------------------- Workspace Variables
	final FastQueue<ImageInfo> listInfo = new FastQueue<>(ImageInfo::new, ImageInfo::reset);
	final FastQueue<Point2D_F64> triangulateObs = new FastQueue<>(Point2D_F64::new);
	final FastQueue<Se3_F64> triangulateViews = new FastQueue<>(Se3_F64::new);
	final Se3_F64 origin_to_world = new Se3_F64();

	/**
	 * Specifies which implementations of internal classes to use as well as settings
	 */
	public ProjectiveToMetricReconstruction(ConfigProjectiveToMetric config,
											BundleAdjustment<SceneStructureMetric> bundleAdjustment,
											TriangulateNViewsMetric triangulator)
	{
		this.config = config;
		this.bundleAdjustment = bundleAdjustment;
		this.triangulator = triangulator;

		this.selfCalib = new SelfCalibrationLinearDualQuadratic(config.aspectRatio);
	}

	/** Default constructor for unit testing */
	protected ProjectiveToMetricReconstruction() {}

	/**
	 * Performs the projective to metric escalation
	 *
	 * @param db Image database
	 * @param sceneGraph (Input/Output) Scene graph with a projective estimate for camera matrices.
	 *                   Output will have the metric scene (views and points)
	 * @return true if successful.
	 */
	public boolean process( LookupSimilarImages db , SceneWorkingGraph sceneGraph )
	{
		initialize(db, sceneGraph);

		// Self calibration and upgrade views
		if (!upgradeViewsToMetric())
			return false;

		// Compute 3D feature locations and prepare for bundle adjustment
		createFeaturesFromInliers();

		// Prune observation from features which are impossible
		pruneObservationsBehindCamera();

		// Copy into SBA data structure for consistency when reading results
		if (!buildMetricSceneForBundleAdjustment())
			return false;

		// Refine the estimated using using bundle adjustment
		if( config.sbaConverge.maxIterations > 0 ) {
			if (refineWithBundleAdjustment()) {
				copyBundleAdjustmentResults();
			}
		}
		// undo how the principle point was forced to be (0,0)
		for (int viewCnt = 0; viewCnt < sceneGraph.viewList.size(); viewCnt++) {
			SceneWorkingGraph.View v = sceneGraph.viewList.get(viewCnt);
			v.pinhole.cx = v.pinhole.width/2;
			v.pinhole.cy = v.pinhole.height/2;
		}

		return true;
	}

	/**
	 * Initialize internal data structures
	 */
	void initialize(LookupSimilarImages db, SceneWorkingGraph sceneGraph) {
		this.db = db;
		this.graph = sceneGraph;

		// Save the shape of each image
		ImageDimension shape = new ImageDimension();
		for (int viewCnt = 0; viewCnt < sceneGraph.viewList.size(); viewCnt++) {
			SceneWorkingGraph.View v = sceneGraph.viewList.get(viewCnt);
			db.lookupShape(v.pview.id,shape);
			v.pinhole.width = shape.width;
			v.pinhole.height = shape.height;
		}
	}

	/**
	 * Uses found projective view cameras to elevate the scene into metric
	 *
	 * @return true if successful
	 */
	boolean upgradeViewsToMetric() {
		// Perform self calibration using all the found camera matrices
		// For self calibration pixel observations must have a principle point of (0,0) this is done by shifting
		// all observation and camera matrices over by 1/2 the width and height
		// C = [1 0 -w/2; 0 1 -h/2; 0 0 1]
//		var C = CommonOps_DDRM.identity(3);
		var P = new DMatrixRMaj(3,4);
		graph.viewList.forEach(o->{
			// Create a matrix which will make the projective camera matrix centered at the image's origin
			// C*x = C*P*X
//			C.set(0,2,-o.pinhole.width/2);
//			C.set(1,2,-o.pinhole.height/2);
			// Apply to the found projective camera matrix
//			CommonOps_DDRM.mult(C,o.projective,P);
			// Add it to the list. A copy is made internally so we can recycle P
			selfCalib.addCameraMatrix(o.projective);
		});

		GeometricResult result = selfCalib.solve();
		if( result != GeometricResult.SUCCESS ) {
			if( verbose != null ) {
				verbose.println("Self calibration failed. "+result+" used views.size="+graph.viewList.size());
				// Print out singular values to see if there was a clear null space
				double[] sv = selfCalib.getSvd().getSingularValues();
				for (int i = 0; i < sv.length; i++) {
					verbose.println("  sv["+i+"] = "+sv[i]);
				}
			}
			return false;
		}
		// homography to go from projective to metric
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		// convert camera matrix from projective to metric
		if( !MultiViewOps.absoluteQuadraticToH(selfCalib.getQ(),H) ) {
			if( verbose != null ) verbose.println("Projective to metric failed to compute H");
			return false;
		}

		// Save the upgraded metric calibration for each camera
		DMatrixRMaj K = new DMatrixRMaj(3,3);
		FastAccess<SelfCalibrationLinearDualQuadratic.Intrinsic> solutions = selfCalib.getSolutions();
		for (int viewIdx = 0; viewIdx < graph.viewList.size(); viewIdx++) {
			SelfCalibrationLinearDualQuadratic.Intrinsic intrinsic = solutions.get(viewIdx);
			SceneWorkingGraph.View wv = graph.viewList.get(viewIdx);
			// the image shape was already set
			wv.pinhole.fsetK(intrinsic.fx,intrinsic.fy,intrinsic.skew,0,0,wv.pinhole.width,wv.pinhole.height);

			// ignore K since we already have that
			MultiViewOps.projectiveToMetric(wv.projective,H,wv.world_to_view,K);
		}

		// scale is arbitrary. Set max translation to 1. This should be better numerically
		double maxT = 0;
		for (int viewIdx = 0; viewIdx < graph.viewList.size(); viewIdx++) {
			SceneWorkingGraph.View wv = graph.viewList.get(viewIdx);
			maxT = Math.max(maxT,wv.world_to_view.T.norm());
		}
		for (int viewIdx = 0; viewIdx < graph.viewList.size(); viewIdx++) {
			SceneWorkingGraph.View wv = graph.viewList.get(viewIdx);
			wv.world_to_view.T.scale(1.0/maxT);
		}
		return true;
	}

	/**
	 * Using the list of inliers, create a set of features for the entire scene. Since multiple sets of inliers
	 * a set is arbitrarily used, but all sets have their observations combined. This can create inconsistent
	 * observations and results should be sanity checked.
	 */
	public void createFeaturesFromInliers() {
		if( verbose != null ) verbose.println("ENTER create features");
		graph.features.clear();

		// Storage for triangulated point
		var triangulatedPt = new Point3D_F64();

		for( SceneWorkingGraph.View target_v : graph.views.values() ) {
			// if there are no inliers saved with this view skip it.
			if( target_v.inliers.isEmpty() )
				continue;
			// quick sanity check to see if the data structure fulfills its contract
			assertBoof(target_v.inliers.views.get(0)==target_v.pview);

			// grab inlier information for this view. local variables are just short cuts
			final SceneWorkingGraph.InlierInfo inliers = target_v.inliers;
			assertBoof(inliers.observations.size == inliers.views.size);
			final int numViews = inliers.views.size;
			final int numInliers = inliers.observations.get(0).size;

			if( verbose != null ) verbose.println(" view["+target_v.pview.id+"] inliers="+numInliers+" views="+numViews);

			// Look up the observations for all views in the inlier set. This is used for triangulation
			loadInlierObservations(inliers);

			// Go through all the features in the inlier set and create a new feature, merge features,
			// or assign features
			for (int inlierCnt = 0; inlierCnt < numInliers; inlierCnt++) {
				// See if any of the other views have this observation assigned to a feature
				SceneWorkingGraph.Feature feature = null;
				for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
					SceneWorkingGraph.View wview = graph.views.get( inliers.views.get(viewCnt).id );
					// index of the observation in this view
					int observationIdx = inliers.observations.get(viewCnt).get(inlierCnt);
					SceneWorkingGraph.Feature found = wview.getFeatureFromObs(observationIdx);
					if( found == null || found == feature)
						continue;
					if( feature != null ) {
						// The same object has been assigned two different features.
						mergeFeatures(found,feature);
					} else {
						feature = found;
					}
				}

				if( feature == null ) {
					// Create a new feature since none exist for any of these observation
					feature = graph.createFeature();
					if( !triangulateFeature(inliers,inlierCnt,triangulatedPt) ) {
						// skip feature if triangulation fails
						graph.features.remove(graph.features.size()-1);
						continue;
					}
					feature.location.set(triangulatedPt.x,triangulatedPt.y,triangulatedPt.z,1.0);
					for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
						SceneWorkingGraph.View wview = graph.views.get( inliers.views.get(viewCnt).id );
						createNewObservation(inliers, inlierCnt, feature, viewCnt, wview);
					}
				}

				// Assign the Feature to any views where it wasn't already assigned
				for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
					SceneWorkingGraph.View wview = graph.views.get( inliers.views.get(viewCnt).id );
					int observationIdx = inliers.observations.get(viewCnt).get(inlierCnt);
					if( wview.getFeatureFromObs(observationIdx) != null )
						continue;
					createNewObservation(inliers, inlierCnt, feature, viewCnt, wview);
				}
			}
		}
		if( verbose != null ) verbose.println("EXIT create features");
	}

	/**
	 * Loads observations for this set of inliers
	 */
	void loadInlierObservations(SceneWorkingGraph.InlierInfo inliers) {
		final int numViews = inliers.views.size;
		final int numInliers = inliers.observations.get(0).size;
		listInfo.reset();
		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			// sanity check that the list of inlier observations are all the same size
			assertBoof(numInliers==inliers.observations.get(viewCnt).size);
			// Load the actual pixel observations from each view
			PairwiseImageGraph2.View v = inliers.views.get(viewCnt);
			db.lookupPixelFeats(v.id,listInfo.grow().pixels);
		}
	}

	/**
	 * Creates a new observation from the inlier set to a feature and a view
	 */
	void createNewObservation(SceneWorkingGraph.InlierInfo inliers, int inlierIndex,
							  SceneWorkingGraph.Feature feature,
							  int viewIndex,
							  SceneWorkingGraph.View wview)
	{
		SceneWorkingGraph.Observation o = new SceneWorkingGraph.Observation();
		o.view = wview;
		o.observationIdx = inliers.observations.get(viewIndex).get(inlierIndex);
		o.pixel.set(listInfo.get(viewIndex).pixels.get(o.observationIdx));
		o.view.obs_to_feat.put(o.observationIdx, feature);
		feature.observations.add(o);
	}

	/**
	 * Merges 'src' into 'dst' feature so that there is only one
	 */
	void mergeFeatures(SceneWorkingGraph.Feature src , SceneWorkingGraph.Feature dst ) {
		// Re-map the Feature each view points to dst
		for (int obsCnt = 0; obsCnt < src.observations.size(); obsCnt++) {
			SceneWorkingGraph.Observation o = src.observations.get(obsCnt);
			o.view.obs_to_feat.put(o.observationIdx,dst);
		}
		// Add all Feature observations to dst
		dst.observations.addAll(src.observations);

		// Remove this from the list
		// WARNING! This will become expensive for large scenes
		assertBoof(graph.features.remove(src));
	}

	/**
	 * Performs triangulation on the specified feature from inlier observations
	 *
	 * @param info (Input) Information on the inlier set
	 * @param featureIdx (Input) index of the feature in the inlier set which is to be triangulated
	 * @param X (output) storage for the triangulated feature in world coordinates
	 * @return true if successful
	 */
	boolean triangulateFeature(SceneWorkingGraph.InlierInfo info, int featureIdx, Point3D_F64 X)
	{
		assertBoof(info.observations.size==info.views.size);
		int numViews = info.views.size;

		// For numerical reasons, triangulate in the reference frame of the first view
		Se3_F64 world_to_origin = graph.views.get(info.views.get(0).id).world_to_view;
		world_to_origin.invert(origin_to_world);

		triangulateObs.reset();
		triangulateViews.reset();
		for (int inlierViewCnt = 0; inlierViewCnt < numViews; inlierViewCnt++) {
			SceneWorkingGraph.View wview = graph.views.get(info.views.get(inlierViewCnt).id);

			// get the index of the observation for this feature in this view
			int observationIdx = info.observations.get(inlierViewCnt).get(featureIdx);
			// look up the value of the observation and save it
			Point2D_F64 observationPixel = listInfo.get(inlierViewCnt).pixels.get(observationIdx);

			// Don't forget the the camera model has it's origin at the image center
			double pixelX = observationPixel.x - wview.pinhole.width/2;
			double pixelY = observationPixel.y - wview.pinhole.height/2;

			// Go from pixels to normalized image coordinates
			PerspectiveOps.convertPixelToNorm(wview.pinhole,pixelX,pixelY,triangulateObs.grow());

			// compute origin_to_viewI in view0's reference frame
			origin_to_world.concat(wview.world_to_view,triangulateViews.grow());
		}

		// Compute the 3D location
		if( !triangulator.triangulate(triangulateObs.toList(), triangulateViews.toList(),X) )
			return false;

		// convert it back into the world frame
		SePointOps_F64.transform(origin_to_world,X,X);

		return true;
	}

	/**
	 * Checks to see if a feature appears behind the view. if so the observation is removed. A feature is removed
	 * if there are less than two observation left.
	 */
	void pruneObservationsBehindCamera() {
		// storage for point in camera view
		Point3D_F64 cameraPt = new Point3D_F64();
		for (int featureCnt = graph.features.size()-1; featureCnt >= 0; featureCnt--) {
			SceneWorkingGraph.Feature f = graph.features.get(featureCnt);

			for (int observationCnt = f.observations.size()-1; observationCnt >= 0; observationCnt--) {
				SceneWorkingGraph.Observation o = f.observations.get(observationCnt);

				SePointOps_F64.transform(o.view.world_to_view, f.location, cameraPt);
				// if the feature is behind the camera assume it's incorrect and remove it
				if( cameraPt.z < 0 ) {
					o.view.obs_to_feat.remove(o.observationIdx);
					f.observations.remove(observationCnt);
				}
			}

			// remove features which are no longer valid. This is also a weird situation that shouldn't happen
			if( f.observations.size() < 2 ) {
				graph.features.remove(featureCnt);
			}
		}
	}

	boolean buildMetricSceneForBundleAdjustment() {
		final int numViews = graph.viewList.size();

		// Construct bundle adjustment data structure
		structure.initialize(numViews,numViews, graph.features.size());
		observations.initialize(numViews);

		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			SceneWorkingGraph.View wview = graph.viewList.get(viewCnt);
			wview.index = viewCnt;
			CameraPinhole cp = wview.pinhole;
			BundlePinholeSimplified bp = new BundlePinholeSimplified();

			bp.f = cp.fx;

			structure.setCamera(viewCnt,false,bp);
			structure.setView(viewCnt,viewCnt==0,wview.world_to_view);
			structure.connectViewToCamera(viewCnt,viewCnt);
		}
		for (int featureCnt = 0; featureCnt < graph.features.size(); featureCnt++) {
			SceneWorkingGraph.Feature f = graph.features.get(featureCnt);

			structure.setPoint(featureCnt, f.location.x, f.location.y, f.location.z);

			for (int obsCnt = 0; obsCnt < f.observations.size(); obsCnt++) {
				SceneWorkingGraph.Observation o = f.observations.get(obsCnt);
				structure.connectPointToView(featureCnt,o.view.index);
				// the camera model assumes the optical center is (0,0)
				CameraPinhole cp = o.view.pinhole;
				final double recentered_x = o.pixel.x - cp.width/2;
				final double recentered_y = o.pixel.y - cp.height/2;
				observations.getView(o.view.index).add(featureCnt, (float)recentered_x, (float)recentered_y);
			}
		}
		return true;
	}

	boolean refineWithBundleAdjustment() {
		bundleAdjustment.configure(config.sbaConverge.ftol, config.sbaConverge.gtol, config.sbaConverge.maxIterations);
		bundleAdjustment.setParameters(structure,observations);
		if( !bundleAdjustment.optimize(structure) ) {
			if( verbose != null ) verbose.println("Bundle adjustment failed!");
			return false;
		}
		return true;
	}

	void copyBundleAdjustmentResults() {
		final int numViews = graph.viewList.size();
		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			SceneWorkingGraph.View wview = graph.viewList.get(viewCnt);
			wview.world_to_view.set(structure.getViews().get(viewCnt).worldToView);
			// TODO what to do with optimized model?
		}

		for (int featureCnt = 0; featureCnt < graph.features.size(); featureCnt++) {
			SceneWorkingGraph.Feature f = graph.features.get(featureCnt);
			structure.getPoints().get(featureCnt).get3(f.location);
		}
	}

	/**
	 * Returns the bundle adjustment camera model.
	 */
	public <C extends BundleAdjustmentCamera>C getRefinedCamera( String viewID ) {
		int index = graph.views.get(viewID).index;
		return (C)structure.getCameras().get(index).model;
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		this.verbose = out;
	}

	static class ImageInfo
	{
		final public FastQueue<Point2D_F64> pixels = new FastQueue<>(Point2D_F64::new,p->p.set(-1,-1));

		public void reset() {
			pixels.reset();
		}
	}
}
