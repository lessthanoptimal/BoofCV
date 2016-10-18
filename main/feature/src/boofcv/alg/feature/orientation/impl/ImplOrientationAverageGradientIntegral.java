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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseGradientSafe;
import boofcv.struct.sparse.SparseImageGradient;


/**
 * <p>
 * Estimates the orientation of a region by computing the image derivative from an integral image.
 * The derivative along each axis is summed up and the angle computed from that.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationAverageGradientIntegral<T extends ImageGray,G extends GradientValue>
		extends OrientationIntegralBase<T,G>
{
	/**
	 *
	 * @param sampleRadius Radius of the region being considered in terms of Wavelet samples. Typically 6.
	 * @param weightSigma Sigma for weighting distribution.  Zero for unweighted.
	 */
	public ImplOrientationAverageGradientIntegral(double radiusToScale,
												  int sampleRadius, double period,
												  int sampleWidth, double weightSigma,
												  Class<T> imageType) {
		super(radiusToScale,sampleRadius,period,sampleWidth,weightSigma, true, imageType);
	}

	@Override
	public double compute(double c_x, double c_y) {

		double period = scale*this.period;
		double tl_x = c_x - sampleRadius *period;
		double tl_y = c_y - sampleRadius *period;

		SparseImageGradient<T,G> g;
		// use a faster algorithm if it is entirely inside
		if( !SurfDescribeOps.isInside(ii.width,ii.height,tl_x,tl_y, sampleWidth *period, kernelWidth *scale))  {
			g = new SparseGradientSafe<>(this.g);
		} else {
			g = this.g;
		}

		if( weights == null )
			return computeUnweighted(tl_x,tl_y,period,g);
		else
			return computeWeighted(tl_x, tl_y, period,g);
	}

	/**
	 * Compute the gradient while checking for border conditions
	 */
	protected double computeUnweighted( double tl_x, double tl_y, 
										double samplePeriod ,
										SparseImageGradient<T,G> g)
	{
		// add 0.5 to c_x and c_y to have it round
		tl_x += 0.5;
		tl_y += 0.5;

		double Dx=0,Dy=0;
		for(int y = 0; y < sampleWidth; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);

			for(int x = 0; x < sampleWidth; x++ ) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				GradientValue v = g.compute(pixelsX,pixelsY);
				Dx += v.getX();
				Dy += v.getY();
			}
		}
		return Math.atan2(Dy,Dx);
	}

	/**
	 * Compute the gradient while checking for border conditions
	 */
	protected double computeWeighted( double tl_x, double tl_y, 
									  double samplePeriod , 
									  SparseImageGradient<T,G> g )
	{
		// add 0.5 to c_x and c_y to have it round
		tl_x += 0.5;
		tl_y += 0.5;

		double Dx=0,Dy=0;
		int i = 0;
		for(int y = 0; y < sampleWidth; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);

			for(int x = 0; x < sampleWidth; x++ , i++ ) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				double w = weights.data[i];
				GradientValue v = g.compute(pixelsX,pixelsY);
				Dx += w*v.getX();
				Dy += w*v.getY();
			}
		}

		return Math.atan2(Dy,Dx);
	}
}
