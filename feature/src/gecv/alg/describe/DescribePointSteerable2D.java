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
import gecv.alg.filter.kernel.SteerableKernel;
import gecv.core.image.border.BorderType;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.feature.TupleFeature;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
// todo add a 12 DOF variant which divides by the first deriviative as specified in paper
@SuppressWarnings({"unchecked"})
public class DescribePointSteerable2D <T extends ImageBase, D extends ImageBase, K extends Kernel2D> {


	SteerableKernel<K> kernels[];

	ImageConvolveSparse<T,K> convolver;
	RegionOrientation<D> orientation;

	public DescribePointSteerable2D( RegionOrientation<D> orientation ,
									 SteerableKernel<K> kernels[] ,
									 Class<T> imageType ) {
		this.orientation = orientation;
		this.kernels = kernels;

		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);
		convolver = FactoryConvolveSparse.create(imageType);
		convolver.setImageBorder(border);
	}

	public void setImage( T image , D derivX , D derivY ) {
		convolver.setImage(image);
		orientation.setImage(derivX,derivY);
	}

	public TupleFeature describe( int x , int y ) {
		TupleFeature ret = new TupleFeature(kernels.length);

		// determine feature's orientation
		double angle = orientation.compute(x,y);

		// compute the image feature's characteristics
		for( int i = 0; i < kernels.length; i++ ) {
			SteerableKernel<K> filter = kernels[i];
			K kernel = filter.compute(angle);

			convolver.setKernel(kernel);
			ret.value[i] = convolver.compute(x,y);
		}

		return ret;
	}

}
