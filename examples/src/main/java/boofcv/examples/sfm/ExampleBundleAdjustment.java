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

package boofcv.examples.sfm;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.geo.CodecBundleAdjustmentInTheLarge;
import boofcv.misc.BoofMiscOps;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.SingleAxisRgb;
import boofcv.visualize.VisualizeData;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration for running sparse bundle adjustment in BoofCV. Bundle adjustment is a problem where a
 * a batch optimization is applied to camera pose, camera parameters, and point locations are all optimized.
 * It's the last step in scene reconstruction and is a very important step. In recent years, applying bundle
 * adjustment to very large scale problems has become important.
 *
 * In this example, data from the "Bundle Adjustment in the Large" paper is used. These data sets are often used
 * to compare different bundle adjustment algorithms against each other. Which is why BoofCV provides a parser
 * and camera model for their dataset. The algorithms in BoofCV are in the early stage of development but on more
 * modest problems shows performance that's comparable to Ceres Solver. BoofCV has yet to be applied to what would
 * be now considered a truly large scale problem that takes day(s) to solve.
 *
 * @author Peter Abeles
 */
public class ExampleBundleAdjustment {
	public static void main( String[] args ) throws IOException {
		// Because the Bundle Adjustment in the Large data set is popular, a file reader and writer is included
		// with BoofCV. BoofCV uses two data types to describe the parameters in a bundle adjustment problem
		// BundleAdjustmentSceneStructure is used for camera parameters, camera locations, and 3D points
		// BundleAdjustmentObservations for image observations of 3D points
		// ExampleMultiViewSceneReconstruction gives a better feel for these data structures or you can look
		// at the source code of CodecBundleAdjustmentInTheLarge
		CodecBundleAdjustmentInTheLarge parser = new CodecBundleAdjustmentInTheLarge();

		parser.parse(new File(UtilIO.pathExample("sfm/problem-16-22106-pre.txt")));

		// Print information which gives you an idea of the problem's scale
		System.out.println("Optimizing " + parser.scene.getParameterCount() +
				" parameters with " + parser.observations.getObservationCount() + " observations\n\n");

		// Configure the sparse Levenberg-Marquardt solver
		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		// Important tuning parameter. Won't converge to a good solution if picked improperly. Small changes
		// to this problem and speed up or slow down convergence and change the final result. This is true for
		// basically all solvers.
		configLM.dampeningInitial = 1e-3;
		// Improves Jacobian matrix's condition. Recommended in general but not important in this problem
		configLM.hessianScaling = true;

		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		// Create and configure the bundle adjustment solver
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		// prints out useful debugging information that lets you know how well it's converging
		bundleAdjustment.setVerbose(System.out, null);
		// Specifies convergence criteria
		bundleAdjustment.configure(1e-6, 1e-6, 50);

		// Scaling each variable type so that it takes on a similar numerical value. This aids in optimization
		// Not important for this problem but is for others
		ScaleSceneStructure bundleScale = new ScaleSceneStructure();
		bundleScale.applyScale(parser.scene, parser.observations);
		bundleAdjustment.setParameters(parser.scene, parser.observations);

		// Runs the solver. This will take a few minutes. 7 iterations takes about 3 minutes on my computer
		long startTime = System.currentTimeMillis();
		double errorBefore = bundleAdjustment.getFitScore();
		if (!bundleAdjustment.optimize(parser.scene)) {
			throw new RuntimeException("Bundle adjustment failed?!?");
		}

		// Print out how much it improved the model
		System.out.println();
		System.out.printf("Error reduced by %.1f%%\n", (100.0*(errorBefore/bundleAdjustment.getFitScore() - 1.0)));
		System.out.println(BoofMiscOps.milliToHuman(System.currentTimeMillis() - startTime));

		// Return parameters to their original scaling. Can probably skip this step.
		bundleScale.undoScale(parser.scene, parser.observations);

		// Visualize the results using a point cloud viewer
		visualizeInPointCloud(parser.scene);
	}

	private static void visualizeInPointCloud( SceneStructureMetric structure ) {
		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point3D_F64 world = new Point3D_F64();
		Point3D_F64 camera = new Point3D_F64();

		for (int i = 0; i < structure.points.size; i++) {
			// Get 3D location
			SceneStructureCommon.Point p = structure.points.get(i);
			p.get(world);

			// Project point into an arbitrary view
			for (int j = 0; j < p.views.size; j++) {
				int viewIdx = p.views.get(j);
				SePointOps_F64.transform(structure.getParentToView(viewIdx), world, camera);
				cloudXyz.add(world.copy());
				break;
			}
		}

		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
		viewer.setColorizer(new SingleAxisRgb.Z().fperiod(20)); // makes it easier to see points without RGB color
		viewer.setClipDistance(70); // done for visualization to make it easier to separate objects with the fog
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.05);
		viewer.addCloud(cloudXyz);
		viewer.setCameraHFov(UtilAngle.radian(60));

		// Give it a good initial pose. This was determined through trial and error
		Se3_F64 cameraToWorld = new Se3_F64();
		cameraToWorld.T.setTo(-10.848385, -6.957626, 2.9747992);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, -2.734419, -0.27446, -0.24310, cameraToWorld.R);
		viewer.setCameraToWorld(cameraToWorld);

		SwingUtilities.invokeLater(() -> {
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Refined Scene", true);
		});
	}
}
