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

package boofcv.alg.geo.bundle.cameras;

import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.ejml.UtilEjml.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBundleKannalaBrandt extends BoofStandardJUnit {
	/**
	 * Makes sure parameterization is consistent
	 */
	@Test void encode_decode() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.01, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06).
				fsetRadialTrig(0.01, 0.02, -0.03, 0.12).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		BundleKannalaBrandt alg = new BundleKannalaBrandt(model);

		// Get the parameters
		double[] parameters = new double[alg.getIntrinsicCount()+4];
		alg.getIntrinsic(parameters, 2);

		// Discard the old and encode. See if it has the expected results
		alg = new BundleKannalaBrandt();
		alg.configure(false,2,3);
		alg.setIntrinsic(parameters, 2);

		assertTrue(model.isIdentical(alg.model));
	}

	/**
	 * Compare the forward distortion to the lens distortion model
	 */
	@Test void compareForward() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.1, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06).
				fsetRadialTrig(0.01, 0.02, -0.03, 0.12).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		BundleKannalaBrandt alg = new BundleKannalaBrandt(model);
		Point3Transform2_F64 n2p = LensDistortionFactory.wide(model).distortStoP_F64();

		Point2D_F64 found = new Point2D_F64();

		double X = 0.1, Y = -0.2, Z = 2;
		alg.project(X, Y, Z, found);

		Point2D_F64 expected = new Point2D_F64();
		// convert to unit sphere
		double n = Math.sqrt(X*X + Y*Y + Z*Z);
		n2p.compute(X/n, Y/n, Z/n, expected);

		Assertions.assertTrue(found.distance(expected) < UtilEjml.TEST_F64);
	}

	/**
	 * Check the Jacobian with all parameters
	 */
	@Test void jacobian_all() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.1, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06).
				fsetRadialTrig(0.01, 0.02, -0.03, 0.12).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);
		BundleKannalaBrandt alg = new BundleKannalaBrandt(model);
		double[] parameters = new double[alg.getIntrinsicCount()];
		alg.getIntrinsic(parameters, 0);

		new GenericChecksBundleAdjustmentCamera(alg, 0.01) {}
//				.setPrint(true)
				.setParameters(new double[][]{parameters})
				.checkAll();
	}

	/**
	 * Check the Jacobian when skew is set to zero
	 */
	@Test void jacobian_ZeroSkew() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.05).fsetTangent(0.5, -0.1, 0.06).
				fsetRadialTrig(0.01, 0.02, -0.03, 0.12).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);
		BundleKannalaBrandt alg = new BundleKannalaBrandt(model);
		assertTrue(alg.isZeroSkew()); // sanity check
		double[] parameters = new double[alg.getIntrinsicCount()];
		alg.getIntrinsic(parameters, 0);

		new GenericChecksBundleAdjustmentCamera(alg, 0.01) {}
//				.setPrint(true)
				.setParameters(new double[][]{parameters})
				.checkAll();
	}

	/**
	 * Check the Jacobian with only radial parameters
	 */
	@Test void jacobian_RadialOnly() {
		double[][] parameters = new double[][]{{300, 200, 400, 405, 0.04, 0.02, -0.01}};
		BundleKannalaBrandt alg = new BundleKannalaBrandt();
		alg.configure(false, 2, 0);
		new GenericChecksBundleAdjustmentCamera(alg, 0.01) {}
//				.setPrint(true)
				.setParameters(parameters)
				.checkAll();
	}
}
