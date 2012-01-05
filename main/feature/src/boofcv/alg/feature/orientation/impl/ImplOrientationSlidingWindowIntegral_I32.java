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
import georegression.metric.UtilAngle;


/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.orientation.OrientationSlidingWindow} for integral images.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationSlidingWindowIntegral_I32
		extends OrientationIntegralBase<ImageSInt32>
{
	// where the output from the derivative is stored
	int[] derivX;
	int[] derivY;

	// derivative needed for border algorithm
	double[] borderDerivX;
	double[] borderDerivY;

	// number of different angles it will consider
	protected int numAngles;
	// the size of the angle window it will consider in radians
	protected double windowSize;
	// size of the sample kernel
	protected int sampleKernelWidth;
	// the angle each pixel is pointing
	protected double angles[];

	/**
	 *
	 * @param numAngles Number of different center points for the sliding window that will be considered
	 * @param windowSize Angular window that is slide across
	 * @param radius Radius of the region being considered in terms of samples. Typically 6.
	 * @param weighted If edge intensities are weighted using a Gaussian kernel.
	 * @param sampleKernelWidth Size of kernel doing the sampling.  Typically 4.
	 */
	public ImplOrientationSlidingWindowIntegral_I32(int numAngles, double windowSize,
													int radius, boolean weighted , int sampleKernelWidth ) {
		super(radius,weighted);
		this.numAngles = numAngles;
		this.windowSize = windowSize;
		this.sampleKernelWidth = sampleKernelWidth;

		derivX = new int[width*width];
		derivY = new int[width*width];

		borderDerivX = new double[width*width];
		borderDerivY = new double[width*width];

		angles = new double[ width*width ];
	}

	@Override
	public double compute(double c_x, double c_y) {

		// use a faster algorithm if it is entirely inside
		if( SurfDescribeOps.isInside(ii,c_x,c_y,radius,sampleKernelWidth,scale, 0))  {
			SurfDescribeOps.gradient_noborder(ii,c_x,c_y,radius,sampleKernelWidth,scale,derivX,derivY);
		} else {
			SurfDescribeOps.gradient(ii,c_x,c_y,radius,sampleKernelWidth,scale, true, borderDerivX,borderDerivY);
			BoofMiscOps.convertTo_I32(borderDerivX, derivX);
			BoofMiscOps.convertTo_I32(borderDerivY, derivY);
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

	private double unweighted() {
		double windowRadius = windowSize/2.0;
		int bestScore = -1;
		double bestAngle = 0;
		double stepAngle = Math.PI*2.0/numAngles;

		for( double angle = -Math.PI; angle < Math.PI; angle += stepAngle ) {
			int dx = 0;
			int dy = 0;
			for( int i = 0; i < angles.length; i++ ) {
				double diff = UtilAngle.dist(angle, angles[i]);
				if( diff <= windowRadius) {
					dx += derivX[i];
					dy += derivY[i];
				}
			}
			int n = dx*dx + dy*dy;
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
			for( int i = 0; i < angles.length; i++ ) {
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

	@Override
	public Class<ImageSInt32> getImageType() {
		return ImageSInt32.class;
	}
}
