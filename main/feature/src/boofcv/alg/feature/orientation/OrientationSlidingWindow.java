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

package boofcv.alg.feature.orientation;

import boofcv.abst.feature.orientation.OrientationGradient;
import boofcv.alg.InputSanityCheck;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Estimates the orientation by sliding window across all angles.  All pixels which are pointing
 * at an angle inside of this window have their gradient summed.  The window with the largest normal
 * is selected as the best window.  The angle is then computed from the best window using atan2()
 * and the summed gradient.
 * </p>
 *
 * <p>
 * NOTE: There are probably additional performance enhancements that could be done.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationSlidingWindow<D extends ImageGray>
		implements OrientationGradient<D>
{
	// The actual radius being sampled in pixels
	protected int pixelRadius;

	// used to adjust the size of the sample region
	protected double objectRadiusToScale;

	// image x and y derivatives
	protected D derivX;
	protected D derivY;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	protected ImageRectangle rect = new ImageRectangle();

	// number of different angles it will consider
	protected int numAngles;
	// the size of the window it will consider
	protected double windowSize;
	// the angle each pixel is pointing
	protected double angles[];

	// if it uses weights or not
	protected boolean isWeighted;
	// optional weights
	protected Kernel2D_F32 weights;

	/**
	 * Configures orientation estimating algorithm.
	 *
	 * @param objectRadiusToScale Convert object radius into scale factor
	 * @param numAngles Number of discrete points in which the sliding window will be centered around.
	 * @param windowSize Number of radians in the window being considered.
	 * @param isWeighted Should points be weighted using a Gaussian kernel.
	 */
	public OrientationSlidingWindow( double objectRadiusToScale, int numAngles , double windowSize , boolean isWeighted ) {
		this.objectRadiusToScale = objectRadiusToScale;
		this.numAngles = numAngles;
		this.windowSize = windowSize;
		this.isWeighted = isWeighted;
	}

	public Kernel2D_F32 getWeights() {
		return weights;
	}

	@Override
	public void setObjectRadius(double objRadius) {
		pixelRadius = (int)Math.ceil(objRadius*objectRadiusToScale);
		if( isWeighted ) {
			weights = FactoryKernelGaussian.gaussian(2,true, 32, -1, pixelRadius);
		}
		int w = pixelRadius*2+1;
		angles = new double[ w*w ];
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
		rect.x0 = c_x- pixelRadius;
		rect.y0 = c_y- pixelRadius;
		rect.x1 = c_x+ pixelRadius +1;
		rect.y1 = c_y+ pixelRadius +1;

		BoofMiscOps.boundRectangleInside(derivX,rect);

		if( isWeighted )
			return computeWeightedOrientation(c_x,c_y);
		else
			return computeOrientation();
	}

	/**
	 * Compute the angle without using the optional weights
	 */
	protected abstract double computeOrientation();

	/**
	 * Compute the angle using the weighting kernel.
	 */
	protected abstract double computeWeightedOrientation(int c_x , int c_y );
}
