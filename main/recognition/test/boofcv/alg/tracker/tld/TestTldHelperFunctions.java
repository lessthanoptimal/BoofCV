/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import boofcv.struct.ImageRectangle;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTldHelperFunctions {
	@Test
	public void convertRegion_F64_I32() {
		Rectangle2D_F64 a = new Rectangle2D_F64();
		Rectangle2D_I32 b = new Rectangle2D_I32();

		a.set(10,12,10.8,60.9);

		TldHelperFunctions.convertRegion(a,b);

		assertEquals(10,b.x0);
		assertEquals(12,b.y0);
		assertEquals(11,b.x1);
		assertEquals(61,b.y1);

	}

	@Test
	public void convertRegion_I32_F32() {
		Rectangle2D_F64 a = new Rectangle2D_F64();
		Rectangle2D_I32 b = new Rectangle2D_I32();

		b.set(10,12,11,61);

		TldHelperFunctions.convertRegion(b,a);

		assertEquals(10,a.p0.x,1e-8);
		assertEquals(12,a.p0.y,1e-8);
		assertEquals(11,a.p1.x,1e-8);
		assertEquals(61,a.p1.y,1e-8);
	}

	@Test
	public void computeOverlap() {
		TldHelperFunctions alg = new TldHelperFunctions();

		ImageRectangle a = new ImageRectangle(0,100,10,120);
		ImageRectangle b = new ImageRectangle(2,3,8,33);

		// no overlap
		assertEquals(0,alg.computeOverlap(a,b),1e-8);

		// non-zero overlap

		ImageRectangle c = new ImageRectangle(0,100,2,102);
		double expected = (4.0)/(200.0);
		assertEquals(expected,alg.computeOverlap(a,c),1e-8);
	}

}
