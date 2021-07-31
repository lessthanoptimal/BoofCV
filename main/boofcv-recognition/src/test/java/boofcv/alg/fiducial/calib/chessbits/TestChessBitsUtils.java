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

package boofcv.alg.fiducial.calib.chessbits;

import boofcv.struct.GridCoordinate;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestChessBitsUtils extends BoofStandardJUnit {
	@Test void findLargestCellCount() {fail("Implement");}

	@Test void bitRect() {fail("Implement");}

	@Test void computeGridToImage() {
		var alg = new ChessBitsUtils();
		alg.addMarker(4, 3);
		alg.fixate();

		// The transform will be known and is basically a scale difference
		double w = 100.0;
		assertTrue(alg.computeGridToImage(new Point2D_F64(0, 0), new Point2D_F64(w, 0), new Point2D_F64(w, w), new Point2D_F64(0, w)));

		// Test a few points
		Point2D_F64 pixel = new Point2D_F64();
		GeometryMath_F64.mult(alg.squareToPixel, new Point2D_F64(0, 0), pixel);
		assertEquals(0.0, pixel.distance(0, 0), UtilEjml.TEST_F64);

		GeometryMath_F64.mult(alg.squareToPixel, new Point2D_F64(1, 1), pixel);
		assertEquals(0.0, pixel.distance(w, w), UtilEjml.TEST_F64);

		GeometryMath_F64.mult(alg.squareToPixel, new Point2D_F64(0, 1), pixel);
		assertEquals(0.0, pixel.distance(0, w), UtilEjml.TEST_F64);
	}

	/**
	 * Have it compute all the sample points then make sure the points it expects are inside one of the squares
	 */
	@Test void selectPixelsToSample() {
		var alg = new ChessBitsUtils();
		alg.addMarker(4, 3);
		alg.fixate();

		// Set it to identity so that the pixels and bit-units are the same
		CommonOps_DDRM.setIdentity(alg.squareToPixel);

		int row = 1;
		int col = 2;

		Rectangle2D_F64 bitRect = new Rectangle2D_F64();
		alg.bitRect(row, col, bitRect);

		var pixels = new DogArray<>(Point2D_F64::new);
		pixels.grow(); // add an element to make sure it resets

		alg.selectPixelsToSample(pixels);
		int w = alg.codec.gridBitLength;
		assertEquals(alg.bitSampleCount*w*w, pixels.size);

		// find the block if pixels for this bit
		int index = (row*w + col)*alg.bitSampleCount;

		// Every pixel should be inside the rect
		for (int i = 0; i < alg.bitSampleCount; i++) {
			Point2D_F64 p = pixels.get(index+i);
			assertTrue(Intersection2D_F64.contains(bitRect,p.x, p.y));
		}
	}

	/**
	 * Hand selected test cases
	 */
	@Test void cellToCoordinate_case0() {
		var alg = new ChessBitsUtils();
		alg.addMarker(6, 5);
		alg.fixate();

		var found = new GridCoordinate();
		alg.cellToCoordinate(0, 0, found);
		assertTrue(found.equals(0, 1));
		alg.cellToCoordinate(0, 1, found);
		assertTrue(found.equals(1, 0));
		alg.cellToCoordinate(0, 3, found);
		assertTrue(found.equals(2, 1));
		alg.cellToCoordinate(0, 5, found);
		assertTrue(found.equals(3, 2));
	}

	/**
	 * Hand selected test cases
	 */
	@Test void cellToCoordinate_case1() {
		var alg = new ChessBitsUtils();
		alg.addMarker(4, 6);
		alg.fixate();

		var found = new GridCoordinate();
		alg.cellToCoordinate(0, 0, found);
		assertTrue(found.equals(0, 1));
		alg.cellToCoordinate(0, 1, found);
		assertTrue(found.equals(0, 3));
		alg.cellToCoordinate(0, 2, found);
		assertTrue(found.equals(1, 0));
		alg.cellToCoordinate(0, 3, found);
		assertTrue(found.equals(1, 2));
	}

	/**
	 * Test using hand computed solutions
	 */
	@Test void encodedSquaresInMarker() {
		var alg = new ChessBitsUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 6);
		alg.addMarker(4, 5);

		assertEquals(6, alg.countEncodedSquaresInMarker(0));
		assertEquals(8, alg.countEncodedSquaresInMarker(1));
		assertEquals(7, alg.countEncodedSquaresInMarker(2));
	}
}
