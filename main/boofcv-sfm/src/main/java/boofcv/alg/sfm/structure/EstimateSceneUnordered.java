/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.RefineEpipolar;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.robust.RansacMultiView;
import boofcv.alg.sfm.EstimateSceneStructure;
import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraMotion;
import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraView;
import boofcv.alg.sfm.structure.PairwiseImageGraph.Feature3D;
import boofcv.alg.sfm.structure.PairwiseImageGraph.ViewState;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.io.PrintStream;
import java.util.*;

/**
 * Assumes the input images are in an arbitrary order and that any image can be connected to any other image.
 * A brute force approach is used to determine connectivity between images making this an O(N<sup>2</sup>) algorithm.
 * This its not suitable for a large number of images.
 *
 * All cameras must either be calibrated or uncalibrated.
 *
 * Sensitivity to the selection of the origin view and the motion which defines the coordinate system is minimized
 * by estimating the 3D coordinate of points using local information alone. An alternative and common approach uses
 * information built up from sequential view poses and 3D point estimates which have unbounded error.
 *
 * @author Peter Abeles
 */
public class EstimateSceneUnordered<T extends ImageBase<T>> implements EstimateSceneStructure<T>
{
	// Used to pre-maturely stop the scene estimation process
	private volatile boolean stopRequested = false;

	private double TRIANGULATE_MIN_ANGLE = Math.PI/20.0;

	// Transform (including distortion terms) from pixel into normalized image coordinates
	Map<String,Point2Transform2_F64> camerasPixelToNorm = new HashMap<>();
	// Approximate camera model used to compute pixel errors
	Map<String,CameraPinhole> camerasIntrinsc = new HashMap<>();
	// camera name to index
	Map<String,Integer> cameraToIndex = new HashMap<>();

	// indicates if all the cameras are calibrated or uncalibrated
	boolean calibrated;

	PairwiseImageMatching<T> imageMatching;

	RansacMultiView<Se3_F64, Point2D3D> ransacPnP;

	RefineEpipolar refineEpipolar;

	// Triangulates the 3D coordinate of a point from two observations
	TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

	PairwiseImageGraph graph;

	// This are the views were actually added
	List<CameraView> viewsAdded = new ArrayList<>();

	// work space for feature angles
	private Vector3D_F64 arrowA = new Vector3D_F64();
	private Vector3D_F64 arrowB = new Vector3D_F64();

	// Output
	BundleAdjustmentSceneStructure structure;
	BundleAdjustmentObservations observations;

	// Verbose output to standard out
	PrintStream verbose;

	public EstimateSceneUnordered( PairwiseImageMatching<T> imageMatching ) {
		this.imageMatching = imageMatching;
	}

	public EstimateSceneUnordered( DetectDescribePoint<T, TupleDesc> detDesc ) {
		this( new PairwiseImageMatching<>(detDesc) );
	}

	EstimateSceneUnordered(){}

	@Override
	public void addCamera(String cameraName) {
		if( camerasPixelToNorm.isEmpty() )
			calibrated = false;
		else if( calibrated )
			throw new IllegalArgumentException("All cameras must be calibrated or uncalibrated");
		camerasPixelToNorm.put( cameraName , null );

		imageMatching.addCamera(cameraName);
		cameraToIndex.put(cameraName,cameraToIndex.size());
	}

	@Override
	public void addCamera(String cameraName, LensDistortionNarrowFOV intrinsic, int width, int height ) {
		if( camerasPixelToNorm.isEmpty() )
			calibrated = true;
		else if( !calibrated )
			throw new IllegalArgumentException("All cameras must be calibrated or uncalibrated");

		Point2Transform2_F64 pixelToNorm = intrinsic.undistort_F64(true,false);

		camerasPixelToNorm.put( cameraName, pixelToNorm );
		camerasIntrinsc.put(cameraName, PerspectiveOps.estimatePinhole(pixelToNorm,width,height));

		imageMatching.addCamera(cameraName,camerasPixelToNorm.get(cameraName),camerasIntrinsc.get(cameraName));
		cameraToIndex.put(cameraName,cameraToIndex.size());
	}

