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

package boofcv.alg.geo.bundle.jacobians;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBundleCameraNumericJacobian extends BoofStandardJUnit {
	@Test void jacobianIntrinsics() {
		BundleCameraNumericJacobian alg = new BundleCameraNumericJacobian();
		double[] params = new double[]{2.1, 3.1};
		SimpleCamera camera = new SimpleCamera();
		camera.setIntrinsic(params, 0);

		double X = 1.5, Y = 2.0, Z = 2.5;
		double[] calibX = new double[2];
		double[] calibY = new double[2];

		alg.setModel(camera);
		alg.jacobianIntrinsics(X, Y, Z, calibX, calibY);

		double[] expectedX = new double[2];
		double[] expectedY = new double[2];
		camera.jacobian(X, Y, Z, new double[3], new double[3], true, expectedX, expectedY);

		for (int i = 0; i < 2; i++) {
			assertEquals(expectedX[i], calibX[i], UtilEjml.TEST_F64);
			assertEquals(expectedY[i], calibY[i], UtilEjml.TEST_F64);
		}
	}

	@Test void jacobianPoint() {
		BundleCameraNumericJacobian alg = new BundleCameraNumericJacobian();
		double[] params = new double[]{2.1, 3.1};
		SimpleCamera camera = new SimpleCamera();
		camera.setIntrinsic(params, 0);

		double X = 1.5, Y = 2.0, Z = 2.5;
		double[] pointX = new double[3];
		double[] pointY = new double[3];

		alg.setModel(camera);
		alg.jacobianPoint(X, Y, Z, pointX, pointY);

		double[] expectedX = new double[3];
		double[] expectedY = new double[3];
		camera.jacobian(X, Y, Z, expectedX, expectedY, false, null, null);

		for (int i = 0; i < 3; i++) {
			assertEquals(expectedX[i], pointX[i], 10*UtilEjml.TEST_F64);
			assertEquals(expectedY[i], pointY[i], 10*UtilEjml.TEST_F64);
		}
	}

	public static class SimpleCamera implements BundleAdjustmentCamera {

		double a, b;

		@Override
		public void setIntrinsic( double[] parameters, int offset ) {
			a = parameters[offset];
			b = parameters[offset + 1];
		}

		@Override
		public void getIntrinsic( double[] parameters, int offset ) {
			parameters[offset] = a;
			parameters[offset + 1] = b;
		}

		@Override
		public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
			output.x = (camX + a)/camZ;
			output.y = (camY + b)/camZ;
		}

		@Override
		public void jacobian( double camX, double camY, double camZ,
							  double[] pointX, double[] pointY, boolean computeIntrinsic,
							  @Nullable double[] calibX, @Nullable double[] calibY ) {
			pointX[0] = 1.0/camZ;
			pointX[1] = 0;
			pointX[2] = -(camX + a)/(camZ*camZ);
			pointY[0] = 0;
			pointY[1] = 1.0/camZ;
			pointY[2] = -(camY + b)/(camZ*camZ);

			if (!computeIntrinsic)
				return;

			calibX[0] = 1.0/camZ;
			calibX[1] = 0;
			calibY[0] = 0;
			calibY[1] = 1.0/camZ;
		}

		@Override
		public int getIntrinsicCount() {
			return 2;
		}
	}
}
