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

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.TwoViewToCalibratingHomography;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Expands a metric {@link SceneWorkingGraph scene} by one view (the taget) using the geometric relationship between
 * the target and two known metric views.
 *
 * <ol>
 *     <li>Input: A seed view and the known graph</li>
 *     <li>Selects two other views with known camera matrices</li>
 *     <li>Finds features in common with all three views</li>
 *     <li>Trifocal tensor and RANSAC to find the unknown seed camera matrix</li>
 *     <li>Estimate calibrating homography from found projective scene and known metric values</li>
 *     <li>Elevate target from projective to metric</li>
 *     <li>Bundle Adjustment to refine estimate</li>
 *     <li>Convert from local coordinates to world / scene coordinates</li>
 *     <li>Add found metric view for target to scene</li>
 * </ol>
 *
 * <p>The initial projective scene is found independently using common observations in an attempt to reduce the
 * influence of past mistakes. To mitigate past mistakes, the intrisic parameters for the known views are
 * optimized inside of bundle adjustment even though they are "known". Only the found intrinsic and Se3 for
 * the target view will be added/modified in the scene graph.</p>
 *
 * @author Peter Abeles
 */
public class MetricExpandByOneView extends ExpandByOneView {

	// Finds the calibrating homography when metric parameters are known for two views
	protected TwoViewToCalibratingHomography projectiveHomography = new TwoViewToCalibratingHomography();

	/** Bundle Adjustment functions and configurations */
	public final MetricBundleAdjustmentUtils bundleAdjustment = new MetricBundleAdjustmentUtils();

	// Used for triangulation
	protected final List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(3, Point2D_F64::new);
	protected final List<Se3_F64> listMotion = new ArrayList<>();
	protected final RemoveBrownPtoN_F64 normalize1 = new RemoveBrownPtoN_F64();
	protected final RemoveBrownPtoN_F64 normalize2 = new RemoveBrownPtoN_F64();
	protected final RemoveBrownPtoN_F64 normalize3 = new RemoveBrownPtoN_F64();

	//------------------------- Local work space

	// Storage fort he two selected connections with known cameras
	List<Motion> connections = new ArrayList<>();

