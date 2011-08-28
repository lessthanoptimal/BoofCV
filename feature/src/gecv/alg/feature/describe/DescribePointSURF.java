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

import gecv.alg.transform.ii.DerivativeIntegralImage;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.alg.transform.ii.IntegralKernel;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.feature.SurfFeature;
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

	// used to weigh feature computation
	private Kernel2D_F64 weight = FactoryKernelGaussian.gaussian(2,true,64,-1,10);

	public void setImage( T integralImage ) {
		ii = integralImage;
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param ret storage for the feature. Must have 64 values. If null a new feature will be declared internally.
	 * @return The SURF interest point or null if the feature region goes outside the image.
	 */
	public SurfFeature describe( int x , int y ,
								 double scale , double angle ,
								 SurfFeature ret ) {

		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
		if( !SurfDescribeOps.isInside(ii,x,y,10,4,scale,angle)) {
			return null;
		}

		// declare the feature if needed
		if( ret == null )
			ret = new SurfFeature(64);
		else if( ret.features.value.length != 64 )
			throw new IllegalArgumentException("Provided feature must have 64 values");


		// extract descriptor
		SurfDescribeOps.features(ii,x,y,angle,weight,20,5,scale,true,ret.features.value);
		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		SurfDescribeOps.normalizeFeatures(ret.features.value);

		// Laplacian's sign
		ret.laplacianPositive = computeLaplaceSign(x, y, scale);

		return ret;
	}

	/**
	 * Compute the sign of the Laplacian using a sparse convolution.
	 *
	 * @param x center
	 * @param y center
	 * @param scale scale of the feature
	 * @return true if positive
	 */
	private boolean computeLaplaceSign(int x, int y, double scale) {
		int s = (int)Math.ceil(scale);
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(9*s);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(9*s);
		double lap = GIntegralImageOps.convolveSparse(ii,kerXX,x,y);
		lap += GIntegralImageOps.convolveSparse(ii,kerYY,x,y);

		return lap > 0;
	}
}
