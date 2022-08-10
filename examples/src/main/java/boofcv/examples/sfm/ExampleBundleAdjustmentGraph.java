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

package boofcv.examples.sfm;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;
import java.util.Random;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;

/**
 * Example which shows you how to construct the scene graph and observations that are feed into bundle adjustment.
 *
 * Here we will optimize a synchronized stereo camera system. The baseline is known and the location of camera[0] in
 * each time step is estimated. By synchronized we mean that the two cameras capture images at the exact same time.
 * We will intentionally give it an incorrect set of parameters then see if bundle adjustment will fix it given
 * perfect observations.
 *
 * @author Peter Abeles
 */
public class ExampleBundleAdjustmentGraph {
	public static void main( String[] args ) {
		var rand = new Random(234);
		int numPoints = 100;
		int numMotions = 10;
		int numCameras = 2;
		var intrinsic0 = new CameraPinholeBrown(500, 510, 0, 400, 405, 800, 700);
		var intrinsic1 = new CameraPinholeBrown(300, 299, 0, 500, 400, 1000, 800);
		var view0_to_view1 = eulerXyz(-0.15, 0.05, 0, 0, 0, 0.05, null);

		// Initialize data structures by telling it the number of features, cameras, views, motions
		// Homogenous coordinates will be used since they can handle points at ininfity
		var structure = new SceneStructureMetric(/*homogenous*/ true);
		var observations = new SceneObservations();

		// Index of the motion where the stereo baseline is stored
		int baselineIndex = 0;

		structure.initialize(
				numCameras, /*views*/ numMotions*numCameras, /* motions */numMotions + 1,
				numPoints, /* known rigid objects */0);
		observations.initialize(numMotions*2);

		structure.setCamera(0, true, intrinsic0);
		structure.setCamera(1, true, intrinsic1);

		// Set up the motion from camera[0] to camera[1] that define the stereo pair
		structure.motions.grow(); // motion is the one data structure that isn't predeclared at init()
		structure.motions.get(baselineIndex).known = false;
		structure.motions.get(baselineIndex).parent_to_view.setTo(view0_to_view1);
		structure.motions.get(baselineIndex).parent_to_view.T.y += 0.05; // give it an imperfect estimate

		// A synthetic scene is going to be created to enable us to focus on the main problem
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 3), -1, 1, 100, rand);

		// Add points to bundle adjustment parameters. Here we will add noise to make it more interesting.
		for (int pointIdx = 0; pointIdx < cloud.size(); pointIdx++) {
			Point3D_F64 p = cloud.get(pointIdx);
			structure.points.get(pointIdx).set(p.x, p.y, p.z, 1.0);
			// ADDING NOISE IS FOR DEMONSTRATION PURPOSES. DO NOT DO THIS WITH REAL DATA
			for (int i = 0; i < 3; i++) {
				structure.points.get(pointIdx).coordinate[1] += rand.nextGaussian()*0.05;
			}
		}

		System.out.println("Simulating scene:");
		var w2p0 = new WorldToCameraToPixel();
		var w2p1 = new WorldToCameraToPixel();
		var pixel = new Point2D_F64();

		for (int motionIdx = 0; motionIdx < numMotions; motionIdx++) {
			// Two views for every motion. Index of view[0] at this time step
			int viewIdx0 = motionIdx*2;

			// Specify where the views are located
			Se3_F64 worldToView0 = eulerXyz(-1.2 + motionIdx*0.4, 0, 0, rand.nextGaussian()*0.1, 0, 0, null);
			Se3_F64 worldToView1 = worldToView0.concat(view0_to_view1, null);

			// Set up projection from a point in world coordinates to a point in a camera view
			w2p0.configure(intrinsic0, worldToView0);
			w2p1.configure(intrinsic1, worldToView1);

			// Get observations for this view
			// views for the two cameras will be interleaved together
			SceneObservations.View pview0 = observations.getView(viewIdx0);
			SceneObservations.View pview1 = observations.getView(viewIdx0 + 1);

			// camera[0] is easy to configure since it's always relative to the global frame
			// this view has a known location to anchor it at the origin. Otherwise everything will float
			structure.setView(/* view */ viewIdx0, /* camera */0, /* known */motionIdx == 0, worldToView0, -1);

			// camera[1] is more difficult since multiple views share the same motion, but link to camera[0] view
			SceneStructureMetric.View sview1 = structure.views.get(viewIdx0 + 1);
			sview1.parent = structure.views.get(viewIdx0);
			sview1.camera = 1;
			sview1.parent_to_view = baselineIndex;

			for (int pointIdx = 0; pointIdx < cloud.size(); pointIdx++) {
				Point3D_F64 p = cloud.get(pointIdx);

				// Don't add the observation if it's behind the camera or outside the image
				if (w2p0.transform(p, pixel) && intrinsic0.isInside(pixel.x, pixel.y)) {
					// Save the pixel observations
					pview0.add(/* feature */ pointIdx, (float)pixel.x, (float)pixel.y);
					// Add a connection between this point and the view in the scene graph
					structure.connectPointToView(pointIdx, viewIdx0);
				}

				if (w2p1.transform(p, pixel) && intrinsic1.isInside(pixel.x, pixel.y)) {
					pview1.add(/* feature */ pointIdx, (float)pixel.x, (float)pixel.y);
					structure.connectPointToView(pointIdx, viewIdx0 + 1);
				}
			}
			System.out.printf(" view[%2d] observations=%d\n", viewIdx0, pview0.size());
			System.out.printf(" view[%2d] observations=%d\n", viewIdx0 + 1, pview1.size());
		}

		// Let's optimize everything now and see if it fixes the noise we injected
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(null);

		// Tell it to print results every iteration. More interesting that way
		bundleAdjustment.setVerbose(System.out, null);
		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.configure(1e-12, 1e-12, /* max iterations */ 30);

		// Perform the optimization. This will take a moment. Note you can pass in the same structure for output
		if (!bundleAdjustment.optimize(/* output */structure))
			throw new RuntimeException("Optimization failed");

		// Print and see if it fixed the incorrect baseline that was passed in
		structure.motions.get(baselineIndex).parent_to_view.print();
	}
}
