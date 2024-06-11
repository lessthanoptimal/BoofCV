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
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.BigDogGrowth;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestPackedBigArrayPoint2D_F64 extends GenericPackedArrayChecks<Point2D_F64> {

	@Override protected PackedArray<Point2D_F64> createAlg() {
		return new PackedBigArrayPoint2D_F64(3, 11, BigDogGrowth.GROW_FIRST);
	}

	@Override protected Point2D_F64 createRandomPoint() {
		var point = new Point2D_F64();
		point.x = (double)rand.nextGaussian();
		point.y = (double)rand.nextGaussian();
		return point;
	}

	@Override protected void checkEquals( Point2D_F64 a, Point2D_F64 b ) {
		assertEquals(0.0, a.distance(b), UtilEjml.TEST_F64);
	}

	@Override protected void checkNotEquals( Point2D_F64 a, Point2D_F64 b ) {
		assertNotEquals(0.0, a.distance(b), UtilEjml.TEST_F64);
	}

	@Test void appendValues() {
		var alg = new PackedBigArrayPoint2D_F64();
		assertEquals(0, alg.size());
		alg.append(1, 2);

		assertEquals(1, alg.size());

		var p = alg.getTemp(0);
		assertEquals(1.0, p.x);
		assertEquals(2.0, p.y);
	}

	@Test void setTo() {
		var src = new PackedBigArrayPoint2D_F64();
		src.append(1, 2);
		src.append(2, 3);

		var dst = new PackedBigArrayPoint2D_F64();
		dst.append(4, 5);

		dst.setTo(src);
		assertEquals(2, dst.size());
		src.forIdx(0, 2, ( idx, a ) -> assertEquals(0.0, a.distance(dst.getTemp(idx))));
	}
}
