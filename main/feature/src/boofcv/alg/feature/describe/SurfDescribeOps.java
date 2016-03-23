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

import boofcv.alg.feature.describe.impl.ImplSurfDescribeOps;
import boofcv.factory.transform.ii.FactorySparseIntegralFilters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.SparseScaleGradient;


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
	 * @param tl_x Top left corner.
	 * @param tl_y Top left corner.
	 * @param samplePeriod Distance between sample points (in pixels)
	 * @param regionSize Width of region being considered in samples points (not pixels).
	 * @param kernelWidth Size of the kernel's width (in pixels) .
	 * @param useHaar
	 * @param derivX Derivative x wavelet output. length = radiusRegions*radiusRegions
	 * @param derivY Derivative y wavelet output. length = radiusRegions*radiusRegions
	 */
	public static <T extends ImageGray>
	void gradient(T ii, double tl_x, double tl_y, double samplePeriod ,
				  int regionSize, double kernelWidth,
				  boolean useHaar, double[] derivX, double derivY[])
	{
		ImplSurfDescribeOps.naiveGradient(ii, tl_x, tl_y, samplePeriod , regionSize, kernelWidth, useHaar, derivX, derivY);
	}

	/**
	 * Faster version of {@link #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder(GrayF32 ii , double tl_x , double tl_y , double samplePeriod ,
						   int regionSize , double kernelWidth ,
						   float[] derivX , float[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,tl_x,tl_y,samplePeriod,regionSize, kernelWidth, derivX,derivY);
	}

	/**
	 * Faster version of {@link #gradient} which assumes the region is entirely contained inside the
	 * of the image.  This includes the convolution kernel's radius.
	 */
	public static
	void gradient_noborder(GrayS32 ii , double tl_x , double tl_y , double samplePeriod ,
						   int regionSize , double kernelWidth ,
						   int[] derivX , int[] derivY )
	{
		ImplSurfDescribeOps.gradientInner(ii,tl_x,tl_y,samplePeriod, regionSize, kernelWidth, derivX,derivY);
	}

	/**
	 * Creates a class for computing the image gradient from an integral image in a sparse fashion.
	 * All these kernels assume that the kernel is entirely contained inside the image!
	 *
	 * @param useHaar Should it use a haar wavelet or an derivative kernel.
	 * @param imageType Type of image being processed.
	 * @return Sparse gradient algorithm
	 */
	public static <T extends ImageGray>
	SparseScaleGradient<T,?> createGradient( boolean useHaar , Class<T> imageType )
	{
		if( useHaar )
			return FactorySparseIntegralFilters.haar(imageType);
		else
			return FactorySparseIntegralFilters.gradient(imageType);
	}

	/**
	 * Checks to see if the region is contained inside the image.  This includes convolution
	 * kernel.  Take in account the orientation of the region.
	 *
	 * @param X Center of the interest point.
	 * @param Y Center of the interest point.
	 * @param radiusRegions Radius in pixels of the whole region at a scale of 1
	 * @param kernelSize Size of the kernel in pixels at a scale of 1
	 * @param scale Scale factor for the region.
	 * @param c Cosine of the orientation
	 * @param s Sine of the orientation   
	 */
	public static <T extends ImageGray>
	boolean isInside( T ii , double X , double Y , int radiusRegions , int kernelSize ,
					  double scale, double c , double s )
	{
		int c_x = (int)Math.round(X);
		int c_y = (int)Math.round(Y);
		
		kernelSize = (int)Math.ceil(kernelSize*scale);
		int kernelRadius = kernelSize/2+(kernelSize%2);

		// find the radius of the whole area being sampled
		int radius = (int)Math.ceil(radiusRegions*scale);

		// integral image convolutions sample the pixel before the region starts
		// which is why the extra minus one is there
		int kernelPaddingMinus = radius+kernelRadius+1;
		int kernelPaddingPlus = radius+kernelRadius;

		// take in account the rotation
		if( c != 0 || s != 0) {
			double xx = Math.abs(c*kernelPaddingMinus - s*kernelPaddingMinus);
			double yy = Math.abs(s*kernelPaddingMinus + c*kernelPaddingMinus);

			double delta = xx>yy? xx - kernelPaddingMinus : yy - kernelPaddingMinus;

			kernelPaddingMinus += (int)Math.ceil(delta);
			kernelPaddingPlus += (int)Math.ceil(delta);
		}

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
	 * Tests to see if the rectangular region being sampled is contained inside the image.  Sampling
	 * is done using a square region with the specified size, where size corresponds to the length
	 * of each side.  The sample region's size is discretized and rounded up, making this a conservative
	 * estimate for containment.
	 * </p>
	 * <p>
	 * This takes in account how integral images are read.  To read in a rectangular region the pixel
	 * below the lower left corner is read, which results in an extra minus along enough axis for the
	 * lower bound.  It is also assumed that points are discretized by rounding.
	 * </p>
	 *
	 *
	 * @param width Image's width.
	 * @param height Image's height.
	 * @param tl_x Top left corner of region being sampled.
	 * @param tl_y Top left corner of region being sampled.
	 * @param regionSize Size of the region being sampled.
	 * @param sampleSize Length of each side in the sample region.  See comment above.
	 * @return If all samples are contained inside the image.
	 */
	public static boolean isInside( int width , int height, 
									double tl_x , double tl_y , 
									double regionSize  , double sampleSize )
	{
		int w = (int)(sampleSize+0.5);
		int r = w/2 + w%2; // be conservative  and round up

		// the extra minus one is because integral images are being used
		int x0 = (int)(tl_x+0.5) - r - 1;
		int y0 = (int)(tl_y+0.5) - r - 1;

		if( x0 < 0 || y0 < 0)
			return false;

		int x1 = (int)(tl_x+regionSize+0.5) + r;
		int y1 = (int)(tl_y+regionSize+0.5) + r;

		if( x1 >= width || y1 >= height )
			return false;

		return true;
	}

	/**
	 * Computes the width of a square containment region that contains a rotated rectangle.
	 *
	 * @param width Size of the original rectangle.
	 * @param c Cosine(theta)
	 * @param s Sine(theta)
	 * @return Side length of the containment square.
	 */
	public static double rotatedWidth( double width , double c , double s )
	{
		return Math.abs(c)*width + Math.abs(s)*width;
	}
}
