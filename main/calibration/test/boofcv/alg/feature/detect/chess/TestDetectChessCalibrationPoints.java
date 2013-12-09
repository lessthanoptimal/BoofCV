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

import java.util.ArrayList;
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

	int squareLength = 30;
	int w = 500;
	int h = 550;

	int offsetX = 15;
	int offsetY = 10;

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {
		for( int numRows = 3; numRows <= 7; numRows++ ) {
			for( int numCols = 3; numCols <= 7; numCols++ ) {
//				System.out.println(numCols+"  "+numRows);
				basicTest(numCols, numRows);
			}
		}
	}

	public void basicTest( int numCols , int numRows ) {
		ImageFloat32 gray = renderTarget(numCols,numRows);

//		ImageUInt8 b = new ImageUInt8(w,h);
//		ThresholdImageOps.threshold(gray,b,50,true);
//
//		System.out.println("-----------------------------");
//		b.printBinary();

		ImageMiscOps.addGaussian(gray,rand,0.1,0,255);

		DetectChessCalibrationPoints alg = new DetectChessCalibrationPoints(numCols,numRows,5,1.0,ImageFloat32.class);

		assertTrue(alg.process(gray));

		List<Point2D_F64> found = alg.getPoints();
		List<Point2D_F64> expected = calibrationPoints(numCols,numRows);

		assertEquals(expected.size(), found.size());

		// check the ordering of the points
		for( int i = 0; i < expected.size(); i++ ) {
			Point2D_F64 e = expected.get(i);
			Point2D_F64 f = found.get(i);

			assertEquals("i = " + i, e.x, f.x, 2);
			assertEquals("i = " + i,e.y,f.y,2);
		}
	}

	public ImageFloat32 renderTarget( int numCols , int numRows ) {
		ImageFloat32 gray = new ImageFloat32(w,h);
		ImageMiscOps.fill(gray,80f);

		int numCols2 = numCols/2;
		int numRows2 = numRows/2;

		numCols = numCols/2 + numCols%2;
		numRows = numRows/2 + numRows%2;

		// create the grid
		for( int y = 0; y < numRows; y++) {
			for( int x = 0; x < numCols; x++ ) {
				int pixelY = 2*y*squareLength+offsetY;
				int pixelX = 2*x*squareLength+offsetX;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}
		for( int y = 0; y < numRows2; y++) {
			for( int x = 0; x < numCols2; x++ ) {
				int pixelY = 2*y*squareLength+offsetY+squareLength;
				int pixelX = 2*x*squareLength+offsetX+squareLength;

				ImageMiscOps.fillRectangle(gray, 20, pixelX, pixelY, squareLength, squareLength);
			}
		}

		return gray;
	}

	public List<Point2D_F64> calibrationPoints( int numCols , int numRows ) {

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for( int y = 0; y < numRows-1; y++) {
			for( int x = 0; x < numCols-1; x++ ) {
				int pixelY = y*squareLength+offsetY+squareLength;
				int pixelX = x*squareLength+offsetX+squareLength;

				ret.add( new Point2D_F64(pixelX,pixelY));
			}
		}

		return ret;
	}
}
