/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSphereToNarrowPixel_F64 {
	@Test
	public void basic() {

		SphereToNarrowPixel_F64 alg = new SphereToNarrowPixel_F64(new Dummy());

		Point2D_F64 found = new Point2D_F64();
		alg.compute(1,2,3, found);

		assertEquals(1.0/3.0,found.x, UtilEjml.TEST_F64);
		assertEquals(2.0/3.0,found.y, UtilEjml.TEST_F64);
	}

	private static class Dummy implements Point2Transform2_F64 {

		@Override
		public void compute(double x, double y, Point2D_F64 out) {
			out.set(x,y);
		}
	}
}