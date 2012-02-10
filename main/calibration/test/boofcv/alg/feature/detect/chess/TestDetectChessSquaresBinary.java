/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectChessSquaresBinary {

	/**
	 * Give it a simple target and see if it finds the expected number of squares
	 */
	@Test
	public void basicTest() {
		int squareLength = 30;
		int squareLength2 = 28;
		int w = 400;
		int h = 500;
		ImageUInt8 binary = new ImageUInt8(w,h);

		// create the grid
		for( int y = 0; y < 3; y++) {
			for( int x = 0; x < 4; x++ ) {
				int pixelY = 2*y*squareLength+10;
				int pixelX = 2*x*squareLength+15;

				ImageTestingOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength, squareLength);
			}
		}
		for( int y = 0; y < 2; y++) {
			for( int x = 0; x < 3; x++ ) {
				int pixelY = 2*y*squareLength+10+squareLength+1;
				int pixelX = 2*x*squareLength+15+squareLength+1;

				ImageTestingOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength2, squareLength2);
			}
		}
		
		DetectChessSquaresBinary alg = new DetectChessSquaresBinary(4,3,50);
		
		assertTrue(alg.process(binary));
		
		List<QuadBlob> allBlobs = alg.getGraphBlobs();
		List<QuadBlob> cornerBlobs = alg.getCornerBlobs();

		assertEquals(3*4+2*3,allBlobs.size());
		assertEquals(4,cornerBlobs.size());
	}
}
