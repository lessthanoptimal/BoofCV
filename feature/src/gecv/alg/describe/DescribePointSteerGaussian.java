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

package gecv.alg.describe;

import gecv.abst.filter.convolve.FactoryConvolveSparse;
import gecv.abst.filter.convolve.ImageConvolveSparse;
import gecv.alg.filter.kernel.FactorySteerable;
import gecv.alg.filter.kernel.SteerableKernel;
import gecv.core.image.border.BorderType;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
// todo add a 12 DOF variant which divides by the first deriviative as specified in paper
@SuppressWarnings({"unchecked"})
public class DescribePointSteerGaussian {

	int radius = 20;
	SteerableKernel<Kernel2D_F32> kernels[];

	ImageConvolveSparse convolver;

	public DescribePointSteerGaussian() {
		// create the steerable filters which are used to compute the region features
		kernels = (SteerableKernel<Kernel2D_F32>[])new SteerableKernel[14];

		int index = 0;
		for( int N = 1; N <= 4; N++ ) {
			for( int i = 0; i <= N; i++ ) {
				int orderX = N-i;

				kernels[index++] = FactorySteerable.gaussian(Kernel2D_F32.class,orderX,i,radius);
			}
		}
		ImageBorder<ImageFloat32> border = FactoryImageBorder.general(ImageFloat32.class, BorderType.EXTENDED);
		convolver = FactoryConvolveSparse.create(ImageFloat32.class);
		convolver.setImageBorder(border);
	}

	public void setImage( ImageFloat32 image , ImageFloat32 derivX , ImageFloat32 derivY ) {
		convolver.setImage(image);
	}

//	public TupleFeature describe( int x , int y ) {
//		TupleFeature ret = new TupleFeature(kernels.length);
//
//		// todo determine feature's orientation
//
//		double angle;
//
//		// compute
//		for( int i = 0; i < kernels.length; i++ ) {
//			SteerableKernel<Kernel2D_F32> filter = kernels[i];
//			Kernel2D_F32 kernel = filter.compute(angle);
//
//			convolver.setKernel(kernel);
//			ret.value[i] = convolver.compute(x,y);
//		}
//
//		return ret;
//	}

}