	/**
	 * Detect features inside the image and save the results
	 * @param image
	 */
	@Override
	public void add(T image , String cameraName )
	{
		imageMatching.addImage(image,cameraName);
	}

	@Override
	public boolean estimate() {
		declareModelFitting();

		if( !imageMatching.process(camerasPixelToNorm,camerasIntrinsc) )
			return false;
		
		graph = imageMatching.getGraph();

		// TODO how is triangulation angle handled for projective?
		for (int i = 0; i < graph.edges.size(); i++) {
			CameraMotion e = graph.edges.get(i);
			e.triangulationAngle = medianTriangulationAngle(e);
		}

		System.out.println("Selecting root");
		// Select the view which will act as the origin
		CameraView origin = selectOriginNode();
		// Select the motion which will define the coordinate system
		CameraMotion baseMotion = selectCoordinateBase( origin );

		System.out.println("Stereo triangulation");
		// Triangulate features in all motions which exceed a certain angle
		for (int i = 0; i < graph.edges.size() && !stopRequested ; i++) {
			CameraMotion e = graph.edges.get(i);
			if( e.triangulationAngle > Math.PI/10 || e == baseMotion) {
				System.out.println("   Edge "+i+" / "+graph.edges.size());
				if( e.metric ) {
					triangulateMetricStereoEdges(e);
				} else {
					// TODO handle uncalibrated case
//					triangulateProjectiveStereoEdges(e);
				}
			}
		}

		// TODO Upgrade all views to metric

		System.out.println("Defining the coordinate system");
		// Using the selecting coordinate frames and triangulated points define the coordinate system
		defineCoordinateSystem(origin, baseMotion);
		if( stopRequested )
			return false;

		System.out.println("Estimate all features");
		// Now estimate all the other view locations and 3D features
		estimateAllFeatures(origin, baseMotion.destination(origin));
		if( stopRequested )
			return false;

		// Convert the graph into the output format
		convertToOutput(origin);

		return viewsAdded.size() >= 2;
	}

	double medianTriangulationAngle( CameraMotion edge ) {

		GrowQueue_F64 angles = new GrowQueue_F64(edge.associated.size());
		angles.size = edge.associated.size();

		for (int i = 0; i < edge.associated.size(); i++) {
			AssociatedIndex a = edge.associated.get(i);
			Point2D_F64 normA = edge.viewSrc.observationNorm.get( a.src );
			Point2D_F64 normB = edge.viewDst.observationNorm.get( a.dst );

			double acute = triangulationAngle(normA,normB,edge.a_to_b);
			angles.data[i] = acute;
		}

		angles.sort();
		return angles.getFraction(0.5);
	}

	protected void declareModelFitting() {
		if( calibrated ) {
			ransacPnP = FactoryMultiViewRobust.pnpRansac(null,imageMatching.getConfigRansac());
		} else {
			// TODO figure out how to do  PnP in uncalibrated case
		}
		refineEpipolar = FactoryMultiView.refineFundamental(1e-32,10,EpipolarError.SAMPSON);
	}

