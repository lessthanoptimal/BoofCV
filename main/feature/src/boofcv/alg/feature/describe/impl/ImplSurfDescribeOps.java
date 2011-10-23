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

package boofcv.alg.feature.describe.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;


/**
 *
 * Design Note:
 * When estimating the
 *
 * @author Peter Abeles
 */
public class ImplSurfDescribeOps {

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link boofcv.alg.transform.ii.DerivativeIntegralImage)}.
	 * Assumes that the entire region, including the surrounding pixels, are inside the image.
	 */
	public static void gradientInner(ImageFloat32 ii, double c_x, double c_y,
									 int radius, int kernelSize, double scale,
									 float[] derivX, float derivY[])
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		c_x += 0.5;
		c_y += 0.5;

		int r = (int)Math.ceil(kernelSize*scale)/2; // todo think about this ceil more
		int w = r*2+1;
		int i = 0;
		for( int y = -radius; y <= radius; y++ ) {
			int pixelsY = (int)(c_y + y * scale);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = (int)(c_x + x * scale);

				final int indexSrc1 = indexRow1 + pixelsX;
				final int indexSrc2 = indexRow2 + pixelsX;
				final int indexSrc3 = indexRow3 + pixelsX;
				final int indexSrc4 = indexRow4 + pixelsX;

				final float p0 = ii.data[indexSrc1];
				final float p1 = ii.data[indexSrc1+r];
				final float p2 = ii.data[indexSrc1+r+1];
				final float p3 = ii.data[indexSrc1+w];
				final float p11 = ii.data[indexSrc2];
				final float p4 = ii.data[indexSrc2+w];
				final float p10 = ii.data[indexSrc3];
				final float p5 = ii.data[indexSrc3+w];
				final float p9 = ii.data[indexSrc4];
				final float p8 = ii.data[indexSrc4+r];
				final float p7 = ii.data[indexSrc4+r+1];
				final float p6 = ii.data[indexSrc4+w];

				final float left = p8-p9-p1+p0;
				final float right = p6-p7-p3+p2;
				final float top = p4-p11-p3+p0;
				final float bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;
			}
		}
	}

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link boofcv.alg.transform.ii.DerivativeIntegralImage)}.
	 * Assumes that the entire region, including the surrounding pixels, are inside the image.
	 */
	public static void gradientInner(ImageSInt32 ii, double c_x, double c_y,
									 int radius, int kernelSize, double scale,
									 int[] derivX, int derivY[])
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		c_x += 0.5;
		c_y += 0.5;

		int r = (int)Math.ceil(kernelSize*scale)/2;
		int w = r*2+1;
		int i = 0;
		for( int y = -radius; y <= radius; y++ ) {
			int pixelsY = (int)(c_y + y * scale);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = (int)(c_x + x * scale);

				final int indexSrc1 = indexRow1 + pixelsX;
				final int indexSrc2 = indexRow2 + pixelsX;
				final int indexSrc3 = indexRow3 + pixelsX;
				final int indexSrc4 = indexRow4 + pixelsX;

				final int p0 = ii.data[indexSrc1];
				final int p1 = ii.data[indexSrc1+r];
				final int p2 = ii.data[indexSrc1+r+1];
				final int p3 = ii.data[indexSrc1+w];
				final int p11 = ii.data[indexSrc2];
				final int p4 = ii.data[indexSrc2+w];
				final int p10 = ii.data[indexSrc3];
				final int p5 = ii.data[indexSrc3+w];
				final int p9 = ii.data[indexSrc4];
				final int p8 = ii.data[indexSrc4+r];
				final int p7 = ii.data[indexSrc4+r+1];
				final int p6 = ii.data[indexSrc4+w];

				final int left = p8-p9-p1+p0;
				final int right = p6-p7-p3+p2;
				final int top = p4-p11-p3+p0;
				final int bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;
			}
		}
	}

	/**
	 * Simple algorithm for computing the gradient of a region.  Can handle image borders
	 */
	public static <T extends ImageBase>
	void naiveGradient(T ii, double c_x, double c_y,
					   int radiusRegions, int kernelSize, double scale,
					   boolean useHaar, double[] derivX, double derivY[])
	{
		SparseImageGradient<T,?> g =  SurfDescribeOps.createGradient(false,useHaar,kernelSize,scale,(Class<T>)ii.getClass());
		g.setImage(ii);

		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		c_x += 0.5;
		c_y += 0.5;

		int i = 0;
		for( int y = -radiusRegions; y <= radiusRegions; y++ ) {
			for( int x = -radiusRegions; x <= radiusRegions; x++ , i++) {
				int xx = (int)(c_x + x * scale);
				int yy = (int)(c_y + y * scale);

				GradientValue deriv = g.compute(xx,yy);
				derivX[i] = deriv.getX();
				derivY[i] = deriv.getY();
//				System.out.printf("%2d %2d dx = %6.2f  dy = %6.2f\n",x,y,derivX[i],derivY[i]);
			}
		}
	}

	/**
	 * Computes SURF features for the specified region.
	 */
	// todo change param
	public static <T extends ImageBase>
	void features( double c_x , double c_y ,
				   double theta , Kernel2D_F64 weight ,
				   int widthLargeGrid , int widthSubRegion,
				   double scale ,
				   SparseImageGradient<T,?> gradient,
				   double []features )
	{
		int regionSize = widthLargeGrid*widthSubRegion;
		if( weight.width != regionSize+1 ) {
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
}
