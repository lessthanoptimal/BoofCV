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

package boofcv.factory.filter.kernel;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.kernel.GKernelMath;
import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.alg.filter.kernel.impl.SteerableKernel_F32;
import boofcv.alg.filter.kernel.impl.SteerableKernel_I32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Creates different steerable kernels.  Steerable kernels are kernels which can be computed at an arbitrary angle easily
 * and efficiently using a set of bases.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactorySteerable {

	/**
	 * Steerable filter for 2D Gaussian derivatives.  The basis is composed of a set of rotated kernels.
	 *
	 * @param kernelType Specifies which type of 2D kernel should be generated.
	 * @param orderX Order of the derivative in the x-axis.
	 * @param orderY Order of the derivative in the y-axis.
	 *
	 * @param sigma
	 *@param radius Radius of the kernel.  @return Steerable kernel generator for the specified gaussian derivative.
	 */
	public static <K extends Kernel2D> SteerableKernel<K> gaussian(Class<K> kernelType, int orderX, int orderY, double sigma, int radius) {
		if( orderX < 0 || orderX > 4 )
			throw new IllegalArgumentException("derivX must be from 0 to 4 inclusive.");
		if( orderY < 0 || orderY > 4 )
			throw new IllegalArgumentException("derivT must be from 0 to 4 inclusive.");

		int order = orderX + orderY;

		if( order > 4 ) {
			throw new IllegalArgumentException("The total order of x and y can't be greater than 4");
		}
		int maxOrder = Math.max(orderX,orderY);

		if( sigma <= 0 )
			sigma = (float)FactoryKernelGaussian.sigmaForRadius(radius,maxOrder);
		else if( radius <= 0 )
			radius = FactoryKernelGaussian.radiusForSigma(sigma,maxOrder);

		Class kernel1DType = FactoryKernel.get1DType(kernelType);
		Kernel1D kerX =  FactoryKernelGaussian.derivativeK(kernel1DType,orderX,sigma,radius);
		Kernel1D kerY = FactoryKernelGaussian.derivativeK(kernel1DType,orderY,sigma,radius);
		Kernel2D kernel = GKernelMath.convolve(kerY,kerX);

		Kernel2D []basis = new Kernel2D[order+1];

		// convert it into an image which can be rotated
		ImageGray image = GKernelMath.convertToImage(kernel);
		ImageGray imageRotated = (ImageGray)image.createNew(image.width,image.height);

		basis[0] = kernel;

		// form the basis by created rotated versions of the kernel
		double angleStep = Math.PI/basis.length;

		for( int index = 1; index <= order; index++ ) {
			float angle = (float)(angleStep*index);

			GImageMiscOps.fill(imageRotated, 0);
			new FDistort(image,imageRotated).rotate(angle).apply();

			basis[index] = GKernelMath.convertToKernel(imageRotated);
		}

		SteerableKernel<K> ret;

		if( kernelType == Kernel2D_F32.class )
			ret = (SteerableKernel<K>)new SteerableKernel_F32();
		else
			ret = (SteerableKernel<K>)new SteerableKernel_I32();

		ret.setBasis(FactorySteerCoefficients.polynomial(order),basis);

		return ret;
	}
}
