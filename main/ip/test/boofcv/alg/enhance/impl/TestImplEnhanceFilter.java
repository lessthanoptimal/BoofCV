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

package boofcv.alg.enhance.impl;

import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
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
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageSingleBand output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenInner4(input, output);

			BoofTesting.checkSubImage(this, "sharpenInner4", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenInner4( ImageSingleBand input , ImageSingleBand output ) {

		ImageSingleBand expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0,255);

			expected = new ImageSInt16(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_I32,(ImageUInt8)input,(ImageSInt16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0f,255f);

			expected = new ImageFloat32(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_F32,(ImageFloat32)input,(ImageFloat32)expected);
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
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageSingleBand output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenBorder4(input, output);

			BoofTesting.checkSubImage(this, "sharpenBorder4", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenBorder4( ImageSingleBand input , ImageSingleBand output ) {

		ImageSingleBand expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder4",input,output,0,255);

			expected = new ImageSInt16(input.width,input.height);
			ImageBorder_I32 border = BoofDefaults.borderDerivative_I32();
			border.setImage(input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance4_I32,border,(ImageSInt16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder4",input,output,0f,255f);

			expected = new ImageFloat32(input.width,input.height);
			ImageBorder_F32 border = BoofDefaults.borderDerivative_F32();
			border.setImage((ImageFloat32)input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance4_F32, border, (ImageFloat32) expected);
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
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageSingleBand output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenInner8(input, output);

			BoofTesting.checkSubImage(this, "sharpenInner8", true, input, output);
		}

		assertEquals(2, numFound);
	}

	public void sharpenInner8( ImageSingleBand input , ImageSingleBand output ) {

		ImageSingleBand expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0,255);

			expected = new ImageSInt16(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_I32,(ImageUInt8)input,(ImageSInt16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0f,255f);

			expected = new ImageFloat32(input.width,input.height);
			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_F32,(ImageFloat32)input,(ImageFloat32)expected);
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
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(imageType, width, height);
			ImageSingleBand output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			sharpenBorder8(input, output);

			BoofTesting.checkSubImage(this, "sharpenBorder8", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void sharpenBorder8( ImageSingleBand input , ImageSingleBand output ) {

		ImageSingleBand expected;
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getDataType().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder8",input,output,0,255);

			expected = new ImageSInt16(input.width,input.height);
			ImageBorder_I32 border = BoofDefaults.borderDerivative_I32();
			border.setImage(input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance8_I32,border,(ImageSInt16)expected);
			GPixelMath.boundImage(expected, 0, 255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenBorder8",input,output,0f,255f);

			expected = new ImageFloat32(input.width,input.height);
			ImageBorder_F32 border = BoofDefaults.borderDerivative_F32();
			border.setImage((ImageFloat32)input);
			ConvolveJustBorder_General.convolve(ImplEnhanceFilter.kernelEnhance8_F32, border, (ImageFloat32) expected);
			GPixelMath.boundImage(expected, 0, 255);
		}

		BoofTesting.assertEquals(expected,output,1e-5);
	}

}
