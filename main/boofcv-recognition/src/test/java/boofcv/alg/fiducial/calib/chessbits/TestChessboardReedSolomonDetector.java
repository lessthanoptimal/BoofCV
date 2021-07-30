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

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.struct.GridCoordinate;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestChessboardReedSolomonDetector extends BoofStandardJUnit {

	// Reduce boilerplate by pre-declaring the algorithm being tested
	ConfigChessboardX config = new ConfigChessboardX();
	ChessBitsUtils utils = new ChessBitsUtils();
	ChessboardReedSolomonDetector<GrayU8> alg;

	List<Point2D_F64> truthCorners;

	// Quite zone around rendered marker
	int borderPixels = 10;

	@BeforeEach void setup() {
		utils.addMarker(5, 6);
		utils.fixate();
		alg = new ChessboardReedSolomonDetector<>(utils, config, GrayU8.class);
	}

	/**
	 * Test the entire pipeline on a very simple synthetic scenario with different shaped targets
	 */
	@Test void oneTargetShapes() {
		oneTargetShapes(5, 6);
		oneTargetShapes(5, 5);
		oneTargetShapes(4, 6);
	}

	void oneTargetShapes( int squareRows, int squareCols ) {
		utils = new ChessBitsUtils();
		utils.addMarker(squareRows, squareCols);
		utils.fixate();
		alg = new ChessboardReedSolomonDetector<>(utils, config, GrayU8.class);

		GrayU8 image = createMarker(utils);

		int numEncodedSquares = (squareCols - 2)*(squareRows - 2)/2;

		alg.process(image);

		FastAccess<ChessboardBitPattern> found = alg.getFound();
		assertEquals(1, found.size);

		ChessboardBitPattern marker = found.get(0);
		assertEquals(0, marker.marker);
		assertEquals(squareRows, marker.squareRows);
		assertEquals(squareCols, marker.squareCols);
		assertEquals(numEncodedSquares, marker.decodedSquares);
		assertEquals(truthCorners.size(), marker.corners.size);

		// see if the corners are at all the correct locations and have the correct ID
		for (int i = 0; i < marker.corners.size; i++) {
			PointIndex2D_F64 c = marker.corners.get(i);
			Point2D_F64 truth = truthCorners.get(c.index);

			// Check that their locations are the same and compensate for the border added to the marker
			assertTrue(truth.isIdentical(c.p.x - borderPixels, c.p.y - borderPixels, UtilEjml.TEST_F64));
		}
	}

	@Test void simpleOneTarget_Rotate() {
		fail("Implement");
	}

	/**
	 * See if it will return an anonymous chessboard
	 */
	@Test void chessboard_NoData() {
		fail("Implement");
	}

	/**
	 * Two patterns are so close their grids will overlap. See if this correctly splits them up
	 */
	@Test void touchingTargets() {
		fail("Implement");
	}

	/**
	 * Multiple patterns are visible at once
	 */
	@Test void multipleTargets() {
		fail("Implement");
	}

	private GrayU8 createMarker( ChessBitsUtils utils ) {
		var engine = new FiducialImageEngine();
		engine.configure(10, 400);
		var renderer = new ChessboardReedSolomonGenerator(utils);
//		renderer.multiplier = ChessboardSolomonMarkerCodec.Multiplier.LEVEL_2;
		renderer.render = engine;
		renderer.squareWidth = 80;
		renderer.render(0);

		truthCorners = renderer.corner;

		return engine.getGray();
	}

	/**
	 * Change the coordinate system's orientation and see if the (0,0) is as expected
	 */
	@Test void rotateObserved() {
		var found = new GridCoordinate();
		int rows = alg.utils.markers.get(0).rows;
		int cols = alg.utils.markers.get(0).cols;
		alg.clusterToGrid.sparseToDense();

		int row = 0;
		int col = 0;

		ChessboardReedSolomonDetector.rotateObserved(rows, cols, row, col, 0, found);
		assertTrue(found.equals(row, col));

		ChessboardReedSolomonDetector.rotateObserved(rows, cols, row, col, 1, found);
		assertTrue(found.equals(cols - 1 - col, row));

		ChessboardReedSolomonDetector.rotateObserved(rows, cols, row, col, 2, found);
		assertTrue(found.equals(rows - 1 - row, cols - 1 - col));

		ChessboardReedSolomonDetector.rotateObserved(rows, cols, row, col, 3, found);
		assertTrue(found.equals(col, rows - 1 - row));
	}

	@Test void findMatching() {
		fail("Implement");
	}

	@Test void createCorrectedTarget() {
		fail("Implement");
	}

	@Test void createAnonymousTarget() {
		fail("Implement");
	}
}
