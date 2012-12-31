/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestAssociatedTriple {

	@Test
	public void constructor_ThreePts() {
		Point2D_F64 p1 = new Point2D_F64(1,2);
		Point2D_F64 p2 = new Point2D_F64(3,4);
		Point2D_F64 p3 = new Point2D_F64(5,6);

		AssociatedTriple a = new AssociatedTriple(p1,p2,p3);

		// make sure it isn't saving the reference
		assertTrue(p1 != a.p1);
		assertTrue(p2 != a.p2);
		assertTrue(p3 != a.p3);

		// see if they have the same value
		assertTrue(p1.isIdentical(a.p1,1e-8));
		assertTrue(p2.isIdentical(a.p2,1e-8));
		assertTrue(p3.isIdentical(a.p3,1e-8));
	}

	@Test
	public void constructor_ThreePts_flag() {
		Point2D_F64 p1 = new Point2D_F64(1,2);
		Point2D_F64 p2 = new Point2D_F64(3,4);
		Point2D_F64 p3 = new Point2D_F64(5,6);

		AssociatedTriple a = new AssociatedTriple(p1,p2,p3,false);

		// make sure it is saving the reference
		assertTrue(p1 == a.p1);
		assertTrue(p2 == a.p2);
		assertTrue(p3 == a.p3);

		a = new AssociatedTriple(p1,p2,p3,true);
		// make sure it isn't saving the reference
		assertTrue(p1 != a.p1);
		assertTrue(p2 != a.p2);
		assertTrue(p3 != a.p3);

		// see if they have the same value
		assertTrue(p1.isIdentical(a.p1,1e-8));
		assertTrue(p2.isIdentical(a.p2,1e-8));
		assertTrue(p3.isIdentical(a.p3,1e-8));
	}

	@Test
	public void set_AssociatedTriple() {
		Point2D_F64 p1 = new Point2D_F64(1,2);
		Point2D_F64 p2 = new Point2D_F64(3,4);
		Point2D_F64 p3 = new Point2D_F64(5,6);

		AssociatedTriple a = new AssociatedTriple(p1,p2,p3);
		AssociatedTriple b = new AssociatedTriple();

		b.set(a);

		assertTrue(b.p1.isIdentical(a.p1, 1e-8));
		assertTrue(b.p2.isIdentical(a.p2,1e-8));
		assertTrue(b.p3.isIdentical(a.p3,1e-8));
	}

	@Test
	public void set_ThreePts() {
		Point2D_F64 p1 = new Point2D_F64(1,2);
		Point2D_F64 p2 = new Point2D_F64(3,4);
		Point2D_F64 p3 = new Point2D_F64(5,6);

		AssociatedTriple a = new AssociatedTriple();
		a.set(p1,p2,p3);

		// make sure it isn't saving the reference
		assertTrue(p1 != a.p1);
		assertTrue(p2 != a.p2);
		assertTrue(p3 != a.p3);

		// see if they have the same value
		assertTrue(p1.isIdentical(a.p1,1e-8));
		assertTrue(p2.isIdentical(a.p2,1e-8));
		assertTrue(p3.isIdentical(a.p3,1e-8));
	}

	@Test
	public void copy() {
		Point2D_F64 p1 = new Point2D_F64(1,2);
		Point2D_F64 p2 = new Point2D_F64(3,4);
		Point2D_F64 p3 = new Point2D_F64(5,6);

		AssociatedTriple a = new AssociatedTriple(p1,p2,p3);

		AssociatedTriple b = a.copy();

		// make sure it isn't saving the reference
		assertTrue(b.p1 != a.p1);
		assertTrue(b.p2 != a.p2);
		assertTrue(b.p3 != a.p3);

		// see if they have the same value
		assertTrue(b.p1.isIdentical(a.p1,1e-8));
		assertTrue(b.p2.isIdentical(a.p2,1e-8));
		assertTrue(b.p3.isIdentical(a.p3,1e-8));
	}
}
