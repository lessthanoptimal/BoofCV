/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.core.encoding;

import boofcv.alg.color.ColorYuv;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
class TestConvertYuyv extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	@Test
	void testGray() {
		Class[] types = new Class[]{GrayU8.class, GrayF32.class};

		byte[] data = random(width,height);

		for( Class type : types ) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type,width,height);
			ConvertYuyv.yuyvToBoof(data,width,height,image);

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double found = GeneralizedImageOps.get(image,x,y);
					int expected = data[y*width*2+x*2] & 0xFF;

					assertEquals(expected,found,1e-8);
				}
			}
		}
	}

	@Test
	void testColor() {
		ImageType[] types = new ImageType[]{ImageType.PL_U8,ImageType.PL_F32,ImageType.IL_U8,ImageType.PL_F32};

		byte[] data = random(width,height);
		Planar<GrayU8> yuv = new Planar<>(GrayU8.class,width,height,3);
		Planar<GrayU8> rgb = new Planar<>(GrayU8.class,width,height,3);

		yuyvToPlanar(data, width, height, yuv);
		ColorYuv.yuvToRgb(yuv,rgb);

		for( ImageType type : types ) {
			ImageMultiBand image = (ImageMultiBand)type.createImage(width,height);
			ConvertYuyv.yuyvToBoof(data,width,height,image);

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int band = 0; band < 3; band++) {
						double found = GeneralizedImageOps.get(image,x,y,band);
						int expected = rgb.getBand(band).get(x,y);

						assertEquals(expected,found,1e-4);
					}
				}
			}
		}
	}

	private byte[] random( int width , int height ) {
		int length = width*height*2;
		byte[] data = new byte[length];
		rand.nextBytes(data);
		return data;
	}

	void yuyvToPlanar(byte[] data , int width , int height , Planar<GrayU8> yuv ) {
		yuv.reshape(width, height);

		int size = width*height;

		System.arraycopy(data,0,yuv.getBand(0).getData(),0,size);

		for (int y = 0; y < height; y++) {
			int indexY = y*width*2;
			int indexU = indexY+1;
			for (int x = 0; x < width; x++, indexY+=2) {
				int Y = data[indexY] & 0xFF;
				int U = data[indexU] & 0xFF;
				int V = data[indexU+2] & 0xFF;

				yuv.getBand(0).set(x,y,Y);
				yuv.getBand(1).set(x,y,U);
				yuv.getBand(2).set(x,y,V);

				indexU += 4*(x&0x1);
			}
		}
	}
}