	/**
	 * Converts the internal data structures into the output format for bundle adjustment. Camera models are omitted
	 * since they are not available
	 * @param origin The origin of the coordinate system
	 */
	private void convertToOutput( CameraView origin ) {
		structure = new BundleAdjustmentSceneStructure(false);
		observations = new BundleAdjustmentObservations(viewsAdded.size());

		structure.initialize(cameraToIndex.size(),viewsAdded.size(), graph.features3D.size());

		// look up table from old index to new index
		int viewOldToView[] = new int[ graph.nodes.size() ];
		Arrays.fill(viewOldToView,-1);
		for (int i = 0; i < viewsAdded.size(); i++) {
			viewOldToView[ graph.nodes.indexOf(viewsAdded.get(i))] = i;
		}

		for( int i = 0; i < viewsAdded.size(); i++ ) {
			CameraView v = viewsAdded.get(i);
			int cameraIndex = cameraToIndex.get(v.camera.camera);
			structure.setView(i,v==origin,v.viewToWorld.invert(null));
			structure.connectViewToCamera(i,cameraIndex);
		}

		for (int indexPoint = 0; indexPoint < graph.features3D.size(); indexPoint++) {
			Feature3D f = graph.features3D.get(indexPoint);

			structure.setPoint(indexPoint,f.worldPt.x,f.worldPt.y,f.worldPt.z);

			if( f.views.size() != f.obsIdx.size )
				throw new RuntimeException("BUG!");

			for (int j = 0; j < f.views.size(); j++) {
				CameraView view = f.views.get(j);
				int viewIndex = viewOldToView[view.index];
				structure.connectPointToView(indexPoint,viewIndex);

				Point2D_F64 pixel = viewsAdded.get(viewIndex).observationPixels.get(f.obsIdx.get(j));
				observations.getView(viewIndex).add(indexPoint,(float)(pixel.x),(float)(pixel.y));
			}
		}

	}

	/**
	 * Adds features which were triangulated using the stereo pair after the scale factor has been determined.
	 * Don't mark the other view as being processed. It's 3D pose will be estimated later on using PNP with the
	 * new features and features determined later on
	 */
	void addTriangulatedStereoFeatures( CameraView base , CameraMotion edge , double scale ) {
		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		boolean baseIsA = base == viewA;
		CameraView other = baseIsA ? viewB : viewA;

		// Determine transform from other to world
		edge.a_to_b.T.scale(scale);
		Se3_F64 otherToBase = baseIsA ? edge.a_to_b.invert(null) : edge.a_to_b.copy();
		otherToBase.concat(base.viewToWorld, other.viewToWorld);

		// Convert already computed stereo 3D features and turn them into real features
		for (int i = 0; i < edge.stereoTriangulations.size(); i++) {
			Feature3D edge3D = edge.stereoTriangulations.get(i);

			int indexSrc = edge3D.obsIdx.get(0);
			int indexDst = edge3D.obsIdx.get(1);

			Feature3D world3D = baseIsA ? viewA.features3D[indexSrc] : viewB.features3D[indexDst];

			// find the 3D location of the point in world frame
			edge3D.worldPt.scale(scale);
			if( baseIsA ) {
				viewA.viewToWorld.transform(edge3D.worldPt, edge3D.worldPt);
			} else {
				edge.a_to_b.transform(edge3D.worldPt, edge3D.worldPt);
				viewB.viewToWorld.transform(edge3D.worldPt, edge3D.worldPt);
			}

			// See if the feature is already known
			if( world3D != null ) {
				// Add the other view if another feature in the other view was not already associated with this feature
				if( !world3D.views.contains(other) ) {
					world3D.views.add(other);
					world3D.obsIdx.add( baseIsA ? indexDst : indexSrc );
				}

				// Retriangulate the point if it appears that this stereo pair is better than the one which originally
				// computed it
				if( world3D.triangulationAngle >= edge3D.triangulationAngle ) {
					continue;
				}
				world3D.worldPt.set(edge3D.worldPt);
				world3D.triangulationAngle = edge3D.triangulationAngle;
				other.features3D[baseIsA ? indexDst : indexSrc] = edge3D;
			} else {
				graph.features3D.add(edge3D);
				viewA.features3D[indexSrc] = edge3D;
				viewB.features3D[indexDst] = edge3D;
			}
		}

		// free memory
		edge.stereoTriangulations = new ArrayList<>();
	}

