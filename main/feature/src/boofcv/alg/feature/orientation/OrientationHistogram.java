/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.abst.feature.orientation.OrientationGradient;
import boofcv.alg.InputSanityCheck;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageSingleBand;


/**
 * <p>
 * Estimates the orientation by creating a histogram of discrete angles around
 * the entire circle.  The angle with the largest sum of edge intensities is considered
 * to be the direction of the region.    If weighted a Gaussian kernel centered around the targeted
 * pixel is used.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationHistogram <D extends ImageSingleBand>
		implements OrientationGradient<D>
{
	// the region's radius
	protected int radius;
	// the radius at the set scale
	protected int radiusScale;

	// image x and y derivatives
	protected D derivX;
	protected D derivY;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	protected ImageRectangle rect = new ImageRectangle();

	// number of different angles it will consider
	protected int numAngles;
	// used to compute the score for each angle
	protected double sumDerivX[];
	protected double sumDerivY[];

	// resolution of each angle
	protected double angleDiv;
	// used to round towards the nearest angle
	protected double angleRound;

	// if it uses weights or not
	protected boolean isWeighted;
	// optional weights
	protected Kernel2D_F32 weights;

	/**
	 * Constructor. Specify region size and if it is weighted or not.
	 *
	 * @param numAngles Number of discrete angles that the orientation is estimated inside of
	 */
	public OrientationHistogram( int numAngles , boolean isWeighted ) {
		this.numAngles = numAngles;
		sumDerivX = new double[ numAngles ];
		sumDerivY = new double[ numAngles ];

		angleDiv = 2.0*Math.PI/numAngles;
		angleRound = Math.PI+angleDiv/2.0;
		this.isWeighted = isWeighted;
	}

	public int getRadius() {
		return radius;
	}

	/**
	 * Specify the size of the region that is considered.
	 *
	 * @param radius
	 */
	public void setRadius(int radius) {
		this.radius = radius;
		setScale(1);
	}

	public Kernel2D_F32 getWeights() {
		return weights;
	}

	@Override
	public void setScale(double scale) {
		radiusScale = (int)Math.ceil(scale*radius);
		if( isWeighted ) {
			weights = FactoryKernelGaussian.gaussian(2,true, 32, -1,radiusScale);
		}
	}

	@Override
	public void setImage( D derivX, D derivY) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public double compute(double X, double Y) {

		int c_x = (int)X;
		int c_y = (int)Y;

		// compute the visible region while taking in account
		// the image borders
		rect.x0 = c_x-radiusScale;
		rect.y0 = c_y-radiusScale;
		rect.x1 = c_x+radiusScale+1;
		rect.y1 = c_y+radiusScale+1;

		BoofMiscOps.boundRectangleInside(derivX,rect);

		for( int i = 0; i < numAngles; i++ ) {
			sumDerivX[i] = 0;
			sumDerivY[i] = 0;
		}

		if( weights == null )
			computeUnweightedScore();
		else
			computeWeightedScore(c_x,c_y);

		// find the angle with the best score
		double bestScore = -1;
		int bestIndex = -1;
		double bestX=-1;
		double bestY=-1;
		for( int i = 0; i < numAngles; i++ ) {
			double x = sumDerivX[i];
			double y = sumDerivY[i];
			double score = x*x + y*y;
			if( score > bestScore ) {
				bestScore = score;
				bestIndex = i;
				bestX = x;
				bestY = y;
			}
		}

		return Math.atan2(bestY,bestX);
	}

	/**
	 * Compute the score without using the optional weights
	 */
	protected abstract void computeUnweightedScore();

	/**
	 * Compute the score using the weighting kernel.
	 */
	protected abstract void computeWeightedScore(int c_x , int c_y );
}
