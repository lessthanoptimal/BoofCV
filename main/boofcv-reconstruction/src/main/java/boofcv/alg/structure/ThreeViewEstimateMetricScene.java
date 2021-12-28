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

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.RefineThreeViewProjective;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.geo.selfcalib.ProjectiveToMetricCameras;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.TwoViewToCalibratingHomography;
import boofcv.factory.geo.*;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.*;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static boofcv.alg.geo.MultiViewOps.triangulatePoints;

/**
 * Estimates the metric scene's structure given a set of sparse features associations from three views. This is
 * intended to give the best possible solution from the sparse set of matching features. Its internal
 * methods are updated as better strategies are found.
 *
 * Assumptions:
 * <ul>
 *     <li>Principle point is zero</li>
 *     <li>Zero skew</li>
 *     <li>fx = fy approximately</li>
 * </ul>
 *
 * The zero principle point is enforced prior to calling {@link #process} by subtracting the image center from
 * each pixel observations.
 *
 * Steps:
 * <ol>
 *     <li>Fit Trifocal tensor using RANSAC</li>
 *     <li>Get and refine camera matrices</li>
 *     <li>Compute dual absolute quadratic</li>
 *     <li>Estimate intrinsic parameters from DAC</li>
 *     <li>Estimate metric scene structure</li>
 *     <li>Sparse bundle adjustment</li>
 *     <li>Tweak parameters and sparse bundle adjustment again</li>
 * </ol>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class ThreeViewEstimateMetricScene implements VerbosePrint {
	// TODO modify so that you can tell it if each view has the same intrinsics or not

	// Make all configurations public for ease of manipulation
	public ConfigRansac configRansac = new ConfigRansac();
	public ConfigTrifocal configTriRansac = new ConfigTrifocal();
	public ConfigTrifocal configTriFit = new ConfigTrifocal();
	public ConfigTrifocalError configError = new ConfigTrifocalError();
	public ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
	public ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
	public ConfigConverge convergeSBA = new ConfigConverge(1e-6, 1e-6, 100);

	// estimating the trifocal tensor and storing which observations are in the inlier set
	public Ransac<TrifocalTensor, AssociatedTriple> ransac;
	public List<AssociatedTriple> inliers;
	public Estimate1ofTrifocalTensor trifocalEstimator;

	// how much and where it should print to
	private @Nullable PrintStream verbose;

	// Projective camera matrices
	protected DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);
	protected DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
	protected DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

	// storage for pinhole cameras
	public final List<CameraPinhole> listPinhole = new ArrayList<>();

	// Refines the structure
	public BundleAdjustment<SceneStructureMetric> bundleAdjustment;

	// Bundle adjustment data structure and tuning parameters
	public SceneStructureMetric structure;
	public SceneObservations observations;

	// If a positive number the focal length will be assumed to be that
	public double manualFocalLength = -1;

	// How many features it will keep when pruning
	public double pruneFraction = 0.7;

	// shape of input images.
	private int width, height; // TODO Get size for each image individually

	// metric location of each camera. The first view is always identity
	protected List<Se3_F64> listWorldToView = new ArrayList<>();

	/**
	 * Sets configurations to their default value
	 */
	public ThreeViewEstimateMetricScene() {
		configRansac.iterations = 500;
		configRansac.inlierThreshold = 1;

		configError.model = ConfigTrifocalError.Model.REPROJECTION_REFINE;

		configTriFit.which = EnumTrifocal.ALGEBRAIC_7;
		configTriFit.converge.maxIterations = 100;

		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		configSBA.configOptimizer = configLM;

		for (int i = 0; i < 3; i++) {
			listWorldToView.add(new Se3_F64());
		}
	}

	/**
	 * Determines the metric scene. The principle point is assumed to be zero in the passed in pixel coordinates.
	 * Typically this is done by subtracting the image center from each pixel coordinate for each view.
	 *
	 * @param associated List of associated features from 3 views. pixels
	 * @param width width of all images
	 * @param height height of all images
	 * @return true if successful or false if it failed
	 */
	public boolean process( List<AssociatedTriple> associated, int width, int height ) {
		init(width, height);

		// Fit a trifocal tensor to the input observations
		if (!robustFitTrifocal(associated))
			return false;

		// estimate the scene's structure
		if (!estimateProjectiveScene())
			return false;

		if (!projectiveToMetric())
			return false;

		// Run bundle adjustment while make sure a valid solution is found
		setupMetricBundleAdjustment(inliers);

		bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		findBestValidSolution(bundleAdjustment);

		// Prune outliers and run bundle adjustment one last time
		pruneOutliers(bundleAdjustment);

		return true;
	}

	@SuppressWarnings("NullAway")
	private void init( int width, int height ) {
		this.width = width;
		this.height = height;
		ransac = FactoryMultiViewRobust.trifocalRansac(configTriRansac, configError, configRansac);
		trifocalEstimator = FactoryMultiView.trifocal_1(configTriFit);
		structure = null;
		observations = null;
	}

	/**
	 * Fits a trifocal tensor to the list of matches features using a robust method
	 */
	private boolean robustFitTrifocal( List<AssociatedTriple> associated ) {
		// Fit a trifocal tensor to the observations robustly
		ransac.process(associated);

		inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		if (verbose != null)
			verbose.println("Remaining after RANSAC " + inliers.size() + " / " + associated.size());

		// estimate using all the inliers
		// No need to re-scale the input because the estimator automatically adjusts the input on its own
		if (!trifocalEstimator.process(inliers, model)) {
			if (verbose != null) {
				verbose.println("Trifocal estimator failed");
			}
			return false;
		}
		return true;
	}

	/**
	 * Prunes the features with the largest reprojection error
	 */
	private void pruneOutliers( BundleAdjustment<SceneStructureMetric> bundleAdjustment ) {
		// see if it's configured to not prune
		if (pruneFraction == 1.0)
			return;
		if (verbose != null) verbose.println("Pruning Outliers");

		PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure, observations);
		pruner.pruneObservationsByErrorRank(pruneFraction);
		pruner.pruneViews(10);
		pruner.pruneUnusedMotions();
		pruner.prunePoints(1);
		bundleAdjustment.setParameters(structure, observations);
		double before = bundleAdjustment.getFitScore();
		bundleAdjustment.optimize(structure);
		if (verbose != null) verbose.println("   before " + before + " after " + bundleAdjustment.getFitScore());

		if (verbose != null) {
			verbose.println("\nCamera");
			for (int i = 0; i < structure.cameras.size; i++) {
				verbose.println("  " + Objects.requireNonNull(structure.cameras.data[i].getModel()).toString());
			}
			verbose.println("\nworldToView");
			for (int i = 0; i < structure.views.size; i++) {
				if (verbose != null) {
					Se3_F64 se = structure.getParentToView(i);
					Rodrigues_F64 rod = ConvertRotation3D_F64.matrixToRodrigues(se.R, null);
					verbose.println("  T=" + se.T + "  R=" + rod);
				}
			}
		}
	}

	/**
	 * Tries a bunch of stuff to ensure that it can find the best solution which is physically possible
	 */
	private void findBestValidSolution( BundleAdjustment<SceneStructureMetric> bundleAdjustment ) {
		// Specifies convergence criteria
		bundleAdjustment.configure(convergeSBA.ftol, convergeSBA.gtol, convergeSBA.maxIterations);

		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		// ensure that the points are in front of the camera and are a valid solution
		if (checkBehindCamera(structure)) {
			if (verbose != null)
				verbose.println("  #1 Points Behind. Flipping view");
			flipAround(structure, observations);
			bundleAdjustment.setParameters(structure, observations);
			bundleAdjustment.optimize(structure);
		}

		double bestScore = bundleAdjustment.getFitScore();
		if (verbose != null) verbose.println("First Pass: SBA score " + bestScore);
		List<Se3_F64> bestPose = new ArrayList<>();
		List<BundlePinholeSimplified> bestCameras = new ArrayList<>();
		for (int i = 0; i < structure.views.size; i++) {
			BundlePinholeSimplified c = Objects.requireNonNull(structure.cameras.data[i].getModel());
			bestPose.add(structure.getParentToView(i).copy());
			bestCameras.add(c.copy());
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			BundlePinholeSimplified c = Objects.requireNonNull(structure.cameras.data[i].getModel());
			c.f = listPinhole.get(i).fx;
			c.k1 = c.k2 = 0;
		}
		// flip rotation assuming that it was done wrong
		for (int i = 1; i < structure.views.size; i++) {
			CommonOps_DDRM.transpose(structure.getParentToView(i).R);
		}
		triangulatePoints(structure, observations);

		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		if (checkBehindCamera(structure)) {
			if (verbose != null)
				verbose.println("  #2 Points Behind. Flipping view");
			flipAround(structure, observations);
			bundleAdjustment.setParameters(structure, observations);
			bundleAdjustment.optimize(structure);
		}

		// revert to old settings
		if (verbose != null)
			verbose.println(" First Pass / Transpose(R) = " + bestScore + " / " + bundleAdjustment.getFitScore());
		if (bundleAdjustment.getFitScore() > bestScore) {
			if (verbose != null)
				verbose.println("  recomputing old structure");
			for (int i = 0; i < structure.cameras.size; i++) {
				BundlePinholeSimplified c = Objects.requireNonNull(structure.cameras.data[i].getModel());
				c.setTo(bestCameras.get(i));
				structure.getParentToView(i).setTo(bestPose.get(i));
			}
			triangulatePoints(structure, observations);
			bundleAdjustment.setParameters(structure, observations);
			bundleAdjustment.optimize(structure);
			if (verbose != null)
				verbose.println("  score after reverting = " + bundleAdjustment.getFitScore() + "  original " + bestScore);
		}
	}

	/**
	 * Estimate the projective scene from the trifocal tensor
	 */
	private boolean estimateProjectiveScene() {
		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();

		MultiViewOps.trifocalToCameraMatrices(model, P2, P3);

		// Most of the time this makes little difference, but in some edges cases this enables it to
		// converge correctly
		RefineThreeViewProjective refineP23 = FactoryMultiView.threeViewRefine(null);
		if (!refineP23.process(inliers, P2, P3, P2, P3)) {
			if (verbose != null) {
				verbose.println("Can't refine P2 and P3!");
			}
			return false;
		}
		return true;
	}

	/**
	 * Using the initial metric reconstruction, provide the initial configurations for bundle adjustment
	 */
	private void setupMetricBundleAdjustment( List<AssociatedTriple> inliers ) {
		// Construct bundle adjustment data structure
		structure = new SceneStructureMetric(false);
		structure.initialize(3, 3, inliers.size());
		observations = new SceneObservations();
		observations.initialize(3);

		for (int i = 0; i < listPinhole.size(); i++) {
			CameraPinhole cp = listPinhole.get(i);
			BundlePinholeSimplified bp = new BundlePinholeSimplified();

			bp.f = cp.fx;

			structure.setCamera(i, false, bp);
			structure.setView(i, i, i == 0, listWorldToView.get(i));
		}

		for (int i = 0; i < inliers.size(); i++) {
			AssociatedTriple t = inliers.get(i);

			observations.getView(0).add(i, (float)t.p1.x, (float)t.p1.y);
			observations.getView(1).add(i, (float)t.p2.x, (float)t.p2.y);
			observations.getView(2).add(i, (float)t.p3.x, (float)t.p3.y);

			structure.connectPointToView(i, 0);
			structure.connectPointToView(i, 1);
			structure.connectPointToView(i, 2);
		}
		// Initial estimate for point 3D locations
		triangulatePoints(structure, observations);
	}

	/**
	 * Estimates the transform from projective to metric geometry
	 *
	 * @return true if successful
	 */
	boolean projectiveToMetric() {

		// homography from projective to metric

		listPinhole.clear();

		boolean successfulSelfCalibration = false;
		if (manualFocalLength <= 0) {
			// Estimate calibration parameters
			var config = new ConfigSelfCalibDualQuadratic();
//			var config = new ConfigSelfCalibEssentialGuess();
//			config.numberOfSamples = 200;
//			config.fixedFocus = true;
//			config.sampleMin = 0.6;
//			config.sampleMax = 1.5;

			ProjectiveToMetricCameras selfcalib = FactoryMultiView.projectiveToMetric(config);

			List<ElevateViewInfo> views = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				views.add(new ElevateViewInfo(width, height, i));
			}
			List<DMatrixRMaj> cameras = new ArrayList<>();
			cameras.add(P2);
			cameras.add(P3);
			DogArray<AssociatedTuple> observations = new DogArray<>(() -> new AssociatedTupleN(3));
			MultiViewOps.convertTr(ransac.getMatchSet(), observations);

			var results = new MetricCameras();
			boolean success = selfcalib.process(views, cameras, observations.toList(), results);

			if (success) {
				successfulSelfCalibration = true;
				listPinhole.addAll(results.intrinsics.toList());
				listWorldToView.get(0).reset();
				listWorldToView.get(1).setTo(results.motion_1_to_k.get(0));
				listWorldToView.get(2).setTo(results.motion_1_to_k.get(1));
				if (verbose != null) verbose.println("Auto calibration success");
			} else {
				if (verbose != null) verbose.println("Auto calibration failed");
			}
		}

		if (!successfulSelfCalibration) {
			// Use provided focal length or guess using an "average" focal length across cameras
			double focalLength = manualFocalLength <= 0 ? (double)(Math.max(width, height)/2) : manualFocalLength;

			if (verbose != null) verbose.println("Assuming fixed focal length for all views. f=" + focalLength);

			final var estimateH = new TwoViewToCalibratingHomography();
			DMatrixRMaj F21 = MultiViewOps.projectiveToFundamental(P2, null);
			estimateH.initialize(F21, P2);
			DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(focalLength, focalLength, 0, 0, 0);
			DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);
			MultiViewOps.convertTr(ransac.getMatchSet(), 0, 1, pairs);
			if (!estimateH.process(K, K, pairs.toList()))
				throw new RuntimeException("Failed to estimate H given 'known' intrinsics");

			// Use the found calibration homography to find motion estimates
			DMatrixRMaj H = estimateH.getCalibrationHomography();
			listPinhole.clear();
			for (int i = 0; i < 3; i++) {
				listPinhole.add(PerspectiveOps.matrixToPinhole(K, width, height, null));
			}
			listWorldToView.get(0).reset();
			MultiViewOps.projectiveToMetric(P2,H, listWorldToView.get(1), K);
			MultiViewOps.projectiveToMetric(P3,H, listWorldToView.get(2), K);
		}

		if (verbose != null) {
			verbose.println("Initial Intrinsic Estimate:");
			for (int i = 0; i < 3; i++) {
				CameraPinhole r = listPinhole.get(i);
				verbose.printf("  fx = %6.1f, fy = %6.1f, skew = %6.3f\n", r.fx, r.fy, r.skew);
			}
			verbose.println("Initial Motion Estimate:");
		}

		// scale is arbitrary. Set max translation to 1
		double maxT = 0;
		for (int i = 0; i < listWorldToView.size(); i++) {
			Se3_F64 world_to_view = listWorldToView.get(i);
			maxT = Math.max(maxT, world_to_view.T.norm());
		}

		for (int i = 0; i < listWorldToView.size(); i++) {
			Se3_F64 world_to_view = listWorldToView.get(i);
			world_to_view.T.scale(1.0/maxT);
			if (verbose != null) {
				Rodrigues_F64 rod = ConvertRotation3D_F64.matrixToRodrigues(world_to_view.R, null);
				verbose.println("  T=" + world_to_view.T + "  R=" + rod);
			}
		}

		return true;
	}

	/**
	 * Checks to see if a solution was converged to where the points are behind the camera. This is
	 * physically impossible
	 */
	private boolean checkBehindCamera( SceneStructureMetric structure ) {

		int totalBehind = 0;
		Point3D_F64 X = new Point3D_F64();
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.data[i].get(X);
			if (X.z < 0)
				totalBehind++;
		}

		if (verbose != null) {
			verbose.println("points behind " + totalBehind + " / " + structure.points.size);
		}

		return totalBehind > structure.points.size/2;
	}

	/**
	 * Flip the camera pose around. This seems to help it converge to a valid solution if it got it backwards
	 * even if it's not technically something which can be inverted this way
	 */
	private static void flipAround( SceneStructureMetric structure, SceneObservations observations ) {
		// The first view will be identity
		for (int i = 1; i < structure.views.size; i++) {
			Se3_F64 w2v = structure.getParentToView(i);
			w2v.setTo(w2v.invert(null));
		}
		triangulatePoints(structure, observations);
	}

	public SceneStructureMetric getStructure() {
		return structure;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