	/**
	 * Determine scale factor difference between edge triangulation and world
	 */
	static double determineScale( CameraView base , CameraMotion edge )
		throws Exception
	{
		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		boolean baseIsA = base == viewA;

		// determine the scale factor difference
		Point3D_F64 worldInBase3D = new Point3D_F64();
		Point3D_F64 localInBase3D = new Point3D_F64();
		GrowQueue_F64 scales = new GrowQueue_F64();
		for (int i = 0; i < edge.stereoTriangulations.size(); i++) {
			// get the feature as triangulated in this edge.
			Feature3D edge3D = edge.stereoTriangulations.get(i);

			int indexSrc = edge3D.obsIdx.get(0);
			int indexDst = edge3D.obsIdx.get(1);

			Feature3D world3D = baseIsA ? viewA.features3D[indexSrc] : viewB.features3D[indexDst];
			if( world3D == null )
				continue;
			// Find the world point in the local coordinate system
			SePointOps_F64.transformReverse(base.viewToWorld,world3D.worldPt,worldInBase3D);

			// put this point into the base frame
			if( !baseIsA ) {
				SePointOps_F64.transform(edge.a_to_b,edge3D.worldPt,localInBase3D);
			} else {
				localInBase3D.set(edge3D.worldPt);
			}

			scales.add(worldInBase3D.z / localInBase3D.z);
		}

		if( scales.size < 20 ) {
			throw new Exception("Not enough matches with known points");
		}
		// Get the scale offset as the median value to make it robust to noise
		scales.sort();
		return scales.getFraction(0.5);
	}

	/**
	 * Perform a breath first search to find the structure of all the remaining camrea views
	 */
	private void estimateAllFeatures(CameraView seedA, CameraView seedB ) {
		List<CameraView> open = new ArrayList<>();

		// Add features for all the other views connected to the root view and determine the translation scale factor
		addUnvistedToStack(seedA, open);
		addUnvistedToStack(seedB, open);

		// Do a breath first search. The queue is first in first out
		while( !open.isEmpty() ) {
			if( stopRequested )
				return;
			if( verbose != null )
				verbose.println("### open.size="+open.size());

			// select the view with the 3D features. This view can be estimated which the highest degree of confience
			int bestCount = countFeaturesWith3D(open.get(0));
			int bestIndex = 0;

			for (int i = 1; i < open.size(); i++) {
				int count = countFeaturesWith3D(open.get(i));
				if( count > bestCount ) {
					bestCount = count;
					bestIndex = i;
				}
			}

			CameraView v = open.remove(bestIndex);
			if( verbose != null )
				verbose.println("   processing view="+v.index+" | 3D Features="+bestCount);

			// Determine the view's location in the 3D view. This might have been previously estimated using
			// stereo and the estimated scale factor. That will be ignored and the new estimate used instead
			if( !determinePose(v) ) {
				// The pose could not be determined, so remove it from the graph
				if( verbose != null )
					verbose.println("   Removing connection");
				for (CameraMotion m : v.connections) {
					CameraView a = m.destination(v);
					a.connections.remove(m);
					graph.edges.remove(m);
				}
				graph.nodes.remove(v);

				for (int i = 0; i < graph.nodes.size(); i++) {
					graph.nodes.get(i).index = i;
				}

//				throw new RuntimeException("Crap handle this");
			} else {
				// If possible use triangulation from stereo
				addTriangulatedFeaturesForAllEdges(v);
				triangulateNoLocation(v);

				viewsAdded.add(v);

				// Update the open list
				addUnvistedToStack(v, open);
			}
		}
	}

	private void addTriangulatedFeaturesForAllEdges(CameraView v) {
		for (int i = 0; i < v.connections.size(); i++) {
			CameraMotion e = v.connections.get(i);
			if( !e.stereoTriangulations.isEmpty() ) {
				try {
					double scale = determineScale(v, e);
					addTriangulatedStereoFeatures(v, e, scale);
				} catch (Exception ignore) {}
			}
		}
	}

