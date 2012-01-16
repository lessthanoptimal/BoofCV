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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageSInt32;


/**
 * <p>
 * Estimates the orientation of a region by computing the image derivative from an integral image.
 * The derivative along each axis is summed up and the angle computed from that.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationAverageIntegral_I32
		extends OrientationIntegralBase<ImageSInt32>
{
	// where the output from the derivative is stored
	int[] derivX;
	int[] derivY;

	// derivative needed for border algorithm
	double[] borderDerivX;
	double[] borderDerivY;

	/**
	 *
	 * @param radius Radius of the region being considered in terms of Wavelet samples. Typically 6.
	 * @param weightSigma Sigma for weighting distribution.  Zero for unweighted.
	 */
	public ImplOrientationAverageIntegral_I32( int radius , double period , int sampleWidth , double weightSigma ) {
		super(radius,period,sampleWidth,weightSigma);

		derivX = new int[width*width];
		derivY = new int[width*width];

		borderDerivX = new double[width*width];
		borderDerivY = new double[width*width];
	}

	@Override
	public double compute(double c_x, double c_y) {

		double period = scale*this.period;
		double tl_x = c_x - radius*period;
		double tl_y = c_y - radius*period;

		// use a faster algorithm if it is entirely inside
		if( SurfDescribeOps.isInside(ii.width,ii.height,tl_x,tl_y,width*period,sampleWidth*scale))  {
			SurfDescribeOps.gradient_noborder(ii,tl_x,tl_y,period,width,sampleWidth*scale,derivX,derivY);
		} else {
			SurfDescribeOps.gradient(ii,tl_x,tl_y,period,width,sampleWidth*scale, false, borderDerivX,borderDerivY);
			BoofMiscOps.convertTo_I32(borderDerivX,derivX);
			BoofMiscOps.convertTo_I32(borderDerivY,derivY);
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
