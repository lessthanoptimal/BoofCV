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

package boofcv.io.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilImageIO extends BoofStandardJUnit {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test void loadImage_saveImage() throws IOException {
		BufferedImage orig = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				int a = rand.nextInt(255);
				int rgb = a << 16 | a << 8 << a;
				orig.setRGB(j,i,rgb);
			}
		}

		File temp = File.createTempFile("temp", ".png");

		UtilImageIO.saveImage(orig,temp.getPath());
		BufferedImage found = UtilImageIO.loadImage(temp.getPath());

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {

				int a = orig.getRGB(j,i) & 0xFF;
				int b = found.getRGB(j,i) & 0xFF;

				assertEquals(a,b);
			}
		}

		// clean up
		assertTrue(temp.delete());
	}

	@Test void loadImage_saveImage_PPM() throws IOException {

		Planar<GrayU8> orig = new Planar<>(GrayU8.class,width,height,3);
		GImageMiscOps.fillUniform(orig,rand,0,256);

		File temp = File.createTempFile("temp", ".png");
		UtilImageIO.savePPM(orig,temp.getPath(),null);
		Planar<GrayU8> found = UtilImageIO.loadPPM_U8(temp.getPath(),null,null);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				for( int k = 0; k < 3; k++ )
					assertEquals(orig.getBand(k).get(x,y),found.getBand(k).get(x,y));
			}
		}

		// clean up
		temp.delete();// no assertTrue() here because in windows it will fail
	}

	@Test void loadImage_saveImage_PGM() throws IOException {
		GrayU8 orig = new GrayU8(width,height);
		GImageMiscOps.fillUniform(orig,rand,0,256);

		File temp = File.createTempFile("temp", ".png");
		UtilImageIO.savePGM(orig,temp.getPath());
		GrayU8 found = UtilImageIO.loadPGM_U8(temp.getPath(),null);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(orig.get(x,y),found.get(x,y));
			}
		}

		// clean up
		temp.delete();// no assertTrue() here because in windows it will fail
	}

	/**
	 * See if load image fails gracefully if an image is not present
	 */
	@Test void loadImage_negative() {
		assertTrue( UtilImageIO.loadImage("asdasdasdasd") == null );
	}

}