	/**
	 * Count how many 3D features are in view.
	 */
	private int countFeaturesWith3D(CameraView v ) {

		int count = 0;

		for (int i = 0; i < v.connections.size(); i++) {
			CameraMotion m = v.connections.get(i);

			boolean isSrc = m.viewSrc == v;

			for (int j = 0; j < m.associated.size(); j++) {
				AssociatedIndex a = m.associated.get(j);

				if( isSrc ) {
					count += m.viewDst.features3D[a.dst] != null ? 1 : 0;
				} else {
					count += m.viewSrc.features3D[a.src] != null ? 1 : 0;
				}
			}
		}

		return count;
	}

	/**
	 * Uses the previously found motion between the two cameras to estimate the scale and 3D point of common features.
	 * If a feature already has a known 3D point that is not modified. Scale is found by computing the 3D coordinate
	 * of all points with a 3D point again then dividing the two distances. New features are also triangulated
	 * and have their location's update using this scale.
	 *
	 * A known feature has the current view added to its list of views.
	 */
	private boolean determinePose(CameraView target ) {

		// Find all Features which are visible in this view and have a known 3D location
		List<Point2D3D> list = new ArrayList<>();
		List<Feature3D> features = new ArrayList<>();
		GrowQueue_I32 featureIndexes = new GrowQueue_I32();


		// TODO mark need to handle casees where the target's index has changed due to node removal
		// Find all the known 3D features which are visible in this view
		for( CameraMotion c : target.connections ) {
			boolean isSrc = c.viewSrc == target;
			CameraView other = c.destination(target);
			if( other.state != ViewState.PROCESSED )
				continue;

			for (int i = 0; i < c.associated.size(); i++) {
				AssociatedIndex a = c.associated.get(i);

				Feature3D f = other.features3D[isSrc?a.dst:a.src];
				if( f == null || f.mark == target.index)
					continue;
				f.mark = target.index;
				features.add(f);
				featureIndexes.add( isSrc?a.src:a.dst);
				Point2D_F64 norm = target.observationNorm.get( isSrc?a.src:a.dst);

				Point2D3D p = new Point2D3D();
				p.location.set(f.worldPt);
				p.observation.set(norm);
				list.add(p);
			}
		}

		// Estimate the target's location using robust PNP
		ransacPnP.setIntrinsic(0,target.camera.pinhole);
		if( list.size() < 20 || !ransacPnP.process(list) ) {
			if( verbose != null )
				verbose.println("   View="+target.index+" RANSAC failed. list.size="+list.size());
			return false;
		}

		target.state = ViewState.PROCESSED;

		// add inliers to the features
		int N = ransacPnP.getMatchSet().size();
		if( verbose != null )
			verbose.println("   View="+target.index+" PNP RANSAC "+N+"/"+list.size());
		for (int i = 0; i < N; i++) {
			int which = ransacPnP.getInputIndex(i);
			Feature3D f = features.get(which);
			if( f.views.contains(target))
				continue;
			f.views.add(target);
			f.obsIdx.add(featureIndexes.get(which));
			target.features3D[featureIndexes.get(which)] = f;

			if( f.views.size() != f.obsIdx.size )
				throw new RuntimeException("BUG!");
		}

		Se3_F64 worldToView = ransacPnP.getModelParameters();
		target.viewToWorld.set( worldToView.invert(null) );

		return true;
	}

