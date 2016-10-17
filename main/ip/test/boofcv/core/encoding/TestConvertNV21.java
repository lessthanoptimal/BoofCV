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

package boofcv.core.encoding;

import boofcv.alg.color.ColorYuv;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvertNV21 {
	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test
	public void testGray() {
		Class types[] = new Class[]{GrayU8.class, GrayF32.class};

		byte[] data = random(width,height);

		for( Class type : types ) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type, width, height);
			ConvertYV12.yu12ToBoof(data,width,height,image);

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double found = GeneralizedImageOps.get(image,x,y);
					int expected = data[y*width+x] & 0xFF;

					assertEquals(expected,found,1e-8);
				}
			}
		}
	}

	@Test
	public void testColor() {
		ImageType types[] = new ImageType[]{ImageType.pl(3, ImageDataType.U8),ImageType.pl(3,ImageDataType.F32),
				ImageType.il(3, ImageDataType.U8),ImageType.il(3, ImageDataType.F32)};

		byte[] data = random(width,height);
		Planar<GrayU8> yuv = new Planar<>(GrayU8.class,width,height,3);
		Planar<GrayU8> rgb = new Planar<>(GrayU8.class,width,height,3);

		nv21ToMulti(data, width, height, yuv);
		ColorYuv.ycbcrToRgb_U8(yuv, rgb);

		for( ImageType type : types ) {
			ImageMultiBand image = (ImageMultiBand)type.createImage(width,height);
			ConvertNV21.nv21ToBoof(data, width, height, image);


			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int band = 0; band < 3; band++) {
						double found = GeneralizedImageOps.get(image,x,y,band);
						int expected = rgb.getBand(band).get(x,y);

						assertEquals(x+" "+y+" "+band,expected,found,1e-4);
					}
				}
			}
		}
	}

	private byte[] random( int width , int height ) {
		int length = width*height + (width*height/4)*2;
		byte[] data = new byte[length];
		rand.nextBytes(data);
		return data;
	}

	public void nv21ToMulti( byte[] data , int width , int height , Planar<GrayU8> yuv ) {
		yuv.reshape(width, height);

		int size = width*height;

		System.arraycopy(data,0,yuv.getBand(0).getData(),0,size);

		int index = size;
		for (int y = 0; y < height / 2; y++) {
			for (int x = 0; x < width / 2; x++) {
				int value0 = data[index++] & 0xFF;
				int value1 = data[index++] & 0xFF;

				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 2; j++) {
						yuv.getBand(2).set(2*x+j,2*y+i,value0);
						yuv.getBand(1).set(2*x+j,2*y+i,value1);
					}
				}
			}
		}
	}

}
