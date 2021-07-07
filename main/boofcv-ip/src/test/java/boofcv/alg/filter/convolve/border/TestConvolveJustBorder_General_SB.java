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

import boofcv.BoofTesting;
import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestConvolveJustBorder_General_SB extends CompareImageBorder {

	BorderType borderType = BorderType.REFLECT;

	public TestConvolveJustBorder_General_SB() {
		super(ConvolveJustBorder_General_SB.class);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		Class<?>[] e = m.getParameterTypes();

		for (Class<?> c : e) {
			if (ImageGray.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	/**
	 * Compare the results along the border to the results obtained by convolving a larger image with the noborder algorithm
	 * whose border has been filled with the fillValue.
	 */
	@Test void compareToNoBorder() {
		performTests(18);
	}

	@Override
	protected boolean isEquivalent( Method candidate, Method evaluation ) {
		if (evaluation.getName().compareTo(candidate.getName()) != 0)
			return false;

		Class<?>[] e = evaluation.getParameterTypes();
		Class<?>[] c = candidate.getParameterTypes();

		if (candidate.getName().equals("vertical") && e.length == 4) {
			if (c.length != 5)
				return false;
		} else if (e.length != c.length)
			return false;
		if (e[0] != c[0])
			return false;
		if (!e[2].isAssignableFrom(c[2]))
			return false;

		return true;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Object[] ret = new Object[Math.max(m.getParameterTypes().length, targetParam.length)];
		System.arraycopy(targetParam, 0, ret, 0, targetParam.length);

		ImageBorder border = (ImageBorder)targetParam[1];
		ImageGray inputImage = (ImageGray)border.getImage();

		KernelBase kernel = (KernelBase)targetParam[0];

		computeBorder(kernel, m.getName());

		int borderWidthX = borderX0 + borderX1;
		int borderWidthY = borderY0 + borderY1;

		// input image
		ret[1] = inputImage.createNew(width + borderWidthX, height + borderWidthY);
		// output image
		ret[2] = ((ImageGray)targetParam[2]).createNew(width + borderWidthX, height + borderWidthY);

		GImageMiscOps.growBorder(inputImage, border, borderX0, borderY0, borderX1, borderY1, (ImageBase)ret[1]);

		return ret;
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class<?>[] paramTypes = candidate.getParameterTypes();

		// Adjust border size for the different  convolution types
		int kernelLength = 5;

		KernelBase kernel = createKernel(paramTypes[0], kernelLength, kernelLength/2);

		ImageBase src = ConvolutionTestHelper.createImage(validation.getParameterTypes()[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageBase dst = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);

		ImageBorder border = FactoryImageBorder.generic(borderType, src.getImageType());
		border.setImage(src);

		Object[][] ret = new Object[2][paramTypes.length];
		// normal symmetric odd kernel
		ret[0][0] = kernel;
		ret[0][1] = border;
		ret[0][2] = dst;
		if (paramTypes.length == 4)
			ret[0][3] = BoofTesting.primitive(3, paramTypes[3]);

		// change the offset
		kernel = createKernel(paramTypes[0], kernelLength, kernelLength/2 - 1);
		ret[1][0] = kernel;
		ret[1][1] = border;
		ret[1][2] = ConvolutionTestHelper.createImage(validation.getParameterTypes()[2], width, height);
		if (paramTypes.length == 4)
			ret[1][3] = BoofTesting.primitive(3, paramTypes[3]);
		return ret;
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		ImageGray targetOut = (ImageGray)targetParam[2];
		ImageGray validationOut = (ImageGray)validationParam[2];

		// remove the border
		computeBorder((KernelBase)targetParam[0], methodTest.getName());
		validationOut = (ImageGray)stripBorder(validationOut, borderX0, borderY0, borderX1, borderY1);

		GImageGray t = FactoryGImageGray.wrap(targetOut);
		GImageGray v = FactoryGImageGray.wrap(validationOut);

		for (int y = 0; y < targetOut.height; y++) {
			if (y >= borderX0 && y < targetOut.height - borderX1)
				continue;
			for (int x = 0; x < targetOut.width; x++) {
				if (x >= borderX0 && x < targetOut.width - borderY1)
					continue;

				assertEquals(v.get(x, y).doubleValue(), t.get(x, y).doubleValue(), 1e-4f);
			}
		}
	}
}
