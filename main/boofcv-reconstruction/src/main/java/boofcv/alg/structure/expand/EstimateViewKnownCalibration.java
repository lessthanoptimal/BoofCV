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

import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.alg.geo.robust.ModelMatcherMultiview;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.alg.structure.expand.EstimateViewUtils.RemoveResults;
import boofcv.factory.geo.*;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Expands to a new view using know camera intrinsics for all views. Inliers from 3-view are triangulated
 * using the two known views. Then the 3rd view (the one being added) has its pose estimated using PNP + RANSAC. Then
 * all the points and views are refined using bundle adjustment. The cameras will not be optimized as they are
 * considered to be known.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class EstimateViewKnownCalibration implements VerbosePrint {
	// Contains functions for working with pairwise graph AND information for which view is being estimated
	PairwiseGraphUtils pairwiseUtils;

	// Information about the known metric scene
	SceneWorkingGraph workGraph;

	/** If less than this number of features fail the physical constraint test, attempt to recover by removing them */
	public double fractionBadFeaturesRecover = 0.05;

	/** Minimum number of inliers for it to accept the solution */
	public ConfigLength minimumInliers = ConfigLength.relative(0.5, 50);

	public final EstimateViewUtils estimateUtils = new EstimateViewUtils();

	/** Triangulate from the two known views */
	public Triangulate2ViewsMetricH triangulate2;
	/** Estimates the new view's location */
	public ModelMatcherMultiview<Se3_F64, Point2D3D> ransacPnP;

	// Intrinsic parameters in a format ransac can understand
	final DogArray<RemoveBrownPtoN_F64> listPixelToNorm = new DogArray<>(RemoveBrownPtoN_F64::new);
	// 2D/3D observations for computing the camera's pose
	final DogArray<Point2D3D> list2D3D = new DogArray<>(Point2D3D::new);

	// conversion from input element for PnP to inlier element in 3-view inlier set
	public final DogArray_I32 inputPnP_to_inliersThreeView = new DogArray_I32();

	//------------- Internal Work Space
	public final CameraPinhole pinhole = new CameraPinhole();
	final Point2D_F64 norm1 = new Point2D_F64();
	final Point2D_F64 norm2 = new Point2D_F64();
	final Point2D_F64 norm3 = new Point2D_F64();
	final Point4D_F64 X = new Point4D_F64();

	@Nullable PrintStream verbose;

	public EstimateViewKnownCalibration() {
		defaultConfiguration();
	}

	/**
	 * Instantiates triangulation and RANSAC using default parameters
	 */
	public void defaultConfiguration() {
		var configTriangulate = new ConfigTriangulation();
		var configPnP = new ConfigPnP();
		var configRansac = new ConfigRansac();
		configRansac.iterations = 500;
		configRansac.inlierThreshold = 1.5;
		configTriangulate.type = ConfigTriangulation.Type.GEOMETRIC;

		configure(configTriangulate, configPnP, configRansac);
	}

	/**
	 * Use Configuration classes to initialize triangulate and RANSAC
	 */
	public void configure( ConfigTriangulation configTriangulate, ConfigPnP configPnP, ConfigRansac configRansac ) {
		triangulate2 = FactoryMultiView.triangulate2ViewMetricH(configTriangulate);
		ransacPnP = FactoryMultiViewRobust.pnpRansac(configPnP, configRansac);
	}

	/**
	 * Estimates the pose of a view and which features are in its inlier set.
	 *
	 * @param pairwiseUtils (Input) Pairwise information and specifies which view is being estimated
	 * @param workGraph (Input) Working graph with metric information
	 * @param solution (Output) The estimated state of the new view
	 * @return true if successful or false if it failed
	 */
	public boolean process( PairwiseGraphUtils pairwiseUtils,
							SceneWorkingGraph workGraph,
							MetricExpandByOneView.Solution solution ) {
		this.pairwiseUtils = pairwiseUtils;
		this.workGraph = workGraph;
		solution.reset();

		estimateUtils.initialize(true, workGraph, pairwiseUtils);

		// Estimate camera's pose using PNP
		if (!estimateViewPose()) {
			return false;
		}

		// Refine estimate using SBA. Original estimate does not use information from view-3 in point's 3D location
		if (!refineWithBundleAdjustment()) {
			if (verbose != null) verbose.println("SBA failed");
			return false;
		}

		// Remove points which fail physical sanity checks
		if (!removedBadFeatures()) {
			if (verbose != null) verbose.println("Too many bad features");
			return false;
		}

		// See if there are too few inliers. Relative to the number of image based matches.
		if (!checkEnoughRemainingInliers())
			return false;

		estimateUtils.copyToSolution(pairwiseUtils, solution);

		return true;
	}

	/**
	 * Triangulates points using the two known views then uses PNP to determine view-3's location
	 * @return true if nothing went wrong
	 */
	boolean estimateViewPose() {
		triangulateForPnP();

		pinhole.fsetK(estimateUtils.camera3.f, estimateUtils.camera3.f, 0, 0, 0, 0, 0);
		ransacPnP.setIntrinsic(0, pinhole);
		if (!ransacPnP.process(list2D3D.toList())) {
			if (verbose != null) verbose.println("PNP RANSAC failed");
			return false;
		}

		estimateUtils.view1_to_target.setTo(ransacPnP.getModelParameters());

		// Save which inputs are inliers in RANSAC-PNP
		List<Point2D3D> inliersPnP = ransacPnP.getMatchSet();
		estimateUtils.usedThreeViewInliers.resize(inliersPnP.size());
		for (int inlierCnt = 0; inlierCnt < inliersPnP.size(); inlierCnt++) {
			estimateUtils.usedThreeViewInliers.set(inlierCnt, inputPnP_to_inliersThreeView.get(ransacPnP.getInputIndex(inlierCnt)));
		}

		if (verbose != null) {
			Se3_F64 view_1_to_2 = estimateUtils.view1_to_view2;
			Se3_F64 view_1_to_3 = estimateUtils.view1_to_target;

			verbose.printf("inliersPNP.size=%d inliersTri.size=%d common.size=%d\n",
					inliersPnP.size(), pairwiseUtils.inliersThreeView.size, pairwiseUtils.commonIdx.size);
			verbose.printf("local   1_to_2 T=(%.2f %.2f %.2f)\n", view_1_to_2.T.x, view_1_to_2.T.y, view_1_to_2.T.z);
			verbose.printf("local   1_to_3 T=(%.2f %.2f %.2f)\n", view_1_to_3.T.x, view_1_to_3.T.y, view_1_to_3.T.z);
		}

		return true;
	}

	/**
	 * Triangulates points. Triangulation is done in homogenous coordinates but then converted to 3D points
	 * since there current isn't a variant of PnP in BoofCV which can handle homogenous points.
	 *
	 * As a results, thigns will go poorly if a point is at or very near to infinity
	 */
	private void triangulateForPnP() {
		list2D3D.reset();
		inputPnP_to_inliersThreeView.reset();

		listPixelToNorm.resetResize(3);

		for (int tripleCnt = 0; tripleCnt < pairwiseUtils.inliersThreeView.size; tripleCnt++) {
			AssociatedTriple triple = pairwiseUtils.inliersThreeView.get(tripleCnt);

			// Find the points coordinate by triangulating with the two known views
			estimateUtils.normalize1.compute(triple.p1.x, triple.p1.y, norm1);
			estimateUtils.normalize2.compute(triple.p2.x, triple.p2.y, norm2);

			// Point will be in view-2's coordinate system
			triangulate2.triangulate(norm1, norm2, estimateUtils.view1_to_view2, X);

			// Reject points that are behind the camera or at infinity since PnP can't handle infinity
			if (X.w*X.z <= 0)
				continue;
			// NOTE: In the future it would be best if we could pass in homogenous points into PNP

			// Note that this point was added to the input list
			inputPnP_to_inliersThreeView.add(tripleCnt);

			// Create the 2D-3D point for PnP
			estimateUtils.normalize3.compute(triple.p3.x, triple.p3.y, norm3);

			list2D3D.grow().setTo(norm3.x, norm3.y, X.x/X.w, X.y/X.w, X.z/X.w);
		}
	}

	/**
	 * Refines the pose estimate using bundle adjustment now that there's an estimate for everything
	 *
	 * @return true if an error did not occur
	 */
	private boolean refineWithBundleAdjustment() {
		estimateUtils.configureSbaStructure(pairwiseUtils.inliersThreeView.toList());

		// We will refine its pose, everything else is static
		estimateUtils.metricSba.structure.motions.get(2).known = false;

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
		RemoveResults results = estimateUtils.removedBadFeatures(pairwiseUtils, fractionBadFeaturesRecover, verbose);
		if (results == RemoveResults.FAILED) {
			return false;
		}

		if (results == RemoveResults.GOOD)
			return true;

		if (verbose != null) verbose.println("Removed bad features. Optimizing again.");

		// Refine again and see if those issues were fixed
		if (!refineWithBundleAdjustment())
			return false;

		return estimateUtils.verifyPhysicalConstraints(0.0, verbose);
	}

	/**
	 * Makes sure it hasn't prune too many features and needs to abort.
	 */
	private boolean checkEnoughRemainingInliers() {
		int numMatches = estimateUtils.usedThreeViewInliers.size();
		if (numMatches < minimumInliers.computeI(pairwiseUtils.commonIdx.size)) {
			if (verbose != null)
				verbose.printf("Rejected: matches.size=%d / common.size=%d\n", numMatches, pairwiseUtils.commonIdx.size);
			return false;
		}
		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, estimateUtils.checks, estimateUtils.metricSba);
	}
}
