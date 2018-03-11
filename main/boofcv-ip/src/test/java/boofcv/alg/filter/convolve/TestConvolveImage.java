/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
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
public class TestConvolveImage extends CompareImageBorder {

	private int kernelWidth;
	private int kernelOffset;

	public TestConvolveImage() {
		super(ConvolveImage.class);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> e[] = m.getParameterTypes();

		for( Class<?> c : e ) {
			if( ImageGray.class.isAssignableFrom(c))
				return true;
			if( ImageInterleaved.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	@Test
	public void compareToNoBorder() {
		kernelWidth = 5; kernelOffset = 2;
		performTests(30);
		kernelWidth = 5; kernelOffset = 0;
		performTests(30);
		kernelWidth = 5; kernelOffset = 4;
		performTests(30);
		kernelWidth = 4; kernelOffset = 1;
		performTests(30);
	}

	/**
	 * Fillers the border in the larger image with an extended version of the smaller image.  A duplicate
	 * of the smaller image is contained in the center of the larger image.
	 */
	protected void fillTestImage(ImageBase smaller, ImageBase larger,
								 KernelBase kernel , String functionName ) {

		computeBorder(kernel,functionName);

		stripBorder(larger,borderX0,borderY0,borderX1,borderY1).setTo(smaller);

		GImageMultiBand s = FactoryGImageMultiBand.wrap(smaller);
		GImageMultiBand l = FactoryGImageMultiBand.wrap(larger);

		float pixel[] = new float[ s.getNumberOfBands() ];

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

				s.get(sx,sy, pixel);
				l.set(x,y,   pixel);
			}
		}

//		ShowImages.showWindow((GrayF32)larger,"large",true);
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

		ImageBase src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageBase dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		Object[][] ret = new Object[1][paramTypes.length];
		ret[0][0] = createKernel(paramTypes[0], kernelWidth, kernelOffset);
		ret[0][1] = src;
		ret[0][2] = dst;
		ret[0][3] = FactoryImageBorder.wrap(BorderType.EXTENDED, src);

		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret =  new Object[]{targetParam[0],targetParam[1],targetParam[2]};

		ImageBase inputImage = (ImageBase)targetParam[1];

		KernelBase kernel = (KernelBase)targetParam[0];

		computeBorder(kernel,m.getName());

		int w = borderX0+borderX1;
		int h = borderY0+borderY1;

		ret[1] = inputImage.createNew(width+w,height+h);
		ret[2] = ((ImageBase)targetParam[2]).createNew(width+w,height+h);

		fillTestImage(inputImage,(ImageBase)ret[1],kernel,m.getName());

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		ImageBase targetOut = (ImageBase)targetParam[2];
		ImageBase validationOut = (ImageBase)validationParam[2];

		// remove the border
		computeBorder((KernelBase)targetParam[0],methodTest.getName());
		validationOut = stripBorder(validationOut,borderX0,borderY0,borderX1,borderY1);

		GImageMultiBand t = FactoryGImageMultiBand.wrap(targetOut);
		GImageMultiBand v = FactoryGImageMultiBand.wrap(validationOut);

		float valueT[] = new float[ t.getNumberOfBands() ];
		float valueV[] = new float[ v.getNumberOfBands() ];

		for( int y = 0; y < targetOut.height; y++ ) {
			for( int x = 0; x < targetOut.width; x++ ) {
				t.get(x,y, valueT);
				v.get(x,y, valueV);

				for (int band = 0; band < valueT.length; band++) {
					assertEquals("Failed at "+x+" "+y+" band "+band,valueV[band],valueT[band],1e-4f);
				}
			}
		}
	}
}
