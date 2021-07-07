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

package boofcv.alg.transform.ii.impl;

import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GeneralSparseGradientIntegralTests;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F32;
import org.junit.jupiter.api.Test;


/**
 * @author Peter Abeles
 */
public class TestSparseIntegralHaar_NoBorder_F32
		extends GeneralSparseGradientIntegralTests<GrayF32,GradientValue_F32>
{
	final static int size = 4;
	final static int radius = size/2;

	public TestSparseIntegralHaar_NoBorder_F32() {
		super(GrayF32.class, -radius,-radius,radius,radius);

		alg = new SparseIntegralHaar_NoBorder_F32();
		((SparseIntegralHaar_NoBorder_F32)alg).setWidth(size);
		IntegralKernel kernelX = DerivativeIntegralImage.kernelHaarX(radius,null);
		IntegralKernel kernelY = DerivativeIntegralImage.kernelHaarY(radius,null);
		setKernels(kernelX,kernelY);
	}

	@Test void allStandard() {
		allTests(false);
	}
}
