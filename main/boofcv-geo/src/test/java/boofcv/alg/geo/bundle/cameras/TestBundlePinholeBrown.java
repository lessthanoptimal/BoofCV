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

import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBundlePinholeBrown extends BoofStandardJUnit {

	@Test
	void compareForward() {
		CameraPinholeBrown cam = new CameraPinholeBrown(1);
		cam.fx = 300; cam.fy = 200;
		cam.cx = cam.cy = 400;
		cam.radial[0] = 0.01;
		cam.skew = 0.001;
		cam.t1 = 0.01;
		cam.t2 = -0.01;

		BundlePinholeBrown alg = BundleAdjustmentOps.convert(cam, (BundlePinholeBrown)null);
		Point2Transform2_F64 n2p = LensDistortionFactory.narrow(cam).distort_F64(false, true);

		Point2D_F64 found = new Point2D_F64();

		double X = 0.1, Y = -0.2, Z = 2;
		alg.project(X, Y, Z, found);

		Point2D_F64 expected = new Point2D_F64();
		n2p.compute(X/Z, Y/Z, expected);

		assertEquals(0.0, found.distance(expected), UtilEjml.TEST_F64);
	}

	@Test
	void withSkew() {
		double[][] parameters = new double[][]{{300, 200, 400, 400, 0.01, 0.02, -0.001, 0.002, 0.1}, {400, 600, 1000, 1000, 0.01, 0.02, -0.001, 0.002, 2}};
		new GenericChecksBundleAdjustmentCamera(new BundlePinholeBrown(false, true), 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void withoutSkew() {
		double[][] parameters = new double[][]{{300, 200, 400, 400, 0.01, 0.02, -0.001, 0.002}, {400, 600, 1000, 1000, 0.01, 0.02, -0.001, 0.002}};
		new GenericChecksBundleAdjustmentCamera(new BundlePinholeBrown(true, true), 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}

	@Test
	void variousRadialLengths() {
		for (int i = 0; i <= 3; i++) {
			CameraPinholeBrown cam = new CameraPinholeBrown(i);
			cam.fx = 300; cam.fy = 200;
			cam.cx = cam.cy = 400;
			for (int j = 0; j < i; j++) {
				cam.radial[j] = 0.02 - j*0.001;
			}
			cam.t1 = -0.001;
			cam.t2 = 0.002;

			BundlePinholeBrown alg = BundleAdjustmentOps.convert(cam, (BundlePinholeBrown)null);
			double[][] parameters = new double[1][alg.getIntrinsicCount()];
			alg.getIntrinsic(parameters[0], 0);
			new GenericChecksBundleAdjustmentCamera(alg, 0.02) {}
					.setParameters(parameters)
//					.setPrint(true)
					.checkAll();
		}
	}

	@Test
	void zeroTangential() {
		CameraPinholeBrown cam = new CameraPinholeBrown(1);
		cam.fx = 300; cam.fy = 200;
		cam.cx = cam.cy = 400;
		cam.radial[0] = 0.02;
		// since t1 and t2 are zero it will automatically turn off tangential

		BundlePinholeBrown alg = BundleAdjustmentOps.convert(cam, (BundlePinholeBrown)null);
		double[][] parameters = new double[1][alg.getIntrinsicCount()];
		alg.getIntrinsic(parameters[0], 0);
		new GenericChecksBundleAdjustmentCamera(alg, 0.02) {}
				.setParameters(parameters)
				.checkAll();
	}
}
