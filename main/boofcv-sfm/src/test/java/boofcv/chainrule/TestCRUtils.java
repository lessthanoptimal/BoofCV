/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.chainrule.CRArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCRUtils extends BoofStandardJUnit {

	// ---------------- toCRArray( Point2D_F32 ) ----------------

	@Test void toCRArray_Point2D_F32_allocates() {
		var p = new Point2D_F32(1.5f, -2.25f);

		CRArray<double[]> out = CRUtils.toCRArray(p, null);

		assertNotNull(out);
		assertEquals(0, out.offset);
		assertEquals(2, out.a.length);
		assertEquals(p.x, out.d(0));
		assertEquals(p.y, out.d(1));
	}

	@Test void toCRArray_Point2D_F32_reuseAtOffset() {
		var p = new Point2D_F32(1.5f, -2.25f);
		var out = new CRArray<>(new double[5], 2);

		CRArray<double[]> found = CRUtils.toCRArray(p, out);

		assertSame(out, found);
		assertEquals(p.x, out.a[2]);
		assertEquals(p.y, out.a[3]);
		// neighbours outside the written window are untouched
		assertEquals(0.0, out.a[0]);
		assertEquals(0.0, out.a[1]);
		assertEquals(0.0, out.a[4]);
	}

	// ---------------- toCRArray( Point2D_F64 ) ----------------

	@Test void toCRArray_Point2D_F64_allocates() {
		var p = new Point2D_F64(1.5, -2.25);

		CRArray<double[]> out = CRUtils.toCRArray(p, null);

		assertNotNull(out);
		assertEquals(0, out.offset);
		assertEquals(2, out.a.length);
		assertEquals(p.x, out.d(0));
		assertEquals(p.y, out.d(1));
	}

	@Test void toCRArray_Point2D_F64_reuseAtOffset() {
		var p = new Point2D_F64(1.5, -2.25);
		var out = new CRArray<>(new double[5], 2);

		CRArray<double[]> found = CRUtils.toCRArray(p, out);

		assertSame(out, found);
		assertEquals(p.x, out.a[2]);
		assertEquals(p.y, out.a[3]);
		assertEquals(0.0, out.a[0]);
		assertEquals(0.0, out.a[1]);
		assertEquals(0.0, out.a[4]);
	}

	// ---------------- toCRArray( Point3D_F64 ) ----------------

	@Test void toCRArray_Point3D_F64_allocates() {
		var p = new Point3D_F64(1.5, -2.25, 7.0);

		CRArray<double[]> out = CRUtils.toCRArray(p, null);

		assertNotNull(out);
		assertEquals(0, out.offset);
		assertEquals(3, out.a.length);
		assertEquals(p.x, out.d(0));
		assertEquals(p.y, out.d(1));
		assertEquals(p.z, out.d(2));
	}

	@Test void toCRArray_Point3D_F64_reuseAtOffset() {
		var p = new Point3D_F64(1.5, -2.25, 7.0);
		var out = new CRArray<>(new double[6], 2);

		CRArray<double[]> found = CRUtils.toCRArray(p, out);

		assertSame(out, found);
		assertEquals(p.x, out.a[2]);
		assertEquals(p.y, out.a[3]);
		assertEquals(p.z, out.a[4]);
		assertEquals(0.0, out.a[0]);
		assertEquals(0.0, out.a[1]);
		assertEquals(0.0, out.a[5]);
	}

	// ---------------- toCRArray( Point4D_F64 ) ----------------

	@Test void toCRArray_Point4D_F64_allocates() {
		var p = new Point4D_F64(1.5, -2.25, 7.0, -0.5);

		CRArray<double[]> out = CRUtils.toCRArray(p, null);

		assertNotNull(out);
		assertEquals(0, out.offset);
		assertEquals(4, out.a.length);
		assertEquals(p.x, out.d(0));
		assertEquals(p.y, out.d(1));
		assertEquals(p.z, out.d(2));
		assertEquals(p.w, out.d(3));
	}

	@Test void toCRArray_Point4D_F64_reuseAtOffset() {
		var p = new Point4D_F64(1.5, -2.25, 7.0, -0.5);
		var out = new CRArray<>(new double[7], 2);

		CRArray<double[]> found = CRUtils.toCRArray(p, out);

		assertSame(out, found);
		assertEquals(p.x, out.a[2]);
		assertEquals(p.y, out.a[3]);
		assertEquals(p.z, out.a[4]);
		assertEquals(p.w, out.a[5]);
		assertEquals(0.0, out.a[0]);
		assertEquals(0.0, out.a[1]);
		assertEquals(0.0, out.a[6]);
	}

	// ---------------- toArray( x,y,z ) ----------------

	@Test void toArray_3D_writesAtOffset() {
		var array = new double[6];

		CRUtils.toArray(1.5, -2.25, 7.0, array, 2);

		assertArrayEquals(new double[]{0, 0, 1.5, -2.25, 7.0, 0}, array);
	}

	// ---------------- toArray( x,y,z,w ) ----------------

	@Test void toArray_4D_writesAtOffset() {
		var array = new double[7];

		CRUtils.toArray(1.5, -2.25, 7.0, -0.5, array, 2);

		assertArrayEquals(new double[]{0, 0, 1.5, -2.25, 7.0, -0.5, 0}, array);
	}

	// ---------------- toPoint3 ----------------

	@Test void toPoint3_readsAtOffset() {
		var array = new double[]{9, 9, 1.5, -2.25, 7.0, 9};
		var p = new Point3D_F64();

		CRUtils.toPoint3(array, 2, p);

		assertEquals(1.5, p.x);
		assertEquals(-2.25, p.y);
		assertEquals(7.0, p.z);
	}

	// ---------------- toPoint4 ----------------

	@Test void toPoint4_readsAtOffset() {
		var array = new double[]{9, 9, 1.5, -2.25, 7.0, -0.5, 9};
		var p = new Point4D_F64();

		CRUtils.toPoint4(array, 2, p);

		assertEquals(1.5, p.x);
		assertEquals(-2.25, p.y);
		assertEquals(7.0, p.z);
		assertEquals(-0.5, p.w);
	}

	// ---------------- round trip: toArray -> toPoint ----------------

	@Test void roundTrip_3D() {
		var expected = new Point3D_F64(uniform(), uniform(), uniform());
		var array = new double[3];
		CRUtils.toArray(expected.x, expected.y, expected.z, array, 0);

		var found = new Point3D_F64();
		CRUtils.toPoint3(array, 0, found);

		assertEquals(expected.x, found.x);
		assertEquals(expected.y, found.y);
		assertEquals(expected.z, found.z);
	}

	@Test void roundTrip_4D() {
		var expected = new Point4D_F64(uniform(), uniform(), uniform(), uniform());
		var array = new double[4];
		CRUtils.toArray(expected.x, expected.y, expected.z, expected.w, array, 0);

		var found = new Point4D_F64();
		CRUtils.toPoint4(array, 0, found);

		assertEquals(expected.x, found.x);
		assertEquals(expected.y, found.y);
		assertEquals(expected.z, found.z);
		assertEquals(expected.w, found.w);
	}

	/// Random value from -1 to 1
	double uniform() {
		return 2.0*rand.nextDouble() - 1.0;
	}
}
