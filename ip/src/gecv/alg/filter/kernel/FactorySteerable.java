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

package gecv.alg.filter.kernel;

import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.misc.PixelMath;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class FactorySteerable {

	// todo generalize to make image type agnostic
	public static SteerableKernel gaussianKernel( int orderX , int orderY , int radius ) {
		if( orderX < 0 || orderX > 4 )
			throw new IllegalArgumentException("derivX must be from 0 to 4 inclusive.");
		if( orderY < 0 || orderY > 4 )
			throw new IllegalArgumentException("derivT must be from 0 to 4 inclusive.");

		int order = orderX + orderY;

		if( order > 4 ) {
			throw new IllegalArgumentException("The total order of x and y can't be greater than 4");
		}
		int maxOrder = Math.max(orderX,orderY);

		float sigma = (float)FactoryKernelGaussian.sigmaForRadius(radius,maxOrder);

		Kernel1D_F32 kerX =  FactoryKernelGaussian.derivativeK(Kernel1D_F32.class,orderX,sigma,radius);
		Kernel1D_F32 kerY = FactoryKernelGaussian.derivativeK(Kernel1D_F32.class,orderY,sigma,radius);
		Kernel2D_F32 kernel = KernelMath.convolve(kerY,kerX);

		Kernel2D_F32 []basis = new Kernel2D_F32[order+1];

		// convert it into an image which can be rotated
		ImageFloat32 image = KernelMath.convertToImage(kernel);
		ImageFloat32 imageRotated = new ImageFloat32(image.width,image.height);

		float centerX = image.width/2;
		float centerY = image.height/2;

		float e = energy(image);
		PixelMath.multiply(image,image,1.0f/(float)Math.sqrt(e));
		basis[0] = KernelMath.convertToKernel(image);

		// form the basis by created rotated versions of the kernel
		double angleStep = Math.PI/basis.length;

		for( int index = 1; index <= order; index++ ) {
			float angle = (float)(angleStep*index);

			ImageTestingOps.fill(imageRotated,0);
			DistortImageOps.rotate(image,imageRotated, TypeInterpolate.BILINEAR,centerX,centerY,angle);

			e = energy(imageRotated);
			PixelMath.multiply(imageRotated,imageRotated,1.0f/(float)Math.sqrt(e));

			basis[index] = KernelMath.convertToKernel(imageRotated);
		}

		SteerableKernel ret = new SteerableKernel();
		ret.setBasis(FactorySteerCoefficients.polynomial(order),basis);

		return ret;
	}

	private static float energy( ImageFloat32 img ) {
		float total = 0;

		for( int y = 0; y < img.height; y++ ) {
			for( int x = 0; x < img.width; x++ ) {
				float val = img.get(x,y);
				total += val*val;
			}
		}

		return total;
	}
}
