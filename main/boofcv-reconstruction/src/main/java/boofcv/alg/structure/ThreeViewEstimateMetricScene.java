/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.robust.RansacProjective;
import boofcv.alg.geo.selfcalib.MetricCameraTriple;
import boofcv.factory.geo.*;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import lombok.Getter;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static boofcv.alg.geo.MultiViewOps.triangulatePoints;

/**
 * <p>Estimates the metric scene's structure given a set of sparse features associations from three views. This is
 * intended to give the best possible solution from the sparse set of matching features. Its internal
 * methods are updated as better strategies are found.</p>
 *
 * Assumptions:
 * <ul>
 *     <li>Principle point is zero</li>
 *     <li>Zero skew</li>
 *     <li>fx = fy approximately</li>
 * </ul>
 *
 * <p>The zero principle point is enforced prior to calling {@link #process} by subtracting the image center from
 * each pixel observations.</p>
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
	// NOTE: Consider changing from RANSAC to LSMED. It provides better performance when the error tolerance is
	//       unreasonably high. This could indicate that features are not matching perfectly but close.

	// Make all configurations public for ease of manipulation
	public ConfigPixelsToMetric configSelfCalib = new ConfigPixelsToMetric();
	public ConfigRansac configRansac = new ConfigRansac();
	public ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
	public ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
	public ConfigConverge convergeSBA = new ConfigConverge(1e-6, 1e-6, 100);

	/** Optimize points in homogenous coordinates */
	public boolean homogenous = true;

	/** Use to specify if multiple views share a camera. Values from 0 to 2, inclusive. */
	public int[] viewToCamera = new int[]{0, 0, 0};

	/** If a positive number the focal length will be assumed to be that */
	public double manualFocalLength = -1;

	/** How many features it will keep when pruning */
	public double pruneFraction = 0.7;

	// estimating the trifocal tensor and storing which observations are in the inlier set
	public RansacProjective<MetricCameraTriple, AssociatedTriple> ransac;
	public List<AssociatedTriple> inliers;

	// how much and where it should print to
	private @Nullable PrintStream verbose;

	/** Found intrinsics for each camera */
	public final DogArray<CameraPinholeBrown> listPinhole = new DogArray<>(CameraPinholeBrown::new, CameraPinholeBrown::reset);

	/** Found extrinsics for each view. view 0 is always identity */
	public final DogArray<Se3_F64> listWorldToView = new DogArray<>(Se3_F64::new, Se3_F64::reset);

	// Refines the structure
	public BundleAdjustment<SceneStructureMetric> bundleAdjustment;

	// Bundle adjustment data structure and tuning parameters
	private @Getter SceneStructureMetric structure;
	private @Getter SceneObservations observations;

	// Image width and height
	protected int width, height;

	/**
	 * Sets configurations to their default value
	 */
	public ThreeViewEstimateMetricScene() {
		configRansac.iterations = 1000;
		configRansac.inlierThreshold = 4;

		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		configSBA.configOptimizer = configLM;
	}

	/**
	 * Initializes data structures and fixates configurations
	 *
	 * @param width width of all images
	 * @param height height of all images
	 */
	public void initialize( int width, int height ) {
		// TODO let it specify image shape for each view independently
		ransac = FactoryMultiViewRobust.metricThreeViewRansac(configSelfCalib, configRansac);

		// Let it know some information about the cameras. More than one view can share the same camera
		for (int idx = 0; idx < 3; idx++) {
			int camId = viewToCamera[idx];
			BoofMiscOps.checkTrue(camId <= idx, "camID must be <= array index");
			BoofMiscOps.checkTrue(camId >= 0 && camId < 3, "Camera must be from 0 to 2");
			ransac.setView(idx, new ElevateViewInfo(width, height, camId));
		}

		structure = new SceneStructureMetric(homogenous);
		observations = new SceneObservations();
		bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		bundleAdjustment.configure(convergeSBA.ftol, convergeSBA.gtol, convergeSBA.maxIterations);
	}

	/**
	 * Determines the metric scene. Pixel coordinates are assumed to already have been adjusted for the principle
	 * point being zero, e.g. minus (cx, cy).
	 *
	 * @param associated List of associated features from 3 views. pixels
	 * @return true if successful or false if it failed
	 */
	public boolean process( List<AssociatedTriple> associated ) {
		Objects.requireNonNull(structure, "Did you call initialize?");

		// Fit a trifocal tensor to the input observations
		if (!robustSelfCalibration(associated))
			return false;

		// Run bundle adjustment while make sure a valid solution is found
		setupMetricBundleAdjustment(inliers);

		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		// Prune outliers and run bundle adjustment one last time
		return pruneOutliers(bundleAdjustment);
	}

	/**
	 * Fits a trifocal tensor to the list of matches features using a robust method
	 */
	private boolean robustSelfCalibration( List<AssociatedTriple> associated ) {
		// Fit a trifocal tensor to the observations robustly
		if (!ransac.process(associated)) {
			if (verbose != null) verbose.println("RANSAC failed!");
			return false;
		}

		inliers = ransac.getMatchSet();

		// TODO make configurable
		if (inliers.size() < associated.size()/10) {
			if (verbose != null) verbose.println("Too few inliers: " + inliers.size() + "/" + associated.size());
			return false;
		}

		if (verbose != null) verbose.println("Remaining after RANSAC " + inliers.size() + " / " + associated.size());

		// Save intrinsics
		averageIntrinsicParameters(ransac.getModelParameters());

		// Save extrinsics
		listWorldToView.resetResize(3);
		for (int i = 0; i < 3; i++) {
			ransac.getModelParameters().getView1ToIdx(i, listWorldToView.get(i));
		}

		return true;
	}

	/**
	 * Using the initial metric reconstruction, provide the initial configurations for bundle adjustment
	 */
	private void setupMetricBundleAdjustment( List<AssociatedTriple> inliers ) {
		// Construct bundle adjustment data structure
		structure = new SceneStructureMetric(false);
		structure.initialize(listPinhole.size(), 3, inliers.size());
		observations = new SceneObservations();
		observations.initialize(3);

		for (int i = 0; i < listPinhole.size(); i++) {
			CameraPinhole cp = listPinhole.get(i);
			var bp = new BundlePinholeSimplified();

			bp.f = cp.fx;

			structure.setCamera(i, false, bp);
		}

		MetricCameraTriple found = ransac.getModelParameters();
		for (int i = 0; i < 3; i++) {
			structure.setView(i, viewToCamera[i], i == 0, found.getView1ToIdx(i));
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
	 * Prunes the features with the largest reprojection error
	 */
	private boolean pruneOutliers( BundleAdjustment<SceneStructureMetric> bundleAdjustment ) {
		// see if it's configured to not prune
		if (pruneFraction == 1.0)
			return true;
		if (verbose != null) verbose.println("Pruning Outliers");

		var pruner = new PruneStructureFromSceneMetric(structure, observations);
		pruner.pruneObservationsByErrorRank(pruneFraction);
		pruner.pruneViews(10);
		pruner.pruneUnusedMotions();
		pruner.prunePoints(1);
		bundleAdjustment.setParameters(structure, observations);
		double before = bundleAdjustment.getFitScore();
		if (!bundleAdjustment.optimize(structure))
			return false;

		// Save results
		listPinhole.resetResize(structure.cameras.size);
		for (int i = 0; i < structure.cameras.size; i++) {
			BundleAdjustmentOps.convert(structure.cameras.get(i).model, width, height, listPinhole.get(i));
		}
		listWorldToView.resetResize(structure.views.size);
		for (int i = 0; i < structure.views.size; i++) {
			listWorldToView.get(i).setTo(structure.getParentToView(i));
		}

		// Print debugging info if requested
		if (verbose != null) {
			verbose.println("   before " + before + " after " + bundleAdjustment.getFitScore());
			verbose.println("\nCamera");
			for (int i = 0; i < structure.cameras.size; i++) {
				verbose.println("  " + Objects.requireNonNull(structure.cameras.data[i].getModel()));
			}
			verbose.println("\nworldToView");
			for (int i = 0; i < structure.views.size; i++) {
				Se3_F64 se = structure.getParentToView(i);
				Rodrigues_F64 rod = ConvertRotation3D_F64.matrixToRodrigues(se.R, null);
				verbose.println("  T=" + se.T + "  R=" + rod);
			}
		}
		return true;
	}

	/**
	 * <p>Average intrinsics from all views with the same camera ID.</p>
	 */
	private void averageIntrinsicParameters( MetricCameraTriple results ) {
		// NOTE: Instead of an average maybe picking the one which lowers residual error the most? With average if there's
		// one bonkers answer it will produce garbage results.

		listPinhole.reset();

		int total = 0;
		for (int target = 0; target < 3 && total < 3; target++) {
			listPinhole.grow().setTo(results.getIntrinsics(target));
			CameraPinhole ave = listPinhole.getTail();

			int count = 0;
			for (int i = 0; i < viewToCamera.length; i++) {
				if (viewToCamera[i] != target)
					continue;
				CameraPinhole a = results.getIntrinsics(i);
				ave.fx += a.fx;
				ave.fy += a.fy;
				ave.cx += a.cx;
				ave.cy += a.cy;
				count++;
			}

			ave.fx /= count;
			ave.fy /= count;
			ave.cx /= count;
			ave.cy /= count;
			total += count;
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
