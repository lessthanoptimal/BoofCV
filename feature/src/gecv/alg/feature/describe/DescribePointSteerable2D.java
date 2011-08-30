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

package gecv.alg.feature.describe;

import gecv.abst.filter.convolve.ImageConvolveSparse;
import gecv.alg.filter.kernel.SteerableKernel;
import gecv.core.image.border.BorderType;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder;
import gecv.factory.filter.convolve.FactoryConvolveSparse;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.feature.TupleDesc_F64;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Extracts a set of image features using a steerable kernel.  THe kernels are steered in the provided
 * direction, making the set of features rotation invariant. 
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class DescribePointSteerable2D <T extends ImageBase, K extends Kernel2D> {

	// the set of steerable kernels used to compute image feature points.
	SteerableKernel<K> kernels[];

	// Applies the kernel to the interest point
	ImageConvolveSparse<T,K> convolver;
	// should the feature vector be normalized to one?
	// this provides some intensity invariance
	boolean normalize;

	public DescribePointSteerable2D( SteerableKernel<K> kernels[] ,
									 boolean normalize ,
									 Class<T> imageType ) {

		this.kernels = kernels;
		this.normalize = normalize;

		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.SKIP);
		convolver = FactoryConvolveSparse.create(imageType);
		convolver.setImageBorder(border);
	}

	public int getRadius() {
		return kernels[0].getBasis(0).getRadius();
	}

	public void setImage( T image ) {
		convolver.setImage(image);
	}

	public TupleDesc_F64 describe( int x , int y , double angle ) {
		TupleDesc_F64 ret = new TupleDesc_F64(kernels.length);

		// compute the image feature's characteristics
		for( int i = 0; i < kernels.length; i++ ) {
			SteerableKernel<K> filter = kernels[i];
			K kernel = filter.compute(angle);

			convolver.setKernel(kernel);
			ret.value[i] = convolver.compute(x,y);
		}

		if( normalize )
			SurfDescribeOps.normalizeFeatures(ret.value);
		
		return ret;
	}

}
