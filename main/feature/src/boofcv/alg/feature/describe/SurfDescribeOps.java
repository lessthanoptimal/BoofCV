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

import boofcv.alg.feature.describe.impl.ImplSurfDescribeOps;
import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.alg.transform.ii.SparseIntegralGradientKernel;
import boofcv.factory.transform.ii.FactorySparseIntegralFilters;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;


/**
 * Operations related to computing SURF descriptors.
 *
 * @author Peter Abeles
 */
public class SurfDescribeOps {

	/**
	 * <p>
	 * Computes the of a square region.  The region considered has a radius
	 * of ceil(radius*s) pixels.  The derivative is computed every 's' pixels.
	 * </p>
	 *
	 * <p>
	 * Deviation from paper:<br>
	 * <ul>
	 * <li>An symmetric box derivative is used instead of the Haar wavelet.</li>
	 * </ul>
	 * </p>
	 *
	 * @param c_x Center pixel.
	 * @param c_y Center pixel.
	 * @param radiusRegions Radius of region being considered in samples points (not pixels).
	 * @param kernelSize Size of the kernel's width (in pixels) at scale of 1.
	 * @param scale Scale of feature.  Changes sample points.
	 * @param derivX Derivative x wavelet output. length = radiusRegions*radiusRegions
	 * @param derivY Derivative y wavelet output. length = radiusRegions*radiusRegions
	 */
	public static <T extends ImageBase>
	void gradient( T ii ,int c_x , int c_y ,
				   int radiusRegions, int kernelSize , double scale,
				   double []derivX , double derivY[] )
	{
		ImplSurfDescribeOps.naiveGradient(ii,c_x,c_y, radiusRegions, kernelSize, scale, derivX,derivY);
	}

	/**
	 * Faster version of {@lin #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder( ImageFloat32 ii , int c_x , int c_y ,
							int radius , int kernelSize , double scale,
							float[] derivX , float[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,c_x,c_y,radius, kernelSize, scale, derivX,derivY);
	}

	/**
	 * Faster version of {@lin #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder( ImageSInt32 ii , int c_x , int c_y ,
							int radius , int kernelSize , double scale,
							int[] derivX , int[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,c_x,c_y,radius, kernelSize, scale, derivX,derivY);
	}

	/**
	 * Create class for computing the image gradient from an integral image in a sparse fashion.
	 *
	 * @param assumeInsideImage Can it assume that the feature is contained entirely inside the image.
	 * @param useHaar Should it use a haar wavelet or an derivative kernel.
	 * @param kernelSize Size of the kernel's width in pixels (before scale adjustment).
	 * @param scale Scale of the kernel.
	 * @param imageType Type of image being processed.
	 * @return Sparse gradient algorithm
	 */
	public static <T extends ImageBase>
	SparseImageGradient<T,?> createGradient( boolean assumeInsideImage ,
											 boolean useHaar , int kernelSize , double scale,
											 Class<T> imageType )
	{
		// adjust the size for the scale factor
		kernelSize = (int)Math.ceil(kernelSize*scale);

		if( assumeInsideImage && !useHaar ) {
			int regionRadius = kernelSize/2;
			return FactorySparseIntegralFilters.gradient(regionRadius,imageType);
		} else {
			IntegralKernel kernelX,kernelY;
			if( useHaar ) {
				kernelX = DerivativeIntegralImage.kernelHaarX(kernelSize);
				kernelY = DerivativeIntegralImage.kernelHaarY(kernelSize);
			} else {
				kernelX = DerivativeIntegralImage.kernelDerivX(kernelSize);
				kernelY = DerivativeIntegralImage.kernelDerivY(kernelSize);
			}
			return new SparseIntegralGradientKernel<T>(kernelX,kernelY);
		}
	}

