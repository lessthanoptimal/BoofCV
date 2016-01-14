/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestZhang99DecomposeHomography {

	/**
	 * Test against a simple known case
	 */
	@Test
	public void knownCase() {
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.02, -0.05, 0.01, null);
		Vector3D_F64 T = new Vector3D_F64(100,50,-1000);
		DenseMatrix64F K = GenericCalibrationGrid.createStandardCalibration();
		DenseMatrix64F H = GenericCalibrationGrid.computeHomography(K,R,T);

		Zhang99DecomposeHomography alg = new Zhang99DecomposeHomography();
		alg.setCalibrationMatrix(K);
		Se3_F64 motion = alg.decompose(H);

		assertTrue(MatrixFeatures.isIdentical(R, motion.getR(), 1e-5));
		assertEquals(T.x,motion.getX(), 1e-5);
	}
}
