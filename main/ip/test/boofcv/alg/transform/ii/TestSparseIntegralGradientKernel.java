/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.ii;

import boofcv.alg.filter.derivative.GeneralSparseGradientTests;
import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F64;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestSparseIntegralGradientKernel
		extends GeneralSparseGradientTests<GrayF32,GrayF32,GradientValue_F64>
{
	final static int size = 5;
	final static int radius = size/2;

	public TestSparseIntegralGradientKernel() {
		super(GrayF32.class, GrayF32.class,-radius-1,-radius-1,radius,radius);

		IntegralKernel kernelX = DerivativeIntegralImage.kernelDerivX(radius,null);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelDerivY(radius,null);

		alg = new SparseIntegralGradientKernel<>(kernelX,kernelY);
	}

	@Test
	public void allStandard() {
		allTests(true);
	}


	@Override
	protected void imageGradient(GrayF32 input, GrayF32 derivX, GrayF32 derivY) {
		IntegralKernel kernelX = DerivativeIntegralImage.kernelDerivX(radius,null);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelDerivY(radius,null);

		GIntegralImageOps.convolve(input,kernelX,derivX);
		GIntegralImageOps.convolve(input,kernelY,derivY);
	}

}
