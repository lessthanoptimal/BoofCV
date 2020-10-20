/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.cameras;

import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBundleUniversalOmni extends BoofStandardJUnit {

	@Test
	void compareForward() {
		CameraUniversalOmni cam = new CameraUniversalOmni(2);
		cam.fx = 300; cam.fy = 200;
		cam.cx = cam.cy = 400;
		cam.radial[0] = 0.01;
		cam.radial[1] = -0.02;
		cam.skew = 0.001;
		cam.t1 = 0.01;
		cam.t2 = -0.01;
		cam.mirrorOffset = 1;

		BundleUniversalOmni alg = new BundleUniversalOmni(cam);
		Point3Transform2_F64 n2p = LensDistortionFactory.wide(cam).distortStoP_F64();

		Point2D_F64 found = new Point2D_F64();

		double X = 0.1, Y = -0.2, Z = 2;
		alg.project(X, Y, Z, found);

		Point2D_F64 expected = new Point2D_F64();
		// convert to unit sphere
		double n = Math.sqrt(X*X + Y*Y + Z*Z);
		n2p.compute(X/n, Y/n, Z/n, expected);

		assertTrue(found.distance(expected) < UtilEjml.TEST_F64);
	}

	@Test
	void withAllParameters() {
		double[][] parameters = new double[][]{{300, 200, 400, 400, 0.01, 0.015, -0.001, 0.002, 0.1, 0.9}, {400, 600, 1000, 1000, 0.01, 0.015, -0.001, 0.002, 2, 0.9}};
		new GenericChecksBundleAdjustmentCamera(new BundleUniversalOmni(false, 2, true, false), 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void withFixedMirror() {
		double[][] parameters = new double[][]{{300, 200, 400, 400, 0.01, 0.015, -0.001, 0.002, 0.1}, {400, 600, 1000, 1000, 0.01, 0.015, -0.001, 0.002, 2}};
		new GenericChecksBundleAdjustmentCamera(new BundleUniversalOmni(false, 2, true, 0.9), 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void withoutSkew() {
		double[][] parameters = new double[][]{{300, 200, 400, 400, 0.01, 0.02, -0.001, 0.002, 0.9}, {400, 600, 1000, 1000, 0.01, 0.02, -0.001, 0.002, 0.9}};
		new GenericChecksBundleAdjustmentCamera(new BundleUniversalOmni(true, 2, true, false), 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void variousRadialLengths() {
		for (int i = 0; i <= 3; i++) {
			CameraUniversalOmni cam = new CameraUniversalOmni(i);
			cam.fx = 300; cam.fy = 200;
			cam.cx = cam.cy = 400;
			cam.skew = 0.01;
			for (int j = 0; j < i; j++) {
				cam.radial[j] = 0.01 - j*0.001;
			}
			cam.t1 = -0.001;
			cam.t2 = 0.002;
			cam.mirrorOffset = 0.05;

			BundleUniversalOmni alg = new BundleUniversalOmni(cam);
			double[][] parameters = new double[1][alg.getIntrinsicCount()];
			alg.getIntrinsic(parameters[0], 0);
			new GenericChecksBundleAdjustmentCamera(alg, 0.02) {}
					.setParameters(parameters)
					.checkAll();
		}
	}

	@Test
	void zeroTangential() {
		CameraUniversalOmni cam = new CameraUniversalOmni(1);
		cam.fx = 300; cam.fy = 200;
		cam.cx = cam.cy = 400;
		cam.skew = 0.01;
		cam.radial[0] = 0.01;
		cam.mirrorOffset = 0.9;
		// since t1 and t2 are zero it will automatically turn off tangential

		BundleUniversalOmni alg = new BundleUniversalOmni(cam);
		double[][] parameters = new double[1][alg.getIntrinsicCount()];
		alg.getIntrinsic(parameters[0], 0);
		new GenericChecksBundleAdjustmentCamera(alg, 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}
}
