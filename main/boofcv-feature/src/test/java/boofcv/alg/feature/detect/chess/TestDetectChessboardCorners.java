/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.distort.FDistort;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Peter Abeles
 */
class TestDetectChessboardCorners extends CommonChessboardCorners {

	/**
	 * Rotate a chessboard pattern and see if all the corners are detected
	 */
	@Test
	void process_rotate() {
		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(p,1);
		renderer.chessboard(rows,cols,w);

		GrayF32 original = renderer.getGrayF32();
		GrayF32 rotated = original.createSameShape();

		DetectChessboardCorners<GrayF32,GrayF32> alg = new DetectChessboardCorners<>(GrayF32.class);

		for (int i = 0; i < 10; i++) {
			angle = i*Math.PI/10;
			new FDistort(original,rotated).rotate(angle).apply();
			alg.process(rotated);
			checkSolution(rotated, alg);
		}
	}

	/**
	 * Give it a small chessboard and see if it detects it
	 */
	@Test
	void process_small() {
		w = 5;
		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(p,1);
		renderer.chessboard(rows,cols,w);

		DetectChessboardCorners<GrayF32,GrayF32> alg = new DetectChessboardCorners<>(GrayF32.class);
		alg.setKernelRadius(1); // should fail if 2
		checkSolution(renderer.getGrayF32(), alg);
	}

	private void checkSolution( GrayF32 input, DetectChessboardCorners<GrayF32, GrayF32> alg) {
		alg.process(input);
		FastQueue<ChessboardCorner> found = alg.getCorners();
		List<ChessboardCorner> expected = createExpected(rows,cols, input.width, input.height);

		assertEquals(expected.size(),found.size);

		for( ChessboardCorner c : expected ) {
			int matches = 0;
			for (int i = 0; i < found.size; i++) {
				ChessboardCorner f = found.get(i);
				if( f.distance(c) < 1.5 ) {
					matches++;
					assertEquals(c.orientation,f.orientation, 0.2);
					assertTrue(f.intensity>0);
				}
			}
			assertEquals(1,matches);
		}
	}

	@Test
	void meanShiftLocation() {
		Random rand = new Random(234);
		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(p,1);
		renderer.chessboard(rows,cols,w);

		DetectChessboardCorners<GrayF32,GrayF32> alg = new DetectChessboardCorners<>(GrayF32.class);
		alg.process(renderer.getGrayF32());

		FastQueue<ChessboardCorner> found = alg.getCorners();
		for (int trial = 0; trial < 5; trial++) {
			for (int i = 0; i < found.size; i++) {
				ChessboardCorner c = found.get(i);
				double beforeX = c.x;
				double beforeY = c.y;

				// move the corner away by a little bit
				c.x += rand.nextGaussian()*0.5;
				c.y += rand.nextGaussian()*0.5;

				// see if it snaps back
				alg.meanShiftLocation(c);
				assertEquals(0,c.distance(beforeX,beforeY), 0.1);
			}
		}
	}
}
