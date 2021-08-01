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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.GridShape;
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
		setUp(5, 6);
	}

	void setUp( int rows, int cols ) {
		utils = new ChessBitsUtils();
		utils.addMarker(rows, cols);
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
		setUp(squareRows, squareCols);

		GrayU8 image = createMarker(utils, 0);

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

	/**
	 * Take one target and rotate it. See if anything goes horribly wrong
	 */
	@Test void simpleOneTarget_Rotate() {
		GrayU8 image = createMarker(utils, 0);
		GrayU8 rotated = new GrayU8(1, 1);

		for (int i = 0; i < 3; i++) {
			ImageMiscOps.rotateCCW(image, rotated);

			alg.process(rotated);
			image.setTo(rotated);

			FastAccess<ChessboardBitPattern> found = alg.getFound();
			assertEquals(1, found.size);
			ChessboardBitPattern marker = found.get(0);
			assertEquals(0, marker.marker);
			assertEquals(5, marker.squareRows);
			assertEquals(6, marker.squareCols);
			assertEquals(truthCorners.size(), marker.corners.size);
		}
	}

	/**
	 * Part of the target goes outside the image
	 */
	@Test void partialTarget() {
		fail("Implement");
	}

	/**
	 * See if it will return an anonymous chessboard
	 */
	@Test void chessboard_NoData() {
		GrayU8 image = createMarker(utils, 0);

		// zap all the encoded data
		for (int row = 1; row < 4; row++) {
			for (int cols = row%2 + 1; cols < 5; cols += 2) {
				ImageMiscOps.fillRectangle(image, 255, 50 + (cols - 1)*80, 50 + (row - 1)*80, 80, 80);
			}
		}

		alg.process(image);

		FastAccess<ChessboardBitPattern> found = alg.getFound();
		assertEquals(1, found.size);

		ChessboardBitPattern marker = found.get(0);
		assertEquals(-1, marker.marker);
		assertEquals(5, marker.squareRows);
		assertEquals(6, marker.squareCols);
		assertEquals(truthCorners.size(), marker.corners.size);
		assertEquals(0, marker.decodedSquares);
		assertEquals(0, marker.touchBinary.size);
	}

	/**
	 * Two patterns are so close their grids will overlap. See if this correctly splits them up
	 */
	@Test void touchingTargets() {
		utils = new ChessBitsUtils();
		utils.addMarker(3, 4); // two targets with the same shape
		utils.addMarker(3, 4);
		utils.fixate();
		alg = new ChessboardReedSolomonDetector<>(utils, config, GrayU8.class);

		GrayU8 image0 = createMarker(utils, 0);
		GrayU8 image1 = createMarker(utils, 1);

		GrayU8 combined = new GrayU8(image0.width, image0.height*2);

		ImageMiscOps.copy(0, 0, 0, 0, image0.width, image0.height, image0, combined);
		ImageMiscOps.copy(0, 10, 0, image0.height - 10,
				image1.width, image1.height - 10, image1, combined);

		alg.process(combined);

		FastAccess<ChessboardBitPattern> found = alg.getFound();
		assertEquals(2, found.size);

		found.forEach(t -> {
			assertEquals(3, t.squareRows);
			assertEquals(4, t.squareCols);
			assertEquals(truthCorners.size(), t.corners.size);
		});
	}

	/**
	 * Multiple patterns are visible at once. Targets have enough space that they shouldn't be joined.
	 */
	@Test void multipleTargets() {
		utils = new ChessBitsUtils();
		utils.addMarker(3, 4);
		utils.addMarker(4, 3);
		utils.fixate();
		alg = new ChessboardReedSolomonDetector<>(utils, config, GrayU8.class);

		GrayU8 image0 = createMarker(utils, 0);
		GrayU8 image1 = createMarker(utils, 1);

		int space = 40;
		GrayU8 combined = new GrayU8(Math.max(image0.width, image1.width), image0.height + image1.height + space);

		ImageMiscOps.copy(0, 0, 0, 0, image0.width, image0.height, image0, combined);
		ImageMiscOps.copy(0, 0, 0, image0.height + space,
				image1.width, image1.height, image1, combined);

		alg.process(combined);

		FastAccess<ChessboardBitPattern> found = alg.getFound();
		assertEquals(2, found.size);
	}

	private GrayU8 createMarker( ChessBitsUtils utils, int markerID ) {
		int squareWidth = 80;
		GridShape shape = utils.markers.get(markerID);
		var engine = new FiducialImageEngine();
		engine.configure(10, squareWidth*(shape.cols - 1), squareWidth*(shape.rows - 1));
		var renderer = new ChessboardReedSolomonGenerator(utils);
		renderer.render = engine;
		renderer.squareWidth = squareWidth;
		renderer.render(markerID);

		truthCorners = renderer.corner;

		return engine.getGray();
	}

	/**
	 * Two targets will overlap and only the first should be successful
	 */
	@Test void createCorrectedTarget_Overlap() {
		fail("Implement");
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
