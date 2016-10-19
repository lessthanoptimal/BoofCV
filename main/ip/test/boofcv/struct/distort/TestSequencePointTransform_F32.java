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

package boofcv.struct.distort;

import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSequencePointTransform_F32 {

	@Test
	public void simpleTest() {

		Point2Transform2_F32 a = new Point2Transform2_F32() {
			@Override
			public void compute(float x, float y, Point2D_F32 out) {
				out.x = x+1;
				out.y = y+2;
			}
		};

		SequencePoint2Transform2_F32 alg = new SequencePoint2Transform2_F32(a,a);

		Point2D_F32 p = new Point2D_F32();
		alg.compute(3,4,p);

		assertEquals(5,p.x,1e-8);
		assertEquals(8,p.y,1e-8);
	}
}
