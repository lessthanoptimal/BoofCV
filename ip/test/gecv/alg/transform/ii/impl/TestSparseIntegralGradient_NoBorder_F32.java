/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.transform.ii.impl;

import gecv.alg.filter.derivative.GeneralSparseGradientTests;
import gecv.alg.transform.ii.DerivativeIntegralImage;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.alg.transform.ii.IntegralKernel;
import gecv.struct.deriv.GradientValue;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestSparseIntegralGradient_NoBorder_F32 extends GeneralSparseGradientTests<ImageFloat32,ImageFloat32,GradientValue>
{

	final static int size = 5;
	SparseIntegralGradient_NoBorder_F32 alg;

	public TestSparseIntegralGradient_NoBorder_F32() {
		super(ImageFloat32.class, ImageFloat32.class,size/2+1);

		alg = new SparseIntegralGradient_NoBorder_F32(size/2);
	}

	@Test
	public void allStandard() {
		allTests(false);
	}


	@Override
	protected void imageGradient(ImageFloat32 input, ImageFloat32 derivX, ImageFloat32 derivY) {
		IntegralKernel kernelX = DerivativeIntegralImage.kernelDerivX(size);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelDerivY(size);

		GIntegralImageOps.convolve(input,kernelX,derivX);
		GIntegralImageOps.convolve(input,kernelY,derivY);
	}

	@Override
	protected GradientValue sparseGradient(ImageFloat32 input, int x, int y) {
		alg.setImage(input);
		return alg.compute(x,y);
	}
}
