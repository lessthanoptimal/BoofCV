/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPointGradient_F64 {
	@Test
	public void set_object() {
		PointGradient_F64 a = new PointGradient_F64(1,2,3,4);
		PointGradient_F64 b = new PointGradient_F64();
		b.set(a);

		assertEquals(a.x,b.x,1e-8);
		assertEquals(a.y,b.y,1e-8);
		assertEquals(a.dx,b.dx,1e-8);
		assertEquals(a.dy,b.dy,1e-8);
	}

	@Test
	public void set_values() {
		PointGradient_F64 b = new PointGradient_F64();
		b.set(1,2,3,4);

		assertEquals(1,b.x,1e-8);
		assertEquals(2,b.y,1e-8);
		assertEquals(3,b.dx,1e-8);
		assertEquals(4,b.dy,1e-8);
	}

	@Test
	public void copy() {
		PointGradient_F64 a = new PointGradient_F64(1,2,3,4);
		PointGradient_F64 b = a.copy();

		assertEquals(a.x,b.x,1e-8);
		assertEquals(a.y,b.y,1e-8);
		assertEquals(a.dx,b.dx,1e-8);
		assertEquals(a.dy,b.dy,1e-8);
	}
}
