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
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import georegression.metric.UtilAngle;


/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.orientation.OrientationSlidingWindow} for integral images.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationSlidingWindowIntegral
		<T extends ImageSingleBand,G extends GradientValue>
		extends OrientationIntegralBase<T,G>
{
	// where the output from the derivative is stored
	double[] derivX;
	double[] derivY;

	// number of different angles it will consider
	protected int numAngles;
	// the size of the angle window it will consider in radians
	protected double windowSize;
	// the angle each pixel is pointing
	protected double angles[];
	// number of valid sample points
	protected int num;

	/**
	 *
	 * @param numAngles Number of different center points for the sliding window that will be considered
	 * @param samplePeriod How often (in units of scale) does it sample the image?
	 * @param windowSize Angular window that is slide across
	 * @param radius Radius of the region being considered in terms of samples. Typically 6.
	 * @param weightSigma Sigma for weighting distribution.  Zero for unweighted.
	 * @param sampleKernelWidth Size of kernel doing the sampling.  Typically 4.
	 */
	public ImplOrientationSlidingWindowIntegral(int numAngles, double samplePeriod, double windowSize,
												int radius, double weightSigma, int sampleKernelWidth,
												Class<T> imageType) {
		super(radius,samplePeriod,sampleKernelWidth,weightSigma,imageType);
		this.numAngles = numAngles;
		this.windowSize = windowSize;

		derivX = new double[width*width];
		derivY = new double[width*width];

		angles = new double[ width*width ];
	}

	@Override
	public double compute(double c_x, double c_y) {

		double period = scale*this.period;
		// top left corner of the region being sampled
		double tl_x = c_x - radius*period;
		double tl_y = c_y - radius*period;

		// use a faster algorithm if it is entirely inside
		if( SurfDescribeOps.isInside(ii.width,ii.height,tl_x,tl_y,width*period,sampleWidth*scale))  {
			gradientInner(tl_x,tl_y,period);
		} else {
			gradientBorder(tl_x,tl_y,period);
		}

		for( int i = 0; i < derivX.length; i++ ) {
			angles[i] = Math.atan2(derivY[i],derivX[i]);
		}

		if( weights == null ) {
			return unweighted();
		} else {
			return weighted();
		}
	}

	/**
	 * Compute the gradient while checking for border conditions
	 */
	protected void gradientBorder( double tl_x, double tl_y, double samplePeriod )
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		num = 0;
		for( int y = 0; y < width; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);

			for( int x = 0; x < width; x++ ) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				if( g.isInBounds(pixelsX, pixelsY)) {
					G v = g.compute(pixelsX,pixelsY);
					derivX[num] = v.getX();
					derivY[num] = v.getY();
					num++;
				}
			}
		}
	}

	protected void gradientInner( double tl_x, double tl_y, double samplePeriod )
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		num = 0;
		for( int y = 0; y < width; y++ ) {
			int pixelsY = (int)(tl_y + y * samplePeriod);

			for( int x = 0; x < width; x++ ) {
				int pixelsX = (int)(tl_x + x * samplePeriod);

				G v = g.compute(pixelsX,pixelsY);
				derivX[num] = v.getX();
				derivY[num] = v.getY();
				num++;
			}
		}
	}

	private double unweighted() {
		double windowRadius = windowSize/2.0;
		double bestScore = -1;
		double bestAngle = 0;
		double stepAngle = Math.PI*2.0/numAngles;

		for( double angle = -Math.PI; angle < Math.PI; angle += stepAngle ) {
			double dx = 0;
			double dy = 0;
			for( int i = 0; i < num; i++ ) {
				double diff = UtilAngle.dist(angle, angles[i]);
				if( diff <= windowRadius) {
					dx += derivX[i];
					dy += derivY[i];
				}
			}
			double n = dx*dx + dy*dy;
			if( n > bestScore) {
				bestAngle = Math.atan2(dy,dx);
				bestScore = n;
			}
		}

		return bestAngle;
	}

	private double weighted() {
		double windowRadius = windowSize/2.0;
		double bestScore = -1;
		double bestAngle = 0;
		double stepAngle = Math.PI*2.0/numAngles;

		for( double angle = -Math.PI; angle < Math.PI; angle += stepAngle ) {
			double dx = 0;
			double dy = 0;
			for( int i = 0; i < num; i++ ) {
				double diff = UtilAngle.dist(angle, angles[i]);
				if( diff <= windowRadius) {
					dx += weights.data[i]*derivX[i];
					dy += weights.data[i]*derivY[i];
				}
			}
			double n = dx*dx + dy*dy;
			if( n > bestScore) {
				bestAngle = Math.atan2(dy,dx);
				bestScore = n;
			}
		}

		return bestAngle;
	}
}
