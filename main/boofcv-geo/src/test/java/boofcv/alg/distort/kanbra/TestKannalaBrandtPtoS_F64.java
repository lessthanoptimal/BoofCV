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

package boofcv.alg.distort.kanbra;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static boofcv.alg.distort.kanbra.KannalaBrandtUtils_F64.polynomial;
import static boofcv.alg.distort.kanbra.KannalaBrandtUtils_F64.polytrig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//CUSTOM ignore KannalaBrandtUtils_F64
//CUSTOM ignore DMatrixRMaj

/**
 * @author Peter Abeles
 */
class TestKannalaBrandtPtoS_F64 extends BoofStandardJUnit {
	/**
	 * Given spherical coordinates, compute pixel coordinates and see if we can invert them correctly.
	 */
	@Test void simpleSanityCheck_SymmetricOnly() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650);
		model.fsetSymmetric(1.0, 0.1);

		var expected = new Point3D_F64(0.1, -0.05, 0.8);
		var pixel = new Point2D_F64();
		var found = new Point3D_F64();

		new KannalaBrandtStoP_F64(model).compute(expected.x, expected.y, expected.z, pixel);
		new KannalaBrandtPtoS_F64(model).compute(pixel.x, pixel.y, found);

		// make sure both have them have a norm of 1
		expected.divideIP(expected.norm());
		found.divideIP(found.norm());

		// This should be very accurate. The inaccurate part isn't being called
		assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F64);
	}

	/**
	 * The entire motion model will be exercised here
	 */
	@Test void simpleSanityCheck_Everything() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06, 0.12).
				fsetRadialTrig(0.01, 0.02, -0.03, 0.04).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		var expected = new Point3D_F64(0.1, -0.12, 0.8);
		var pixel = new Point2D_F64();
		var found = new Point3D_F64();

		new KannalaBrandtStoP_F64(model).compute(expected.x, expected.y, expected.z, pixel);
		new KannalaBrandtPtoS_F64(model).compute(pixel.x, pixel.y, found);

		// make sure both have them have a norm of 1
		expected.divideIP(expected.norm());
		found.divideIP(found.norm());

		// The paper says this will be noisy. Using Newton's method seems to be much more accurate
		assertEquals(0.0, expected.distance(found), 1e-4);
	}

	/**
	 * Compare to numerical Jacobian
	 */
	@Test void jacobianOfDistorted() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06, 0.12).
				fsetRadialTrig(0.01, 0.03, -0.03, 0.04).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		FunctionNtoM function = new FunctionNtoM() {
			@Override public void process( /**/double[] input, /**/double[] output ) {
				double theta = (double) input[0];
				double psi = (double) input[1];

				double r = (double) polynomial(model.symmetric, theta);

				double cospsi = Math.cos(psi);
				double sinpsi = Math.sin(psi);

				// distortion terms. radial and tangential
				double dr = (double) (polynomial(model.radial, theta)*polytrig(model.radialTrig, cospsi, sinpsi));
				double dt = (double) (polynomial(model.tangent, theta)*polytrig(model.tangentTrig, cospsi, sinpsi));

				// put it all together to get normalized image coordinates
				output[0] = (r + dr)*cospsi - dt*sinpsi;
				output[1] = (r + dr)*sinpsi + dt*cospsi;
			}

			@Override public int getNumOfInputsN() {return 2;}

			@Override public int getNumOfOutputsM() {return 2;}
		};

		var kb = new KannalaBrandtPtoS_F64(model);
		FunctionNtoMxN<DMatrixRMaj> jacobian = new FunctionNtoMxN<>() {
			final DMatrix2x2 a = new DMatrix2x2();

			@Override public int getNumOfInputsN() {return 2;}

			@Override public int getNumOfOutputsM() {return 2;}

			@Override public DMatrixRMaj declareMatrixMxN() {return new DMatrixRMaj(2,2);}

			@Override public void process( /**/double[] input, DMatrixRMaj output ) {
				double theta = (double) input[0];
				double psi = (double) input[1];

				double cospsi = Math.cos(psi);
				double sinpsi = Math.sin(psi);

				kb.jacobianOfDistorted(theta, cospsi, sinpsi, a);
				BoofMiscOps.convertMatrix(a, output);
			}
		};

//		DerivativeChecker.jacobianPrint(function, jacobian, new double[]{0.2, -0.4}, 1e-4);
		assertTrue(DerivativeChecker.jacobian(function, jacobian, new /**/double[]{0.2, -0.4},
				UtilEjml.TEST_F64_SQ, Math.sqrt(UtilEjml.EPS)));
	}
}
