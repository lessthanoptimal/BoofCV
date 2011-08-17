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

package gecv.alg.describe;

import gecv.misc.GecvMiscOps;
import gecv.struct.ImageRectangle;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageBase;


/**
 * <p>Computes the orientation of a region by summing up the derivative along each axis independently
 * then computing the direction fom the sum.</p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationAverage<T extends ImageBase> implements RegionOrientation<T> {
	// image gradient
	protected T derivX;
	protected T derivY;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	protected ImageRectangle rect = new ImageRectangle();

	protected int radius;

	// optional weights
	protected Kernel2D_F32 weights;

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public Kernel2D_F32 getWeights() {
		return weights;
	}

	public void setWeights(Kernel2D_F32 weights) {
		this.weights = weights;
	}

	@Override
	public void setImage(T derivX, T derivY) {
		this.derivX = derivX;
		this.derivY = derivY;
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

		if( weights == null )
			return computeUnweightedScore();
		else
			return computeWeightedScore(c_x,c_y);

	}

	/**
	 * Compute the score without using the optional weights
	 */
	protected abstract double computeUnweightedScore();

	/**
	 * Compute the score using the weighting kernel.
	 */
	protected abstract double computeWeightedScore(int c_x , int c_y );

}
