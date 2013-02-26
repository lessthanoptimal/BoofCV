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

package boofcv.alg.filter.convolve.border;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorderValue;
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

	protected void fillTestImage(ImageSingleBand smaller, ImageSingleBand larger) {
		// set the while image equal to the specified value
		GImageSingleBand image = FactoryGImageSingleBand.wrap(larger);
		for( int y = 0; y < image.getHeight(); y++ ) {
			for( int x = 0; x < image.getWidth(); x++ ) {
				image.set(x,y,fillValue);
			}
		}

		// make the inner post part equal to the original image
		stripBorder(larger).setTo(smaller);
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
			if( e.length != c.length )
				return false;
		}

		if( e[0] != c[0] )
			return false;
		if( !e[2].isAssignableFrom(c[2]) )
			return false;

		return true;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret;
		if( m.getName().contains("convolve")) {
			ret =  new Object[]{targetParam[0],targetParam[1],targetParam[2]};
		} else {
			ret = new Object[]{targetParam[0],targetParam[1],targetParam[2],false};
		}

		ImageSingleBand inputImage = ((ImageBorder)targetParam[1]).getImage();

		ret[1] = inputImage._createNew(width+kernelRadius*2,height+kernelRadius*2);
		ret[2] = ((ImageSingleBand)targetParam[2])._createNew(width+kernelRadius*2,height+kernelRadius*2);

		fillTestImage(inputImage,(ImageSingleBand)ret[1]);

		return ret;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object kernel = createKernel(paramTypes[0]);

		ImageSingleBand src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		Object[][] ret = new Object[1][paramTypes.length];
		ret[0][0] = kernel;
		ret[0][1] = ImageFloat32.class == src.getClass() ?
				ImageBorderValue.wrap((ImageFloat32)src,fillValue) : ImageBorderValue.wrap((ImageInteger)src,fillValue);
		ret[0][2] = dst;
		ret[0][3] = kernelRadius;

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		ImageSingleBand targetOut = (ImageSingleBand)targetParam[2];
		ImageSingleBand validationOut = (ImageSingleBand)validationParam[2];

		// remove the border
		validationOut = stripBorder(validationOut);

		GImageSingleBand t = FactoryGImageSingleBand.wrap(targetOut);
		GImageSingleBand v = FactoryGImageSingleBand.wrap(validationOut);

		for( int y = 0; y < targetOut.height; y++ ) {
			if( y >= kernelRadius &&  y < targetOut.height-kernelRadius )
				continue;
			for( int x = 0; x < targetOut.width; x++ ) {
				if( x >= kernelRadius &&  x < targetOut.width-kernelRadius )
					continue;

				assertEquals(v.get(x,y).doubleValue(),t.get(x,y).doubleValue(),1e-4f);
			}
		}
	}
}
