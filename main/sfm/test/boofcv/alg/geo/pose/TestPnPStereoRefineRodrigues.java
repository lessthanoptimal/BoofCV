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

package boofcv.alg.geo.pose;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPStereoRefineRodrigues extends CommonStereoMotionNPoint {

	/**
	 * Perfect input.  Shouldn't change anything
	 */
	@Test
	public void perfect() {
		generateScene(10,null,false);

		PnPStereoRefineRodrigues alg = new PnPStereoRefineRodrigues(1e-12,200);
		alg.setLeftToRight(leftToRight);


		Se3_F64 found = new Se3_F64();
		assertTrue(alg.fitModel(pointPose, worldToLeft.copy(), found));
		assertTrue(alg.minimizer.getFunctionValue() < 1e-10);

		assertTrue(MatrixFeatures.isIdentical(worldToLeft.getR(), found.getR(), 1e-8));
		assertTrue(found.getT().isIdentical(worldToLeft.getT(), 1e-8));
	}

	/**
	 * Noisy input.  Should generate something close to the actual solution
	 */
	@Test
	public void noisy() {
		generateScene(30,null,false);

		PnPStereoRefineRodrigues alg = new PnPStereoRefineRodrigues(1e-12,200);
		alg.setLeftToRight(leftToRight);


		Se3_F64 input = worldToLeft.copy();
		// noise up the initial guess
		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, -0.04, -0.2, null);
		CommonOps.mult(R,input.getR().copy(),input.getR());
		input.T.x += 0.2;
		input.T.x -= 0.05;
		input.T.z += 0.03;

		Se3_F64 found = new Se3_F64();
		assertTrue(alg.fitModel(pointPose, input, found));
		assertTrue(alg.minimizer.getFunctionValue()<1e-12);

		assertTrue(MatrixFeatures.isIdentical(worldToLeft.getR(), found.getR(), 1e-8));
		assertTrue(found.getT().isIdentical(worldToLeft.getT(), 1e-8));
	}
}
