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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.alg.feature.orientation.OrientationAverageIntegral;
import boofcv.misc.GecvMiscOps;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.ImageSInt32;


/**
 * <p>
 * Implementation of {@link OrientationAverageIntegral} for a specific type.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationAverageIntegral_I32
		extends OrientationAverageIntegral<ImageSInt32>
{
	// where the output from the derivative is stored
	int[] derivX;
	int[] derivY;

	// derivative needed for border algorithm
	double[] borderDerivX;
	double[] borderDerivY;

	// optional weights
	protected Kernel2D_F64 weights;

	/**
	 *
	 * @param radius Radius of the region being considered in terms of Wavelet samples. Typically 6.
	 * @param weighted If edge intensities are weighted using a Gaussian kernel.
	 */
	public ImplOrientationAverageIntegral_I32( int radius , boolean weighted ) {
		super(radius,weighted);

		derivX = new int[width*width];
		derivY = new int[width*width];

		borderDerivX = new double[width*width];
		borderDerivY = new double[width*width];
	}

	@Override
	public double compute(int c_x, int c_y ) {
		// use a faster algorithm if it is entirely inside
		if( SurfDescribeOps.isInside(ii,c_x,c_y,4,radius,scale))  {
			SurfDescribeOps.gradient_noborder(ii,c_x,c_y,radius,4,scale,derivX,derivY);
		} else {
			SurfDescribeOps.gradient(ii,c_x,c_y,radius,4,scale,borderDerivX,borderDerivY);
			GecvMiscOps.convertTo_I32(borderDerivX,derivX);
			GecvMiscOps.convertTo_I32(borderDerivY,derivY);
		}

		double Dx=0,Dy=0;
		if( weights == null ) {
			for( int i = 0; i < derivX.length; i++ ) {
				Dx += derivX[i];
				Dy += derivY[i];
			}
		} else {
			for( int i = 0; i < derivX.length; i++ ) {
				double w = weights.data[i];
				Dx += w*derivX[i];
				Dy += w*derivY[i];
			}
		}

		return Math.atan2(Dy,Dx);
	}

	@Override
	public Class<ImageSInt32> getImageType() {
		return ImageSInt32.class;
	}
}
