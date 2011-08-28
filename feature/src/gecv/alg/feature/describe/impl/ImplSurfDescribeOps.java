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

package gecv.alg.feature.describe.impl;

import gecv.alg.feature.describe.SurfDescribeOps;
import gecv.misc.GecvMiscOps;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.deriv.GradientValue;
import gecv.struct.deriv.SparseImageGradient;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;


/**
 * @author Peter Abeles
 */
public class ImplSurfDescribeOps {

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link gecv.alg.transform.ii.DerivativeIntegralImage)}.
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
			int indexRow2 = ii.startIndex + (pixelsY-1)*ii.stride - r - 1;
			int indexRow3 = ii.startIndex + pixelsY*ii.stride - r - 1;
			int indexRow4 = ii.startIndex + (pixelsY+r)*ii.stride - r - 1;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = c_x + (int)Math.floor(x*scale);

				int indexSrc1 = indexRow1 + pixelsX;
				int indexSrc2 = indexRow2 + pixelsX;
				int indexSrc3 = indexRow3 + pixelsX;
				int indexSrc4 = indexRow4 + pixelsX;

				float p0 = ii.data[indexSrc1];
				float p1 = ii.data[indexSrc1+r];
				float p2 = ii.data[indexSrc1+r+1];
				float p3 = ii.data[indexSrc1+w];
				float p11 = ii.data[indexSrc2];
				float p4 = ii.data[indexSrc2+w];
				float p10 = ii.data[indexSrc3];
				float p5 = ii.data[indexSrc3+w];
				float p9 = ii.data[indexSrc4];
				float p8 = ii.data[indexSrc4+r];
				float p7 = ii.data[indexSrc4+r+1];
				float p6 = ii.data[indexSrc4+w];

				float left = p8-p9-p1+p0;
				float right = p6-p7-p3+p2;
				float top = p4-p11-p3+p0;
				float bottom = p6-p9-p5+p10;

				derivX[i] = right-left;
				derivY[i] = bottom-top;
			}
		}
	}

	/**
	 * Computes the gradient for a using the derivX kernel found in {@link gecv.alg.transform.ii.DerivativeIntegralImage)}.
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
			int indexRow2 = ii.startIndex + (pixelsY-1)*ii.stride - r - 1;
			int indexRow3 = ii.startIndex + pixelsY*ii.stride - r - 1;
			int indexRow4 = ii.startIndex + (pixelsY+r)*ii.stride - r - 1;

			for( int x = -radius; x <= radius; x++ , i++) {
				int pixelsX = c_x + (int)Math.floor(x*scale);

				int indexSrc1 = indexRow1 + pixelsX;
				int indexSrc2 = indexRow2 + pixelsX;
				int indexSrc3 = indexRow3 + pixelsX;
				int indexSrc4 = indexRow4 + pixelsX;

				int p0 = ii.data[indexSrc1];
				int p1 = ii.data[indexSrc1+r];
				int p2 = ii.data[indexSrc1+r+1];
				int p3 = ii.data[indexSrc1+w];
				int p11 = ii.data[indexSrc2];
				int p4 = ii.data[indexSrc2+w];
				int p10 = ii.data[indexSrc3];
				int p5 = ii.data[indexSrc3+w];
				int p9 = ii.data[indexSrc4];
				int p8 = ii.data[indexSrc4+r];
				int p7 = ii.data[indexSrc4+r+1];
				int p6 = ii.data[indexSrc4+w];

				int left = p8-p9-p1+p0;
				int right = p6-p7-p3+p2;
				int top = p4-p11-p3+p0;
				int bottom = p6-p9-p5+p10;

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

		GecvMiscOps.zero(features,64);

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
