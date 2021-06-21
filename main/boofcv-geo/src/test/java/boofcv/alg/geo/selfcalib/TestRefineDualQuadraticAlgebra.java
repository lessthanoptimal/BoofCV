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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.calib.CameraPinhole;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix4x4;
import org.ejml.ops.DConvertMatrixStruct;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineDualQuadraticAlgebra extends CommonAutoCalibrationChecks {
	@Test void solvePerfect() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.1, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));
		}

		RefineDualQuadraticAlgebra alg = new RefineDualQuadraticAlgebra();
		checkRefine(alg, expected, found, UtilEjml.TEST_F64);
	}

	@Test void solveNoise() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.1, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 1.5*rand.nextGaussian();
			found.get(i).fy += 1.5*rand.nextGaussian();
			found.get(i).cx += 1.5*rand.nextGaussian();
			found.get(i).cy += 1.5*rand.nextGaussian();
		}

		RefineDualQuadraticAlgebra alg = new RefineDualQuadraticAlgebra();
		checkRefine(alg, expected, found, 5);
	}

	@Test void solveNoise_ZeroSkew() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 2*rand.nextGaussian();
			found.get(i).fy += 2*rand.nextGaussian();
			found.get(i).cx += 2*rand.nextGaussian();
			found.get(i).cy += 2*rand.nextGaussian();
		}

		RefineDualQuadraticAlgebra alg = new RefineDualQuadraticAlgebra();
		alg.setZeroSkew(true);
		checkRefine(alg, expected, found, 6);
	}

	@Test void solveNoise_ZeroPrinciplePoint() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.1, 0, 0, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 1*rand.nextGaussian();
			found.get(i).fy += 1*rand.nextGaussian();
		}

		RefineDualQuadraticAlgebra alg = new RefineDualQuadraticAlgebra();
		alg.setZeroPrinciplePoint(true);
		checkRefine(alg, expected, found, 6);
	}

	@Test void solveFixedAspect() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.1, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 2*rand.nextGaussian();
			found.get(i).fy += 2*rand.nextGaussian();
			found.get(i).cx += 2*rand.nextGaussian();
			found.get(i).cy += 2*rand.nextGaussian();
		}

		RefineDualQuadraticAlgebra alg = new RefineDualQuadraticAlgebra();
		alg.setFixedAspectRatio(true);
		checkRefine(alg, expected, found, 6);
	}

	private void checkRefine( RefineDualQuadraticAlgebra alg,
							  List<CameraPinhole> expected, List<CameraPinhole> found, double tol ) {
		renderGood(expected);

		addProjectives(alg);

		DMatrix4x4 Q = new DMatrix4x4();
		DConvertMatrixStruct.convert(this.Q, Q);

		double[] ratio = new double[expected.size()];
		for (int i = 0; i < found.size(); i++) {
			CameraPinhole p = found.get(i);
			ratio[i] = p.fy/p.fx;
		}

		assertTrue(alg.refine(found, Q));

		// estimate gets worse
		for (int i = 0; i < expected.size(); i++) {
			CameraPinhole e = expected.get(i);
			CameraPinhole f = found.get(i);

			if (alg.isFixedAspectRatio()) {
				assertEquals(e.fx, f.fx, tol);
				assertEquals(f.fx*ratio[i], f.fy, UtilEjml.TEST_F64_SQ);
			} else {
				assertEquals(e.fx, f.fx, tol);
				assertEquals(e.fy, f.fy, tol);
			}
			if (alg.isZeroPrinciplePoint()) {
				assertEquals(0, f.cx, UtilEjml.TEST_F64_SQ);
				assertEquals(0, f.cy, UtilEjml.TEST_F64_SQ);
			} else {
				assertEquals(e.cx, f.cx, tol);
				assertEquals(e.cy, f.cy, tol);
			}
			if (alg.isZeroSkew()) {
				assertEquals(0, f.skew, UtilEjml.TEST_F64_SQ);
			} else {
				assertEquals(e.skew, f.skew, tol);
			}
		}
	}
}
