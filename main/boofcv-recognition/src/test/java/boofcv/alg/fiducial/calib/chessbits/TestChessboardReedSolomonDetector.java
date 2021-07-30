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
import boofcv.io.image.UtilImageIO;
import boofcv.struct.GridCoordinate;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public class TestChessboardReedSolomonDetector extends BoofStandardJUnit {

	// Reduce boilerplate by pre-declaring the algorithm being tested
	ConfigChessboardX config = new ConfigChessboardX();
	ChessBitsUtils utils = new ChessBitsUtils();
	ChessboardReedSolomonDetector<GrayU8> alg;

	@BeforeEach void setup() {
		utils.addMarker(5, 6);
		utils.fixate();
		alg = new ChessboardReedSolomonDetector<>(utils, config, GrayU8.class);
	}

	/**
	 * Test the entire pipeline on a very simple synthetic scenario
	 */
	@Test void simpleOneTarget() {
		GrayU8 image = createMarker(utils);

		UtilImageIO.saveImage(image, "gray.png");
//		ShowImages.showWindow(image, "Rendered");
//		BoofMiscOps.sleep(30_000);

		alg.process(image);

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
}
