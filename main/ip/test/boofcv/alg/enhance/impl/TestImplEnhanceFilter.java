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

package boofcv.alg.enhance.impl;

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
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

		ImageSingleBand expected = GeneralizedImageOps.createSingleBand(input.getClass(), input.width, input.height);
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getTypeInfo().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0,255);

			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_I32,(ImageUInt8)input,(ImageUInt8)expected,0,255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner4",input,output,0f,255f);

			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance4_F32,(ImageFloat32)input,(ImageFloat32)expected,0,255);
		}

		BoofTesting.assertEqualsInner(expected,output,1e-5,1,1,false);
		BoofTesting.checkBorderZero(output, 1);
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

			sharpenInner4(input, output);

			BoofTesting.checkSubImage(this, "sharpenInner8", true, input, output);
		}

		assertEquals(2, numFound);
	}

	public void sharpenInner8( ImageSingleBand input , ImageSingleBand output ) {

		ImageSingleBand expected = GeneralizedImageOps.createSingleBand(input.getClass(), input.width, input.height);
		GImageMiscOps.fillUniform(input, rand, 0, 10);

		if( input.getTypeInfo().isInteger()) {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0,255);

			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_I32,(ImageUInt8)input,(ImageUInt8)expected,0,255);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceFilter.class,"sharpenInner8",input,output,0f,255f);

			ConvolveImageStandard.convolve(ImplEnhanceFilter.kernelEnhance8_F32,(ImageFloat32)input,(ImageFloat32)expected,0,255);
		}

		BoofTesting.assertEqualsInner(expected,output,1e-5,1,1,false);
		BoofTesting.checkBorderZero(output, 1);
	}

}
