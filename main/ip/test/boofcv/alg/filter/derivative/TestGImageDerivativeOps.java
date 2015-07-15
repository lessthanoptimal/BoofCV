/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGImageDerivativeOps {

	@Test
	public void lookupKernelX() {
		assertTrue(GradientPrewitt.kernelDerivX_I32==GImageDerivativeOps.lookupKernelX(DerivativeType.PREWITT,true));
		assertTrue(GradientPrewitt.kernelDerivX_F32==GImageDerivativeOps.lookupKernelX(DerivativeType.PREWITT,false));

		assertTrue(GradientSobel.kernelDerivX_I32==GImageDerivativeOps.lookupKernelX(DerivativeType.SOBEL,true));
		assertTrue(GradientSobel.kernelDerivX_F32==GImageDerivativeOps.lookupKernelX(DerivativeType.SOBEL,false));

		assertTrue(GradientThree.kernelDeriv_I32==GImageDerivativeOps.lookupKernelX(DerivativeType.THREE,true));
		assertTrue(GradientThree.kernelDeriv_F32==GImageDerivativeOps.lookupKernelX(DerivativeType.THREE,false));

		assertTrue(GradientTwo0.kernelDeriv_I32==GImageDerivativeOps.lookupKernelX(DerivativeType.TWO_0,true));
		assertTrue(GradientTwo0.kernelDeriv_F32==GImageDerivativeOps.lookupKernelX(DerivativeType.TWO_0,false));

		assertTrue(GradientTwo1.kernelDeriv_I32==GImageDerivativeOps.lookupKernelX(DerivativeType.TWO_1,true));
		assertTrue(GradientTwo1.kernelDeriv_F32==GImageDerivativeOps.lookupKernelX(DerivativeType.TWO_1,false));
	}
}
