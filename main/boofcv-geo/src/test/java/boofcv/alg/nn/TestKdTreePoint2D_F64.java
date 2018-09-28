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

package boofcv.alg.nn;

import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestKdTreePoint2D_F64 {
	KdTreePoint2D_F64 alg = new KdTreePoint2D_F64();

	@Test
	public void distance() {
		Point2D_F64 a = new Point2D_F64(1,2);
		Point2D_F64 b = new Point2D_F64(-2,9);

		assertEquals(a.distance2(b),alg.distance(a,b), UtilEjml.TEST_F64);
	}

	@Test
	public void valueAt() {
		Point2D_F64 b = new Point2D_F64(-2,2);

		assertEquals(b.x, alg.valueAt(b,0),UtilEjml.TEST_F64);
		assertEquals(b.y, alg.valueAt(b,1),UtilEjml.TEST_F64);
	}

	@Test
	public void length() {
		assertEquals(2,alg.length());
	}
}