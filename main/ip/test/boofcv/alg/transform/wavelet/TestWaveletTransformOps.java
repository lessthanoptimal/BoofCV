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

package boofcv.alg.transform.wavelet;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformOps {

	Random rand = new Random(234);
	int width = 250;
	int height = 300;

   Class<?> typeInput;

	Class<?> types[]={ImageFloat32.class,ImageSInt32.class};

	/**
	 * See if it is possible to overflow the image
	 */
	@Test
	public void checkOverflow1() {
		for( Class t : types ) {
			checkOverflow1(t);
		}
	}

	public <T extends ImageSingleBand> void checkOverflow1( Class<T> typeInput ) {
		this.typeInput = typeInput;
		WaveletDescription<?> desc = createDesc(typeInput);

		int width = 20;
		int height = 22;

		T transform = GeneralizedImageOps.createSingleBand(typeInput, width + (width % 2), height + (height % 2));
		T found = GeneralizedImageOps.createSingleBand(typeInput, width, height);

		GImageMiscOps.fillUniform(transform, rand, 100, 150);

		invokeTransform(desc, null, transform, found,100,150);

		checkBounds(found,100,150);
	}

	/**
	 * See if images which are the smallest possible can be transformed.
	 */
	@Test
	public void smallImage1(){
		for( Class t : types ) {
			testSmallImage1(t);
		}
	}

	public <T extends ImageSingleBand> void testSmallImage1( Class<T> typeInput ) {
		this.typeInput = typeInput;
		WaveletDescription<?> desc = createDesc(typeInput);

		int length = Math.max(desc.getForward().getScalingLength(),desc.getForward().getWaveletLength());

		for( int i = length; i < 20; i++ ) {

			T input = GeneralizedImageOps.createSingleBand(typeInput, i, i);
			T output = GeneralizedImageOps.createSingleBand(typeInput, input.width + (input.width % 2), input.height + (input.height % 2));
			T found = GeneralizedImageOps.createSingleBand(typeInput, input.width, input.height);

			GImageMiscOps.fillUniform(input, rand, 0, 50);

			invokeTransform(desc, input, output, found,0,255);

			BoofTesting.assertEquals(input, found, 1e-4f);
		}
	}

	private WaveletDescription<?> createDesc(Class<?> typeInput) {
		boolean isFloat = GeneralizedImageOps.isFloatingPoint(typeInput);

		WaveletDescription<?> desc;

		if( isFloat )
			desc = FactoryWaveletDaub.daubJ_F32(4);
		else
			desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.WRAP);
		return desc;
	}


	@Test
	public void multipleLevel() {
		for( Class<?> t : types ) {
			testMultipleLevels(t);
		}
	}

	private void testMultipleLevels(Class typeInput) {
		this.typeInput = typeInput;

		WaveletDescription<?> desc = createDesc(typeInput);

		// try different sized images
		for( int adjust = 0; adjust < 5; adjust++ ) {
			int w = width+adjust;
			int h = height+adjust;
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(typeInput, w, h);
			ImageSingleBand found = GeneralizedImageOps.createSingleBand(typeInput, w, h);

			GImageMiscOps.fillUniform(input, rand, 0, 50);

			for( int level = 1; level <= 5; level++ ) {
				ImageDimension dim = UtilWavelet.transformDimension(w,h,level);
				ImageSingleBand output = GeneralizedImageOps.createSingleBand(typeInput, dim.width, dim.height);
//				System.out.println("adjust "+adjust+" level "+level+" scale "+ div);

				invokeTransformN(desc, (ImageSingleBand)input.clone(), output, found, level, 0, 255);

				BoofTesting.assertEquals(input, found, 1e-4f);
			}
		}
	}

	/**
	 * See if it is possible to overflow the image
	 */
	@Test
	public void checkOverflowN() {
		for( Class t : types ) {
			checkOverflowN(t);
		}
	}

	public <T extends ImageSingleBand> void checkOverflowN( Class<T> typeInput ) {
		this.typeInput = typeInput;
		WaveletDescription<?> desc = createDesc(typeInput);

		int width = 20;
		int height = 22;
		int level = 3;

		ImageDimension dim = UtilWavelet.transformDimension(width,height,level);

		T transform = GeneralizedImageOps.createSingleBand(typeInput, dim.width,dim.height);
		T found = GeneralizedImageOps.createSingleBand(typeInput, width, height);

		GImageMiscOps.fillUniform(transform, rand, 100, 150);

		invokeTransformN(desc, null, transform, found, 3, 100, 150);

		checkBounds(found,100,150);
	}

	private void invokeTransform( WaveletDescription desc,
								  ImageSingleBand input, ImageSingleBand output, ImageSingleBand found,
								  double minValue , double maxValue ) {
		if( input != null ) {
			if( input.getDataType().isInteger() ) {
				WaveletTransformOps.transform1(desc, (ImageSInt32) input, (ImageSInt32) output, null);
			} else {
				WaveletTransformOps.transform1(desc, (ImageFloat32) input, (ImageFloat32) output, null);
			}
		}

		if( output.getDataType().isInteger() ) {
			WaveletTransformOps.inverse1(desc,(ImageSInt32)output,(ImageSInt32)found,null,
					(int)minValue,(int)maxValue);
		} else {
			WaveletTransformOps.inverse1(desc,(ImageFloat32)output,(ImageFloat32)found,null,
					(float)minValue,(float)maxValue);
		}
	}

	private void invokeTransformN( WaveletDescription desc,
								   ImageSingleBand input, ImageSingleBand output, ImageSingleBand found,
								   int numLevels ,
								   double minValue , double maxValue ) {
		if( input != null ) {
			if( input.getDataType().isInteger() ) {
				WaveletTransformOps.transformN(desc, (ImageSInt32) input, (ImageSInt32) output, null, numLevels);
			} else {
				WaveletTransformOps.transformN(desc, (ImageFloat32) input, (ImageFloat32) output, null, numLevels);
			}
		}

		if( output.getDataType().isInteger() ) {
			WaveletTransformOps.inverseN(desc, (ImageSInt32) output, (ImageSInt32) found, null, numLevels,
					(int) minValue, (int) maxValue);
		} else {
			WaveletTransformOps.inverseN(desc, (ImageFloat32) output, (ImageFloat32) found, null, numLevels,
					(float) minValue, (float) maxValue);
		}
	}


	private void checkBounds( ImageSingleBand image , double low , double upper ) {
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				double v = GeneralizedImageOps.get(image,x,y);
				if( v < low || v > upper )
					fail("out of bounds");
			}
		}
	}
}
