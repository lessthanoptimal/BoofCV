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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.struct.image.GrayF32;
import georegression.metric.UtilAngle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
abstract class GenericChessboardCornersChecks extends CommonChessboardCorners {

	public abstract List<ChessboardCorner> process( GrayF32 image );

	/**
	 * Test everything together with perfect input, but rotate the chessboard
	 */
	@Test void process_rotate() {
		// make it bigger so that being a pyramid matters
		this.w = 50;

		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(p,1);
		renderer.chessboard(rows,cols,w);

		GrayF32 original = renderer.getGrayF32();
		GrayF32 rotated = original.createSameShape();

		for (int i = 0; i < 10; i++) {
			angle = i*Math.PI/10;
			new FDistort(original,rotated).rotate(angle).apply();
			checkSolution(rotated.width,rotated.height,process(rotated));
		}
	}

	/**
	 * Apply heavy blurring to the input image so that the bottom most layer won't reliably detect corners
	 */
	@Test void process_blurred() {
		// make it bigger so that being a pyramid matters
		this.w = 50;

		RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(p,1);
		renderer.chessboard(rows,cols,w);

		GrayF32 original = renderer.getGrayF32();
		GrayF32 blurred = original.createSameShape();


		// mean blur messes it up much more than gaussian. This won't work if no pyramid
		BlurImageOps.mean(original,blurred,5,null,null);

		checkSolution(blurred.width,blurred.height,process(blurred));
	}

	private void checkSolution( int width , int height , List<ChessboardCorner> found ) {
//		System.out.println("------- ENTER");

		List<ChessboardCorner> expected = createExpected(rows,cols, width, height);

		assertEquals(expected.size(),found.size());

//		for (int i = 0; i < found.size; i++) {
//			found.get(i).print();
//		}
//		System.out.println("-------");

		for( ChessboardCorner c : expected ) {
			int matches = 0;
			for (int i = 0; i < found.size(); i++) {
				ChessboardCorner f = found.get(i);
				if( f.distance(c) < 1.5 ) {
					matches++;
					assertEquals(0.0, UtilAngle.distHalf(c.orientation,f.orientation), 0.2);
					assertTrue(f.intensity>0);
				}
			}
//			c.print();
			assertEquals(1,matches);
		}
	}
}
