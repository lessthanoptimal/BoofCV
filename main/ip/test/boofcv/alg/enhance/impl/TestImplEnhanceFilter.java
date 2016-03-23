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

package boofcv.alg.enhance.impl;

import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplEnhanceFilter {

	int width = 15;
	int height = 20;
	Random rand = new Random(234);

	/**
	 * Compare the sharpen filter to a bounded convolution
	 */
	@Test
	public void sharpenInner4() {
		int numFound = 0;

		Method methods[] = ImplEnhanceFilter.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("sharpenInner4") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			ImageGray input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenInner4(input, output);

			BoofTesting.checkSubImage(this, "sharpenInner4", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenInner4(ImageGray input , ImageGray output ) {

		ImageGray expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0,255);

			expected = new GrayS16(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_I32,(GrayU8)input,(GrayS16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0f,255f);

			expected = new GrayF32(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_F32,(GrayF32)input,(GrayF32)expected);
			GPixelMath.boundImage(expected, 0, 255);
		}

		BoofTesting.assertEqualsInner(expected,output,1e-5,1,1,false);
		BoofTesting.checkBorderZero(output, 1);
	}

	/**
	 * Compare the sharpen filter to a bounded convolution
	 */
	@Test
	public void sharpenBorder4() {
		int numFound = 0;

		Method methods[] = ImplEnhanceFilter.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("sharpenBorder4") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			ImageGray input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenBorder4(input, output);

			BoofTesting.checkSubImage(this, "sharpenBorder4", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenBorder4(ImageGray input , ImageGray output ) {

		ImageGray expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder4",input,output,0,255);

			expected = new GrayS16(input.width,input.height);
			ImageBorder_S32 border = BoofDefaults.borderDerivative_I32();
			border.setImage(input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance4_I32,border,(GrayS16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder4",input,output,0f,255f);

			expected = new GrayF32(input.width,input.height);
			ImageBorder_F32 border = BoofDefaults.borderDerivative_F32();
			border.setImage((GrayF32)input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance4_F32, border, (GrayF32) expected);
			GPixelMath.boundImage(expected, 0, 255);
		}

		BoofTesting.assertEquals(expected, output, 1e-5);
	}

	/**
	 * Compare the sharpen filter to a bounded convolution
	 */
	@Test
	public void sharpenInner8() {
		int numFound = 0;

		Method methods[] = ImplEnhanceFilter.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("sharpenInner8") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			ImageGray input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenInner8(input, output);

			BoofTesting.checkSubImage(this, "sharpenInner8", true, input, output);
		}

		assertEquals(2, numFound);
	}

	public void sharpenInner8(ImageGray input , ImageGray output ) {

		ImageGray expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0,255);

			expected = new GrayS16(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_I32,(GrayU8)input,(GrayS16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0f,255f);

			expected = new GrayF32(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_F32,(GrayF32)input,(GrayF32)expected);
			GPixelMath.boundImage(expected, 0, 255);
		}

		BoofTesting.assertEqualsInner(expected,output,1e-5,1,1,false);
		BoofTesting.checkBorderZero(output, 1);
	}

	/**
	 * Compare the sharpen filter to a bounded convolution
	 */
	@Test
	public void sharpenBorder8() {
		int numFound = 0;

		Method methods[] = ImplEnhanceFilter.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("sharpenBorder8") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			ImageGray input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenBorder8(input, output);

			BoofTesting.checkSubImage(this, "sharpenBorder8", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenBorder8(ImageGray input , ImageGray output ) {

		ImageGray expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder8",input,output,0,255);

			expected = new GrayS16(input.width,input.height);
			ImageBorder_S32 border = BoofDefaults.borderDerivative_I32();
			border.setImage(input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance8_I32,border,(GrayS16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder8",input,output,0f,255f);

			expected = new GrayF32(input.width,input.height);
			ImageBorder_F32 border = BoofDefaults.borderDerivative_F32();
			border.setImage((GrayF32)input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance8_F32, border, (GrayF32) expected);
			GPixelMath.boundImage(expected, 0, 255);
		}

		BoofTesting.assertEquals(expected,output,1e-5);
	}

}
