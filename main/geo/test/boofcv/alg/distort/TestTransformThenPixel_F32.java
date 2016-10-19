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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTransformThenPixel_F32 {
	@Test
	public void set() {
		Transform2ThenPixel_F32 alg = new Transform2ThenPixel_F32(null);

		assertTrue(alg == alg.set(1, 2, 3, 4, 5));
		assertEquals(1,alg.fx,1e-8);
		assertEquals(2,alg.fy,1e-8);
		assertEquals(3,alg.skew,1e-8);
		assertEquals(4,alg.cx,1e-8);
		assertEquals(5,alg.cy,1e-8);
		
	}

	@Test
	public void compute() {
		Transform2ThenPixel_F32 alg = new Transform2ThenPixel_F32(new Dummy());
		alg.set(1, 2, 3, 4, 5);

		float nx = 0.1f, ny = 0.2f;

		float expectedX = 1*nx + 3*ny + 4;
		float expectedY = 2*ny + 5;

		Point2D_F32 found = new Point2D_F32();
		alg.compute(1,2,found);

		assertEquals(expectedX, found.x, 1e-8);
		assertEquals(expectedY, found.y, 1e-8);
	}

	protected static class Dummy implements Point2Transform2_F32 {

		@Override
		public void compute(float x, float y, Point2D_F32 out) {
			assertEquals(1,x,1e-8);
			assertEquals(2,y,1e-8);
			out.x = 0.1f;
			out.y = 0.2f;
		}
	}
}