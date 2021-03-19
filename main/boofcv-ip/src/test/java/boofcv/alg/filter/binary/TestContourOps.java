/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestContourOps extends BoofStandardJUnit {
	@Test void isEquivalent() {
		var contourA = new ArrayList<Point2D_I32>();
		var contourB = new ArrayList<Point2D_I32>();

		assertTrue(ContourOps.isEquivalent(contourA, contourB));

		contourA.add(new Point2D_I32(1, 2));
		assertFalse(ContourOps.isEquivalent(contourA, contourB));

		contourB.add(new Point2D_I32(1, 2));
		assertTrue(ContourOps.isEquivalent(contourA, contourB));

		contourA.add(new Point2D_I32(2, 4));
		contourB.add(new Point2D_I32(2, 4));
		assertTrue(ContourOps.isEquivalent(contourA, contourB));

		contourA.add(new Point2D_I32(3, 4));
		contourB.add(new Point2D_I32(3, 4));
		assertTrue(ContourOps.isEquivalent(contourA, contourB));

		// see if it's can handle a shift
		contourB.add(0, contourB.remove(2));
		assertTrue(ContourOps.isEquivalent(contourA, contourB));
	}

	@Test void isTouchBorder() {
		var contour = new ArrayList<Point2D_I32>();

		assertFalse(ContourOps.isTouchBorder(contour, 20, 10));

		var p = new Point2D_I32();
		contour.add(p);
		assertTrue(ContourOps.isTouchBorder(contour, 20, 10));
		p.x = 1;
		assertTrue(ContourOps.isTouchBorder(contour, 20, 10));
		p.y = 1;
		assertFalse(ContourOps.isTouchBorder(contour, 20, 10));
		p.x = 19;
		assertTrue(ContourOps.isTouchBorder(contour, 20, 10));
		p.x = 9;
		assertFalse(ContourOps.isTouchBorder(contour, 20, 10));
		p.y = 9;
		assertTrue(ContourOps.isTouchBorder(contour, 20, 10));
	}
}
