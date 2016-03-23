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

import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import georegression.metric.UtilAngle;
import org.ddogleg.sorting.QuickSort_F64;


/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.orientation.OrientationSlidingWindow} for integral images.
 * TODO comment how this implementation works
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationSlidingWindowIntegral
		<T extends ImageGray,G extends GradientValue>
		extends OrientationIntegralBase<T,G>
{
	// where the output from the derivative is stored
	double[] derivX;
	double[] derivY;

	// the size of the angle window it will consider in radians
	protected double windowSize;
	// the angle each pixel is pointing
	protected double angles[];
	
	// clockwise ordering of angles
	protected int order[];

	int total = 0;

	QuickSort_F64 sorter = new QuickSort_F64();

	/**
	 * Specifies configuration parameters and initializes data structures
	 *
	 * @param samplePeriod How often the image is sampled.  This number is scaled.  Typically 1.
	 * @param windowSize Angular window that is slide across
	 * @param sampleRadius Radius of the region being considered in terms of samples. Typically 6.
	 * @param weightSigma Sigma for weighting distribution.  Zero for unweighted.
	 * @param sampleKernelWidth Size of kernel doing the sampling.  Typically 4.
	 * @param integralType Type of integral image being processed.
	 */
	public ImplOrientationSlidingWindowIntegral(double radiusToScale , double samplePeriod, double windowSize,
												int sampleRadius, double weightSigma, int sampleKernelWidth,
												Class<T> integralType) {
		super(radiusToScale,sampleRadius,samplePeriod,sampleKernelWidth,weightSigma, true, integralType);
		this.windowSize = windowSize;

		derivX = new double[sampleWidth * sampleWidth];
		derivY = new double[sampleWidth * sampleWidth];

		angles = new double[ sampleWidth * sampleWidth];
		order = new int[ angles.length ];
	}

	@Override
	public double compute(double c_x, double c_y) {

		double period = scale*this.period;
		// top left corner of the region being sampled
		double tl_x = c_x - sampleRadius *period;
		double tl_y = c_y - sampleRadius *period;

		computeGradient(tl_x,tl_y,period);

		// apply weight to each gradient dependent on its position
		if( weights != null ) {
			for( int i = 0; i < total; i++ ) {
				double w = weights.data[i];
				derivX[i] *= w;
				derivY[i] *= w;
			} 
		}
		
		for( int i = 0; i < total; i++ ) {
			angles[i] = Math.atan2(derivY[i],derivX[i]);
		}

		// order points from lowest to highest
		sorter.sort(angles, angles.length, order);

		return estimateAngle();
	}

	private void computeGradient( double tl_x , double tl_y , double samplePeriod ) 
	{
		// add 0.5 to c_x and c_y to have it round when converted to an integer pixel
		// this is faster than the straight forward method
		tl_x += 0.5;
		tl_y += 0.5;

		total = 0;
		for(int y = 0; y < sampleWidth; y++ ) {
			
			for(int x = 0; x < sampleWidth; x++ , total++ ) {

				int xx = (int)(tl_x + x * samplePeriod);
				int yy = (int)(tl_y + y * samplePeriod);

				if( g.isInBounds(xx,yy) ) {
					GradientValue deriv = g.compute(xx,yy);
					double dx = deriv.getX();
					double dy = deriv.getY();

					derivX[total] = dx;
					derivY[total] = dy;
				} else {
					derivX[total] = 0;
					derivY[total] = 0;
				}
			}
		}
	}
	
	private double estimateAngle() {
		int start = 0;
		int end = 1;

		int startIndex = order[start];
		int endIndex = order[end];
		double sumX=derivX[startIndex];
		double sumY=derivY[startIndex];
		double best=sumX*sumX+sumY*sumY;
		double bestX=sumX;
		double bestY=sumY;

		double endAngle = angles[endIndex];

		while( start != total ) {
			startIndex = order[start];
			double startAngle = angles[startIndex];

			// only compute the average if the angles are close to each other
			while( UtilAngle.dist(startAngle,endAngle) <= windowSize ) {
				sumX += derivX[endIndex];
				sumY += derivY[endIndex];

				// see if the magnitude of the gradient inside this bound is greater
				// than the previous best
				double mag = sumX*sumX + sumY*sumY;
				if( mag > best ) {
					best = mag;
					bestX = sumX;
					bestY = sumY;
				}
				end++;
				if( end >= total )
					end = 0;
				endIndex = order[end];
				endAngle = angles[endIndex];

				// if it cycled all the way around stop
				if( endIndex == startIndex )
					break;
			}

			// remove the first element from the list
			sumX -= derivX[startIndex];
			sumY -= derivY[startIndex];
			start++;
		}

		return Math.atan2(bestY,bestX);
	}
}
