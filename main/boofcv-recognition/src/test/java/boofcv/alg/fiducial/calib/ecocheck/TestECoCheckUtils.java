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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.struct.GridCoordinate;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestECoCheckUtils extends BoofStandardJUnit {
	@Test void findMaxEncodedSquares() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 3);
		assertEquals(1, alg.findMaxEncodedSquares());
		alg.addMarker(3, 4);
		assertEquals(1, alg.findMaxEncodedSquares());
		alg.addMarker(3, 5);
		assertEquals(1, alg.findMaxEncodedSquares());
		alg.addMarker(4, 4);
		assertEquals(2, alg.findMaxEncodedSquares());
		alg.addMarker(4, 5);
		assertEquals(3, alg.findMaxEncodedSquares());
		alg.addMarker(5, 4);
		assertEquals(3, alg.findMaxEncodedSquares());
		alg.addMarker(5, 5);
		assertEquals(4, alg.findMaxEncodedSquares());
		alg.addMarker(5, 6);
		assertEquals(6, alg.findMaxEncodedSquares());
		alg.addMarker(6, 5);
		assertEquals(6, alg.findMaxEncodedSquares());
		alg.addMarker(6, 6);
		assertEquals(8, alg.findMaxEncodedSquares());
	}

	@Test void bitRect() {
		var alg = new ECoCheckUtils();
		alg.dataBitWidthFraction = 0.9; // make sure this isn't 1.0
		alg.addMarker(6, 5);
		alg.fixate();

		// Sanity check to make sure it's the expected grid size
		assertEquals(5, alg.bitSampleCount);

		var rect = new Rectangle2D_F64();
		alg.bitRect(2, 3, rect);

		// Size of a square
		double square = (1.0 - 2*alg.dataBorderFraction)/6.0;
		double squareData = square*0.9;

		// make sure it's the expected size
		assertEquals(squareData, rect.getWidth(), UtilEjml.TEST_F64);
		assertEquals(squareData, rect.getHeight(), UtilEjml.TEST_F64);

		// At the expected location
		assertEquals(alg.dataBorderFraction + square*(2 + 0.05), rect.p0.y, UtilEjml.TEST_F64);
		assertEquals(alg.dataBorderFraction + square*(3 + 0.05), rect.p0.x, UtilEjml.TEST_F64);
	}

	@Test void computeGridToImage() {
		var alg = new ECoCheckUtils();
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
		var alg = new ECoCheckUtils();
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
			Point2D_F64 p = pixels.get(index + i);
			assertTrue(Intersection2D_F64.contains(bitRect, p.x, p.y));
		}
	}

	/**
	 * Hand selected test cases
	 */
	@Test void cellToCoordinate_case0() {
		var alg = new ECoCheckUtils();
		alg.addMarker(6, 5);
		alg.fixate();

		var found = new GridCoordinate();
		alg.cellIdToCornerCoordinate(0, 0, found);
		assertTrue(found.identical(0, 1));
		alg.cellIdToCornerCoordinate(0, 1, found);
		assertTrue(found.identical(1, 0));
		alg.cellIdToCornerCoordinate(0, 2, found);
		assertTrue(found.identical(1, 2));
		alg.cellIdToCornerCoordinate(0, 3, found);
		assertTrue(found.identical(2, 1));
		alg.cellIdToCornerCoordinate(0, 4, found);
		assertTrue(found.identical(3, 0));
		alg.cellIdToCornerCoordinate(0, 5, found);
		assertTrue(found.identical(3, 2));
	}

	@Test void cellToCoordinate_case1() {
		var alg = new ECoCheckUtils();
		alg.addMarker(5, 6);
		alg.fixate();

		var found = new GridCoordinate();
		alg.cellIdToCornerCoordinate(0, 0, found);
		assertTrue(found.identical(0, 1));
		alg.cellIdToCornerCoordinate(0, 1, found);
		assertTrue(found.identical(0, 3));
		alg.cellIdToCornerCoordinate(0, 2, found);
		assertTrue(found.identical(1, 0));
		alg.cellIdToCornerCoordinate(0, 3, found);
		assertTrue(found.identical(1, 2));
		alg.cellIdToCornerCoordinate(0, 4, found);
		assertTrue(found.identical(2, 1));
		alg.cellIdToCornerCoordinate(0, 5, found);
		assertTrue(found.identical(2, 3));
	}

	/**
	 * Hand selected test cases
	 */
	@Test void cellToCoordinate_case2() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.fixate();

		var found = new GridCoordinate();
		alg.cellIdToCornerCoordinate(0, 0, found);
		assertTrue(found.identical(0, 1));
		alg.cellIdToCornerCoordinate(0, 1, found);
		assertTrue(found.identical(0, 3));
		alg.cellIdToCornerCoordinate(0, 2, found);
		assertTrue(found.identical(1, 0));
		alg.cellIdToCornerCoordinate(0, 3, found);
		assertTrue(found.identical(1, 2));
	}

	/**
	 * Test using hand computed solutions
	 */
	@Test void encodedSquaresInMarker() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 6);
		alg.addMarker(4, 5);

		assertEquals(4, alg.countEncodedSquaresInMarker(0));
		assertEquals(6, alg.countEncodedSquaresInMarker(1));
		assertEquals(3, alg.countEncodedSquaresInMarker(2));
	}

	@Test void cornerToMarker3D() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 4);

		var p = new Point3D_F64();
		alg.cornerToMarker3D(0, 0, p);
		assertEquals(0.0, p.distance((0.5 - 2.5)/5, -(0.5 - 1.5)/5, 0), UtilEjml.TEST_F64);
		alg.cornerToMarker3D(0, 11, p);
		assertEquals(0.0, p.distance((0.5 + 1 - 2.5)/5, -(0.5 + 2 - 1.5)/5, 0), UtilEjml.TEST_F64);
		alg.cornerToMarker3D(1, 11, p);
		assertEquals(0.0, p.distance((0.5 + 2 - 1.5)/4, -(0.5 + 3 - 2.0)/4, 0), UtilEjml.TEST_F64);
	}

	@Test void createCornerList() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 6);

		List<Point2D_F64> found = alg.createCornerList(1, 1.5);
		Point3D_F64 coordinate = new Point3D_F64();
		for (int cornerID = 0; cornerID < found.size(); cornerID++) {
			alg.cornerToMarker3D(1, cornerID, 1.5, coordinate);
			assertEquals(0.0, found.get(cornerID).distance(coordinate.x, coordinate.y), UtilEjml.TEST_F64);
		}
	}

	@Test void mergeAndRemoveUnknown() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 6);

		var input = new ArrayList<ECoCheckFound>();

		// one unknown that will be filtered
		// one that is unique and should be kept
		// two that will be merged
		input.add(createFound(1, 5, 10));
		input.add(createFound(0, 5, 10));
		input.add(createFound(1, 12, 15));
		input.add(createFound(-1, 0, 10));

		List<ECoCheckFound> found = alg.mergeAndRemoveUnknown(input);

		assertEquals(2, found.size());
		for (int i = 0; i < 2; i++) {
			ECoCheckFound f = found.get(i);
			if (f.markerID == 1) {
				assertEquals(8, f.corners.size);
				assertEquals(8, f.touchBinary.size);
				assertEquals(2, f.decodedCells.size);
			} else if (f.markerID == 0) {
				assertEquals(5, f.corners.size);
				assertEquals(5, f.touchBinary.size);
				assertEquals(1, f.decodedCells.size);
			} else {
				fail("Unexpected ID");
			}
		}
	}

	/**
	 * Makes sure the conflict resolution strategy is correctly implemented
	 */
	@Test void mergeAndRemoveUnknown_conflict() {
		var alg = new ECoCheckUtils();
		alg.addMarker(4, 6);
		alg.addMarker(5, 6);

		var input = new ArrayList<ECoCheckFound>();

		// first two are in confict and the 3rd should be merged with the largest
		input.add(createFound(1, 5, 10));
		input.add(createFound(1, 9, 11));
		input.add(createFound(1, 12, 15));

		List<ECoCheckFound> found = alg.mergeAndRemoveUnknown(input);
		assertEquals(1, found.size());
		assertEquals(8, found.get(0).corners.size);
	}

	private ECoCheckFound createFound( int markerID, int idx0, int idx1 ) {
		var f = new ECoCheckFound();
		f.markerID = markerID;

		for (int i = idx0; i < idx1; i++) {
			f.corners.grow().setTo(rand.nextDouble(), rand.nextDouble(), i);
		}

		f.touchBinary.resize(idx1 - idx0);
		f.decodedCells.add(idx0);

		return f;
	}
}