	private void triangulateNoLocation( CameraView target ) {

		TriangulateTwoViewsCalibrated triangulator = FactoryMultiView.triangulateTwoGeometric();

		Se3_F64 otherToTarget = new Se3_F64();

		Se3_F64 worldToTarget = target.viewToWorld.invert(null);

		for( CameraMotion c : target.connections ) {
			boolean isSrc = c.viewSrc == target;
			CameraView other = c.destination(target);
			if( other.state != ViewState.PROCESSED )
				continue;

			other.viewToWorld.concat(worldToTarget,otherToTarget);

			for (int i = 0; i < c.associated.size(); i++) {
				AssociatedIndex a = c.associated.get(i);

				int indexTarget = isSrc ? a.src : a.dst;
				int indexOther = isSrc ? a.dst : a.src;
				if( target.features3D[indexTarget] != null || other.features3D[indexOther] != null )
					continue;

				Point2D_F64 normOther = other.observationNorm.get( indexOther );
				Point2D_F64 normTarget = target.observationNorm.get( indexTarget );

				// Skip points with poor geometry
				double angle = triangulationAngle(normOther,normTarget,otherToTarget);
				if( angle < TRIANGULATE_MIN_ANGLE )
					continue;

				Feature3D f = new Feature3D();
				if( !triangulator.triangulate(normOther,normTarget,otherToTarget,f.worldPt))
					continue;

				other.viewToWorld.transform(f.worldPt,f.worldPt);
				f.views.add( target );
				f.views.add( other );
				f.obsIdx.add( indexTarget );
				f.obsIdx.add( indexOther );

				graph.features3D.add(f);
				target.features3D[indexTarget] = f;
				other.features3D[indexOther] = f;
			}
		}
	}

	/**
	 * Computes the acture angle between two vectors. Larger this angle is the better the triangulation
	 * of the features 3D location is in general
	 */
	private double triangulationAngle( Point2D_F64 normA , Point2D_F64 normB , Se3_F64 a_to_b ) {
		// the more parallel a line is worse the triangulation. Get rid of bad ideas early here
		arrowA.set(normA.x,normA.y,1);
		arrowB.set(normB.x,normB.y,1);
		GeometryMath_F64.mult(a_to_b.R,arrowA,arrowA); // put them into the same reference frame

		return UtilVector3D_F64.acute(arrowA,arrowB);
	}

	/**
	 * Looks to see which connections have yet to be visited and adds them to the open list
	 */
	private void addUnvistedToStack(CameraView viewed, List<CameraView> open) {
		for (int i = 0; i < viewed.connections.size(); i++) {
			CameraView other = viewed.connections.get(i).destination(viewed);
			if( other.state == ViewState.UNPROCESSED) {
				other.state = ViewState.PENDING;
				open.add(other);
				if( verbose != null )
					verbose.println("  adding to open "+viewed.index+"->"+other.index);
			}
		}
	}

	/**
	 * Sets the origin and scale of the coordinate system
	 *
	 * @param viewA The origin of the coordinate system
	 * @param motion Motion which will define the coordinate system's scale
	 */
	private void defineCoordinateSystem(CameraView viewA, CameraMotion motion) {

		CameraView viewB = motion.destination(viewA);
		viewA.viewToWorld.reset(); // identity since it's the origin
		viewB.viewToWorld.set(motion.motionSrcToDst(viewB));
		// translation is only known up to a scale factor so pick a reasonable scale factor
		double scale = viewB.viewToWorld.T.norm();
		viewB.viewToWorld.T.scale(1.0/scale);

		viewsAdded.add(viewA);
		viewsAdded.add(viewB);

		viewA.state = ViewState.PROCESSED;
		viewB.state = ViewState.PROCESSED;

		// Take the already triangulated points and turn them into official 3D features
		boolean originIsDst = viewA == motion.viewDst;
		for (int i = 0; i < motion.stereoTriangulations.size(); i++) {
			Feature3D f = motion.stereoTriangulations.get(i);

			int indexSrc = f.obsIdx.get(0);
			int indexDst = f.obsIdx.get(1);

			motion.viewSrc.features3D[indexSrc] = f;
			motion.viewDst.features3D[indexDst] = f;

			if( originIsDst ) {
				SePointOps_F64.transform(motion.a_to_b,f.worldPt,f.worldPt);
			}

			f.worldPt.scale(1.0/scale);
			graph.features3D.add(f);
		}

		// free memory and mark as already processed
		motion.stereoTriangulations = new ArrayList<>();

		// All features which can be added using triangulation should now be added
		addTriangulatedFeaturesForAllEdges(viewA);
		addTriangulatedFeaturesForAllEdges(viewB);

		System.out.println("root  = "+viewA.index);
		System.out.println("other = "+viewB.index);
		System.out.println("-------------");

	}

