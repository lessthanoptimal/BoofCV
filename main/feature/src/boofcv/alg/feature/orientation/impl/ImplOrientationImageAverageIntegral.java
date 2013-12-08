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

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.factory.transform.ii.FactorySparseIntegralFilters;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleSample_F64;


/**
 * <p>
 * Estimates the orientation of a region using a "derivative free" method.  Points are sampled using
 * an integral image.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplOrientationImageAverageIntegral<T extends ImageSingleBand,G extends GradientValue>
		extends OrientationIntegralBase<T,G>
{
	// cosine values for each pixel
	protected Kernel2D_F64 kerCosine;
	// sine values for each pixel
	protected Kernel2D_F64 kerSine;

	private SparseScaleSample_F64<T> sampler;
	
	/**
	 *
	 * @param radius Radius of the region being considered in terms of Wavelet samples. Typically 6.
	 * @param weightSigma Sigma for weighting distribution.  Zero for unweighted.
	 */
	public ImplOrientationImageAverageIntegral(int radius, double period,
											   int sampleWidth, double weightSigma,
											   Class<T> imageType) {
		super(radius,period,sampleWidth,weightSigma,imageType);

		int w = radius*2+1;
		kerCosine = new Kernel2D_F64(w);
		kerSine = new Kernel2D_F64(w);

		for( int y=-radius; y <= radius; y++ ) {
			int pixelY = y+radius;
			for( int x=-radius; x <= radius; x++ ) {
				int pixelX = x+radius;
				float r = (float)Math.sqrt(x*x+y*y);
				kerCosine.set(pixelX,pixelY,(float)x/r);
				kerSine.set(pixelX,pixelY,(float)y/r);
			}
		}
		kerCosine.set(radius,radius,0);
		kerSine.set(radius,radius,0);

		sampler = FactorySparseIntegralFilters.sample(sampleWidth/2,imageType);
	}

	@Override
	public void setImage(T integralImage) {
		super.setImage(integralImage);
		sampler.setImage(integralImage);
	}

	@Override
	public void setScale(double scale) {
		super.setScale(scale);
		sampler.setScale(scale);
	}

	@Override
	public double compute(double c_x, double c_y) {

		double period = scale*this.period;
		double tl_x = c_x - radius*period;
		double tl_y = c_y - radius*period;

		if( weights == null )
			return computeUnweighted(tl_x,tl_y,period);
		else
			return computeWeighted(tl_x, tl_y, period);
	}

	protected double computeUnweighted( double tl_x, double tl_y, 
										double samplePeriod )
	{
		// add 0.5 to c_x and c_y to have it round
		tl_x += 0.5;
		tl_y += 0.5;
		
		double Dx=0,Dy=0;
		int i = 0;
		for( int y = 0; y < width; y++ ) {
			int pixelY = (int)(tl_y + y * samplePeriod);

			for( int x = 0; x < width; x++ , i++ ) {
				int pixelX = (int)(tl_x + x * samplePeriod);

				if( sampler.isInBounds(pixelX,pixelY)) {
					try {
						double val = sampler.compute(pixelX,pixelY);
						Dx += kerCosine.data[i]*val;
						Dy += kerSine.data[i]*val;
					} catch( RuntimeException e ) {
						sampler.isInBounds(pixelX,pixelY);
						sampler.compute(pixelX,pixelY);
						throw e;
					}
				}
			}
		}
		
		return Math.atan2(Dy,Dx);
	}

	protected double computeWeighted( double tl_x, double tl_y,
									  double samplePeriod )
	{
		// add 0.5 to c_x and c_y to have it round
		tl_x += 0.5;
		tl_y += 0.5;

		double Dx=0,Dy=0;
		int i = 0;
		for( int y = 0; y < width; y++ ) {
			int pixelY = (int)(tl_y + y * samplePeriod);

			for( int x = 0; x < width; x++ , i++ ) {
				int pixelX = (int)(tl_x + x * samplePeriod);

				if( sampler.isInBounds(pixelX,pixelY)) {
					double val = sampler.compute(pixelX,pixelY);
					double w = weights.data[i];
					Dx += w*kerCosine.data[i]*val;
					Dy += w*kerSine.data[i]*val;
				}
			}
		}

		return Math.atan2(Dy,Dx);
	}
}
