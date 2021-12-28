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

package boofcv.examples.stereo;

import boofcv.abst.geo.bundle.*;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.EstimatePlaneAtInfinityGivenK;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.ConvertImage;
import boofcv.examples.sfm.ExampleComputeFundamentalMatrix;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.examples.stereo.ExampleTrifocalStereoUncalibrated.computeStereoCloud;

/**
 * <p>
 * In this example a stereo point cloud is computed from two uncalibrated stereo images. The uncalibrated problem
 * is much more difficult from the calibrated or semi-calibrated problem and the solution below will often fail.
 * The root cause of this difficulty is that it is impossible to remove all false associations given two views,
 * even if the true fundamental matrix is provided! For that reason it is recommended that you use a minimum of
 * three views with uncalibrated observations.
 * </p>
 *
 * <p>
 * A summary of the algorithm is provided below. There are many ways in which it can be improved upon, but would
 * increase the complexity. There is also no agreed upon best solution found in the literature and the solution
 * presented below is "impractical" because of its sensitivity to tuning parameters. If you got a solution
 * which does a better job let us know!
 * </p>
 *
 * <ol>
 *     <li>Feature association</li>
 *     <li>RANSAC to estimate Fundamental matrix</li>
 *     <li>Guess and check focal length and compute projective to metric homography</li>
 *     <li>Upgrade to metric geometry</li>
 *     <li>Set up bundle adjustment and triangulate 3D points</li>
 *     <li>Run bundle adjustment</li>
 *     <li>Prune outlier points</li>
 *     <li>Run bundle adjustment again</li>
 *     <li>Compute stereo rectification</li>
 *     <li>Rectify images</li>
 *     <li>Compute stereo disparity</li>
 *     <li>Compute and display point cloud</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ExampleStereoUncalibrated {

	public static void main( String[] args ) {
		// Solution below is unstable. Turning concurrency off so that it always produces a valid solution
		// The two view case is very challenging and I've not seen a stable algorithm yet
		BoofConcurrency.USE_CONCURRENT = false;

		// Successful
		String name = "bobcats_";
//		String name = "mono_wall_";
//		String name = "minecraft_cave1_";
//		String name = "chicken_";
//		String name = "books_";

		// Successful Failures
//		String name = "triflowers_";

		// Failures
//		String name = "rock_leaves_";
//		String name = "minecraft_distant_";
//		String name = "rockview_";
//		String name = "pebbles_";
//		String name = "skull_";
//		String name = "turkey_";

		BufferedImage buff01 = UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "01.jpg"));
		BufferedImage buff02 = UtilImageIO.loadImageNotNull(UtilIO.pathExample("triple/" + name + "02.jpg"));

		Planar<GrayU8> color01 = ConvertBufferedImage.convertFrom(buff01, true, ImageType.pl(3, GrayU8.class));
		Planar<GrayU8> color02 = ConvertBufferedImage.convertFrom(buff02, true, ImageType.pl(3, GrayU8.class));

		GrayU8 image01 = ConvertImage.average(color01, null);
		GrayU8 image02 = ConvertImage.average(color02, null);

		// Find a set of point feature matches
		List<AssociatedPair> matches = ExampleComputeFundamentalMatrix.computeMatches(buff01, buff02);

		// Prune matches using the epipolar constraint. use a low threshold to prune more false matches
		var inliers = new ArrayList<AssociatedPair>();
		DMatrixRMaj F = ExampleComputeFundamentalMatrix.robustFundamental(matches, inliers, 0.1);

		// Perform self calibration using the projective view extracted from F
		// Note that P1 = [I|0]
		System.out.println("Self calibration");
		DMatrixRMaj P2 = MultiViewOps.fundamentalToProjective(F);

		// Take a crude guess at the intrinsic parameters. Bundle adjustment will fix this later.
		int width = buff01.getWidth(), height = buff02.getHeight();
		double fx = width/2; double fy = fx;
		double cx = width/2; double cy = height/2;

		// Compute a transform from projective to metric by assuming we know the camera's calibration
		var estimateV = new EstimatePlaneAtInfinityGivenK();
		estimateV.setCamera1(fx, fy, 0, cx, cy);
		estimateV.setCamera2(fx, fy, 0, cx, cy);

		var v = new Vector3D_F64(); // plane at infinity
		if (!estimateV.estimatePlaneAtInfinity(P2, v))
			throw new RuntimeException("Failed!");

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(fx, fy, 0, cx, cy);
		DMatrixRMaj H = MultiViewOps.createProjectiveToMetric(K, v.x, v.y, v.z, 1, null);
		DMatrixRMaj P2m = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P2, H, P2m);

		// Decompose and get the initial estimate for translation
		var tmp = new DMatrixRMaj(3, 3);
		var view1_to_view2 = new Se3_F64();
		MultiViewOps.decomposeMetricCamera(P2m, tmp, view1_to_view2);

		//------------------------- Setting up bundle adjustment
		// bundle adjustment will provide a more refined and accurate estimate of these parameters
		System.out.println("Configuring bundle adjustment");

		// Construct bundle adjustment data structure
		var structure = new SceneStructureMetric(false);
		var observations = new SceneObservations();

		// We will assume that the camera has fixed intrinsic parameters
		structure.initialize(1, 2, inliers.size());
		observations.initialize(2);

		var bp = new BundlePinholeSimplified();
		bp.f = fx;
		structure.setCamera(0, false, bp);

		// The first view is the world coordinate system
		structure.setView(0, 0, true, new Se3_F64());
		// Second view was estimated previously
		structure.setView(1, 0, false, view1_to_view2);

		for (int i = 0; i < inliers.size(); i++) {
			AssociatedPair t = inliers.get(i);

			// substract out the camera center from points. This allows a simple camera model to be used and
			// errors in the this coordinate tend to be non-fatal
			observations.getView(0).add(i, (float)(t.p1.x - cx), (float)(t.p1.y - cy));
			observations.getView(1).add(i, (float)(t.p2.x - cx), (float)(t.p2.y - cy));

			// each point is visible in both of the views
			structure.connectPointToView(i, 0);
			structure.connectPointToView(i, 1);
		}

		// initial location of points is found through triangulation
		MultiViewOps.triangulatePoints(structure, observations);

		//------------------ Running Bundle Adjustment
		System.out.println("Performing bundle adjustment");
		var configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		var configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		// Create and configure the bundle adjustment solver
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		// prints out useful debugging information that lets you know how well it's converging
		bundleAdjustment.setVerbose(System.out, null);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-6, 1e-6, 100);

		// Scaling improve accuracy of numerical calculations
		var bundleScale = new ScaleSceneStructure();
		bundleScale.applyScale(structure, observations);

		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		// Sometimes pruning outliers help improve the solution. In the stereo case the errors are likely
		// to already fatal
		var pruner = new PruneStructureFromSceneMetric(structure, observations);
		pruner.pruneObservationsByErrorRank(0.85);
		pruner.prunePoints(1);
		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		bundleScale.undoScale(structure, observations);

		System.out.println("\nCamera");
		for (int i = 0; i < structure.cameras.size; i++) {
			System.out.println(structure.cameras.data[i].getModel().toString());
		}
		System.out.println("\n\nworldToView");
		for (int i = 0; i < structure.views.size; i++) {
			System.out.println(structure.getParentToView(i).toString());
		}

		// display the inlier matches found using the robust estimator
		System.out.println("\n\nComputing Stereo Disparity");
		BundlePinholeSimplified cp = structure.getCameras().get(0).getModel();
		var intrinsic = new CameraPinholeBrown();
		intrinsic.fsetK(cp.f, cp.f, 0, cx, cy, width, height);
		intrinsic.fsetRadial(cp.k1, cp.k2);

		Se3_F64 leftToRight = structure.getParentToView(1);

		computeStereoCloud(image01, image02, color01, color02, intrinsic, intrinsic, leftToRight, 0, 250);
	}
}