	/**
	 * Select the view which will be coordinate system's origin. This should be a well connected node which have
	 * favorable geometry to the other views it's connected to.
	 * @return The selcted view
	 */
	CameraView selectOriginNode() {
		double bestScore = 0;
		CameraView best = null;

		if( verbose != null )
			verbose.println("selectOriginNode");
		for (int i = 0; i < graph.nodes.size(); i++) {
			double score = 0;
			List<CameraMotion> edges = graph.nodes.get(i).connections;

			for (int j = 0; j < edges.size(); j++) {
				CameraMotion e = edges.get(j);
				score += e.scoreTriangulation();
			}

			if( score > bestScore ) {
				bestScore = score;
				best = graph.nodes.get(i);
			}

			if( verbose != null )
				verbose.printf("  [%2d] score = %s\n",i,score);
		}
		if( verbose != null )
			verbose.println("     selected = "+best.index);

		return best;
	}

	/**
	 * Select motion which will define the coordinate system.
	 */
	CameraMotion selectCoordinateBase(CameraView view ) {
		double bestScore = 0;
		CameraMotion best = null;

		if( verbose != null )
			verbose.println("selectCoordinateBase");
		for (int i = 0; i < view.connections.size(); i++) {
			CameraMotion e = view.connections.get(i);

			double s = e.scoreTriangulation();
			if( verbose != null )
				verbose.printf("  [%2d] score = %s\n",i,s);
			if( s > bestScore ) {
				bestScore = s;
				best = e;
			}
		}
		return best;
	}

	/**
	 * An edge has been declared as defining a good stereo pair. All associated feature will now be
	 * triangulated. It is assumed that there is no global coordinate system at this point.
	 */
	private void triangulateMetricStereoEdges(CameraMotion edge ) {
		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		for (int i = 0; i < edge.associated.size(); i++) {
			AssociatedIndex f = edge.associated.get(i);

			Point2D_F64 normA = viewA.observationNorm.get(f.src);
			Point2D_F64 normB = viewB.observationNorm.get(f.dst);

			Feature3D feature3D = new Feature3D();

			double angle = triangulationAngle(normA,normB,edge.a_to_b);
			if( angle < TRIANGULATE_MIN_ANGLE )
				continue;

			if( !triangulate.triangulate(normA,normB,edge.a_to_b,feature3D.worldPt) ) {
				System.out.println("  triangulation failed??!");
				continue;
			}

			feature3D.views.add(viewA);
			feature3D.views.add(viewB);
			feature3D.obsIdx.add(f.src);
			feature3D.obsIdx.add(f.dst);
			feature3D.triangulationAngle = angle;

			edge.stereoTriangulations.add(feature3D);
		}
	}

	@Override
	public BundleAdjustmentSceneStructure getSceneStructure() {
		return structure;
	}

	@Override
	public BundleAdjustmentObservations getObservations() {
		return observations;
	}

	@Override
	public void reset() {
		stopRequested = false;
		imageMatching.reset();
	}

	@Override
	public void requestStop() {
		imageMatching.requestStop();
		stopRequested = true;
	}

	@Override
	public boolean isStopRequested() {
		return stopRequested;
	}

	public void setVerbose(PrintStream verbose) {
		this.verbose = verbose;
		imageMatching.setVerbose(verbose);
	}

	public PairwiseImageMatching<T> getImageMatching() {
		return imageMatching;
	}
}