	// Fundamental matrix between view-2 and view-3 in triplet
	DMatrixRMaj F21 = new DMatrixRMaj(3, 3);
	// Storage for intrinsic camera matrices in view-2 and view-3
	DMatrixRMaj K1 = new DMatrixRMaj(3, 3);
	DMatrixRMaj K2 = new DMatrixRMaj(3, 3);
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair::new);

	// Found Se3 from view-1 to target
	Se3_F64 view1_to_view1 = new Se3_F64();
	Se3_F64 view1_to_view2 = new Se3_F64();
	Se3_F64 view1_to_target = new Se3_F64();
	Se3_F64 view1_to_view2H = new Se3_F64(); // found with calibrating homography
	// K calibration matrix for target view
	DMatrixRMaj K_target = new DMatrixRMaj(3, 3);

	public MetricExpandByOneView() {
		listMotion.add(view1_to_view1);
		listMotion.add(view1_to_view2);
		listMotion.add(view1_to_target);
//		bundleAdjustment.keepFraction = 0.95; <-- this made it worse by a lot?!
	}

	/**
	 * Attempts to estimate the camera model in the global projective space for the specified view
	 *
	 * @param db (Input) image data base
	 * @param workGraph (Input/Output) scene graph. On input it will have the known scene and if successful the metric
	 * information for the target view.
	 * @param target (Input) The view that needs its projective camera estimated and the graph is being expanded into
	 * @return true if successful target view has an estimated calibration matrix and pose, which have already been
	 * added to "workGraph"
	 */
	public boolean process( LookupSimilarImages db,
							SceneWorkingGraph workGraph,
							View target ) {
		assertBoof(!workGraph.isKnown(target), "Target shouldn't already be in the workGraph");
		this.workGraph = workGraph;
		this.utils.db = db;

		// Select two known connected Views
		if (!selectTwoConnections(target, connections)) {
			if (verbose != null) {
				verbose.println("Failed to expand because two connections couldn't be found. valid.size=" +
						validCandidates.size());
				for (int i = 0; i < validCandidates.size(); i++) {
					verbose.println("   valid view.id='" + validCandidates.get(i).other(target).id + "'");
				}
			}
			return false;
		}

		// Find features which are common between all three views
		utils.seed = connections.get(0).other(target);
		utils.viewB = connections.get(1).other(target);
		utils.viewC = target; // easier if target is viewC when doing metric elevation
		utils.createThreeViewLookUpTables();
		utils.findCommonFeatures();

		if (verbose != null) {
			verbose.println("Expanding to view='" + target.id + "' using views ( '" + utils.seed.id + "' , '" + utils.viewB.id +
					"') common=" + utils.commonIdx.size + " valid.size=" + validCandidates.size());
		}

		// Estimate trifocal tensor using three view observations
		utils.createTripleFromCommon();
		if (!utils.estimateProjectiveCamerasRobustly())
			return false;
		if (verbose != null) verbose.println("Trifocal RANSAC inliers.size=" + utils.inliersThreeView.size());

		// Using known camera information elevate to a metric scene
		if (!computeCalibratingHomography())
			return false;

		// Now that the metric upgrade is known add it to work graph
		SceneWorkingGraph.View wtarget = workGraph.addView(utils.viewC);
		db.lookupShape(wtarget.pview.id, wtarget.imageDimension);

		// Find the metric upgrade of the target
		upgradeToMetric(wtarget, projectiveHomography.getCalibrationHomography());

		// Refine using bundle adjustment, if configured to do so
		if (utils.configConvergeSBA.maxIterations > 0) {
			refineWithBundleAdjustment(workGraph);
		}

		// Match the local coordinate system's scale to the global coordinate system's scale
		int which = UtilPoint3D_F64.axisLargestAbs(view1_to_view2.T);
		double scale = view1_to_view2.T.getIdx(which)/view1_to_view2H.T.getIdx(which);
		view1_to_target.T.scale(scale);

		// Convert local coordinate into world coordinates for the view's pose
		Se3_F64 world_to_view1 = workGraph.views.get(utils.seed.id).world_to_view;
		world_to_view1.concat(view1_to_target, wtarget.world_to_view);

		if (verbose != null) {
			verbose.printf("Rescaled Local T={%.1f %.1f %.1f) scale=%f\n",
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z, scale);
			verbose.printf("Final Global   T={%.1f %.1f %.1f)\n",
					wtarget.world_to_view.T.x, wtarget.world_to_view.T.y, wtarget.world_to_view.T.z);
		}

		return true;
	}

	/**
	 * Use previously computed calibration homography to upgrade projective scene to metric
	 */
	private void upgradeToMetric( SceneWorkingGraph.View wview, DMatrixRMaj H_cal ) {
		// Compute metric upgrade from found projective camera matrices
		MultiViewOps.projectiveToMetric(utils.P2, H_cal, view1_to_view2H, K_target); // discard K_target
		MultiViewOps.projectiveToMetric(utils.P3, H_cal, view1_to_target, K_target);
		wview.intrinsic.set(K_target);

		// Normalize the scale so that it's close to 1.0
		double normTarget = view1_to_target.T.norm();
		view1_to_target.T.divide(normTarget);
		view1_to_view2H.T.divide(normTarget);

		// Let's use the "known" view1_to_view2, but to do that we will need to resolve the scale ambiguity
		SceneWorkingGraph.View wview1 = workGraph.lookupView(utils.seed.id);
		SceneWorkingGraph.View wview2 = workGraph.lookupView(utils.viewB.id);
		wview1.world_to_view.invert(null).concat(wview2.world_to_view, view1_to_view2);

		if (verbose != null) {
			// print the found view 1 to view 2 using local information only
			verbose.printf("L View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
		}

		// Now set the global view-1 to view-2 at the local scale
		double scale = MultiViewOps.findScale(view1_to_view2.T, view1_to_view2H.T);
		view1_to_view2H.set(view1_to_view2);
		view1_to_view2H.T.scale(scale);

		if (verbose != null) {
			verbose.printf("G View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
			verbose.printf("Initial fx=%6.1f k1=%6.3f k2=%6.3f T={%.1f %.1f %.1f)\n",
					wview.intrinsic.f, wview.intrinsic.k1, wview.intrinsic.k2,
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z);
		}
	}

	/**
	 * Optimize the three view metric local scene using SBA
	 */
	private void refineWithBundleAdjustment( SceneWorkingGraph workGraph ) {
		final SceneStructureMetric structure = bundleAdjustment.structure;
		final SceneObservations observations = bundleAdjustment.observations;

		// Look up known information
		SceneWorkingGraph.View wview1 = workGraph.lookupView(utils.seed.id);
		SceneWorkingGraph.View wview2 = workGraph.lookupView(utils.viewB.id);
		SceneWorkingGraph.View wview3 = workGraph.lookupView(utils.viewC.id);

		// configure camera pose and intrinsics
		List<AssociatedTriple> triples = utils.inliersThreeView;
		final int numFeatures = triples.size();
		structure.initialize(3, 3, numFeatures);
		observations.initialize(3);

		observations.getView(0).resize(numFeatures);
		observations.getView(1).resize(numFeatures);
		observations.getView(2).resize(numFeatures);

		// Camera parameters for previous views are fixed. If not a copy should be passed in since it will be modified
		// Argument for fixing: Reduce drift and past information can make up for sparse or poor observations in
		// the current triplet
		// Argument against: Past mistakes are propagated and go from bad to total failure
		structure.setCamera(0, true, wview1.intrinsic);
		structure.setCamera(1, true, wview2.intrinsic);
		structure.setCamera(2, false, wview3.intrinsic);
		structure.setView(0, 0, true, view1_to_view1);
		structure.setView(1, 1, false, view1_to_view2H);
		structure.setView(2, 2, false, view1_to_target);

		// Add observations and 3D feature locations
		normalize1.setK(wview1.intrinsic.f, wview1.intrinsic.f, 0, 0, 0).setDistortion(wview1.intrinsic.k1, wview1.intrinsic.k2);
		normalize2.setK(wview2.intrinsic.f, wview2.intrinsic.f, 0, 0, 0).setDistortion(wview2.intrinsic.k1, wview2.intrinsic.k2);
		normalize3.setK(wview3.intrinsic.f, wview3.intrinsic.f, 0, 0, 0).setDistortion(wview3.intrinsic.k1, wview3.intrinsic.k2);

		SceneObservations.View viewObs1 = observations.getView(0);
		SceneObservations.View viewObs2 = observations.getView(1);
		SceneObservations.View viewObs3 = observations.getView(2);

		final TriangulateNViewsMetricH triangulator = bundleAdjustment.triangulator;
		var foundX = new Point4D_F64();
		for (int featIdx = 0; featIdx < numFeatures; featIdx++) {
			AssociatedTriple a = triples.get(featIdx);
			viewObs1.set(featIdx, featIdx, (float)a.p1.x, (float)a.p1.y);
			viewObs2.set(featIdx, featIdx, (float)a.p2.x, (float)a.p2.y);
			viewObs3.set(featIdx, featIdx, (float)a.p3.x, (float)a.p3.y);

			normalize1.compute(a.p1.x, a.p1.y, pixelNorms.get(0));
			normalize2.compute(a.p2.x, a.p2.y, pixelNorms.get(1));
			normalize3.compute(a.p3.x, a.p3.y, pixelNorms.get(2));

			if (!triangulator.triangulate(pixelNorms, listMotion, foundX)) {
				throw new RuntimeException("Triangulation failed. Possibly bad input. Handle this problem");
			}

			if (structure.homogenous)
				structure.setPoint(featIdx, foundX.x, foundX.y, foundX.z, foundX.w);
			else
				structure.setPoint(featIdx, foundX.x/foundX.w, foundX.y/foundX.w, foundX.z/foundX.w);

			structure.connectPointToView(featIdx, 0);
			structure.connectPointToView(featIdx, 1);
			structure.connectPointToView(featIdx, 2);
		}

		// Refine using bundle adjustment
		if (!bundleAdjustment.process(null))
			throw new RuntimeException("Bundle adjustment failed. Handle this");

		// copy results for output
		wview3.intrinsic.set((BundlePinholeSimplified)structure.cameras.get(2).model);
		view1_to_view2H.set(structure.getParentToView(1));
		view1_to_target.set(structure.getParentToView(2));

		if (verbose != null) {
			verbose.printf("G View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
			verbose.printf("Refined fx=%6.1f k1=%6.3f k2=%6.3f T={%.1f %.1f %.1f)\n",
					wview3.intrinsic.f, wview3.intrinsic.k1, wview3.intrinsic.k2,
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z);
		}
	}

	/**
	 * Computes the transform needed to go from one projective space into another
	 */
	boolean computeCalibratingHomography() {

		// convert everything in to the correct data format
		MultiViewOps.projectiveToFundamental(utils.P2, F21);
		projectiveHomography.initialize(F21, utils.P2);

		workGraph.lookupView(utils.seed.id).intrinsic.convertTo(K1);
		workGraph.lookupView(utils.viewB.id).intrinsic.convertTo(K2);

		FastQueue<AssociatedTriple> triples = utils.matchesTriple;
		pairs.resize(triples.size());
		for (int idx = 0; idx < triples.size(); idx++) {
			AssociatedTriple a = triples.get(idx);
			pairs.get(idx).set(a.p1, a.p2);
		}

		return projectiveHomography.process(K1, K2, pairs.toList());
	}
}
