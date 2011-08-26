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

import gecv.alg.transform.ii.DerivativeIntegralImage;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.alg.transform.ii.IntegralKernel;
import gecv.misc.GecvMiscOps;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.image.ImageBase;


/**
 * Straight forward un-optimized routines for computing SURF descriptors.
 *
 * @author Peter Abeles
 */
public class NaiveSurfDescribeOps {

	public static <T extends ImageBase>
	void gradient( T ii , int c_x , int c_y ,
				   int radius , double s ,
				   boolean useHaar ,
				   double []derivX , double derivY[] )
	{
		IntegralKernel kernelX,kernelY;
		if( useHaar ) {
			kernelX = DerivativeIntegralImage.kernelHaarX((int)Math.ceil(4*s));
			kernelY = DerivativeIntegralImage.kernelHaarY((int)Math.ceil(4*s));
		} else {
			kernelX = DerivativeIntegralImage.kernelDerivX((int)Math.ceil(4*s));
			kernelY = DerivativeIntegralImage.kernelDerivY((int)Math.ceil(4*s));
		}

		int i = 0;
		for( int y = -radius; y <= radius; y++ ) {
			for( int x = -radius; x <= radius; x++ , i++) {
				int xx = (int)Math.floor(x*s);
				int yy = (int)Math.floor(y*s);

				derivX[i] = GIntegralImageOps.convolveSparse(ii,kernelX,c_x+xx,c_y+yy);
				derivY[i] = GIntegralImageOps.convolveSparse(ii,kernelY,c_x+xx,c_y+yy);
			}
		}
	}

	public static <T extends ImageBase>
	void features( T ii , int c_x , int c_y ,
				   double theta , Kernel2D_F64 weight ,
				   int regionSize , int numSubRegions , double scale ,
				   boolean useHaar ,
				   double []features )
	{
		if( weight.width != regionSize+1 )
			throw new IllegalArgumentException("Weighting kernel has an unexpected size");

		GecvMiscOps.zero(features,64);

		int subSize = regionSize/numSubRegions;
		int regionR = regionSize/2;

		IntegralKernel kernelX,kernelY;
		if( useHaar ) {
			kernelX = DerivativeIntegralImage.kernelHaarX((int)Math.ceil(2*scale));
			kernelY = DerivativeIntegralImage.kernelHaarY((int)Math.ceil(2*scale));
		} else {
			kernelX = DerivativeIntegralImage.kernelDerivX((int)Math.ceil(2*scale));
			kernelY = DerivativeIntegralImage.kernelDerivY((int)Math.ceil(2*scale));
		}

		int regionIndex = 0;
		int weightIndex = 0;

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		// step through the sub-regions
		for( int rY = -regionR; rY < regionR; rY += subSize ) {
			for( int rX = -regionR; rX <regionR; rX += subSize ) {
				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < subSize; i++ ) {
					for( int j = 0; j < subSize; j++ ) {
						double w = weight.data[weightIndex++];

						int regionX = (int)((rX + j)*scale);
						int regionY = (int)((rY + i)*scale);

						// rotate the pixel along the feature's direction
						int pixelX = c_x + (int)(c*regionX - s*regionY);
						int pixelY = c_y + (int)(s*regionX + c*regionY);

						// compute the wavelet and multiply by the weighting factor
						double dx =  w*GIntegralImageOps.convolveSparse(ii,kernelX,pixelX,pixelY);
						double dy =  w*GIntegralImageOps.convolveSparse(ii,kernelY,pixelX,pixelY);

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
