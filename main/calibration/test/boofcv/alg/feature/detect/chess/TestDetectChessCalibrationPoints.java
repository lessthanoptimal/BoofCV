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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestDetectChessCalibrationPoints {

	Random rand = new Random(234);
	
	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {
		int squareLength = 30;
		int w = 400;
		int h = 500;
		ImageFloat32 gray = new ImageFloat32(w,h);
		ImageMiscOps.fill(gray,80f);

		// create the grid
		for( int y = 0; y < 3; y++) {
			for( int x = 0; x < 4; x++ ) {
				int pixelY = 2*y*squareLength+10;
				int pixelX = 2*x*squareLength+15;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}
		for( int y = 0; y < 2; y++) {
			for( int x = 0; x < 3; x++ ) {
				int pixelY = 2*y*squareLength+10+squareLength;
				int pixelX = 2*x*squareLength+15+squareLength;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}
		ImageMiscOps.addGaussian(gray,rand,0.1,0,255);

		DetectChessCalibrationPoints alg = new DetectChessCalibrationPoints(7,5,5,1.0,ImageFloat32.class);

		assertTrue(alg.process(gray));

		List<Point2D_F64> points = alg.getPoints();

		assertEquals(4*6,points.size());

		// should also test for row-major ordering
		// and that the points are in a clockwise orientation
		// then feed it a bunch of different synthetic targets
		// but that would be a lot of work
	}
}
