/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Uses SBA to refine the intrinsic parameters and camera locations inside of a {@link SceneWorkingGraph}. This is
 * more complex than it sounds since {@link SceneWorkingGraph} does not store 3D features it just stores which
 * observations that are part of inlier sets could create a 3D feature. The same observations will belong to different
 * inlier sets and it is possible that they don't point to the same physical feature due to track drift.
 *
 * @author Peter Abeles
 */
public class RefineMetricWorkingGraph implements VerbosePrint {
	/**
	 * If the reprojection error is greater than this then the observation will not be assigned to the feature.
	 * Units are in pixels.
	 * WARNING: Based on test results it seems like this feature makes things worse and is turned off by default.
	 */
	public double maxReprojectionErrorPixel = 1e100;

	/** Bundle Adjustment functions and configurations */
	public final MetricBundleAdjustmentUtils metricSba;

	// pixels to undistorted normalized image coordinates and the reverse
	protected final List<Point2Transform2_F64> listPixelToNorm = new ArrayList<>();
	protected final List<Point2Transform2_F64> listNormToPixel = new ArrayList<>();

	// Storage for the index/ID of a particular view
	TObjectIntHashMap<String> viewToIntegerID = new TObjectIntHashMap<>();

	private @Nullable PrintStream verbose;
	/** If verbose, then it will print view specific information. This can get very verbose in large scenes */
	public boolean verboseViewInfo = true;

	/** Used to provide custom logic when triangulating points. Useful when merging scenes */
	public FilterInlierSet inlierFilter = ( scene, info ) -> true;

	//------------------------ Internal workspace

