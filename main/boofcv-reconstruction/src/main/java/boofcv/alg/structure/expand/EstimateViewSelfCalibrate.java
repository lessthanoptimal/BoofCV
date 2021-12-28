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

package boofcv.alg.structure.expand;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.TwoViewToCalibratingHomography;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Target camera is unknown. Perform self calibration to estimate it from three views
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class EstimateViewSelfCalibrate implements VerbosePrint {

	/** If less than this number of features fail the physical constraint test, attempt to recover by removing them */
	public double fractionBadFeaturesRecover = 0.05;

	// Finds the calibrating homography when metric parameters are known for two views
	protected TwoViewToCalibratingHomography projectiveHomography = new TwoViewToCalibratingHomography();

	public final EstimateViewUtils estimateUtils = new EstimateViewUtils();

	// References to external data structures
	PairwiseGraphUtils pairwiseUtils;
	SceneWorkingGraph workGraph;

	//------------------------- Local work space

	// Fundamental matrix between view-2 and view-3 in triplet
	DMatrixRMaj F21 = new DMatrixRMaj(3, 3);
	// Storage for intrinsic camera matrices in view-2 and view-3
	DMatrixRMaj K1 = new DMatrixRMaj(3, 3);
	DMatrixRMaj K2 = new DMatrixRMaj(3, 3);
	DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

	// Found Se3 from view-1 to target
	Se3_F64 view1_to_view2H = new Se3_F64(); // found with calibrating homography
	// K calibration matrix for target view
	DMatrixRMaj K_target = new DMatrixRMaj(3, 3);
	BundlePinholeSimplified intrinsicTarget = new BundlePinholeSimplified();

	@Nullable PrintStream verbose;

	/**
	 * Estimates the pose and intrinsics of a new view.
	 *
	 * @param pairwiseUtils (Input) Pairwise information and specifies which view is to be estimated
	 * @param workGraph (Input) Information on the metric scene
	 * @param solution  (Output) Parameters for the new view and its inlier set
	 * @return true if successful and solution was found
	 */
	public boolean process( PairwiseGraphUtils pairwiseUtils,
							SceneWorkingGraph workGraph, MetricExpandByOneView.Solution solution ) {
		this.pairwiseUtils = pairwiseUtils;
		this.workGraph = workGraph;
		solution.reset();

		estimateUtils.initialize(false, workGraph, pairwiseUtils);

		// Using known camera information elevate to a metric scene
		if (!computeCalibratingHomography())
			return false;

		// Find the metric upgrade of the target
		if (!upgradeToMetric(projectiveHomography.getCalibrationHomography())) {
			return false;
		}

		// Initialize it to use every feature in the 3-view inlier set
		estimateUtils.usedThreeViewInliers.reset();
		estimateUtils.usedThreeViewInliers.resize(pairwiseUtils.inliersThreeView.size, ( idx ) -> idx);

		// Refine using bundle adjustment
		if (!refineWithBundleAdjustment())
			return false;

		// Look for bad features which fail basic physical sanity checks. Remove them then optimize again.
		if (!removedBadFeatures())
			return false;


		estimateUtils.copyToSolution(pairwiseUtils, solution);

		if (verbose != null) {
			Se3_F64 view1_to_target = estimateUtils.view1_to_target;
			verbose.printf("Rescaled Local T=(%.2f %.2f %.2f)\n",
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z);
			verbose.printf("Final Global   T=(%.2f %.2f %.2f)\n",
					solution.world_to_target.T.x, solution.world_to_target.T.y, solution.world_to_target.T.z);
		}

		return true;
	}

	/**
	 * Computes the transform needed to go from one projective space into another
	 */
	boolean computeCalibratingHomography() {
		// convert everything in to the correct data format
		MultiViewOps.projectiveToFundamental(pairwiseUtils.P2, F21);
		projectiveHomography.initialize(F21, pairwiseUtils.P2);

		BundlePinholeSimplified camera1 = workGraph.getViewCamera(workGraph.lookupView(pairwiseUtils.seed.id)).intrinsic;
		BundlePinholeSimplified camera2 = workGraph.getViewCamera(workGraph.lookupView(pairwiseUtils.viewB.id)).intrinsic;

		BundleAdjustmentOps.convert(camera1, K1);
		BundleAdjustmentOps.convert(camera2, K2);

		DogArray<AssociatedTriple> triples = pairwiseUtils.matchesTriple;
		pairs.resize(triples.size());
		for (int idx = 0; idx < triples.size(); idx++) {
			AssociatedTriple a = triples.get(idx);
			pairs.get(idx).setTo(a.p1, a.p2);
		}

		return projectiveHomography.process(K1, K2, pairs.toList());
	}

	/**
	 * Use previously computed calibration homography to upgrade projective scene to metric
	 */
	private boolean upgradeToMetric( DMatrixRMaj H_cal ) {
		// Compute metric upgrade from found projective camera matrices
		if (!MultiViewOps.projectiveToMetric(pairwiseUtils.P2, H_cal, view1_to_view2H, K_target)) {
			if (verbose != null) verbose.println("FAILED projectiveToMetric P2");
			return false;
		}
		if (!MultiViewOps.projectiveToMetric(pairwiseUtils.P3, H_cal, estimateUtils.view1_to_target, K_target)) {
			if (verbose != null) verbose.println("FAILED projectiveToMetric P3");
			return false;
		}

		// Now that we have an estimate for the camera, let's assign it
		BundleAdjustmentOps.convert(K_target, intrinsicTarget);
		estimateUtils.setCamera3(intrinsicTarget);

		// estimateUtils already has a scaled version of view1_to_view2. We will normalize these two SE3
		// so that the magnitude of view1_to_view2H is also one
		double norm = view1_to_view2H.T.norm();
		estimateUtils.view1_to_target.T.divide(norm);
		view1_to_view2H.T.divide(norm);

		// Need to make sure the sign is in agreement
		boolean negate = MultiViewOps.findScale(view1_to_view2H.T, estimateUtils.view1_to_view2.T) < 0.0;
		if (negate) {
			estimateUtils.view1_to_target.T.scale(-1);
			view1_to_view2H.T.scale(-1);
		}
		// NOTE: We are optimizing using the "known" view1_to_view2 and not the one estimated above

		if (verbose != null) {
			verbose.printf("L  View 1 to 2     T=(%.1f %.1f %.1f) scale=%f\n",
					estimateUtils.view1_to_view2.T.x, estimateUtils.view1_to_view2.T.y, estimateUtils.view1_to_view2.T.z,
					estimateUtils.local_to_global);
			// print the found view 1 to view 2 using local information only
			verbose.printf("H  View 1 to 2     T=(%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);

			SceneWorkingGraph.Camera camera1 = workGraph.getViewCamera(estimateUtils.wview1);
			SceneWorkingGraph.Camera camera2 = workGraph.getViewCamera(estimateUtils.wview2);
			Se3_F64 view1_to_target = estimateUtils.view1_to_target;
			verbose.printf("view1.f=%.2f view2.f=%.2f\n", camera1.intrinsic.f, camera2.intrinsic.f);
			verbose.printf("Initial T=(%.1f %.1f %.1f) f=%.1f k1=%.3f k2=%.3f\n",
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z,
					estimateUtils.camera3.f, estimateUtils.camera3.k1, estimateUtils.camera3.k2);
		}

		return true;
	}

	/**
	 * Performs bundle adjustment on the scene
	 */
	boolean refineWithBundleAdjustment() {
		estimateUtils.configureSbaStructure(pairwiseUtils.inliersThreeView.toList());

		// Mark what is to be estimated.
		estimateUtils.metricSba.structure.cameras.get(2).known = false;
		estimateUtils.metricSba.structure.motions.get(2).known = false;
		// We are ignoring the case that multiple views are from the same camera.
		// This is to allow self calibration to run with more degrees of freedom and less likely to get stuck
		// More study is needed to see if that is really true

		return estimateUtils.performBundleAdjustment(verbose);
	}

	/**
	 * Applies the physical constraints to identify bad image features. It then removes those if there are only
	 * a few and runs bundle adjustment again. If there are no bad features then it accepts this solution.
	 *
	 * @return true if it passes
	 */
	boolean removedBadFeatures() {
		// If only a few bad features, attempt to just remove them
		EstimateViewUtils.RemoveResults results = estimateUtils.removedBadFeatures(pairwiseUtils, fractionBadFeaturesRecover, verbose);
		if (results == EstimateViewUtils.RemoveResults.FAILED) {
			return false;
		}

		if (results == EstimateViewUtils.RemoveResults.GOOD)
			return true;

		if (verbose != null) verbose.println("Removed bad features. Optimizing again.");

		// Refine again and see if those issues were fixed
		if (!refineWithBundleAdjustment())
			return false;

		return estimateUtils.verifyPhysicalConstraints(0.0, verbose);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, estimateUtils.checks);
	}
}
