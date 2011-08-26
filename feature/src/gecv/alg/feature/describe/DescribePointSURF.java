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
 * <p>
 * Implementation of the SURF feature descriptor, see [1].  SURF features are invariant to illumination, scale,
 * and orientation.  Both the orientated and unoriented varieties can be computed.
 * SURF-64 describes an interest point using a 64 values that are computed from 16 sub regions.  Each sub-region
 * contributes 4 features, the sum of dx,|dx|,dy,|dy|, where dx and dy are the local derivatives.
 * </p>
 *
 * <p>
 * To improve performance (stability and/or computational) there are a few (intentional) deviations from the original paper.
 * <ul>
 * <li>Instead of the Haar wavelet a symmetric image derivative is used.</li>
 * <li>How the weighting is computed in some places has been tweaked.</li>
 * </ul>
 * </p>
 *
 * <p>
 * [1] Bay, Herbert and Ess, Andreas and Tuytelaars, Tinne and Van Gool, Luc, "Speeded-Up Robust Features (SURF)"
 * Comput. Vis. Image Underst., vol 110, issue 3, 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointSURF<T extends ImageBase> {

	// integral image transform of input image
	private T ii;

	// estimates feature orientation
	private OrientationIntegral<T> orientation;

	// used to weigh feature computation
	private Kernel2D_F64 weight = FactoryKernelGaussian.gaussian(2,true,64,-1,10);

	/**
	 *
	 * @param orientation Algorithm for estimating orientation.  If null then orientation will not be estimated.
	 */
	public DescribePointSURF(OrientationIntegral<T> orientation) {
		this.orientation = orientation;
	}

	public void setImage( T integralImage ) {
		ii = integralImage;
		orientation.setImage(ii);
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * <p>
	 * A feature point partially outside the image will have a different orientation
	 * (completely changing the description) and some of its characteristics will be garbage.
	 * Computations can also be greatly speed up if it is known that all the pixels are inside
	 * the image.
	 * </p>
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @return The SURF interest point.
	 */
	public SurfFeature describe( int x , int y , double scale ) {
		// todo check to see if it touches the border (including convolution kernel) at all
		// If so
		SurfFeature ret = new SurfFeature(64);

		double angle = 0;

		if( orientation != null ) {
			// compute the feature's orientation
			orientation.setScale(scale);
			angle = orientation.compute(x,y);
		}

		// todo see if rotated kernel touches the image border
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
