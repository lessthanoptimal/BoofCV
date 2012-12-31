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

package boofcv.alg.feature.detect.quadblob;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectQuadBlobsBinary {

	/**
	 * Give it a few easy to detect squares and one that is too skinny and should be filtered
	 */
	@Test
	public void basicTest() {
		int squareLength = 30;
		int w = 400;
		int h = 500;
		ImageUInt8 binary = new ImageUInt8(w,h);

		// create the grid
		for( int y = 0; y < 3; y++) {
			for( int x = 0; x < 4; x++ ) {
				int pixelY = y*(squareLength+10)+10;
				int pixelX = x*(squareLength+10)+15;

				ImageMiscOps.fillRectangle(binary, 1, pixelX, pixelY, squareLength, squareLength);
			}
		}

		// add a rectangle that is too skinny
		ImageMiscOps.fillRectangle(binary, 1, w-50, 100, 25, 2);

		// another one touching the border
		ImageMiscOps.fillRectangle(binary, 1, w-20, h-20, 20, 20);
		
		DetectQuadBlobsBinary alg = new DetectQuadBlobsBinary(15,0.25,0);
		
		assertTrue(alg.process(binary));
		
		assertEquals(3*4,alg.getDetected().size());
	}
}
