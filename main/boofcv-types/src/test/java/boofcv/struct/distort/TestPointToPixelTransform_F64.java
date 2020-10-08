/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.distort;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPointToPixelTransform_F64 extends BoofStandardJUnit {

	@Test
	void manual() {
		Dummy p = new Dummy();
		PointToPixelTransform_F64 alg = new PointToPixelTransform_F64(p);

		Point2D_F64 distorted = new Point2D_F64();
		alg.compute(1, 2, distorted);
		Point2D_F64 expected = new Point2D_F64();
		p.compute(1, 2, expected);

		assertEquals(expected.x, distorted.x, 1e-6);
		assertEquals(expected.y, distorted.y, 1e-6);
	}

	private static class Dummy implements Point2Transform2_F64 {

		@Override
		public void compute( double x, double y, Point2D_F64 out ) {
			out.x = x + 0.1;
			out.y = y + 0.2;
		}

		@Override
		public Point2Transform2_F64 copyConcurrent() {
			return null;
		}
	}
}
