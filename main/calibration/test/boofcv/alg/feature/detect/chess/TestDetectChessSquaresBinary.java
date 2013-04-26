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

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectChessSquaresBinary {

	int w = 400;
	int h = 500;

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {

		basicTest(1,1,1,0);
		basicTest(3,3,5,4);
		basicTest(7,5,3*4+2*3,4);

		// handle non-symmetric cases here
		basicTest(2,2,2,2);
		basicTest(4,4,8,2);
		basicTest(6,6,2*3*3,2);
		basicTest(2,4,4,2);
		basicTest(2,6,6,2);
		basicTest(4,2,4,2);
		basicTest(6,2,6,2);

		basicTest(3,2,3,2);
		basicTest(5,2,5,2);
		basicTest(7,2,7,2);
		basicTest(5,4,2*(3+2),2);
		basicTest(5,6,3*(3+2),2);

		basicTest(2,3,3,2);
		basicTest(2,5,5,2);
		basicTest(2,7,7,2);
		basicTest(4,5,2*(3+2),2);
		basicTest(6,5,3*(3+2),2);
	}

	public void basicTest( int gridWidth , int gridHeight , int expectedAll , int expectedCorner ) {

		ImageUInt8 binary = createTarget(gridWidth,gridHeight);

//		binary.printBinary();

		DetectChessSquaresBinary alg = new DetectChessSquaresBinary(gridWidth,gridHeight,50);

		assertTrue(alg.process(binary));

		List<QuadBlob> allBlobs = alg.getGraphBlobs();
		int cornerBlobs = 0;
		for( QuadBlob b : allBlobs )
			if( b.conn.size() == 1 )
				cornerBlobs++;

		assertEquals(expectedAll,allBlobs.size());
		assertEquals(expectedCorner,cornerBlobs);
	}

	private ImageUInt8 createTarget( int gridWidth , int gridHeight ) {
		int squareLength = 30;
		int squareLength2 = 28;
		ImageUInt8 binary = new ImageUInt8(w,h);

		int offsetX = 15;
		int offsetY = 10;

		SimpleMatrix a = new SimpleMatrix(1,2);
		a.set(5);

		// create the grid
		for( int y = 0; y < gridHeight; y += 2) {
			for( int x = 0; x < gridWidth; x += 2 ) {
				int pixelX = x*squareLength+offsetX;
				int pixelY = y*squareLength+offsetY;

				ImageMiscOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength, squareLength);
			}
		}
		// don't want the square touching each other
		for( int y = 1; y < gridHeight; y += 2) {
			for( int x = 1; x < gridWidth; x += 2 ) {
				int pixelX = x*squareLength+offsetX+1;
				int pixelY = y*squareLength+offsetY+1;

				ImageMiscOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength2, squareLength2);
			}
		}

		return binary;
	}
}
