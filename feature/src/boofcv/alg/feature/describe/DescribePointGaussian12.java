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

package boofcv.alg.feature.describe;

import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactorySteerable;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;


/**
 * <p>
 * Uses steerable Gaussian derivatives to describe an image feature.  Invariance to
 * lighting changes is brought about by dividing the output of each kernel by the
 * image's first derivative.  Thus 12 features are returned, but up to 4th order derivatives
 * are computed.
 * </p>
 *
 * <p>
 * Krystian Mikolajczyk and Cordelia Schmid, "Indexing based on scale invariant interest points"
 * In Proceedings of the 8th International Conference on Computer Vision, 2001
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class DescribePointGaussian12 <T extends ImageBase, K extends Kernel2D> {

	// the set of steerable kernels used to compute image feature points.
	SteerableKernel<K> kernels[];

	// first order derivative used to compute light invariance
	SteerableKernel<K> derivX;
	SteerableKernel<K> derivY;

	// Applies the kernel to the interest point
	ImageConvolveSparse<T,K> convolver;

	public DescribePointGaussian12( int radius , Class<T> imageType ) {

		Class<K> kernelType = (Class) FactoryKernel.getKernelType(imageType,2);

		derivX = FactorySteerable.gaussian(kernelType,1,0, -1, radius);
		derivY = FactorySteerable.gaussian(kernelType,0,1, -1, radius);

		kernels = (SteerableKernel<K>[])new SteerableKernel[12];
		int index = 0;
		for( int N = 2; N <= 4; N++ ) {
			for( int i = 0; i <= N; i++ ) {
				int orderX = N-i;
				kernels[index++] = FactorySteerable.gaussian(kernelType,orderX,i, -1, radius);
			}
		}

		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.SKIP);
		convolver = FactoryConvolveSparse.create(imageType);
		convolver.setImageBorder(border);
	}

	public int getDescriptionLength() {
		return kernels.length;
	}

	public int getRadius() {
		return kernels[0].getBasis(0).getRadius();
	}

	public void setImage( T image ) {
		convolver.setImage(image);
	}

	public TupleDesc_F64 describe( int x , int y , double angle , TupleDesc_F64 ret ) {
		if( ret == null )
			ret = new TupleDesc_F64(kernels.length);
		else if( ret.value.length != kernels.length )
			throw new IllegalArgumentException("Unexpected feature description length");

		// compute the derivative and use it to normalize
		// the other features, providing invariance to changes in image intensity
		convolver.setKernel(derivX.compute(angle));
		double Dx = convolver.compute(x,y);
		convolver.setKernel(derivY.compute(angle));
		double Dy = convolver.compute(x,y);

		double norm = Math.sqrt(Dx*Dx + Dy*Dy);
		// the feature is probably useless, but need to avoid divide by zero error
		if( norm <= 1e-15 )
			norm = 1;

		// compute the image feature's characteristics
		for( int i = 0; i < kernels.length; i++ ) {
			SteerableKernel<K> filter = kernels[i];
			K kernel = filter.compute(angle);

			convolver.setKernel(kernel);
			ret.value[i] = convolver.compute(x,y)/norm;
		}

		return ret;
	}
}
