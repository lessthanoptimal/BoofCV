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

package gecv.abst.filter.derivative;

import gecv.alg.filter.convolve.ConvolveImage;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.struct.convolve.Kernel1D_F32;


/**
 * Finds the derivative using a Gaussian kernel.  This is the same as convolving the image
 * and then computing the derivative
 *
 * @author Peter Abeles
 */
public class DerivativeXY_Gaussian_F32 extends DerivativeXYBase_F32 {

	private Kernel1D_F32 deriv;

	public DerivativeXY_Gaussian_F32(double sigma, int radius) {
		deriv = KernelFactory.gaussianDerivative1D_F32(sigma, radius, true);
	}

	public DerivativeXY_Gaussian_F32(int radius) {
		deriv = KernelFactory.gaussianDerivative1D_F32((2 * radius + 1) / 5.0, radius, true);
	}


	@Override
	public void process() {
		ConvolveImage.horizontal(deriv, image, derivX, true);
		ConvolveImage.vertical(deriv, image, derivY, false);
	}

	@Override
	public int getBorder() {
		return deriv.getRadius();
	}
}