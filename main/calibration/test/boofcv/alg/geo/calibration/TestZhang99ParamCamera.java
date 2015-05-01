/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestZhang99ParamCamera {
	@Test
	public void zeroNotUsed() {
		for (int i = 0; i < 4; i++) {
			boolean skew = i%2 == 0;
			boolean tangent = i/2 == 0;

			Zhang99ParamCamera param = new Zhang99ParamCamera(skew,2,tangent);
			param.a = param.b = param.c = param.x0 = param.y0 = 2;
			param.t1 = param.t2 = 3;

			param.zeroNotUsed();

			assertTrue(param.a != 0);
			assertTrue(param.b != 0);
			assertTrue(skew == (param.c == 0));
			assertTrue(param.x0 != 0);
			assertTrue(param.y0 != 0);

			assertTrue(tangent == (param.t1 != 0));
			assertTrue(tangent == (param.t2 != 0));
		}
	}

	@Test
	public void numParameters() {
		for (int i = 0; i < 4; i++) {
			boolean skew = i % 2 == 0;
			boolean tangent = i / 2 == 0;

			Zhang99ParamCamera param = new Zhang99ParamCamera(skew, 2, tangent);

			int expected = 6;
			if( !skew ) expected++;
			if( tangent ) expected += 2;

			assertEquals(expected, param.numParameters());
		}
	}

	@Test
	public void setFromParam_convertToParam() {
		for (int i = 0; i < 4; i++) {
			boolean skew = i % 2 == 0;
			boolean tangent = i / 2 == 0;

			Zhang99ParamCamera param = new Zhang99ParamCamera(skew, 2, tangent);

			double d[] = new double[ param.numParameters()];
			for (int j = 0; j < d.length; j++) {
				d[j] = j+1;
			}

			param.setFromParam(d);

			int c = 1;
			assertEquals(c++, param.a, 1e-8);
			assertEquals(c++, param.b, 1e-8);
			if( !skew )
				assertEquals(c++, param.c, 1e-8);
			else
				assertEquals(0, param.c, 1e-8);
			assertEquals(c++, param.x0, 1e-8);
			assertEquals(c++, param.y0, 1e-8);
			assertEquals(c++, param.radial[0], 1e-8);
			assertEquals(c++, param.radial[1], 1e-8);
			if (tangent) {
				assertEquals(c++, param.t1, 1e-8);
				assertEquals(c, param.t2, 1e-8);
			} else {
				assertEquals(0, param.t1, 1e-8);
				assertEquals(0, param.t2, 1e-8);
			}

			double e[] = new double[d.length];
			param.convertToParam(e);

			for (int j = 0; j < e.length; j++) {
				assertTrue(e[j]==d[j]);
			}
		}
	}
}
