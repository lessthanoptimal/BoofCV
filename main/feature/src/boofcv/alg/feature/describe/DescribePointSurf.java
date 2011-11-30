/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.describe;

import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageBase;

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
 * <li>Haar wavelet or image derivative can be used.</li>
 * <li>How the weighting is computed in some places has been tweaked.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Usage Notes:<br>
 * If the input image is floating point then normalizing it will very slightly improves stability.  Normalization in
 * this situation means dividing the input image by the maximum pixel intensity value, typically 255.  In stability
 * benchmarks it slightly change the results, but not enough to justify the runtime performance hit.
 * </p>
 *
 * <p>
 * [1] Bay, Herbert and Ess, Andreas and Tuytelaars, Tinne and Van Gool, Luc, "Speeded-Up Robust Features (SURF)"
 * Comput. Vis. Image Underst., vol 110, issue 3, 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointSurf<II extends ImageBase> {

	// Number of sub-regions wide the large grid is
	protected int widthLargeGrid;
	// Number of sample points wide a sub-region is
	protected int widthSubRegion;
	// Size of a sample point
	protected int widthSample;

	// DOF of feature
	protected int featureDOF;

	// integral image transform of input image
	protected II ii;

	// used to weigh feature computation
	protected Kernel2D_F64 weight;

	// Use the Haar wavelet or image derivative approximation
	protected boolean useHaar;

	/**
	 * Creates a SURF descriptor of arbitrary dimension by changing how the local region is sampled.
	 *
	 * @param widthLargeGrid Number of sub-regions wide the large grid is.
	 * @param widthSubRegion Number of sample points wide a sub-region is.
	 * @param widthSample The size of a sample point.
	 * @param weightSigma Weighting factor's sigma.  Try 3.8
	 * @param useHaar If true the Haar wavelet will be used (what was used in [1]), false means an image gradient
	 * approximation will be used.  False is recommended.
	 */
	public DescribePointSurf(int widthLargeGrid, int widthSubRegion, int widthSample,
							 double weightSigma , boolean useHaar) {
		this.widthLargeGrid = widthLargeGrid;
		this.widthSubRegion = widthSubRegion;
		this.widthSample = widthSample;
		this.useHaar = useHaar;

		int radius = (widthLargeGrid*widthSubRegion)/2;
		weight = FactoryKernelGaussian.gaussianWidth(weightSigma, radius * 2);

		// normalize to reduce numerical issues.
		// not sure if this makes any difference.
		double div = weight.get(radius,radius);
		for( int i = 0; i < weight.data.length; i++ )
			weight.data[i] /= div;

		// each sub-region provides 4 features
		featureDOF = widthLargeGrid*widthLargeGrid*4;
	}

	/**
	 * Create a SURF-64 descriptor.  See [1] for details.
	 */
	public DescribePointSurf() {
		this(4,5,2, 4.5 , false);
	}

	public void setImage( II integralImage ) {
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
	public SurfFeature describe( double x , double y ,
								 double scale , double angle ,
								 SurfFeature ret ) {
		// some functions require an integer pixel coordinate, rounding is more accurate
		int xInt = (int)Math.round(x);
		int yInt = (int)Math.round(y);

		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
		boolean isInBounds =
				SurfDescribeOps.isInside(ii,xInt,yInt,(widthLargeGrid*widthSubRegion)/2,widthSample,scale,angle);

		// declare the feature if needed
		if( ret == null )
			ret = new SurfFeature(featureDOF);
		else if( ret.value.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		// Computes the gradient (possibly the Haar wavelet) used compute the descriptor
		SparseImageGradient<II,?> gradient = SurfDescribeOps.createGradient(isInBounds, useHaar, widthSample, scale, (Class<II>) ii.getClass());
		gradient.setImage(ii);

		// extract descriptor
		SurfDescribeOps.features(x, y, angle, scale, weight, widthLargeGrid, widthSubRegion, gradient, ret.value);

		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		SurfDescribeOps.normalizeFeatures(ret.value);

		// Laplacian's sign
		ret.laplacianPositive = computeLaplaceSign(xInt,yInt, scale);

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
	protected boolean computeLaplaceSign(int x, int y, double scale) {
		int s = (int)Math.ceil(scale);
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(9*s);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(9*s);
		double lap = GIntegralImageOps.convolveSparse(ii,kerXX,x,y);
		lap += GIntegralImageOps.convolveSparse(ii,kerYY,x,y);

		return lap > 0;
	}

	public int getDescriptionLength() {
		return featureDOF;
	}

	public int getRadius() {
		int ret = widthLargeGrid*widthSubRegion+widthSample;
		return ret/2 + ret%2;
	}
}
