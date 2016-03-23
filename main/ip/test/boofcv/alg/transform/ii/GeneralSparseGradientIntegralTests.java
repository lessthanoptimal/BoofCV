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
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralSparseGradientIntegralTests
		<T extends ImageGray, D extends ImageGray,G extends GradientValue>
	extends GeneralSparseGradientTests<T,D,G>
{
	// kernels for testing derivative output
	IntegralKernel kernelX;
	IntegralKernel kernelY;

	protected GeneralSparseGradientIntegralTests(Class<T> inputType, Class<D> derivType,
												 int sampleBoxX0 , int sampleBoxY0 ,
												 int sampleBoxX1 , int sampleBoxY1  )
	{
		super(inputType, derivType, sampleBoxX0,sampleBoxY0,sampleBoxX1,sampleBoxY1);
	}

	public void setKernels( IntegralKernel kernelX , IntegralKernel kernelY )
	{
		this.kernelX = kernelX;
		this.kernelY = kernelY;
	}

	@Override
	protected void imageGradient(T input, D derivX, D derivY) {

		GIntegralImageOps.convolve(input,kernelX,derivX);
		GIntegralImageOps.convolve(input,kernelY,derivY);
	}

}
