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

package boofcv.alg.feature.describe.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;


/**
 * @author Peter Abeles
 */
public class ImplSurfDescribeOps {

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link boofcv.alg.transform.ii.DerivativeIntegralImage)}.
	 * Assumes that the entire region, including the surrounding pixels, are inside the image.
	 */
	public static void gradientInner(ImageFloat32 ii, int c_x, int c_y,
									 int radius, int kernelSize, double scale,
									 float[] derivX, float derivY[])
	{
		int r = (int)Math.ceil(kernelSize*scale)/2;
		int w = r*2+1;
		int i = 0;
		for( int y = -radius; y <= radius; y++ ) {
			int pixelsY = c_y + (int)Math.floor(y*scale);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = c_x + (int)Math.floor(x*scale);

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
	public static void gradientInner(ImageSInt32 ii, int c_x, int c_y,
									 int radius, int kernelSize, double scale,
									 int[] derivX, int derivY[])
	{
		int r = (int)Math.ceil(kernelSize*scale)/2;
		int w = r*2+1;
		int i = 0;
		for( int y = -radius; y <= radius; y++ ) {
			int pixelsY = c_y + (int)Math.floor(y*scale);
			int indexRow1 = ii.startIndex + (pixelsY-r-1)*ii.stride - r - 1;
			int indexRow2 = indexRow1 + r*ii.stride;
			int indexRow3 = indexRow2 + ii.stride;
			int indexRow4 = indexRow3 + r*ii.stride;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = c_x + (int)Math.floor(x*scale);

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
	void naiveGradient(T ii, int c_x, int c_y,
					   int radiusRegions, int kernelSize, double scale,
					   double[] derivX, double derivY[])
	{
		SparseImageGradient<T,?> g =  SurfDescribeOps.createGradient(false,false,kernelSize,scale,(Class<T>)ii.getClass());
		g.setImage(ii);

		int i = 0;
		for( int y = -radiusRegions; y <= radiusRegions; y++ ) {
			for( int x = -radiusRegions; x <= radiusRegions; x++ , i++) {
				int xx = (int)Math.floor(x*scale);
				int yy = (int)Math.floor(y*scale);

				GradientValue deriv = g.compute(c_x+xx,c_y+yy);
				derivX[i] = deriv.getX();
				derivY[i] = deriv.getY();
			}
		}
	}

	/**
	 * Computes SURF features for the specified region.
	 */
	public static <T extends ImageBase>
	void features( int c_x , int c_y ,
				   double theta , Kernel2D_F64 weight ,
				   int regionSize , int subSize , double scale ,
				   SparseImageGradient<T,?> gradient,
				   double []features )
	{
		if( weight.width != regionSize+1 )
			throw new IllegalArgumentException("Weighting kernel has an unexpected size");

		BoofMiscOps.zero(features,64);

		int regionR = regionSize/2;
		int regionEnd = regionSize-regionR;

		int regionIndex = 0;
		int weightIndex = 0;

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		// step through the sub-regions
		for( int rY = -regionR; rY < regionEnd; rY += subSize ) {
			for( int rX = -regionR; rX < regionEnd; rX += subSize ) {
				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < subSize; i++ ) {
					int regionY = (int)((rY + i)*scale);
					for( int j = 0; j < subSize; j++ ) {
						double w = weight.data[weightIndex++];

						int regionX = (int)((rX + j)*scale);

						// rotate the pixel along the feature's direction
						int pixelX = c_x + (int)(c*regionX - s*regionY);
						int pixelY = c_y + (int)(s*regionX + c*regionY);

						// compute the wavelet and multiply by the weighting factor
						GradientValue g = gradient.compute(pixelX,pixelY);
						double dx = w*g.getX();
						double dy = w*g.getY();

						// align the gradient along image patch
						// note the transform is transposed
						double pdx =  c*dx + s*dy;
						double pdy = -s*dx + c*dy;

						features[regionIndex] += pdx;
						features[regionIndex+1] += Math.abs(pdx);
						features[regionIndex+2] += pdy;
						features[regionIndex+3] += Math.abs(pdy);
					}
				}
				regionIndex += 4;
			}
		}
	}
}
