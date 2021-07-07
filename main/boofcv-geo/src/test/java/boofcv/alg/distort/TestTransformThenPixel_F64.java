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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
public class TestTransformThenPixel_F64 extends BoofStandardJUnit {
	@Test void set() {
		Transform2ThenPixel_F64 alg = new Transform2ThenPixel_F64(null);

		assertSame(alg, alg.set(1, 2, 3, 4, 5));
		assertEquals(1,alg.fx,1e-8);
		assertEquals(2,alg.fy,1e-8);
		assertEquals(3,alg.skew,1e-8);
		assertEquals(4,alg.cx,1e-8);
		assertEquals(5,alg.cy,1e-8);
		
	}

	@Test void compute() {
		Transform2ThenPixel_F64 alg = new Transform2ThenPixel_F64(new Dummy());
		alg.set(1, 2, 3, 4, 5);

		double nx = 0.1, ny = 0.2;

		double expectedX = 1*nx + 3*ny + 4;
		double expectedY = 2*ny + 5;

		Point2D_F64 found = new Point2D_F64();
		alg.compute(1,2,found);

		assertEquals(expectedX, found.x, 1e-8);
		assertEquals(expectedY, found.y, 1e-8);
	}

	protected static class Dummy implements Point2Transform2_F64 {

		@Override
		public void compute(double x, double y, Point2D_F64 out) {
			assertEquals(1,x,1e-8);
			assertEquals(2,y,1e-8);
			out.x = 0.1;
			out.y = 0.2;
		}

		@Override
		public Point2Transform2_F64 copyConcurrent() {
			return null;
		}
	}
}
