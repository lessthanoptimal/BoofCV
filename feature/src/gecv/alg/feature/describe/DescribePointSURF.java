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

import gecv.alg.feature.orientation.OrientationIntegral;
import gecv.alg.transform.ii.DerivativeIntegralImage;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.alg.transform.ii.IntegralKernel;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
// todo option for unoriented
public class DescribePointSURF<T extends ImageBase> {

	T ii;

	OrientationIntegral<T> orientation;

	Kernel2D_F64 weight = FactoryKernelGaussian.gaussian(2,true,64,-1,10);

	public DescribePointSURF(OrientationIntegral<T> orientation) {
		this.orientation = orientation;
	}

	public void setImage( T integralImage ) {
		ii = integralImage;
		orientation.setImage(ii);
	}

	public SurfFeature describe( int x , int y , double scale ) {
		SurfFeature ret = new SurfFeature(64);

		// compute the feature's orientation
		orientation.setScale(scale);
		double angle = orientation.compute(x,y);

		// extract descriptor
		SurfDescribeOps.features(ii,x,y,angle,weight,20,4,scale,ret.features.value);
		SurfDescribeOps.normalizeFeatures(ret.features.value);

		// Laplacian's sign
		int s = (int)Math.ceil(scale);
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(9*s);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(9*s);
		double lap = GIntegralImageOps.convolveSparse(ii,kerXX,x,y);
		lap += GIntegralImageOps.convolveSparse(ii,kerYY,x,y);

		ret.laplacianPositive = lap > 0;

		return ret;
	}
}
