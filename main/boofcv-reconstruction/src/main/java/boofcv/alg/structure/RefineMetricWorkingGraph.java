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
import boofcv.struct.image.ImageDimension;
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

import static boofcv.misc.BoofMiscOps.checkTrue;

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
	public final MetricBundleAdjustmentUtils bundleAdjustment;

	// pixels to undistorted normalized image coordinates and the erverse
	protected final List<Point2Transform2_F64> listPixelToNorm = new ArrayList<>();
	protected final List<Point2Transform2_F64> listNormToPixel = new ArrayList<>();

	// Storage for the index/ID of a particular view
	TObjectIntHashMap<String> viewToIntegerID = new TObjectIntHashMap<>();

	private PrintStream verbose;

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
	final DogArray_I32 viewIntIds = new DogArray_I32();
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

	public RefineMetricWorkingGraph( MetricBundleAdjustmentUtils bundleAdjustment ) {
		this.bundleAdjustment = bundleAdjustment;
	}

	public RefineMetricWorkingGraph() {
		this(new MetricBundleAdjustmentUtils());
	}

	/**
	 * Use the `graph` to define a 3D scene which can be optimized.
	 *
	 * @param db (Input) Used to lookup common features between views.
	 * @param graph (Input, Output) Describes scene and provides initial estimate for parameters. Updated with refined
	 * parameters on output.
	 */
	public boolean process( LookUpSimilarImages db, SceneWorkingGraph graph ) {
		// Pre-declare and compute basic data structures
		initializeDataStructures(db, graph);
		// Use observations defined in the graph to create the list of 3D features which will be optimized
		createFeatures3D(graph);
		// Clean up by removing observations which were never assigned to a feature
		pruneUnassignedObservations();

		return refineViews(graph);
	}

	/**
	 * Initialized several data structures and resets it into the initial state
	 */
	void initializeDataStructures( LookUpSimilarImages db, SceneWorkingGraph graph ) {
		viewToIntegerID.clear();
		listPixelToNorm.clear();
		listNormToPixel.clear();

		final SceneStructureMetric structure = bundleAdjustment.structure;
		final SceneObservations observations = bundleAdjustment.observations;

		// Initialize the structure, but save initializing the points for later
		structure.initialize(graph.workingViews.size(), graph.workingViews.size(), 0);

		// Declare enough space for each actual observation. This will make keeping track of which observations have
		// features associated with them easier
		observations.initialize(graph.workingViews.size());
		for (int viewIdx = 0; viewIdx < graph.workingViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = graph.workingViews.get(viewIdx);
			SceneObservations.View oview = observations.getView(viewIdx);

			viewToIntegerID.put(wview.pview.id, viewIdx);
			createProjectionModel(wview.intrinsic);

			oview.resize(wview.pview.totalObservations);
			db.lookupPixelFeats(wview.pview.id, pixels);
			BoofMiscOps.checkEq(pixels.size, wview.pview.totalObservations);

			// The camera model assumes the principle point is (0,0) and this is done by assuming it's the image center
			ImageDimension dimension = wview.imageDimension;
			checkTrue(dimension.width > 0,
					"You must assign width and height so that pixels can be re-centered");
			float cx = (float)(dimension.width/2);
			float cy = (float)(dimension.height/2);

			// specify the observation pixel coordinates but not which 3D feature is matched to the observation
			for (int obsIdx = 0; obsIdx < pixels.size; obsIdx++) {
				Point2D_F64 p = pixels.get(obsIdx);
				oview.setPixel(obsIdx, (float)(p.x - cx), (float)(p.y - cy));
			}

			// Add the view pose and intrinsics
			structure.setCamera(viewIdx, false, wview.intrinsic);
			structure.setView(viewIdx, viewIdx, viewIdx == 0, wview.world_to_view);
		}
	}

	/**
	 * Computes the 3D features by triangulating inliers
	 */
	void createFeatures3D( SceneWorkingGraph graph ) {
		// For each view, with a set inliers, create a set of triangulated 3D point features
		for (int workingIdx = 0; workingIdx < graph.workingViews.size(); workingIdx++) {
			SceneWorkingGraph.View wview = graph.workingViews.get(workingIdx);
			if (wview.inliers.isEmpty())
				continue;

			final SceneWorkingGraph.InlierInfo inliers = wview.inliers;
			final FastArray<PairwiseImageGraph.View> inlierViews = inliers.views;

			// Initialize data structures for this particular set of inlier observations
			initLookUpTablesForInlierSet(graph, inlierViews);

			int numInliers = wview.inliers.getInlierCount();

			if (verbose != null) verbose.println("Inlier view='" + wview.pview.id + "' inliers=" + numInliers);

			int countMatched = 0;
			int countMixed = 0;
			int tooFew = 0;

			for (int inlierIdx = 0; inlierIdx < numInliers; inlierIdx++) {
				// Create a list of views which have not yet been assigned an observation and also create a list
				// of 3D features which have been assigned an observation from this set
				findUnassignedObsAndKnown3D(inliers, inlierIdx);

				if (unassigned.size != 0 && featureIdx3D.size != 0) {
					countMixed++;
				} else if (featureIdx3D.size != 0) {
					countMatched++;
				}

				// For each observations which is unassigned, assign it to the 3D feature which has the smallest
				// reprojection error
				if (featureIdx3D.size > 0)
					assignKnown3DToUnassignedObs(graph, inliers, inlierIdx);

				// If there is 2 or more unassigned observations remaining triangulate and create a new 3D feature
				if (unassigned.size < 2) {
					tooFew++;
					continue;
				}

				// Create a new feature and save the unassigned observations to it
				triangulateAndSave(inliers, inlierIdx);

				// NOTE: it is possible that 2+ features are created for one physical feature with this greedy approach
			}

			if (verbose != null)
				verbose.println("   unmatched=" + (numInliers - countMatched) + "  matched=" + countMatched + " mixed=" +
						countMixed + " tooFew=" + tooFew);
		}
	}

	/**
	 * Creates a look up table to go from a view's inlier index to it's int ID and SE3
	 */
	void initLookUpTablesForInlierSet( SceneWorkingGraph graph, FastArray<PairwiseImageGraph.View> inlierViews ) {
		pixelNormalized.resize(inlierViews.size);
		viewIntIds.reset();
		listPoses.resize(inlierViews.size);

		// Origin of local coordinate system
		Se3_F64 world_to_view0 = graph.views.get(inlierViews.get(0).id).world_to_view;
		world_to_view0.invert(view0_to_world);

		for (int i = 0; i < inlierViews.size; i++) {
			String viewID = inlierViews.get(i).id;
			// create a list of the array indexes of all the views included in this inlier set
			viewIntIds.add(viewToIntegerID.get(viewID));
			// also create a list of view locations for triangulation
			view0_to_world.concat(graph.views.get(viewID).world_to_view, listPoses.get(i));
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
		final SceneStructureMetric structure = bundleAdjustment.structure;
		final SceneObservations observations = bundleAdjustment.observations;

		// Go through all the views/observations which have yet to be assigned a 3D feature
		for (int unassignedIdx = unassigned.size - 1; unassignedIdx >= 0; unassignedIdx--) {
			int whichViewInliers = unassigned.get(unassignedIdx);
			int whichViewID = viewIntIds.get(whichViewInliers);
			// Lookup the pixel observation in the view
			int viewObsIdx = inliers.observations.get(whichViewInliers).get(inlierIdx);
			observations.getView(whichViewID).getPixel(viewObsIdx, pixelObserved);

			// look up scene information for this view
			SceneWorkingGraph.View wview = graph.workingViews.get(whichViewID);
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

			// assign this feature to this observation
			observations.getView(whichViewID).safeAssignToFeature(viewObsIdx, bestId);
			structure.connectPointToView(bestId, whichViewID);
			// Remove it since it has been assigned. This is also why we iterate in reverse
			unassigned.remove(unassignedIdx);
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
	 */
	void findUnassignedObsAndKnown3D( SceneWorkingGraph.InlierInfo inliers, int inlierIdx ) {
		unassigned.reset();
		featureIdx3D.reset();

		for (int inlierViewIdx = 0; inlierViewIdx < viewIntIds.size; inlierViewIdx++) {
			// See if this observation in this view has been assigned a 3D feature yet
			int obsIdx = inliers.observations.get(inlierViewIdx).get(inlierIdx);
			int featIdx = bundleAdjustment.observations.views.get(viewIntIds.get(inlierViewIdx)).getPointId(obsIdx);
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
	void triangulateAndSave( SceneWorkingGraph.InlierInfo inliers, int inlierIdx ) {
		final SceneStructureMetric structure = bundleAdjustment.structure;
		final SceneObservations observations = bundleAdjustment.observations;
		final TriangulateNViewsMetricH triangulator = bundleAdjustment.triangulator;

		// Get a list of observations in normalized image coordinates
		for (int inlierViewIdx = 0; inlierViewIdx < viewIntIds.size; inlierViewIdx++) {
			int viewID = viewIntIds.get(inlierViewIdx);
			int obsIdx = inliers.observations.get(inlierViewIdx).get(inlierIdx);
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
			int obsIdx = inliers.observations.get(inlierViewIdx).get(inlierIdx);
			int viewID = viewIntIds.get(inlierViewIdx);
			observations.getView(viewID).point.set(obsIdx, pointID);
			point3D.views.add(viewID);
		}
	}

	/**
	 * Prunes unassigned observations from each view. This is done by swapping which is fast but does change the order
	 */
	void pruneUnassignedObservations() {
		final SceneObservations observations = bundleAdjustment.observations;

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
		if (verbose != null) bundleAdjustment.printCounts(verbose);
		if (!bundleAdjustment.process(verbose))
			return false;

		final SceneStructureMetric structure = bundleAdjustment.structure;

		// save the results
		for (int viewIdx = 0; viewIdx < graph.workingViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = graph.workingViews.get(viewIdx);
			wview.world_to_view.setTo(structure.getParentToView(viewIdx));
			wview.intrinsic.setTo((BundlePinholeSimplified)structure.cameras.get(viewIdx).model);
		}
		return true;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
