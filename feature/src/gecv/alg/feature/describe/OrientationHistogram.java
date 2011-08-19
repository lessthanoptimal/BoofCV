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

package gecv.alg.feature.describe;

import gecv.alg.InputSanityCheck;
import gecv.misc.GecvMiscOps;
import gecv.struct.ImageRectangle;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Estimates the orientation by creating a histogram of discrete angles around
 * the entire circle.  The angle with the largest sum of edge intensities is considered
 * to be the direction of the region.  Optional weighting can be provided using a {@link Kernel2D_F32}
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationHistogram <T extends ImageBase>
		implements RegionOrientation<T>
{
	// the region's radius
	protected int radius;

	// image x and y derivatives
	protected T derivX;
	protected T derivY;

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

	// optional weights
	protected Kernel2D_F32 weights;

	/**
	 * Constructor. Specify region size and weights using {@link #setRadius(int)} and
	 * {@link #setWeights(gecv.struct.convolve.Kernel2D_F32)}.
	 *
	 * @param numAngles Number of discrete angles that the orientation is estimated inside of
	 */
	public OrientationHistogram( int numAngles ) {
		this.numAngles = numAngles;
		sumDerivX = new double[ numAngles ];
		sumDerivY = new double[ numAngles ];

		angleDiv = 2.0*Math.PI/numAngles;
		angleRound = Math.PI+angleDiv/2.0;
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
	}

	public Kernel2D_F32 getWeights() {
		return weights;
	}

	@Override
	public void setImage( T derivX, T derivY) {
		InputSanityCheck.checkSameShape(derivX,derivY);

		this.derivX = derivX;
		this.derivY = derivY;
	}

	/**
	 * Specifies the weights which are centered around the targeted pixel
	 *
	 * @param weights
	 */
	public void setWeights(Kernel2D_F32 weights) {
		if( weights.getRadius() != radius ) {
			throw new IllegalArgumentException("The weight radius is not the same as the region's radius.");
		}
		this.weights = weights;
	}

	@Override
	public double compute(int c_x, int c_y) {

		// compute the visible region while taking in account
		// the image borders
		rect.x0 = c_x-radius;
		rect.y0 = c_y-radius;
		rect.x1 = c_x+radius+1;
		rect.y1 = c_y+radius+1;

		GecvMiscOps.boundRectangleInside(derivX,rect);

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
		for( int i = 0; i < numAngles; i++ ) {
			double x = sumDerivX[i];
			double y = sumDerivY[i];
			double score = x*x + y*y;
			if( score > bestScore ) {
				bestScore = score;
				bestIndex = i;
			}
		}

		return angleDiv*bestIndex-Math.PI;
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
