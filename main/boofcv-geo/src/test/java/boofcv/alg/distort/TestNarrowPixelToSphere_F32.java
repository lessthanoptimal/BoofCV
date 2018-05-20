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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNarrowPixelToSphere_F32 {
	@Test
	public void basic() {

		NarrowPixelToSphere_F32 alg = new NarrowPixelToSphere_F32(new Dummy());

		Point3D_F32 found = new Point3D_F32();
		alg.compute(100,120, found);

		assertEquals(1.0f,found.norm(), UtilEjml.TEST_F32);
		assertEquals(100,found.x/found.z, UtilEjml.TEST_F32);
		assertEquals(120,found.y/found.z, UtilEjml.TEST_F32);
	}

	private static class Dummy implements Point2Transform2_F32 {

		@Override
		public void compute(float x, float y, Point2D_F32 out) {
			out.set(x,y);
		}
	}
}