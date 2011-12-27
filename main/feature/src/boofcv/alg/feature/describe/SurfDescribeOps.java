/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;


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
	 * @param radiusRegion Radius of region being considered in samples points (not pixels).
	 * @param kernelWidth Size of the kernel's width (in pixels) at scale of 1.
	 * @param scale Scale of feature.  Changes sample points.
	 * @param useHaar
	 * @param derivX Derivative x wavelet output. length = radiusRegions*radiusRegions
	 * @param derivY Derivative y wavelet output. length = radiusRegions*radiusRegions
	 */
	public static <T extends ImageSingleBand>
	void gradient(T ii, int c_x, int c_y,
				  int radiusRegion, int kernelWidth, double scale,
				  boolean useHaar, double[] derivX, double derivY[])
	{
		ImplSurfDescribeOps.naiveGradient(ii,c_x,c_y, radiusRegion, kernelWidth, scale, useHaar, derivX,derivY);
	}

	/**
	 * Faster version of {@lin #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder( ImageFloat32 ii , int c_x , int c_y ,
							int radius , int kernelWidth , double scale,
							float[] derivX , float[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,c_x,c_y,radius, kernelWidth, scale, derivX,derivY);
	}

	/**
	 * Faster version of {@lin #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder( ImageSInt32 ii , int c_x , int c_y ,
							int radius , int kernelWidth , double scale,
							int[] derivX , int[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,c_x,c_y,radius, kernelWidth, scale, derivX,derivY);
	}

	/**
	 * Creates a class for computing the image gradient from an integral image in a sparse fashion.
	 *
	 * @param assumeInsideImage Can it assume that the feature is contained entirely inside the image.
	 * @param useHaar Should it use a haar wavelet or an derivative kernel.
	 * @param kernelWidth Size of the kernel's width in pixels (before scale adjustment).
	 * @param scale Scale of the kernel.
	 * @param imageType Type of image being processed.
	 * @return Sparse gradient algorithm
	 */
	public static <T extends ImageSingleBand>
	SparseImageGradient<T,?> createGradient( boolean assumeInsideImage ,
											 boolean useHaar , int kernelWidth , double scale,
											 Class<T> imageType )
	{
		// scale the kernel and round it up to the nearest even size
		kernelWidth = (int)Math.round(scale*kernelWidth);
		int regionRadius = kernelWidth/2 + (kernelWidth%2);

		if( assumeInsideImage ) {
			if( useHaar )
				return FactorySparseIntegralFilters.haar(regionRadius, imageType);
			else
				return FactorySparseIntegralFilters.gradient(regionRadius,imageType);
		} else {
			IntegralKernel kernelX,kernelY;
			if( useHaar ) {
				kernelX = DerivativeIntegralImage.kernelHaarX(regionRadius);
				kernelY = DerivativeIntegralImage.kernelHaarY(regionRadius);
			} else {
				kernelX = DerivativeIntegralImage.kernelDerivX(regionRadius);
				kernelY = DerivativeIntegralImage.kernelDerivY(regionRadius);
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
	 * @param kernelWidth Size of the kernel's width in pixels at a scale of 1
	 * @param scale Scale factor for the region.
	 */
	public static <T extends ImageSingleBand>
	boolean isInside( T ii , int c_x , int c_y , int radiusRegions , int kernelWidth , double scale) {

		// size of the convolution kernel
		kernelWidth = (int)Math.ceil(kernelWidth*scale);
		int kernelRadius = kernelWidth/2;

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
	public static <T extends ImageSingleBand>
	boolean isInside( T ii , int c_x , int c_y , int radiusRegions , int kernelSize ,
					  double scale, double theta )
	{
		kernelSize = (int)Math.ceil(kernelSize*scale);
		int kernelRadius = kernelSize/2+(kernelSize%2);

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
	 * </ul>
	 * </p>
	 *
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param theta Orientation of the features. -pi/2 to pi/2
	 * @param scale The scale of the wavelets.
	 * @param weight Gaussian normalization.
	 * @param widthLargeGrid Number of sub-regions wide the large grid is.
	 * @param widthSubRegion Number of sample points wide a sub-region is.
	 * @param gradient Computes the image gradient at the specified points.  Make sure the scale and image are set.
	 * @param features Where the features are written to.  Must be 4*(widthLargeGrid*widthSubRegion)^2 large.
	 */
	public static <T extends ImageSingleBand>
	void features(double c_x, double c_y,
				  double theta, double scale, Kernel2D_F64 weight,
				  int widthLargeGrid, int widthSubRegion,
				  SparseImageGradient<T, ?> gradient,
				  double[] features)
	{
		int regionSize = widthLargeGrid*widthSubRegion;
		if( weight.width != regionSize ) {
			throw new IllegalArgumentException("Weighting kernel has an unexpected size");
		}

		int regionR = regionSize/2;
		int regionEnd = regionSize-regionR;

		int regionIndex = 0;

		double c = Math.cos(theta);
		double s = Math.sin(theta);

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
	 * <p>
	 * An improved SURF descriptor as presented in CenSurE paper.   The sub-regions now overlap and more
	 * points are sampled in the sub-region to allow overlap.
	 * </p>
	 *
	 * @param c_x Center of the feature x-coordinate.
	 * @param c_y Center of the feature y-coordinate.
	 * @param theta Orientation of the features. -pi/2 to pi/2
	 * @param scale The scale of the wavelets.
	 * @param weightGrid Gaussian normalization across the large grid.
	 * @param weightSub Gaussian normalization across the sub-region.
	 * @param widthLargeGrid Number of sub-regions wide the large grid is.
	 * @param widthSubRegion Number of sample points wide a sub-region is.
	 * @param overLap Number of samples that two adjacent sub-regions will overlap.
	 * @param gradient Computes the image gradient at the specified points.  Make sure the scale and image are set.
	 * @param features Where the features are written to.  Must be 4*(widthLargeGrid*widthSubRegion)^2 large.
	 */
	public static <T extends ImageSingleBand>
	void featuresMod(double c_x, double c_y,
					 double theta,
					 double scale, Kernel2D_F64 weightGrid,
					 Kernel2D_F64 weightSub,
					 int widthLargeGrid, int widthSubRegion, int overLap,
					 SparseImageGradient<T, ?> gradient,
					 double[] features)
	{
		int regionSize = widthLargeGrid*widthSubRegion;

		int totalSampleWidth = widthSubRegion+overLap*2;

		int regionR = regionSize/2;
		int regionEnd = regionSize-regionR;

		int regionIndex = 0;

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		// when computing the pixel coordinates it is more precise to round to the nearest integer
		// since pixels are always positive round() is equivalent to adding 0.5 and then converting
		// to an int, which floors the variable.
		c_x += 0.5;
		c_y += 0.5;

		int indexGridWeight = 0;
		// step through the sub-regions
		for( int rY = -regionR; rY < regionEnd; rY += widthSubRegion ) {
			for( int rX = -regionR; rX < regionEnd; rX += widthSubRegion ) {
				double sum_dx = 0, sum_dy=0, sum_adx=0, sum_ady=0;

				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < totalSampleWidth; i++ ) {
					double regionY = (rY + i - overLap)*scale;
					for( int j = 0; j < totalSampleWidth; j++ ) {
						double regionX = (rX + j - overLap)*scale;

						double w = weightSub.get(j,i);
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

				double w = weightGrid.data[indexGridWeight++];
				features[regionIndex++] = w*sum_dx;
				features[regionIndex++] = w*sum_adx;
				features[regionIndex++] = w*sum_dy;
				features[regionIndex++] = w*sum_ady;
			}
		}
	}

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