	// Storage for pixel observations looked up from the db
	protected final DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new);

	// Used for triangulation
	protected final DogArray<Point2D_F64> pixelNormalized = new DogArray<>(Point2D_F64::new);
	// Poses from local view[0] to view[i]. This is done to reduce numerical issues
	protected final DogArray<Se3_F64> listPoses = new DogArray<>(Se3_F64::new);
	protected final Se3_F64 view0_to_world = new Se3_F64();

	// which observations in the inlier set are unassigned. values are indexes in inlier set
	final DogArray_I32 unassigned = new DogArray_I32();
	// array index of a world / 3D feature
	final DogArray_I32 featureIdx3D = new DogArray_I32();
	// Look up table from inlier view index to index of the view in the graph view list
	final DogArray_I32 sceneViewIntIds = new DogArray_I32();
	// storage for pixel observation
	private final Point2D_F64 pixelObserved = new Point2D_F64();
	// storage for predicted pixel
	private final Point2D_F64 pixelPredicted = new Point2D_F64();
	// storage for 3D feature in homogenous coordinates in world reference frame
	private final Point4D_F64 world3D = new Point4D_F64();
	// 3D feature in camera coordinates
	private final Point4D_F64 camera3D = new Point4D_F64();
	// Storage for triangulated point
	private final Point4D_F64 found3D = new Point4D_F64();

	public RefineMetricWorkingGraph( MetricBundleAdjustmentUtils metricSba ) {
		this.metricSba = metricSba;
	}

	public RefineMetricWorkingGraph() {
		this(new MetricBundleAdjustmentUtils());
	}

	/**
	 * Use the `graph` to define a 3D scene which can be optimized.
	 *
	 * @param dbSimilar (Input) Used to lookup common features between views.
	 * @param graph (Input, Output) Describes scene and provides initial estimate for parameters. Updated with refined
	 * parameters on output.
	 */
	public boolean process( LookUpSimilarImages dbSimilar, SceneWorkingGraph graph ) {
		return process(dbSimilar, graph, ( utils ) -> {});
	}

	/**
	 * Use the `graph` to define a 3D scene which can be optimized.
	 *
	 * @param dbSimilar (Input) Used to lookup common features between views.
	 * @param graph (Input, Output) Describes scene and provides initial estimate for parameters. Updated with refined
	 * parameters on output.
	 */
	public boolean process( LookUpSimilarImages dbSimilar, SceneWorkingGraph graph, CallBeforeRefine op ) {
		if (!constructBundleScene(dbSimilar, graph))
			return false;

		// Use provided function that allows for customization
		op.process(metricSba);

		return refineViews(graph);
	}

	/**
	 * Initializes the scene in bundle adjustment
	 */
	public boolean constructBundleScene( LookUpSimilarImages dbSimilar, SceneWorkingGraph graph ) {
		// Pre-declare and compute basic data structures
		initializeDataStructures(dbSimilar, graph);

		// Use observations defined in the graph to create the list of 3D features which will be optimized
		if (!createFeatures3D(graph))
			return false;

		// Clean up by removing observations which were never assigned to a feature
		pruneUnassignedObservations();

		return true;
	}

	/**
	 * Initialized several data structures and resets it into the initial state
	 */
	void initializeDataStructures( LookUpSimilarImages dbSimilar, SceneWorkingGraph graph ) {
		viewToIntegerID.clear();
		listPixelToNorm.clear();
		listNormToPixel.clear();

		final SceneStructureMetric structure = metricSba.structure;
		final SceneObservations observations = metricSba.observations;

		// Initialize the structure, but save initializing the points for later
		structure.initialize(graph.listCameras.size, graph.listViews.size(), 0);

		// Go through each view and load the observations then add them to the scene, but don't specify which
		// 3D point they are observing yet
		observations.initialize(graph.listViews.size());

		// First add cameras to the structure
		for (int cameraIdx = 0; cameraIdx < graph.listCameras.size; cameraIdx++) {
			SceneWorkingGraph.Camera wcam = graph.listCameras.get(cameraIdx);
			structure.setCamera(cameraIdx, false, wcam.intrinsic);
		}

		// add views next
		for (int viewIdx = 0; viewIdx < graph.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = graph.listViews.get(viewIdx);
			SceneObservations.View oview = observations.getView(viewIdx);

			viewToIntegerID.put(wview.pview.id, viewIdx);
			createProjectionModel(graph.getViewCamera(wview).intrinsic);

			// Add all observations in this view to the SBA observations.
			// Observations that are not assigned to a 3D point will be pruned later on. Much easier this way.
			oview.resize(wview.pview.totalObservations);
			dbSimilar.lookupPixelFeats(wview.pview.id, pixels);
			BoofMiscOps.checkEq(pixels.size, wview.pview.totalObservations);

			// The camera model assumes the principle point is (0,0) and this is done by assuming it's the image center
			SceneWorkingGraph.Camera camera = graph.getViewCamera(wview);
			float cx = (float)camera.prior.cx;
			float cy = (float)camera.prior.cy;

			// specify the observation pixel coordinates but not which 3D feature is matched to the observation
			for (int obsIdx = 0; obsIdx < pixels.size; obsIdx++) {
				Point2D_F64 p = pixels.get(obsIdx);
				oview.setPixel(obsIdx, (float)(p.x - cx), (float)(p.y - cy));
			}

			// Add this view to the graph and it's location
			structure.setView(viewIdx, camera.localIndex, viewIdx == 0, wview.world_to_view);
		}
	}

	/**
	 * Computes the 3D features by triangulating inliers
	 */
	boolean createFeatures3D( SceneWorkingGraph graph ) {
		int filtered = 0;
		int total = 0;
		// For each view, with a set inliers, create a set of triangulated 3D point features
		for (int workingIdx = 0; workingIdx < graph.listViews.size(); workingIdx++) {
			SceneWorkingGraph.View wview = graph.listViews.get(workingIdx);

			for (int infoIdx = 0; infoIdx < wview.inliers.size; infoIdx++) {
				final SceneWorkingGraph.InlierInfo inliers = wview.inliers.get(infoIdx);

				// See if it should skip over these features
				if (!inlierFilter.keep(wview, inliers)) {
					filtered++;
					continue;
				}

				if (verbose != null && verboseViewInfo)
					verbose.print("inlier[" + infoIdx + "] view='" + wview.pview.id + "' size=" + inliers.getInlierCount() + " , ");

				createFeaturesFromInlierInfo(graph, inliers);
			}
			total += wview.inliers.size;
		}

		if (verbose != null) verbose.println("triangulation: sets: skipped=" + filtered + " total=" + total);

		return filtered < total;
	}

	private void createFeaturesFromInlierInfo( SceneWorkingGraph graph, SceneWorkingGraph.InlierInfo inlierSet ) {
		final FastArray<PairwiseImageGraph.View> inlierViews = inlierSet.views;

		// Initialize data structures for this particular set of inlier observations
		initLookUpTablesForInlierSet(graph, inlierViews);

		final int numInliers = inlierSet.getInlierCount();

		int countMatched = 0;
		int countMixed = 0;
		int tooFew = 0;

		// Go through each feature in the inlier list and see if it needs to be triangulated or not, If not determine
		// which feature it belongs to
		for (int inlierIdx = 0; inlierIdx < numInliers; inlierIdx++) {
			// Look up what feature this observation has been assigned to
			findUnassignedObsAndKnown3D(inlierSet, inlierIdx);

			if (unassigned.size != 0 && featureIdx3D.size != 0) {
				countMixed++;
			} else if (featureIdx3D.size != 0) {
				countMatched++;
			}

			// See if this inlier was assigned to one or more known scene features
			if (featureIdx3D.size > 0) {
				// If so, let's see if we can match this scene feature up to others views where it was not assigned
				// already
				assignKnown3DToUnassignedObs(graph, inlierSet, inlierIdx);
			}

			// If there is 2 or more unassigned observations remaining triangulate and create a new 3D feature
			if (unassigned.size < 2) {
				if (unassigned.size > 0)
					tooFew++;
				continue;
			}

			// Create a new feature and save the unassigned observations to it
			triangulateAndSave(inlierSet, inlierIdx);

			// NOTE: it is possible that 2+ features are created for one physical feature with this greedy approach
		}

		if (verbose != null && verboseViewInfo) {
			verbose.println("Adding Points: unmatched=" + (numInliers - countMatched) + " matched=" + countMatched + " mixed=" +
					countMixed + " tooFew=" + tooFew);
		}
	}

	/**
	 * Creates a look up table to go from a view's inlier index to it's int ID and SE3. To reduce numerical
	 * issues when triangulating, put all views in the local coordinate system. IDEA: Also rescale to make
	 * translations around 1.0.
	 */
	void initLookUpTablesForInlierSet( SceneWorkingGraph graph, FastArray<PairwiseImageGraph.View> inlierViews ) {
		pixelNormalized.resize(inlierViews.size);
		sceneViewIntIds.reset();
		listPoses.resize(inlierViews.size);

		// Origin of local coordinate system
		Se3_F64 world_to_view0 = graph.lookupView(inlierViews.get(0).id).world_to_view;
		world_to_view0.invert(view0_to_world);

		for (int i = 0; i < inlierViews.size; i++) {
			String viewID = inlierViews.get(i).id;
			// create a list of the array indexes of all the views included in this inlier set
			sceneViewIntIds.add(viewToIntegerID.get(viewID));
			// also create a list of view locations for triangulation
			view0_to_world.concat(graph.lookupView(viewID).world_to_view, listPoses.get(i));
		}
	}

	/**
	 * Assigns world 3D features that were already matched to others in the inlier view set to the
	 * unassigned observations.
	 *
	 * Part of the idea behind only associating an observation with a 3D feature if the preprojection error is less
	 * than some value is that tracks can drift from one object to another, but it's useful to save both.
	 *
	 * @param inlierIdx Index of the observations. All observations with this index point to the same 3D feature
	 */
	void assignKnown3DToUnassignedObs( SceneWorkingGraph graph, SceneWorkingGraph.InlierInfo inliers,
									   int inlierIdx ) {
		final SceneStructureMetric structure = metricSba.structure;
		final SceneObservations observations = metricSba.observations;

		// Go through all the views/observations which have yet to be assigned a 3D feature
		for (int unassignedIdx = unassigned.size - 1; unassignedIdx >= 0; unassignedIdx--) {
			int whichViewInliers = unassigned.get(unassignedIdx);
			int whichViewID = sceneViewIntIds.get(whichViewInliers);

			// Lookup the pixel observation in the view
			int viewObsIdx = inliers.observations.get(whichViewInliers).get(inlierIdx);
			observations.getView(whichViewID).getPixel(viewObsIdx, pixelObserved);

			// look up scene information for this view
			SceneWorkingGraph.View wview = graph.listViews.get(whichViewID);
			Point2Transform2_F64 normToPixels = listNormToPixel.get(whichViewID);

			// See which 3D feature best matches this observation
			double bestScore = maxReprojectionErrorPixel*maxReprojectionErrorPixel;
			int bestId = -1;
			for (int knownIdx = 0; knownIdx < featureIdx3D.size; knownIdx++) {
				int featureId = featureIdx3D.get(knownIdx);
				// If this feature has already been assigned to this view skip over it
				if (structure.points.get(featureId).views.contains(whichViewID))
					continue;
				structure.getPoints().get(featureId).get(world3D);
				double error = computeReprojectionError(wview.world_to_view, normToPixels, pixelObserved, world3D);
				if (error <= bestScore) {
					bestScore = error;
					bestId = featureId;
				}
			}
			if (bestId == -1) {
				if (verbose != null) verbose.println("Not matching. Reprojection error too large view=" + whichViewID);
				continue;
			}

			// assign this scene feature to this observation
			observations.getView(whichViewID).safeAssignToFeature(viewObsIdx, bestId);
			structure.connectPointToView(bestId, whichViewID);
			// Remove it since it has been assigned. This is also why we iterate in reverse
			unassigned.removeSwap(unassignedIdx);
		}
	}

	double computeReprojectionError( Se3_F64 world_to_view, Point2Transform2_F64 normToPixels,
									 Point2D_F64 pixelObs, Point4D_F64 world3D ) {
		// Compute observed pixel coordinate
		SePointOps_F64.transform(world_to_view, world3D, camera3D);
		// w component is ignored. x = [I(3) 0]*X
		double normX = camera3D.x/camera3D.z;
		double normY = camera3D.y/camera3D.z;
		normToPixels.compute(normX, normY, pixelPredicted);

		return pixelObs.distance2(pixelPredicted);
	}

	/**
	 * Finds observations for this particular inlier which are not assigned to an observation already and creates
	 * a list of 3D features which have been assigned to observations
	 *
	 * @param inlierIdx Index of the feature in the inlierSet.
	 */
	void findUnassignedObsAndKnown3D( SceneWorkingGraph.InlierInfo inlierSet, int inlierIdx ) {
		unassigned.reset();
		featureIdx3D.reset();

		// Go through each view in the inlier set
		for (int inlierViewIdx = 0; inlierViewIdx < sceneViewIntIds.size; inlierViewIdx++) {
			// Get observation index of the inlier in this view
			int obsIdx = inlierSet.observations.get(inlierViewIdx).get(inlierIdx);
			// Look up the corresponding (if any) feature that this observation is observing
			int featIdx = metricSba.observations.views.get(sceneViewIntIds.get(inlierViewIdx)).getPointId(obsIdx);
			if (featIdx >= 0) {
				// This observation has been assigned already and points to a known feature
				if (!featureIdx3D.contains(featIdx))
					featureIdx3D.add(featIdx);
			} else {
				// This observation has not been assigned a 3D feature already
				unassigned.add(inlierViewIdx);
			}
		}
	}

	/**
	 * Add camera projection to and from normalized image coordinate for the specified intrinsic parameters
	 */
	void createProjectionModel( BundlePinholeSimplified intrinsic ) {
		CameraPinholeBrown brown = new CameraPinholeBrown();
		BundleAdjustmentOps.convert(intrinsic, 0, 0, brown);
		LensDistortionNarrowFOV model = LensDistortionFactory.narrow(brown);
		listPixelToNorm.add(model.undistort_F64(true, false));
		listNormToPixel.add(model.distort_F64(false, true));
	}

	/**
	 * Triangulates a new 3D feature, adds it to the structure, and links observations to it
	 *
	 * @param inlierIdx which inlier is being triangulated
	 */
	void triangulateAndSave( SceneWorkingGraph.InlierInfo inlierSet, int inlierIdx ) {
		final SceneStructureMetric structure = metricSba.structure;
		final SceneObservations observations = metricSba.observations;
		final TriangulateNViewsMetricH triangulator = metricSba.triangulator;

		// Get a list of observations in normalized image coordinates
		for (int inlierViewIdx = 0; inlierViewIdx < sceneViewIntIds.size; inlierViewIdx++) {
			int viewID = sceneViewIntIds.get(inlierViewIdx);
			int obsIdx = inlierSet.observations.get(inlierViewIdx).get(inlierIdx);
			observations.getView(viewID).getPixel(obsIdx, pixelObserved);
			listPixelToNorm.get(viewID).compute(pixelObserved.x, pixelObserved.y, pixelNormalized.get(inlierViewIdx));
		}

		// Triangulate 3D feature's location in the coordinate system of view[0]
		// All features are being used to triangulate because they are part of the inlier set, all though it's possible
		// that one or more them has already been assigned to a different 3D feature. Basically there might have been
		// a mistake
		if (!triangulator.triangulate(pixelNormalized.toList(), listPoses.toList(), found3D))
			return;

		// Verify it's not behind the camera. Unclear if it should give up here or try to optimize it
//		if( triangulated.z*triangulated.w < 0 ) {
//			if( verbose != null ) verbose.println("Triangulated point behind the camera");
//			return;
//		}

		// listPoses is relative to view0. Bring it into global reference frame
		SePointOps_F64.transform(view0_to_world, found3D, found3D);
		// Add the new 3D point to the scene
		int pointID = structure.points.size;
		SceneStructureCommon.Point point3D = structure.points.grow();

		if (structure.isHomogenous())
			point3D.set(found3D.x, found3D.y, found3D.z, found3D.w);
		else
			point3D.set(found3D.x/found3D.w, found3D.y/found3D.w, found3D.z/found3D.w);

		// Only assigned this 3D point to views which are unassigned.
		for (int i = 0; i < unassigned.size; i++) {
			int inlierViewIdx = unassigned.get(i);
			int obsIdx = inlierSet.observations.get(inlierViewIdx).get(inlierIdx);
			int viewID = sceneViewIntIds.get(inlierViewIdx);
			observations.getView(viewID).point.set(obsIdx, pointID);
			point3D.views.add(viewID);
		}
	}

	/**
	 * Prunes unassigned observations from each view. This is done by swapping which is fast but does change the order
	 */
	void pruneUnassignedObservations() {
		final SceneObservations observations = metricSba.observations;

		for (int i = 0; i < observations.views.size; i++) {
			SceneObservations.View v = observations.views.get(i);

			for (int j = v.point.size() - 1; j >= 0; j--) {
				if (v.point.get(j) == -1) {
					// Remove it by swapping the last element. This is O(1) as compared to O(N), but changes the order
					v.point.data[j] = v.point.removeTail();
					v.observations.data[j*2] = v.observations.getTail(1);
					v.observations.data[j*2 + 1] = v.observations.getTail(0);
					v.observations.size -= 2;
				}
			}
		}
	}

	/**
	 * Refines the scene and updates the graph.
	 *
	 * @param graph (Output) where the updated scene parameters are written to.
	 * @return true is successful or false is SBA failed
	 */
	protected boolean refineViews( SceneWorkingGraph graph ) {
//		if (verbose != null) bundleAdjustment.printCounts(verbose);

//		for (int viewIdx = 0; viewIdx < graph.listViews.size(); viewIdx++) {
//			if (verbose != null) {
//				SceneWorkingGraph.View wview = graph.listViews.get(viewIdx);
//				Se3_F64 m = bundleAdjustment.structure.getParentToView(viewIdx);
//				double theta = ConvertRotation3D_F64.matrixToRodrigues(m.R, null).theta;
//				verbose.printf("BEFORE SBA T=(%.2f %.2f %.2f) R=%.4f, f=%.1f k1=%.1e k2=%.1e\n",
//						m.T.x, m.T.y, m.T.z, theta,
//						wview.intrinsic.f, wview.intrinsic.k1, wview.intrinsic.k2);
//			}
//		}

		if (!metricSba.process())
			return false;

		final SceneStructureMetric structure = metricSba.structure;

		// save the results
		for (int cameraIdx = 0; cameraIdx < graph.listCameras.size(); cameraIdx++) {
			BundlePinholeSimplified found = (BundlePinholeSimplified)structure.cameras.get(cameraIdx).model;
			graph.listCameras.get(cameraIdx).intrinsic.setTo(found);
		}

		for (int viewIdx = 0; viewIdx < graph.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = graph.listViews.get(viewIdx);
			wview.world_to_view.setTo(structure.getParentToView(viewIdx));


			if (verbose != null && verboseViewInfo) {
				BundlePinholeSimplified intrinsics = graph.getViewCamera(wview).intrinsic;
				Se3_F64 m = metricSba.structure.getParentToView(viewIdx);
				double theta = ConvertRotation3D_F64.matrixToRodrigues(m.R, null).theta;
				verbose.printf("AFTER view='%s' T=(%.2f %.2f %.2f) R=%.4f, f=%.1f k1=%.1e k2=%.1e\n",
						wview.pview.id, m.T.x, m.T.y, m.T.z, theta,
						intrinsics.f, intrinsics.k1, intrinsics.k2);
			}
		}
		return true;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, metricSba);
	}

	/**
	 * Helper function that allows for arbitrary customization before it optimizes.
	 */
	@FunctionalInterface
	public interface CallBeforeRefine {
		void process( MetricBundleAdjustmentUtils utils );
	}

	/**
	 * Filter for inlier sets. if it returns true it should be processed
	 */
	@FunctionalInterface
	public interface FilterInlierSet {
		boolean keep( SceneWorkingGraph.View view, SceneWorkingGraph.InlierInfo info );
	}
}
