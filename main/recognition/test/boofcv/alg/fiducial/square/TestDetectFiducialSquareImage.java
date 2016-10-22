/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.square;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.core.image.ConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDetectFiducialSquareImage {

	private Random rand = new Random(234);
	private BinaryPolygonDetector squareDetector = FactoryShapeDetector.
			polygon(new ConfigPolygonDetector(false, 4,4), GrayU8.class);
	private InputToBinary<GrayU8> inputToBinary = FactoryThresholdBinary.globalFixed(50, true, GrayU8.class);

	@Test
	public void processSquare() {
		// randomly create a pattern
		GrayU8 pattern = new GrayU8(16*4,16*4);
		ImageMiscOps.fillUniform(pattern, rand, 0, 2);
		PixelMath.multiply(pattern,255,pattern);

		// add a border around it
		GrayU8 border = new GrayU8(16*8,16*8);
		border.subimage(16*2,16*2,16*6,16*6,null).setTo(pattern);
		GrayF32 input = new GrayF32(border.width,border.height);
		ConvertImage.convert(border,input);

//		BufferedImage foo = ConvertBufferedImage.convertTo(input,null);
//		ShowImages.showWindow(foo,"target");
//
//		BoofMiscOps.pause(10000);

		// process it in different orientations
		DetectFiducialSquareImage<GrayU8> alg =
				new DetectFiducialSquareImage<>(inputToBinary,squareDetector,0.25,0.65,0.1,GrayU8.class);

		alg.addPattern(threshold(pattern, 125), 1.0);
		BaseDetectFiducialSquare.Result result = new BaseDetectFiducialSquare.Result();
		assertTrue(alg.processSquare(input, result,0,0));
		assertEquals(0,result.which);
		assertEquals(0,result.rotation);
		GrayF32 input2 = new GrayF32(input.width,input.height);
		ImageMiscOps.rotateCCW(input,input2);
		assertTrue(alg.processSquare(input2, result,0,0));
		assertEquals(0,result.which);
		assertEquals(1,result.rotation);
		ImageMiscOps.rotateCCW(input2,input);
		assertTrue(alg.processSquare(input, result,0,0));
		assertEquals(0,result.which);
		assertEquals(2,result.rotation);
		ImageMiscOps.rotateCCW(input,input2);
		assertTrue(alg.processSquare(input2, result,0,0));
		assertEquals(0,result.which);
		assertEquals(3,result.rotation);

		// give it a random input that shouldn't match
		ImageMiscOps.fillUniform(pattern, rand, 0, 2);
		PixelMath.multiply(pattern, 255, pattern);
		border.subimage(16*2,16*2,16*6,16*6,null).setTo(pattern);
		ConvertImage.convert(border,input);
		assertFalse(alg.processSquare(input, result,0,0));
	}

	@Test
	public void addPattern() {
		GrayU8 image = new GrayU8(16*8,16*4);

		// fill just one square
		for (int y = 0; y < 1; y++) {
			for (int x = 0; x < 2; x++) {
				image.set(x,y,255);
			}
		}

		DetectFiducialSquareImage<GrayU8> alg =
				new DetectFiducialSquareImage<>(inputToBinary,squareDetector,0.25,0.65,0.1,GrayU8.class);

		alg.addPattern(threshold(image, 100), 1.0);

		List<DetectFiducialSquareImage.FiducialDef> defs = alg.getTargets();
		assertEquals(1,defs.size());

		DetectFiducialSquareImage.FiducialDef def = defs.get(0);

		// manually construct the descriptor in each corner
		short desc[] = new short[16*16];
		Arrays.fill(desc,(short)0x0000);
		desc[0] = (short)0x0001;
		compare(desc, def.desc[0]);
		desc[0] = (short)0x0000;
		desc[252] = (short)0x0001;
		compare(desc, def.desc[1]);
		desc[252] = (short)0x0000;
		desc[255] = (short)0x8000;
		compare(desc,def.desc[2]);
		desc[255] = (short)0x0000;
		desc[3] = (short)0x8000;
		compare(desc,def.desc[3]);
	}

	private void compare( short a[] , short b[] ) {
		assertEquals(a.length, b.length);
		for (int i = 0; i < a.length; i++) {
			assertEquals("index = "+i,a[i],b[i]);
		}
	}

	@Test
	public void binaryToDef() {
		GrayU8 image = new GrayU8(8,4);

		ImageMiscOps.fillUniform(image,rand,0,2);

		short[] out = new short[2];

		DetectFiducialSquareImage.binaryToDef(image, out);

		for (int i = 0; i < 32; i++) {
			int expected = image.data[i];
			int found = (out[i/16] >> (i%16)) & 1;

			assertEquals(expected,found);
		}
	}

	@Test
	public void hamming() {
		short[] a = new short[3];
		short[] b = new short[3];

		for (int i = 0; i < 3; i++) {
			a[i] = (short)rand.nextInt();
			b[i] = (short)rand.nextInt();
		}

		int expected = 0;
		for (int i = 0; i < 16*3; i++) {
			int valA = (a[i/16] >> (i%16)) & 1;
			int valB = (b[i/16] >> (i%16)) & 1;

			expected += valA != valB ? 1 : 0;
		}

		DetectFiducialSquareImage alg = new DetectFiducialSquareImage(inputToBinary,squareDetector,0.25,0.65,0.1,GrayF32.class);
		int found = alg.hamming(a, b);

		assertEquals(expected, found);
	}

	/**
	 * See if it can process a border that isn't 0.25
	 */
	@Test
	public void checkDifferentBorder() {
		double borderWidths[] = new double[]{0.1,0.2,0.3};

		for( double border : borderWidths ) {
			// randomly create a pattern
			GrayU8 pattern = new GrayU8(16*4,16*4);
			ImageMiscOps.fillUniform(pattern, rand, 0, 2);
			PixelMath.multiply(pattern,255,pattern);

			// add a border around it
			int w = (int)Math.round(16*4/(1.0-2.0*border));
			int r = (w-pattern.width)/2;
			GrayU8 bordered = new GrayU8(w,w);
			bordered.subimage(r, r, r + 16 * 4, r + 16 * 4, null).setTo(pattern);
			GrayF32 input = new GrayF32(bordered.width,bordered.height);
			ConvertImage.convert(bordered,input);

			DetectFiducialSquareImage alg = new DetectFiducialSquareImage(inputToBinary,squareDetector,border,0.65,0.1,GrayF32.class);
			alg.addPattern(threshold(pattern, 125), 1.0);
			BaseDetectFiducialSquare.Result result = new BaseDetectFiducialSquare.Result();
			assertTrue(alg.processSquare(input, result,0,0));
		}
	}

	private GrayU8 threshold(ImageGray image , double threshold ) {
		GrayU8 out = new GrayU8(image.width,image.height);
		GThresholdImageOps.threshold(image,out,threshold,false);
		return out;
	}
}