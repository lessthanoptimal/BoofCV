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

package boofcv.struct.packed;

import boofcv.struct.PackedArray;
import georegression.struct.point.Point2D_I16;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestPackedArrayPoint2D_I16 extends GenericPackedArrayChecks<Point2D_I16> {

	@Override protected PackedArray<Point2D_I16> createAlg() {
		return new PackedArrayPoint2D_I16();
	}

	@Override protected Point2D_I16 createRandomPoint() {
		var point = new Point2D_I16();
		point.x = (short)(rand.nextInt(100) - 50);
		point.y = (short)(rand.nextInt(100) - 50);
		return point;
	}

	@Override protected void checkEquals( Point2D_I16 a, Point2D_I16 b ) {
		assertEquals(0.0, a.distance(b));
	}

	@Override protected void checkNotEquals( Point2D_I16 a, Point2D_I16 b ) {
		assertNotEquals(0.0, a.distance(b));
	}
}
