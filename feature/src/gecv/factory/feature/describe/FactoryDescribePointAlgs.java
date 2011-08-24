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

package gecv.factory.feature.describe;

import gecv.alg.feature.describe.DescribePointSURF;
import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.alg.feature.orientation.OrientationAverageHaarIntegral;
import gecv.alg.feature.orientation.OrientationGradient;
import gecv.alg.feature.orientation.OrientationIntegral;
import gecv.alg.filter.kernel.SteerableKernel;
import gecv.factory.feature.orientation.FactoryOrientationAlgs;
import gecv.factory.filter.kernel.FactoryKernel;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.factory.filter.kernel.FactorySteerable;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDescribePointAlgs {

	public static <T extends ImageBase>
	DescribePointSURF<T> surf() {
		OrientationIntegral<T> orientation = new OrientationAverageHaarIntegral<T>(6,false);

		return new DescribePointSURF<T>(orientation);
	}

	// todo comment
	public static <T extends ImageBase, D extends ImageBase, K extends Kernel2D>
	DescribePointSteerable2D<T,D,K> steerableGaussian( boolean normalized ,
													   double sigma ,
													   int radius ,
													   Class<T> imageType ,
													   Class<D> derivType )
	{
		if( sigma <= 0 )
			sigma = FactoryKernelGaussian.sigmaForRadius(radius,4);
		else if ( radius <= 0 )
			radius = FactoryKernelGaussian.radiusForSigma(sigma,4);

		SteerableKernel<K>[] kernels = (SteerableKernel<K>[])new SteerableKernel[14];

		Class<K> kernelType = (Class) FactoryKernel.getKernelType(imageType,2);
		int index = 0;
		for( int N = 1; N <= 4; N++ ) {
			for( int i = 0; i <= N; i++ ) {
				int orderX = N-i;

				kernels[index++] = FactorySteerable.gaussian(kernelType,orderX,i, sigma, radius);
			}
		}
		OrientationGradient<D> orientation = FactoryOrientationAlgs.average(radius,false,derivType);
//		OrientationGradient<D> orientation = FactoryOrientationAlgs.sliding(20,Math.PI/3.0,radius,true,derivType);

		return new DescribePointSteerable2D<T,D,K>(orientation,kernels,normalized,imageType);
	}
}
