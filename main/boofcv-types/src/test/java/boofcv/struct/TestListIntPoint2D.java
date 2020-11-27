/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestListIntPoint2D extends BoofStandardJUnit {

	@Test
	void configure() {
		var alg = new ListIntPoint2D();
		alg.configure(100, 105);
		assertEquals(100,alg.imageWidth);
		alg.add(10,20);
		assertEquals(1,alg.size());
		alg.configure(67, 105);
		assertEquals(0,alg.size());
		assertEquals(67,alg.imageWidth);
	}

	@Test
	void add() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);
		alg.add(16,2);
		assertEquals(2,alg.size());
		assertEquals(20*100+10,alg.points.get(0));
		assertEquals(2*100+16,alg.points.get(1));
	}

	@Test
	void get_U16() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);

		var p = new Point2D_I16();
		alg.get(0,p);
		assertEquals(10,p.x);
		assertEquals(20,p.y);
	}

	@Test
	void get_S32() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);

		var p = new Point2D_I32();
		alg.get(0,p);
		assertEquals(10,p.x);
		assertEquals(20,p.y);
	}

	@Test
	void get_F32() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);

		var p = new Point2D_F32();
		alg.get(0,p);
		assertEquals(10, p.x, UtilEjml.TEST_F32);
		assertEquals(20, p.y, UtilEjml.TEST_F32);
	}

	@Test
	void get_F64() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);

		var p = new Point2D_F64();
		alg.get(0,p);
		assertEquals(10, p.x, UtilEjml.TEST_F32);
		assertEquals(20, p.y, UtilEjml.TEST_F32);
	}

	@Test
	void copyInto() {
		var alg = new ListIntPoint2D();
		alg.configure(100,105);
		alg.add(10,20);
		alg.add(16,2);

		var list = new DogArray<>(Point2D_I16::new);
		alg.copyInto(list);

		assertEquals(2,list.size());
		assertEquals(10,list.get(0).x);
		assertEquals(20,list.get(0).y);
		assertEquals(16,list.get(1).x);
		assertEquals(2,list.get(1).y);
	}
}
