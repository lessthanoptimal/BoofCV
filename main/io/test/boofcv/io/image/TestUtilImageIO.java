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

package boofcv.io.image;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilImageIO {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test
	public void loadImage_saveImage() {
		BufferedImage orig = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				int a = rand.nextInt(255);
				int rgb = a << 16 | a << 8 << a;
				orig.setRGB(j,i,rgb);
			}
		}

		UtilImageIO.saveImage(orig,"temp.png");
		BufferedImage found = UtilImageIO.loadImage("temp.png");

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {

				int a = orig.getRGB(j,i) & 0xFF;
				int b = found.getRGB(j,i) & 0xFF;

				assertEquals(a,b);
			}
		}

		// clean up
		File f = new File("temp.png");
		assertTrue(f.delete());
	}

	/**
	 * See if load image fails gracefully if an image is not present
	 */
	@Test
	public void loadImage_negative() {
		assertTrue( UtilImageIO.loadImage("asdasdasdasd") == null );
	}

}
