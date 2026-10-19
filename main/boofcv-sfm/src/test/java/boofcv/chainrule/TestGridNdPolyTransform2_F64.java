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
import georegression.struct.point.Point2D_F64;
import org.ddogleg.chainrule.grid.ConfigGridCoefND;
import org.ddogleg.chainrule.grid.GridCoefND;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestGridNdPolyTransform2_F64 extends BoofStandardJUnit {
	static final int W = 400, H = 300;

	static GridCoefND create( int cols, int rows, int coefs ) {
		return new GridCoefND(new ConfigGridCoefND().cols(cols).rows(rows).coefs(coefs).numOut(2));
	}

	/// The grid holds a correction, so a zero grid leaves every point exactly where it was
	@Test void zeroGridIsTheIdentity() {
		var alg = new GridNdPolyTransform2_F64(create(4, 3, 2), W, H).value(0.7);

		var found = new Point2D_F64();
		for (double[] p : new double[][]{{0, 0}, {W, H}, {123.4, 45.6}, {W/2.0, H/2.0}}) {
			alg.compute(p[0], p[1], found);
			assertEquals(p[0], found.x, 1e-12);
			assertEquals(p[1], found.y, 1e-12);
		}
	}

	/// Every knot holding the same shift is that shift everywhere, which is the one case interpolation
	/// cannot get wrong. Also pins that x and y read their own polynomial
	@Test void constantGridIsAConstantShift() {
		var grid = create(4, 3, 1);
		for (int i = 0; i < grid.parameters.length; i += 2) {
			grid.parameters[i] = 2.5;
			grid.parameters[i + 1] = -1.5;
		}
		var alg = new GridNdPolyTransform2_F64(grid, W, H).value(0.3);

		var found = new Point2D_F64();
		for (double[] p : new double[][]{{0, 0}, {W, H}, {310.0, 12.5}, {77.7, 250.1}}) {
			alg.compute(p[0], p[1], found);
			assertEquals(p[0] + 2.5, found.x, 1e-12);
			assertEquals(p[1] - 1.5, found.y, 1e-12);
		}
	}

	/// The shift is a polynomial in value, so changing it moves the output
	@Test void shiftDependsOnValue() {
		var grid = create(3, 3, 2);
		for (int i = 0; i < grid.parameters.length; i += 4) {
			grid.parameters[i] = 1; grid.parameters[i + 1] = 3;        // x: 1 + 3v
			grid.parameters[i + 2] = -2; grid.parameters[i + 3] = 0;   // y: -2
		}
		var alg = new GridNdPolyTransform2_F64(grid, W, H);

		var found = new Point2D_F64();
		for (double v : new double[]{0, 0.5, -1.25}) {
			alg.value(v).compute(100, 80, found);
			assertEquals(100 + 1 + 3*v, found.x, 1e-9, "x at v=" + v);
			assertEquals(80 - 2, found.y, 1e-9, "y at v=" + v);
		}
	}

	/// The transform needs two outputs, one per axis. Anything else is a wiring mistake rather than
	/// something to discover part way through an image
	@Test void requiresTwoOutputs() {
		for (int numOut : new int[]{1, 3}) {
			var grid = new GridCoefND(new ConfigGridCoefND().cols(3).rows(3).coefs(1).numOut(numOut));
			assertThrows(IllegalArgumentException.class,
					() -> new GridNdPolyTransform2_F64(grid, W, H), "numOut " + numOut);
		}
	}

	/// A concurrent copy has to give the same answers, including the value it was set to. It shares the
	/// grid, which is only safe because the coefficients are read and never written
	@Test void copyConcurrent() {
		var grid = create(4, 3, 2);
		for (int i = 0; i < grid.parameters.length; i++)
			grid.parameters[i] = 0.5*Math.sin(i*1.7);

		var alg = new GridNdPolyTransform2_F64(grid, W, H).value(0.4);
		var copy = alg.copyConcurrent();

		var expected = new Point2D_F64();
		var found = new Point2D_F64();
		for (double[] p : new double[][]{{10, 10}, {W - 5, 8}, {W/2.0 + 7, H/2.0 + 7}, {33, H - 3}}) {
			alg.compute(p[0], p[1], expected);
			copy.compute(p[0], p[1], found);
			assertEquals(expected.x, found.x, 1e-12, "x at (" + p[0] + " , " + p[1] + ")");
			assertEquals(expected.y, found.y, 1e-12, "y at (" + p[0] + " , " + p[1] + ")");
		}

		// and it is a copy, so moving one does not move the other
		((GridNdPolyTransform2_F64)copy).value(-1);
		alg.compute(50, 50, expected);
		copy.compute(50, 50, found);
		assertNotEquals(expected.x, found.x, 1e-9);
	}

	/// A point outside the image is an ordinary sample, since an undistorted pixel can easily land off
	/// the sensor and still be needed
	@Test void outsideTheImageIsAllowed() {
		var grid = create(3, 3, 1);
		for (int i = 0; i < grid.parameters.length; i++)
			grid.parameters[i] = 0.5*Math.sin(i*2.3);

		var alg = new GridNdPolyTransform2_F64(grid, W, H).value(0);
		var found = new Point2D_F64();
		alg.compute(-50, -20, found);

		assertTrue(Double.isFinite(found.x) && Double.isFinite(found.y));
	}
}