	/**
	 * Checks to see if the region is contained inside the image.  This includes convolution
	 * kernel.
	 *
	 * @param c_x Center of the interest point.
	 * @param c_y Center of the interest point.
	 * @param radiusRegions Radius in pixels of the whole region at a scale of 1
	 * @param kernelSize Size of the kernel's width in pixels at a scale of 1
	 * @param scale Scale factor for the region.
	 */
	public static <T extends ImageBase>
	boolean isInside( T ii , int c_x , int c_y , int radiusRegions , int kernelSize , double scale) {

		// size of the convolution kernel
		kernelSize = (int)Math.ceil(kernelSize*scale);
		int kernelRadius = kernelSize/2;

		// find the radius of the whole area being sampled
		int radius = (int)Math.ceil(radiusRegions*scale);

		// integral image convolutions sample the pixel before the region starts
		// which is why the extra minus one is there
		int kernelPaddingMinus = -radius-kernelRadius-1;
		int kernelPaddingPlus = radius+kernelRadius;

		// compute the new bounds and see if its inside
		int x0 = c_x+kernelPaddingMinus;
		if( x0 < 0 ) return false;
		int x1 = c_x+kernelPaddingPlus;
		if( x1 >= ii.width ) return false;
		int y0 = c_y+kernelPaddingMinus;
		if( y0 < 0 ) return false;
		int y1 = c_y+kernelPaddingPlus;
		if( y1 >= ii.height) return false;

		return true;
	}

	/**
	 * Checks to see if the region is contained inside the image.  This includes convolution
	 * kernel.  Take in account the orientation of the region.
	 *
	 * @param c_x Center of the interest point.
	 * @param c_y Center of the interest point.
	 * @param radiusRegions Radius in pixels of the whole region at a scale of 1
	 * @param kernelSize Size of the kernel in pixels at a scale of 1
	 * @param scale Scale factor for the region.
	 * @param theta Orientation of the region
	 */
	public static <T extends ImageBase>
	boolean isInside( T ii , int c_x , int c_y , int radiusRegions , int kernelSize ,
					  double scale, double theta )
	{
		kernelSize = (int)Math.ceil(kernelSize*scale);
		int kernelRadius = kernelSize/2;

		// find the radius of the whole area being sampled
		int radius = (int)Math.ceil(radiusRegions*scale);

		// integral image convolutions sample the pixel before the region starts
		// which is why the extra minus one is there
		int kernelPaddingMinus = radius+kernelRadius+1;
		int kernelPaddingPlus = radius+kernelRadius;

		// take in account the rotation
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double xx = Math.abs(c*kernelPaddingMinus - s*kernelPaddingMinus);
		double yy = Math.abs(s*kernelPaddingMinus + c*kernelPaddingMinus);

		double delta = xx>yy? xx - kernelPaddingMinus : yy - kernelPaddingMinus;

		kernelPaddingMinus += (int)Math.ceil(delta);
		kernelPaddingPlus += (int)Math.ceil(delta);

		// compute the new bounds and see if its inside
		int x0 = c_x-kernelPaddingMinus;
		if( x0 < 0 ) return false;
		int x1 = c_x+kernelPaddingPlus;
		if( x1 >= ii.width ) return false;
		int y0 = c_y-kernelPaddingMinus;
		if( y0 < 0 ) return false;
		int y1 = c_y+kernelPaddingPlus;
		if( y1 >= ii.height) return false;

		return true;
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
	 * <li>An symmetric box derivative is used instead of the Haar wavelet.  Haar is not symmetric and the performance
	 * noticeable improved when the derivative was used instead.</li>
	 * </ul>
	 * </p>
	 *
	 * @param ii Integral image.
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param theta Orientation of the features.
	 * @param weight Gaussian normalization.
	 * @param regionSize Size of the region in pixels at a scale of 1..
	 * @param subSize Size of a sub-region that features are computed inside of in pixels at scale of 1.
	 * @param scale The scale of the wavelets.
	 * @param inBounds Can it assume that the entire feature + kernel is inside the image bounds?
	 * @param features Where the features are written to.  Must be 4*numSubRegions^2 large.
	 */
	public static <T extends ImageBase>
	void features( T ii , int c_x , int c_y ,
				   double theta , Kernel2D_F64 weight ,
				   int regionSize , int subSize , double scale ,
				   boolean inBounds ,
				   double []features )
	{
		SparseImageGradient<T,?> gradient = createGradient(inBounds,false,2,scale,(Class<T>)ii.getClass());
		gradient.setImage(ii);

		ImplSurfDescribeOps.features(c_x,c_y,theta,weight,regionSize,subSize,scale,gradient,features);
	}

	// todo move to a generalized class?
	public static void normalizeFeatures( double []features ) {
		double norm = 0;
		for( int i = 0; i < features.length; i++ ) {
			double a = features[i];
			norm += a*a;
		}
		// if the norm is zero, don't normalize
		if( norm == 0 )
			return;
		
		norm = Math.sqrt(norm);
		for( int i = 0; i < features.length; i++ ) {
			features[i] /= norm;
		}
	}
}
