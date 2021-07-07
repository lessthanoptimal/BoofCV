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

package boofcv.alg.filter.convolve.border;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.ImageBorderValue;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.InterleavedF32;
import boofcv.struct.image.InterleavedInteger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestConvolveJustBorder_General_IL extends CompareImageBorder {

	int kernelWidth = 5;
	int fillValue = 1;

	public TestConvolveJustBorder_General_IL() {
		super(ConvolveJustBorder_General_IL.class);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> e[] = m.getParameterTypes();

		for( Class<?> c : e ) {
			if( ImageInterleaved.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	/**
	 * Compare the results along the border to the results obtained by convolving a larger image with the noborder algorithm
	 * whose border has been filled with the fillValue.
	 */
	@Test void compareToNoBorder() {
		performTests(9);
	}

	protected void fillTestImage(ImageInterleaved smaller, ImageInterleaved larger ,
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

		ImageInterleaved inputImage = (ImageInterleaved)((ImageBorder) targetParam[1]).getImage();

		KernelBase kernel = (KernelBase)targetParam[0];

		computeBorder(kernel,m.getName());

		int borderW = borderX0 + borderX1;
		int borderH = borderY0 + borderY1;

		ret[1] = inputImage.createNew(width+borderW,height+borderH);
		ret[2] = ((ImageInterleaved)targetParam[2]).createNew(width+borderW,height+borderH);

		fillTestImage(inputImage,(ImageInterleaved)ret[1],borderX0,borderY0,borderX1,borderY1);

		return ret;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		KernelBase kernel = createKernel(paramTypes[0], kernelWidth, kernelWidth/2);

		ImageBase src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageBase dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		Object[][] ret = new Object[2][paramTypes.length];
		// normal symmetric odd kernel
		ret[0][0] = kernel;
		ret[0][1] = InterleavedF32.class == src.getClass() ?
				ImageBorderValue.wrap((InterleavedF32)src,fillValue) : ImageBorderValue.wrap((InterleavedInteger)src,fillValue);
		ret[0][2] = dst;

		// change the offset
		kernel = createKernel(paramTypes[0], kernelWidth, kernelWidth/2-1);
		ret[1][0] = kernel;
		ret[1][1] = InterleavedF32.class == src.getClass() ?
				ImageBorderValue.wrap((InterleavedF32)src,fillValue) : ImageBorderValue.wrap((InterleavedInteger)src,fillValue);
		ret[1][2] = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		ImageInterleaved targetOut = (ImageInterleaved)targetParam[2];
		ImageInterleaved validationOut = (ImageInterleaved)validationParam[2];

		// remove the border
		computeBorder((KernelBase)targetParam[0],methodTest.getName());
		validationOut = (ImageInterleaved)stripBorder(validationOut,borderX0,borderY0,borderX1,borderY1);

		GImageMultiBand t = FactoryGImageMultiBand.wrap(targetOut);
		GImageMultiBand v = FactoryGImageMultiBand.wrap(validationOut);

		float pixelT[] = new float[ t.getNumberOfBands() ];
		float pixelV[] = new float[ t.getNumberOfBands() ];

		for( int y = 0; y < targetOut.height; y++ ) {
			if( y >= borderX0 &&  y < targetOut.height-borderX1 )
				continue;
			for( int x = 0; x < targetOut.width; x++ ) {
				if( x >= borderX0 &&  x < targetOut.width-borderY1 )
					continue;
				t.get(x,y,pixelT);
				v.get(x,y,pixelV);

				for (int band = 0; band < t.getNumberOfBands(); band++) {
					assertEquals(pixelV[band],pixelT[band],1e-4f);
				}
			}
		}
	}
}
