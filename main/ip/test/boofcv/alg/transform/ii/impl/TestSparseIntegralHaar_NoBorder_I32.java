/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.filter.derivative.GeneralSparseGradientTests;
import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.image.ImageSInt32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestSparseIntegralHaar_NoBorder_I32
		extends GeneralSparseGradientTests<ImageSInt32,ImageSInt32,GradientValue>
{
	final static int size = 4;
	final static int radius = size/2;
	SparseIntegralHaar_NoBorder_I32 alg;

	public TestSparseIntegralHaar_NoBorder_I32() {
		super(ImageSInt32.class, ImageSInt32.class,radius);

		alg = new SparseIntegralHaar_NoBorder_I32(radius);
	}

	@Test
	public void allStandard() {
		allTests(false);
	}

	@Override
	protected void imageGradient(ImageSInt32 input, ImageSInt32 derivX, ImageSInt32 derivY) {
		IntegralKernel kernelX = DerivativeIntegralImage.kernelHaarX(radius);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelHaarY(radius);

		GIntegralImageOps.convolve(input,kernelX,derivX);
		GIntegralImageOps.convolve(input,kernelY,derivY);
	}

	@Override
	protected GradientValue sparseGradient(ImageSInt32 input, int x, int y) {
		alg.setImage(input);
		return alg.compute(x,y);
	}
}
