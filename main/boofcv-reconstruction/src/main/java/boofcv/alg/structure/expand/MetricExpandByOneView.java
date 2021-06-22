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

import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.structure.*;
import boofcv.misc.BoofMiscOps;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

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

	/** Do not consider the uncalibrated case if the camera is already known */
	public boolean onlyConsiderCalibrated = false;

	public EstimateViewKnownCalibration expandCalibrated = new EstimateViewKnownCalibration();
	public EstimateViewSelfCalibrate expandUnknown = new EstimateViewSelfCalibrate();

	MetricExpandByOneView.Solution solutionCalibrated = new MetricExpandByOneView.Solution();
	MetricExpandByOneView.Solution solutionUnknown = new MetricExpandByOneView.Solution();

	/**
	 * Fit score for uncalibrated case needs to be significantly better than calibrated to be selected. This
	 * is how much its score is degraded. The reason is because you will almost always get a better fit when
	 * you allow more parameters to vary.
	 */
	public double overfitHandyCap = 0.5;

	//------------------------- Local work space

	// Storage fort he two selected connections with known cameras
	List<PairwiseImageGraph.Motion> connections = new ArrayList<>();

	/**
	 * Attempts to estimate the camera model in the global projective space for the specified view
	 *
	 * @param dbSimilar (Input) image data base
	 * @param workGraph (Input/Output) scene graph. On input it will have the known scene and if successful the metric
	 * information for the target view.
	 * @param target (Input) The view that needs its projective camera estimated and the graph is being expanded into
	 * @return true if successful target view has an estimated calibration matrix and pose, which have already been
	 * added to "workGraph"
	 */
	public boolean process( LookUpSimilarImages dbSimilar,
							LookUpCameraInfo dbCam,
							SceneWorkingGraph workGraph,
							PairwiseImageGraph.View target ) {
		checkTrue(!workGraph.isKnown(target), "Target shouldn't already be in the workGraph");
		this.workGraph = workGraph;
		this.utils.dbSimilar = dbSimilar;
		this.utils.dbCams = dbCam;

		// Select two known connected Views
		if (!selectTwoConnections(target, connections)) {
			if (verbose != null) {
				verbose.println("Failed to expand because two connections couldn't be found. valid.size=" +
						validCandidates.size());
				for (int i = 0; i < validCandidates.size(); i++) {
					verbose.println("valid view.id='" + validCandidates.get(i).other(target).id + "'");
				}
			}
			return false;
		}

		// Find features which are common between all three views
		utils.seed = connections.get(0).other(target);
		utils.viewB = connections.get(1).other(target);
		utils.viewC = target; // easier if target is viewC when doing metric elevation
		utils.createThreeViewLookUpTables();
		utils.findFullyConnectedTriple();

		if (verbose != null) {
			verbose.println("Expanding to view='" + target.id + "' using views ( '" + utils.seed.id + "' , '" + utils.viewB.id +
					"') common=" + utils.commonIdx.size + " valid.size=" + validCandidates.size());
		}

		// Estimate trifocal tensor using three view observations
		utils.createTripleFromCommon(verbose);
		if (!utils.estimateProjectiveCamerasRobustly())
			return false;
		if (verbose != null) verbose.println("Trifocal RANSAC inliers.size=" + utils.inliersThreeView.size());

		int tripleInliers = utils.inliersThreeView.size;
		int cameraIndexDB = dbCam.viewToCamera(target.id);

		boolean cameraIsKnown = workGraph.cameras.containsKey(cameraIndexDB);
		boolean success = false;

		// Estimate everything by assuming the camera is not known. This is done to handle the situation where a mistake
		// was made in the past
		if  (cameraIsKnown && onlyConsiderCalibrated) {
			solutionUnknown.reset();
			if (verbose != null) verbose.println("Skipping uncalibrated case");
		} else {
			if (expandUnknown.process(utils, workGraph, solutionUnknown)) {
				BoofMiscOps.checkEq(tripleInliers, utils.inliersThreeView.size, "BUG! Don't modify inliers.");
				success = true;
			} else {
				if (verbose != null) verbose.println("FAILED to estimate view using an unknown camera");
			}
		}

		if (workGraph.cameras.containsKey(cameraIndexDB)) {
			// Camera is known. Let's estimate the view using that info
			if (expandCalibrated.process(utils, workGraph, solutionCalibrated)) {
				BoofMiscOps.checkEq(tripleInliers, utils.inliersThreeView.size, "BUG! Don't modify inliers.");
				success = true;
			} else {
				if (verbose != null) verbose.println("FAILED to estimate view using a known calibrated camera");
			}
		} else {
			solutionCalibrated.reset();
		}

	 	if (!success) {
			if (verbose != null) verbose.println("FAILED no valid solution to expand with");
			return false;
		}

		if (verbose != null) {
			printSolutionSummary();
		}

		Solution solutionBest = selectBestSolution();

		addNewViewToWorkGraph(dbCam, workGraph, target, cameraIndexDB, solutionBest);

		return true;
	}

	private void printSolutionSummary() {
		PrintStream verbose = Objects.requireNonNull(this.verbose);

		Se3_F64 calibrated = solutionCalibrated.world_to_target;
		Se3_F64 unknown = solutionUnknown.world_to_target;
		verbose.printf("calibrated.size=%d unknown.size=%d : triples.size=%d\n",
				solutionCalibrated.commonFeatureIndexes.size, solutionUnknown.commonFeatureIndexes.size, utils.inliersThreeView.size);
		verbose.printf("calibrated: T=(%.2f %.2f %.2f) f=%.1f\n",
				calibrated.T.x, calibrated.T.y, calibrated.T.z, solutionCalibrated.intrinsic.f);
		verbose.printf("unknown:    T=(%.2f %.2f %.2f) f=%.1f\n",
				unknown.T.x, unknown.T.y, unknown.T.z, solutionUnknown.intrinsic.f);
	}

	/**
	 * Successfully found the view's parameters. Add it to the work graph
	 */
	private void addNewViewToWorkGraph( LookUpCameraInfo dbCam, SceneWorkingGraph workGraph,
										PairwiseImageGraph.View target, int cameraIndexDB,
										Solution solution ) {


		// Create the camera if it's unknown
		SceneWorkingGraph.Camera camera = workGraph.cameras.get(cameraIndexDB);
		if (camera == null) {
			camera = workGraph.addCamera(cameraIndexDB);
			camera.intrinsic.setTo(solution.intrinsic);
			dbCam.lookupCalibration(cameraIndexDB, camera.prior);
		}

		// Now that the pose of the new view is known, add it ot the scene
		SceneWorkingGraph.View wtarget = workGraph.addView(target, camera);
		wtarget.world_to_view.setTo(solution.world_to_target);

		// Save the inlier set
		utils.saveRansacInliers(wtarget, solution.commonFeatureIndexes);
	}

	/**
	 * Selects the best solution to the view's parameters based on number of inliers with a preference for the
	 * solution based on prior info.
	 */
	private Solution selectBestSolution() {
		Solution solutionBest;
		if (solutionCalibrated.commonFeatureIndexes.size >=
				solutionUnknown.commonFeatureIndexes.size*overfitHandyCap) {
		   solutionBest = solutionCalibrated;
		   if (verbose != null) verbose.println("Selected calibrated");
	   } else {
		   solutionBest = solutionUnknown;
		   if (verbose != null) verbose.println("Selected unknown");
	   }
		return solutionBest;
	}

	/**
	 * Solution for a view's metric state from a particular approach/set of assumptions.
	 */
	public static class Solution {
		/**
		 * The estimated transform from work to target
		 */
		public Se3_F64 world_to_target = new Se3_F64();
		/**
		 * The estimated intrinsic parameters
		 */
		public BundlePinholeSimplified intrinsic = new BundlePinholeSimplified();
		/**
		 * Specifies the index of features that are in {@link PairwiseGraphUtils#commonIdx} which are in the final
		 * inlier set
		 */
		public DogArray_I32 commonFeatureIndexes = new DogArray_I32();

		public void reset() {
			world_to_target.reset();
			intrinsic.reset();
			commonFeatureIndexes.reset();
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		super.setVerbose(out, configuration);
		BoofMiscOps.verboseChildren(verbose, configuration, expandCalibrated, expandUnknown);
	}
}
