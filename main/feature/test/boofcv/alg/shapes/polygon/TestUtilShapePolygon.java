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

package boofcv.alg.shapes.polygon;

import georegression.geometry.UtilLine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestUtilShapePolygon {

	@Test
	public void plus() {
		assertEquals(3,UtilShapePolygon.plusPOffset(2, 1, 5));
		assertEquals(4,UtilShapePolygon.plusPOffset(2, 2, 5));
		assertEquals(0,UtilShapePolygon.plusPOffset(2, 3, 5));
		assertEquals(1,UtilShapePolygon.plusPOffset(2, 4, 5));
	}

	@Test
	public void add() {
		assertEquals(3,UtilShapePolygon.addOffset(2, 1, 5));
		assertEquals(4,UtilShapePolygon.addOffset(2, 2, 5));
		assertEquals(0,UtilShapePolygon.addOffset(2, 3, 5));
		assertEquals(1,UtilShapePolygon.addOffset(2, 4, 5));
		assertEquals(1,UtilShapePolygon.addOffset(2, -1, 5));
		assertEquals(0,UtilShapePolygon.addOffset(2, -2, 5));
		assertEquals(4,UtilShapePolygon.addOffset(2, -3, 5));
		assertEquals(3,UtilShapePolygon.addOffset(2, -4, 5));
	}

	@Test
	public void minus() {
		assertEquals(1,UtilShapePolygon.minusPOffset(2, 1, 5));
		assertEquals(0,UtilShapePolygon.minusPOffset(2, 2, 5));
		assertEquals(4,UtilShapePolygon.minusPOffset(2, 3, 5));
		assertEquals(3,UtilShapePolygon.minusPOffset(2, 4, 5));
	}

	@Test
	public void convert() {
		Polygon2D_F64 orig = new Polygon2D_F64(10,20,30,21,19.5,-10,8,-8);

		LineGeneral2D_F64[] lines = new LineGeneral2D_F64[4];
		lines[0] = UtilLine2D_F64.convert(orig.getLine(0, null), (LineGeneral2D_F64) null);
		lines[1] = UtilLine2D_F64.convert(orig.getLine(1,null),(LineGeneral2D_F64)null);
		lines[2] = UtilLine2D_F64.convert(orig.getLine(2,null),(LineGeneral2D_F64)null);
		lines[3] = UtilLine2D_F64.convert(orig.getLine(3,null),(LineGeneral2D_F64)null);

		Polygon2D_F64 found = new Polygon2D_F64(4);
		assertTrue(UtilShapePolygon.convert(lines, found));

		assertTrue(orig.isIdentical(found, 1e-8));
	}

	@Test
	public void dir() {
		fail("implement");
	}

}
