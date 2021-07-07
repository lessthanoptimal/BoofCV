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

package boofcv.alg.transform.ii.impl;

import boofcv.BoofTesting;
import boofcv.alg.filter.convolve.ConvolveImage;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.struct.ImageRectangle;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplIntegralImageConvolve extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	@Test void convolve() {
		int numFound = BoofTesting.findMethodThenCall(this,"convolve",ImplIntegralImageConvolve.class,"convolve");
		assertEquals(4, numFound);
	}

	public void convolve( Method m ) throws InvocationTargetException, IllegalAccessException {
		Kernel2D_S32 kernel = new Kernel2D_S32(3, new int[]{1,1,1,2,2,2,1,1,1});
		GrayU8 input = new GrayU8(width,height);
		GrayS32 expected = new GrayS32(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 10);
		ImageBorder_S32 border = FactoryImageBorderAlgs.value( input, 0);
		ConvolveImage.convolve(kernel,input,expected,border);

		Class[] paramType = m.getParameterTypes();
		Class inputType = paramType[0];
		Class outputType = paramType[2];

		ImageGray inputII = GeneralizedImageOps.createSingleBand(inputType, width, height);
		ImageGray integral = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageGray expectedII = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageGray found = GeneralizedImageOps.createSingleBand(outputType, width, height);

		GConvertImage.convert(input,inputII);
		GConvertImage.convert(expected,expectedII);

		GIntegralImageOps.transform(inputII, integral);

		IntegralKernel kernelII = new IntegralKernel(2);
		kernelII.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernelII.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernelII.scales = new int[]{1,1};

		m.invoke(null,integral,kernelII,found);

		BoofTesting.assertEqualsRelative(expected, found, 1e-4f);
	}


	@Test void convolveBorder() {
		int numFound = BoofTesting.findMethodThenCall(this,"convolveBorder",ImplIntegralImageConvolve.class,"convolveBorder");
		assertEquals(4,numFound);
	}

	public void convolveBorder( Method m ) throws InvocationTargetException, IllegalAccessException {
		Kernel2D_S32 kernel = new Kernel2D_S32(3, new int[]{1,1,1,2,2,2,1,1,1});
		GrayU8 input = new GrayU8(width,height);
		GrayS32 expected = new GrayS32(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 10);
		ImageBorder_S32 border = FactoryImageBorderAlgs.value( input, 0);
		ConvolveImage.convolve(kernel,input,expected,border);

		Class[] paramType = m.getParameterTypes();
		Class inputType = paramType[0];
		Class outputType = paramType[2];

		ImageGray inputII = GeneralizedImageOps.createSingleBand(inputType, width, height);
		ImageGray integral = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageGray expectedII = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageGray found = GeneralizedImageOps.createSingleBand(outputType, width, height);

		GConvertImage.convert(input,inputII);
		GConvertImage.convert(expected,expectedII);

		GIntegralImageOps.transform(inputII, integral);

		IntegralKernel kernelII = new IntegralKernel(2);
		kernelII.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernelII.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernelII.scales = new int[]{1,1};

		m.invoke(null,integral,kernelII,found,4,5);

		BoofTesting.assertEqualsBorder(expected,found,1e-4f,4,5);
	}
}
