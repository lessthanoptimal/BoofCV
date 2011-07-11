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

import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.ConvolveWithBorder;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.border.BorderIndex1D_Extend;
import gecv.core.image.border.ImageBorder1D_F32;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;


/**
 * Finds the derivative using a Gaussian kernel.  This is the same as convolving the image
 * and then computing the derivative
 *
 * @author Peter Abeles
 */
public class ImageGradient_Gaussian_F32 implements ImageGradient<ImageFloat32, ImageFloat32> {

	private Kernel1D_F32 kernel;
	private boolean processBorder;

	public ImageGradient_Gaussian_F32(double sigma, int radius, boolean processBorder) {
		kernel = KernelFactory.gaussianDerivative1D_F32(sigma, radius, true);
		this.processBorder = processBorder;
	}

	public ImageGradient_Gaussian_F32(int radius) {
		kernel = KernelFactory.gaussianDerivative1D_F32((2 * radius + 1) / 5.0, radius, true);
	}


	@Override
	public void process( ImageFloat32 inputImage , ImageFloat32 derivX, ImageFloat32 derivY ) {
		if( processBorder ) {
			ConvolveWithBorder.horizontal(kernel, inputImage, derivX,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
			ConvolveWithBorder.vertical(kernel, inputImage, derivY,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		} else {
			ConvolveImageNoBorder.horizontal(kernel, inputImage, derivX, true);
			ConvolveImageNoBorder.vertical(kernel, inputImage, derivY, false);
		}
	}

	@Override
	public int getBorder() {
		if( processBorder )
			return 0;
		else
			return kernel.getRadius();
	}
}