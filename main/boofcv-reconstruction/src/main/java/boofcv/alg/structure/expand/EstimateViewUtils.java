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

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.structure.MetricSanityChecks;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Common operations when estimating a new view
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class EstimateViewUtils {

	/** Bundle Adjustment functions and configurations */
	public final MetricBundleAdjustmentUtils metricSba = new MetricBundleAdjustmentUtils();

	// Which inliers from 3-view is it using to estimate the view's pose
	public final DogArray_I32 usedThreeViewInliers = new DogArray_I32();

	// Scale factor from local frame to global frame
	public double local_to_global;

	// Converts pixels to normalized image coordinates
	public final RemoveBrownPtoN_F64 normalize1 = new RemoveBrownPtoN_F64();
	public final RemoveBrownPtoN_F64 normalize2 = new RemoveBrownPtoN_F64();
	public final RemoveBrownPtoN_F64 normalize3 = new RemoveBrownPtoN_F64();

	// Storage for transform from view1. The target is view3
	public final Se3_F64 view1_to_view1 = new Se3_F64();
	public final Se3_F64 view1_to_view2 = new Se3_F64();
	public final Se3_F64 view1_to_target = new Se3_F64();

	public final List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(3, Point2D_F64::new);
	public final List<Se3_F64> listMotion = new ArrayList<>();

	/** Used to check results to see if they can be trusted */
	public MetricSanityChecks checks = new MetricSanityChecks();

	List<CameraPinholeBrown> listPriorCameras = new ArrayList<>();

	SceneWorkingGraph.View wview1, wview2;
	BundlePinholeSimplified camera1, camera2, camera3;

	public EstimateViewUtils() {
		listMotion.add(view1_to_view1);
		listMotion.add(view1_to_view2);
		listMotion.add(view1_to_target);
	}

	/**
	 * Initializes data structures for when a new view is being processed. The state of 'pairwiseUtils' is used to
	 * determine which views are used as input.
	 *
	 * @param knownView3 If true then prior information on view-3 (target view) is being used.
	 * @param workGraph The working graph
	 * @param pairwiseUtils Pairwise information and specifies which views are being considered.
	 */
	public void initialize( boolean knownView3, SceneWorkingGraph workGraph, PairwiseGraphUtils pairwiseUtils ) {
		// Due to some weirdness. view1 and view2 are known views
		// view3/viewC is the target view
		wview1 = workGraph.lookupView(pairwiseUtils.seed.id);
		wview2 = workGraph.lookupView(pairwiseUtils.viewB.id);
		camera1 = workGraph.getViewCamera(wview1).intrinsic;
		camera2 = workGraph.getViewCamera(wview2).intrinsic;
		normalize1.setK(camera1.f, camera1.f, 0, 0, 0).setDistortion(camera1.k1, camera1.k2);
		normalize2.setK(camera2.f, camera2.f, 0, 0, 0).setDistortion(camera2.k1, camera2.k2);

		if (knownView3) {
			int targetCameraDB = pairwiseUtils.dbCams.viewToCamera(pairwiseUtils.viewC.id);
			camera3 = workGraph.cameras.get(targetCameraDB).intrinsic;
			normalize3.setK(camera3.f, camera3.f, 0, 0, 0).setDistortion(camera3.k1, camera3.k2);
		} else {
			dereferenceCamera3();
			normalize3.reset();
		}

		// Looking up the known transform from view1 to view 2
		wview1.world_to_view.invert(null).concat(wview2.world_to_view, view1_to_view2);

		// change the scale to improve numerics locally
		local_to_global = view1_to_view2.T.norm();
		BoofMiscOps.checkTrue(local_to_global != 0.0, "BUG! Two views are at the same location");
		view1_to_view2.T.divide(local_to_global);
	}

	@SuppressWarnings("NullAway")
	private void dereferenceCamera3() {
		camera3 = null;
	}

	/**
	 * Specifies the intrinsics for view-3's camera. This should be called if it's not known at the time
	 * {@link #initialize} was called.
	 *
	 * @param camera3 Intrinsic parameters for view-3.
	 */
	public void setCamera3( BundlePinholeSimplified camera3 ) {
		this.camera3 = camera3;
		normalize3.setK(camera3.f, camera3.f, 0, 0, 0).setDistortion(camera3.k1, camera3.k2);
	}

	/**
	 * Copies the found parameters for the new view into the 'solution' object provided
	 *
	 * @param pairwiseUtils Pairwise information
	 * @param solution (Output) view parameters are copied here.
	 */
	public void copyToSolution( PairwiseGraphUtils pairwiseUtils, MetricExpandByOneView.Solution solution ) {
		// Save the final set of image features that were used.
		solution.commonFeatureIndexes.resize(usedThreeViewInliers.size());
		for (int i = 0; i < usedThreeViewInliers.size(); i++) {
			int commonIndex = pairwiseUtils.inlierIdx.get(usedThreeViewInliers.get(i));
			solution.commonFeatureIndexes.set(i, commonIndex);
		}

		// Change scale back to global
		view1_to_target.T.scale(local_to_global);

		wview1.world_to_view.concat(view1_to_target, solution.world_to_target);
		solution.intrinsic.setTo(camera3);
	}

	/**
	 * Configures data structures for running SBA. Which observations are used is specified by the provided inliers.
	 * By default all cameras and views are set to known. If these need to be optimized for a specific use case then
	 * 'known' should be set to false.
	 *
	 * @param inliersThreeView Specifies the observations
	 */
	public void configureSbaStructure( List<AssociatedTriple> inliersThreeView ) {
		final SceneStructureMetric structure = metricSba.structure;
		final SceneObservations observations = metricSba.observations;

		// Even if the cameras are all the same, we will tell that they are different just because the bookkeeping
		// is so much easier and results are the same
		structure.initialize(3, 3, usedThreeViewInliers.size);
		observations.initialize(3);

		// All cameras are known
		structure.setCamera(0, true, camera1);
		structure.setCamera(1, true, camera2);
		structure.setCamera(2, true, camera3);

		// All transforms are known but the target
		structure.setView(0, 0, true, view1_to_view1);
		structure.setView(1, 1, true, view1_to_view2);
		structure.setView(2, 2, true, view1_to_target);

		observations.getView(0).resize(usedThreeViewInliers.size());
		observations.getView(1).resize(usedThreeViewInliers.size());
		observations.getView(2).resize(usedThreeViewInliers.size());
		SceneObservations.View viewObs1 = observations.getView(0);
		SceneObservations.View viewObs2 = observations.getView(1);
		SceneObservations.View viewObs3 = observations.getView(2);

		final TriangulateNViewsMetricH triangulator = metricSba.triangulator;
		var foundX = new Point4D_F64();

		// Only use features that were in the inlier set for PnP
		for (int inlierCnt = 0; inlierCnt < usedThreeViewInliers.size(); inlierCnt++) {
			int threeViewInlierIndex = usedThreeViewInliers.get(inlierCnt);
			AssociatedTriple a = inliersThreeView.get(threeViewInlierIndex);

			// Pass in pixel observations for each view
			viewObs1.set(inlierCnt, inlierCnt, (float)a.p1.x, (float)a.p1.y);
			viewObs2.set(inlierCnt, inlierCnt, (float)a.p2.x, (float)a.p2.y);
			viewObs3.set(inlierCnt, inlierCnt, (float)a.p3.x, (float)a.p3.y);

			normalize1.compute(a.p1.x, a.p1.y, pixelNorms.get(0));
			normalize2.compute(a.p2.x, a.p2.y, pixelNorms.get(1));
			normalize3.compute(a.p3.x, a.p3.y, pixelNorms.get(2));

			if (!triangulator.triangulate(pixelNorms, listMotion, foundX)) {
				throw new RuntimeException("Triangulation failed. Possibly bad input. Handle this problem");
			}

			if (structure.isHomogenous())
				structure.setPoint(inlierCnt, foundX.x, foundX.y, foundX.z, foundX.w);
			else
				structure.setPoint(inlierCnt, foundX.x/foundX.w, foundX.y/foundX.w, foundX.z/foundX.w);

			structure.connectPointToView(inlierCnt, 0);
			structure.connectPointToView(inlierCnt, 1);
			structure.connectPointToView(inlierCnt, 2);
		}
	}

	/**
	 * Runs bundle adjustment and optimizes the scene.
	 *
	 * @param verbose If not null, then verbose information is printed
	 */
	public boolean performBundleAdjustment( @Nullable PrintStream verbose ) {
		// Refine using bundle adjustment
		if (!metricSba.process())
			return false;

		// Save the results
		view1_to_target.setTo(metricSba.structure.getParentToView(2));

		// NOTE: No need to copy since SBA will modify camera3, if it's not marked as known
//		camera3.setTo((BundlePinholeSimplified)metricSba.structure.cameras.get(2).model);

		if (verbose != null) {
			verbose.printf("refined 1_to_3 T=(%.2f %.2f %.2f) f=%.1f k1=%.3f k2=%.3f\n",
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z,
					camera3.f, camera3.k1, camera3.k2);
		}

		return true;
	}

	/**
	 * Applies the physical constraints to identify bad image features. It then removes those if there are only
	 * a few and runs bundle adjustment again. If there are no bad features then it accepts this solution.
	 *
	 * @return true if it passes
	 */
	public RemoveResults removedBadFeatures( PairwiseGraphUtils utils,
											 double fractionBadFeaturesRecover,
											 @Nullable PrintStream verbose ) {
		if (verbose != null) {
			verbose.printf("prior: A={fx=%.1f cx=%.1f %.1f} B={fx=%.1f cx=%.1f %.1f} C={fx=%.1f cx=%.1f %.1f}\n",
					utils.priorCamA.fx, utils.priorCamA.cx, utils.priorCamA.cy,
					utils.priorCamB.fx, utils.priorCamB.cx, utils.priorCamB.cy,
					utils.priorCamC.fx, utils.priorCamC.cx, utils.priorCamC.cy);
		}
		listPriorCameras.clear();
		listPriorCameras.add(utils.priorCamA);
		listPriorCameras.add(utils.priorCamB);
		listPriorCameras.add(utils.priorCamC);
		if (!checks.checkPhysicalConstraints(metricSba, listPriorCameras)) {
			if (verbose != null) verbose.println("Fatal error when checking constraints");
			return RemoveResults.FAILED;
		}

		BoofMiscOps.checkEq(usedThreeViewInliers.size(), checks.badFeatures.size);

		// See if there are too many bad features for it to trust it
		int countBadFeatures = checks.badFeatures.count(true);
		if (countBadFeatures > fractionBadFeaturesRecover*checks.badFeatures.size) {
			if (verbose != null)
				verbose.println("Failed check on image and physical constraints. bad=" +
						countBadFeatures + "/" + checks.badFeatures.size);
			return RemoveResults.FAILED;
		}

		// No bad features. So no need to run again
		if (countBadFeatures == 0)
			return RemoveResults.GOOD;

		// Remove the bad features
		for (int inlierIdx = checks.badFeatures.size - 1; inlierIdx >= 0; inlierIdx--) {
			if (!checks.badFeatures.get(inlierIdx))
				continue;
			// Order of the inliers doesn't matter, but these two lists need to refer to the same feature
			usedThreeViewInliers.removeSwap(inlierIdx);
		}

		return RemoveResults.AGAIN;
	}

	/**
	 * Inspects the observations to see if they pass a check on known constraints.
	 *
	 * @param fractionAccept Fraction of points which can fail the test and this returns true
	 * @param verbose If not null then verbose information will be printed
	 * @return true if it passes the constraints check
	 * @see MetricSanityChecks
	 */
	public boolean verifyPhysicalConstraints( double fractionAccept, @Nullable PrintStream verbose ) {
		if (!checks.checkPhysicalConstraints(metricSba, listPriorCameras)) {
			if (verbose != null) verbose.println("Fatal error when checking constraints");
			return false;
		}

		BoofMiscOps.checkEq(usedThreeViewInliers.size(), checks.badFeatures.size);

		// See if there are too many bad features for it to trust it
		int countBadFeatures = checks.badFeatures.count(true);
		if (countBadFeatures > fractionAccept*checks.badFeatures.size) {
			if (verbose != null)
				verbose.println("Failed check on image and physical constraints. bad=" +
						countBadFeatures + "/" + checks.badFeatures.size + " maxFraction=" + fractionAccept);
			return false;
		}

		return true;
	}

	enum RemoveResults {
		/** No errors found */
		GOOD,
		/** Too many errors found */
		FAILED,
		/** It should attempt to fix the errors */
		AGAIN
	}
}
