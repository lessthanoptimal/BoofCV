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

package gecv.alg.transform.ii;

import gecv.struct.deriv.GradientValue_F64;
import gecv.struct.deriv.SparseImageGradient;
import gecv.struct.image.ImageBase;


/**
 * Computes the gradient from an integral image.  Does not check for border conditions.
 *
 * @author Peter Abeles
 */
public class SparseIntegralGradientKernel<T extends ImageBase>
		implements SparseImageGradient<T, GradientValue_F64>
{
	// input integral image
	T ii;
	// kernels for computing derivatives along x and y
	IntegralKernel kernelX;
	IntegralKernel kernelY;

	GradientValue_F64 ret = new GradientValue_F64();

	public SparseIntegralGradientKernel(IntegralKernel kernelX, IntegralKernel kernelY) {
		this.kernelX = kernelX;
		this.kernelY = kernelY;
	}

	@Override
	public void setImage(T integralImage) {
		this.ii = integralImage;
	}

	@Override
	public GradientValue_F64 compute(int x, int y) {

		ret.x = GIntegralImageOps.convolveSparse(ii,kernelX,x,y);
		ret.y = GIntegralImageOps.convolveSparse(ii,kernelY,x,y);

		return ret;
	}
}
