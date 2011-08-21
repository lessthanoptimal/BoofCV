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

import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.alg.feature.describe.RegionOrientation;
import gecv.alg.filter.kernel.SteerableKernel;
import gecv.factory.filter.kernel.FactoryKernel;
import gecv.factory.filter.kernel.FactorySteerable;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDescribeRegionAlgs {

	// todo comment
	public <T extends ImageBase, D extends ImageBase, K extends Kernel2D>
	DescribePointSteerable2D<T,D,K> steerableGaussian( int radius , int numAngles ,
													   boolean weighted ,
													   Class<T> imageType ,
													   Class<D> derivType )
	{
		SteerableKernel<K>[] kernels = (SteerableKernel<K>[])new SteerableKernel[14];

		Class<K> kernelType = (Class) FactoryKernel.getKernelType(imageType,2);

		int index = 0;
		for( int N = 1; N <= 4; N++ ) {
			for( int i = 0; i <= N; i++ ) {
				int orderX = N-i;

				kernels[index++] = FactorySteerable.gaussian(kernelType,orderX,i,radius);
			}
		}
		RegionOrientation<D> orientation = FactoryRegionOrientationAlgs.histogram(numAngles,radius,weighted,derivType);

		return new DescribePointSteerable2D<T,D,K>(orientation,kernels,imageType);
	}
}
