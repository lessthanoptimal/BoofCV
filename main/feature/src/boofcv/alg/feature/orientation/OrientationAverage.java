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
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Computes the orientation of a region by summing up the derivative along each axis independently
 * then computing the direction fom the sum.  If weighted a Gaussian kernel centered around the targeted
 * pixel is used.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationAverage<D extends ImageGray> implements OrientationGradient<D> {
	// image gradient
	protected D derivX;
	protected D derivY;

	// local variable used to define the region being examined.
	// this makes it easy to avoid going outside the image
	protected ImageRectangle rect = new ImageRectangle();

	protected double objectToSample;
	protected int sampleRadius;
	// the radius at this scale
	protected int radiusScale;

	// if it uses weights or not
	protected boolean isWeighted;
	// optional weights
	protected Kernel2D_F32 weights;

	protected OrientationAverage(double objectRadiusToScale, boolean weighted) {
		this.objectToSample = objectRadiusToScale;
		isWeighted = weighted;
	}

	public int getSampleRadius() {
		return sampleRadius;
	}

	public void setSampleRadius(int sampleRadius) {
		this.sampleRadius = sampleRadius;
		setObjectRadius(sampleRadius);
	}

	public Kernel2D_F32 getWeights() {
		return weights;
	}

	@Override
	public void setObjectRadius(double radius) {
		radiusScale = (int)Math.ceil(radius*objectToSample);
		if( isWeighted ) {
			weights = FactoryKernelGaussian.gaussian(2,true, 32, -1,radiusScale);
		}
	}

	@Override
	public void setImage(D derivX, D derivY) {
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
