/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseGradientSafe;
import boofcv.struct.sparse.SparseImageGradient;
import boofcv.struct.sparse.SparseScaleGradient;

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
 * <li>Derivative sample coordinates are interpolated by rounding to the nearest integer.</li>
 * <li>Weighting function is applied to each sub region as a whole and not to each wavelet inside the sub
 * region.  This allows the weight to be precomputed once.  Unlikely to degrade quality significantly.</li>
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
public class DescribePointSurf<II extends ImageGray> {

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

	// computes sparse image gradient around specified points
	protected SparseScaleGradient<II,?> gradient;
	// can handle sample requests outside the image border
	protected SparseImageGradient<II,?> gradientSafe;

	// radius of the descriptor at a scale of 1.  Used to determine if it touches the image boundary
	// does not include sample kernel size
	protected int radiusDescriptor;

	// storage for kernels used to compute laplacian sign
	protected IntegralKernel kerXX;
	protected IntegralKernel kerYY;

	/**
	 * Creates a SURF descriptor of arbitrary dimension by changing how the local region is sampled.
	 *
	 * @param widthLargeGrid Number of sub-regions wide the large grid is. Typically 4
	 * @param widthSubRegion Number of sample points wide a sub-region is. Typically 5
	 * @param widthSample The width of a sample point. Typically 4
	 * @param weightSigma Weighting factor's sigma.  Try 3.8
	 * @param useHaar If true the Haar wavelet will be used (what was used in [1]), false means an image gradient
	 * approximation will be used.  False is recommended.
	 */
	public DescribePointSurf(int widthLargeGrid, int widthSubRegion, int widthSample,
							 double weightSigma , boolean useHaar,
							 Class<II> inputType ) {
		this.widthLargeGrid = widthLargeGrid;
		this.widthSubRegion = widthSubRegion;
		this.widthSample = widthSample;

		int radius = (widthLargeGrid*widthSubRegion)/2;
		weight = FactoryKernelGaussian.gaussianWidth(weightSigma, radius * 2);

		// normalize to reduce numerical issues.
		// not sure if this makes any difference.
		double div = weight.get(radius,radius);
		for( int i = 0; i < weight.data.length; i++ )
			weight.data[i] /= div;

		// each sub-region provides 4 features
		featureDOF = widthLargeGrid*widthLargeGrid*4;

		// create the function that the gradient is sampled with=
		gradient = SurfDescribeOps.createGradient(useHaar, inputType);
		gradientSafe = new SparseGradientSafe(this.gradient);

		radiusDescriptor = (widthLargeGrid*widthSubRegion)/2;
	}

	/**
	 * Create a SURF-64 descriptor.  See [1] for details.
	 */
	public DescribePointSurf(Class<II> inputType ) {
		this(4,5,3, 4.5 , false,inputType);
	}

	public BrightFeature createDescription() {
		return new BrightFeature(featureDOF);
	}

