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

package boofcv.alg.filter.convolve.border;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorderValue;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestConvolveJustBorder_General extends CompareImageBorder {

	int kernelWidth = 5;
	int fillValue = 1;

	public TestConvolveJustBorder_General() {
		super(ConvolveJustBorder_General.class);
	}

	/**
	 * Compare the results along the border to the results obtained by convolving a larger image with the noborder algorithm
	 * whose border has been filled with the fillValue.
	 */
	@Test
	public void compareToNoBorder() {
		performTests(9);
	}

	protected void fillTestImage(ImageSingleBand smaller, ImageSingleBand larger ,
								 int borderX0 , int borderY0 ,
								 int borderX1 , int borderY1 )
	{
		// set the while image equal to the specified value
		GImageMiscOps.fill(larger,fillValue);

		// make the inner post part equal to the original image
		stripBorder(larger,borderX0,borderY0,borderX1,borderY1).setTo(smaller);
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method evaluation) {
		if( evaluation.getName().compareTo(candidate.getName()) != 0 )
			return false;

		Class<?> e[] = evaluation.getParameterTypes();
		Class<?> c[] = candidate.getParameterTypes();

		if( e.length != c.length )
			return false;

		if( e[0] != c[0] )
			return false;
		if( !e[2].isAssignableFrom(c[2]) )
			return false;

		return true;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret =  new Object[]{targetParam[0],targetParam[1],targetParam[2]};

		ImageSingleBand inputImage = ((ImageBorder)targetParam[1]).getImage();

		KernelBase kernel = (KernelBase)targetParam[0];

		computeBorder(kernel,m.getName());

		int borderW = borderX0 + borderX1;
		int borderH = borderY0 + borderY1;

		ret[1] = inputImage._createNew(width+borderW,height+borderH);
		ret[2] = ((ImageSingleBand)targetParam[2])._createNew(width+borderW,height+borderH);

		fillTestImage(inputImage,(ImageSingleBand)ret[1],borderX0,borderY0,borderX1,borderY1);

		return ret;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		KernelBase kernel = createKernel(paramTypes[0],kernelWidth/2,kernelWidth);

		ImageSingleBand src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		Object[][] ret = new Object[2][paramTypes.length];
		// normal symmetric odd kernel
		ret[0][0] = kernel;
		ret[0][1] = ImageFloat32.class == src.getClass() ?
				ImageBorderValue.wrap((ImageFloat32)src,fillValue) : ImageBorderValue.wrap((ImageInteger)src,fillValue);
		ret[0][2] = dst;

		// change the offset
		kernel = createKernel(paramTypes[0],kernelWidth/2-1,kernelWidth);
		ret[1][0] = kernel;
		ret[1][1] = ImageFloat32.class == src.getClass() ?
				ImageBorderValue.wrap((ImageFloat32)src,fillValue) : ImageBorderValue.wrap((ImageInteger)src,fillValue);
		ret[1][2] = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		ImageSingleBand targetOut = (ImageSingleBand)targetParam[2];
		ImageSingleBand validationOut = (ImageSingleBand)validationParam[2];

		// remove the border
		computeBorder((KernelBase)targetParam[0],methodTest.getName());
		validationOut = stripBorder(validationOut,borderX0,borderY0,borderX1,borderY1);

		GImageSingleBand t = FactoryGImageSingleBand.wrap(targetOut);
		GImageSingleBand v = FactoryGImageSingleBand.wrap(validationOut);

		for( int y = 0; y < targetOut.height; y++ ) {
			if( y >= borderX0 &&  y < targetOut.height-borderX1 )
				continue;
			for( int x = 0; x < targetOut.width; x++ ) {
				if( x >= borderX0 &&  x < targetOut.width-borderY1 )
					continue;

				assertEquals(x+" "+y,v.get(x,y).doubleValue(),t.get(x,y).doubleValue(),1e-4f);
			}
		}
	}
}
