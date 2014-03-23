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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.border.CompareImageBorder;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Compares convolve extended against an inner convolution performed against a larger image
 * which is an extension of the original image.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestConvolveWithBorder extends CompareImageBorder {

	int kernelWidth = 5;

	public TestConvolveWithBorder() {
		super(ConvolveWithBorder.class);
	}

	@Test
	public void compareToNoBorder() {
		performTests(12);
	}

	/**
	 * Fillers the border in the larger image with an extended version of the smaller image.  A duplicate
	 * of the smaller image is contained in the center of the larger image.
	 */
	protected void fillTestImage(ImageSingleBand smaller, ImageSingleBand larger,
								 KernelBase kernel , String functionName ) {

		computeBorder(kernel,functionName);

		stripBorder(larger,borderX0,borderY0,borderX1,borderY1).setTo(smaller);

		GImageSingleBand s = FactoryGImageSingleBand.wrap(smaller);
		GImageSingleBand l = FactoryGImageSingleBand.wrap(larger);

		for( int y = 0; y < larger.height; y++ ) {
			for( int x = 0; x < larger.width; x++ ) {
				int sx = x-borderX0;
				int sy = y-borderY0;

				if( sx < 0 )
					sx = 0;
				else if( sx >= smaller.width  )
					sx = smaller.width-1;

				if( sy < 0 )
					sy = 0;
				else if( sy >= smaller.height  )
					sy = smaller.height-1;

				l.set(x,y,s.get(sx,sy));
			}
		}

//		ShowImages.showWindow((ImageFloat32)larger,"large",true);
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method evaluation) {
		if( evaluation.getName().compareTo(candidate.getName()) != 0 )
			return false;

		Class<?> e[] = evaluation.getParameterTypes();
		Class<?> c[] = candidate.getParameterTypes();

		if( evaluation.getName().compareTo("convolve") == 0) {
			if( e.length != c.length+1 )
				return false;
		} else  {
			if( e.length != c.length+1 )
				return false;
		}
		if( e[0] != c[0] )
			return false;
		if( !e[1].isAssignableFrom(c[1]) )
			return false;
		if( !e[2].isAssignableFrom(c[2]) )
			return false;

		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object kernel = createKernel(paramTypes[0],kernelWidth/2,kernelWidth);

		ImageSingleBand src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		Object[][] ret = new Object[2][paramTypes.length];
		ret[0][0] = kernel;
		ret[0][1] = src;
		ret[0][2] = dst;
		ret[0][3] = FactoryImageBorder.general(src, BorderType.EXTENDED);

		ret[1][0] = createKernel(paramTypes[0],0,kernelWidth);
		ret[1][1] = src;
		ret[1][2] = dst;
		ret[1][3] = FactoryImageBorder.general(src, BorderType.EXTENDED);

		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret =  new Object[]{targetParam[0],targetParam[1],targetParam[2]};

		ImageSingleBand inputImage = (ImageSingleBand)targetParam[1];

		KernelBase kernel = (KernelBase)targetParam[0];

		computeBorder(kernel,m.getName());

		int w = borderX0+borderX1;
		int h = borderY0+borderY1;

		ret[1] = inputImage._createNew(width+w,height+h);
		ret[2] = ((ImageSingleBand)targetParam[2])._createNew(width+w,height+h);

		fillTestImage(inputImage,(ImageSingleBand)ret[1],kernel,m.getName());

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
			for( int x = 0; x < targetOut.width; x++ ) {
				assertEquals("Failed at "+x+" "+y,v.get(x,y).doubleValue(),t.get(x,y).doubleValue(),1e-4f);
			}
		}
	}
}
