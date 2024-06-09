/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.packed;

import boofcv.struct.PackedArray;
import georegression.struct.point.Point2D_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestPackedArrayPoint2D_I32 extends GenericPackedArrayChecks<Point2D_I32> {

	@Override protected PackedArray<Point2D_I32> createAlg() {
		return new PackedArrayPoint2D_I32();
	}

	@Override protected Point2D_I32 createRandomPoint() {
		var point = new Point2D_I32();
		point.x = rand.nextInt(100) - 50;
		point.y = rand.nextInt(100) - 50;
		return point;
	}

	@Override protected void checkEquals( Point2D_I32 a, Point2D_I32 b ) {
		assertEquals(0.0, a.distance(b));
	}

	@Override protected void checkNotEquals( Point2D_I32 a, Point2D_I32 b ) {
		assertNotEquals(0.0, a.distance(b));
	}

	@Test void appendValues() {
		var alg = new PackedArrayPoint2D_I32();
		assertEquals(0, alg.size());
		alg.append(1,2);

		assertEquals(1, alg.size());

		var p = alg.getTemp(0);
		assertEquals(1, p.x);
		assertEquals(2, p.y);
	}
}
