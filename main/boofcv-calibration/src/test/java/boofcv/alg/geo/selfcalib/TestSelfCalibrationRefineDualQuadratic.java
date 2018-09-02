/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import org.ejml.ops.ConvertDMatrixStruct;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationRefineDualQuadratic extends CommonAutoCalibrationChecks
{
	@Test
	public void solvePerfect() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new CameraPinhole(400+i*5,420,0.1,410,420,0,0));

			found.add(new CameraPinhole(expected.get(i)));
		}

		renderGood(expected);

		SelfCalibrationRefineDualQuadratic alg = new SelfCalibrationRefineDualQuadratic();

		addProjectives(alg);

		DMatrix4x4 Q = new DMatrix4x4();
		ConvertDMatrixStruct.convert(this.Q,Q);

		assertTrue(alg.refine(found,Q));

		for (int i = 0; i < expected.size(); i++) {
			CameraPinhole e = expected.get(i);
			CameraPinhole f = found.get(i);

			assertEquals(e.fx,f.fx, UtilEjml.TEST_F64);
			assertEquals(e.fy,f.fy, UtilEjml.TEST_F64);
			assertEquals(e.cx,f.cx, UtilEjml.TEST_F64);
			assertEquals(e.cy,f.cy, UtilEjml.TEST_F64);
			assertEquals(e.skew,f.skew, UtilEjml.TEST_F64);
		}
	}

	@Test
	public void solvePerfectNoise() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new CameraPinhole(400+i*5,420,0.1,410,420,0,0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 2*rand.nextGaussian();
			found.get(i).fy += 2*rand.nextGaussian();
			found.get(i).cx += 2*rand.nextGaussian();
			found.get(i).cy += 2*rand.nextGaussian();

		}

		renderGood(expected);

		SelfCalibrationRefineDualQuadratic alg = new SelfCalibrationRefineDualQuadratic();

		addProjectives(alg);

		DMatrix4x4 Q = new DMatrix4x4();
		ConvertDMatrixStruct.convert(this.Q,Q);

		assertTrue(alg.refine(found,Q));

		// estimate gets worse
		for (int i = 0; i < expected.size(); i++) {
			CameraPinhole e = expected.get(i);
			CameraPinhole f = found.get(i);

			assertEquals(e.fx,f.fx, 5);
			assertEquals(e.fy,f.fy, 5);
			assertEquals(e.cx,f.cx, 5);
			assertEquals(e.cy,f.cy, 5);
			assertEquals(e.skew,f.skew, 5);
		}
	}
}