	public void setImage( II integralImage ) {
		ii = integralImage;
		gradient.setImage(ii);
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param ret storage for the feature. Must have 64 values.
	 */
	public void describe(double x, double y, double angle, double scale, BrightFeature ret)
	{
		describe(x, y, angle, scale, (TupleDesc_F64) ret);

		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		UtilFeature.normalizeL2(ret);

		// Laplacian's sign
		ret.white = computeLaplaceSign((int)(x+0.5),(int)(y+0.5), scale);
	}

	/**
	 * Compute SURF descriptor, but without laplacian sign
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param ret storage for the feature. Must have 64 values.
	 */
	public void describe(double x, double y, double angle, double scale, TupleDesc_F64 ret)
	{
		double c = Math.cos(angle),s=Math.sin(angle);

		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
		boolean isInBounds =
				SurfDescribeOps.isInside(ii,x,y, radiusDescriptor,widthSample,scale,c,s);

		// declare the feature if needed
		if( ret == null )
			ret = new BrightFeature(featureDOF);
		else if( ret.value.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		gradient.setImage(ii);
		gradient.setWidth(widthSample*scale);

		// use a safe method if its along the image border
		SparseImageGradient gradient = isInBounds ? this.gradient : this.gradientSafe;

		// extract descriptor
		features(x, y, c, s, scale, gradient , ret.value);
	}

	/**
	 * <p>
	 * Computes features in the SURF descriptor.
	 * </p>
	 *
	 * <p>
	 * Deviation from paper:<br>
	 * <ul>
	 * <li>Weighting function is applied to each sub region as a whole and not to each wavelet inside the sub
	 * region.  This allows the weight to be precomputed once.  Unlikely to degrade quality significantly.</li>
	 * </ul>
	 * </p>
	 *
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param c cosine of the orientation
	 * @param s sine of the orientation
	 * @param scale The scale of the wavelets.
	 * @param features Where the features are written to.  Must be 4*(widthLargeGrid*widthSubRegion)^2 large.
	 */
	public void features(double c_x, double c_y,
						 double c , double s, double scale,
						 SparseImageGradient gradient ,
						 double[] features)
	{
		int regionSize = widthLargeGrid*widthSubRegion;
		if( weight.width != regionSize ) {
			throw new IllegalArgumentException("Weighting kernel has an unexpected size");
		}

		int regionR = regionSize/2;
		int regionEnd = regionSize-regionR;

		int regionIndex = 0;

		// when computing the pixel coordinates it is more precise to round to the nearest integer
		// since pixels are always positive round() is equivalent to adding 0.5 and then converting
		// to an int, which floors the variable.
		c_x += 0.5;
		c_y += 0.5;

		// step through the sub-regions
		for( int rY = -regionR; rY < regionEnd; rY += widthSubRegion ) {
			for( int rX = -regionR; rX < regionEnd; rX += widthSubRegion ) {
				double sum_dx = 0, sum_dy=0, sum_adx=0, sum_ady=0;

				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < widthSubRegion; i++ ) {
					double regionY = (rY + i)*scale;
					for( int j = 0; j < widthSubRegion; j++ ) {
						double w = weight.get(regionR+rX + j, regionR+rY + i);

						double regionX = (rX + j)*scale;

						// rotate the pixel along the feature's direction
						int pixelX = (int)(c_x + c*regionX - s*regionY);
						int pixelY = (int)(c_y + s*regionX + c*regionY);

						// compute the wavelet and multiply by the weighting factor
						GradientValue g = gradient.compute(pixelX,pixelY);
						double dx = w*g.getX();
						double dy = w*g.getY();

						// align the gradient along image patch
						// note the transform is transposed
						double pdx =  c*dx + s*dy;
						double pdy = -s*dx + c*dy;

						sum_dx += pdx;
						sum_adx += Math.abs(pdx);
						sum_dy += pdy;
						sum_ady += Math.abs(pdy);
					}
				}
				features[regionIndex++] = sum_dx;
				features[regionIndex++] = sum_adx;
				features[regionIndex++] = sum_dy;
				features[regionIndex++] = sum_ady;
			}
		}
	}

	/**
	 * Compute the sign of the Laplacian using a sparse convolution.
	 *
	 * @param x center
	 * @param y center
	 * @param scale scale of the feature
	 * @return true if positive
	 */
	public boolean computeLaplaceSign(int x, int y, double scale) {
		int s = (int)Math.ceil(scale);
		kerXX = DerivativeIntegralImage.kernelDerivXX(9*s,kerXX);
		kerYY = DerivativeIntegralImage.kernelDerivYY(9*s,kerYY);
		double lap = GIntegralImageOps.convolveSparse(ii,kerXX,x,y);
		lap += GIntegralImageOps.convolveSparse(ii,kerYY,x,y);

		return lap > 0;
	}

	public int getDescriptionLength() {
		return featureDOF;
	}

	/**
	 * Width of sampled region when sampling is aligned with image pixels
	 * @return width of descriptor sample
	 */
	public int getCanonicalWidth() {
		//
		return widthLargeGrid*widthSubRegion+widthSample-(widthSample%2);
	}
}
