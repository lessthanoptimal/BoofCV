/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

public class TestSequencePointTransform_F64 extends BoofStandardJUnit {

	@Test void simpleTest() {

		Point2Transform2_F64 a = new Point2Transform2_F64() {
			@Override
			public void compute( double x, double y, Point2D_F64 out ) {
				out.x = x + 1;
				out.y = y + 2;
			}

			@Override
			public Point2Transform2_F64 copyConcurrent() {
				return null;
			}
		};

		SequencePoint2Transform2_F64 alg = new SequencePoint2Transform2_F64(a, a);

		Point2D_F64 p = new Point2D_F64();
		alg.compute(3, 4, p);

		assertEquals(5, p.x, 1e-8);
		assertEquals(8, p.y, 1e-8);
	}
}
