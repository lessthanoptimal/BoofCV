/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDetectFiducialSquareImage {

	Random rand = new Random(234);
	InputToBinary<ImageUInt8> thesholder = FactoryThresholdBinary.globalFixed(50,true,ImageUInt8.class);

	@Test
	public void processSquare() {
		// randomly create a pattern
		ImageUInt8 pattern = new ImageUInt8(16*4,16*4);
		ImageMiscOps.fillUniform(pattern, rand, 0, 2);
		PixelMath.multiply(pattern,255,pattern);

		// add a border around it
		ImageUInt8 border = new ImageUInt8(16*8,16*8);
		border.subimage(16*2,16*2,16*6,16*6,null).setTo(pattern);
		ImageFloat32 input = new ImageFloat32(border.width,border.height);
		ConvertImage.convert(border,input);

//		BufferedImage foo = ConvertBufferedImage.convertTo(input,null);
//		ShowImages.showWindow(foo,"target");
//
//		BoofMiscOps.pause(10000);

		// process it in different orientations
		DetectFiducialSquareImage<ImageUInt8> alg =
				new DetectFiducialSquareImage<ImageUInt8>(null,null,0.2,0.1,ImageUInt8.class);

		alg.addImage(pattern,125);
		BaseDetectFiducialSquare.Result result = new BaseDetectFiducialSquare.Result();
		assertTrue(alg.processSquare(input, result));
		assertEquals(0, result.which);
		assertEquals(0,result.rotation);
		ImageFloat32 input2 = new ImageFloat32(input.width,input.height);
		ImageMiscOps.rotateCW(input,input2);
		assertTrue(alg.processSquare(input2, result));
		assertEquals(0,result.which);
		assertEquals(1,result.rotation);

		// give it a random input that shouldn't match
		ImageMiscOps.fillUniform(pattern, rand, 0, 2);
		PixelMath.multiply(pattern, 255, pattern);
		border.subimage(16*2,16*2,16*6,16*6,null).setTo(pattern);
		ConvertImage.convert(border,input);
		assertFalse(alg.processSquare(input, result));
	}

	@Test
	public void addImage() {
		ImageUInt8 image = new ImageUInt8(16*8,16*4);

		// fill just one square
		for (int y = 0; y < 1; y++) {
			for (int x = 0; x < 2; x++) {
				image.set(x,y,255);
			}
		}

		DetectFiducialSquareImage<ImageUInt8> alg =
				new DetectFiducialSquareImage<ImageUInt8>(null,null,0.2,0.1,ImageUInt8.class);

		alg.addImage(image,100);

		List<DetectFiducialSquareImage.FiducialDef> defs = alg.getTargets();
		assertEquals(1,defs.size());

		DetectFiducialSquareImage.FiducialDef def = defs.get(0);

		// manually construct the descriptor in each corner
		short desc[] = new short[16*16];
		Arrays.fill(desc,(short)0xFFFF);
		desc[0] = (short)0xFFFE;
		compare(desc,def.desc[0]);
		desc[0] = (short)0xFFFF;
		desc[3] = (short)0x7FFF;
		compare(desc,def.desc[1]);
		desc[3] = (short)0xFFFF;
		desc[255] = (short)0x7FFF;
		compare(desc,def.desc[2]);
		desc[255] = (short)0xFFFF;
		desc[252] = (short)0xFFFE;
		compare(desc,def.desc[3]);
	}

	private void compare( short a[] , short b[] ) {
		assertEquals(a.length,b.length);
		for (int i = 0; i < a.length; i++) {
			assertEquals("index = "+i,a[i],b[i]);
		}
	}

	@Test
	public void binaryToDef() {
		ImageUInt8 image = new ImageUInt8(8,4);

		ImageMiscOps.fillUniform(image,rand,0,2);

		short[] out = new short[2];

		DetectFiducialSquareImage.binaryToDef(image,out);

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

		DetectFiducialSquareImage alg = new DetectFiducialSquareImage(null,null,0.2,0.1,ImageFloat32.class);
		int found = alg.hamming(a, b);

		assertEquals(expected,found);
	}
}