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

package gecv.alg.feature.orientation;

import gecv.alg.feature.describe.SurfDescribeOps;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel2D_F64;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Estimates the orientation of a region by computing the image derivative from an integral image
 * using the Haar wavelet.  The derivative along each axis is summed up and the angle computed from
 * that.
 * </p>
 *
 * @author Peter Abeles
 */
public class OrientationAverageHaarIntegral<T extends ImageBase>
		implements OrientationIntegral<T>
{
	// integral image transform of input image
	T ii;

	// the scale at which the feature was detected
	double scale=1;

	// size of the area being considered in wavelets samples
	int radius;
	int width;

	// where the output from the Haar wavelets are stored
	double derivX[];
	double derivY[];

	// optional weights
	protected Kernel2D_F64 weights;

	/**
	 *
	 * @param radius Radius of the region being considered in terms of Wavelet samples. Typically 6.
	 * @param weighted If edge intensities are weighted using a Gaussian kernel.
	 */
	public OrientationAverageHaarIntegral(int radius , boolean weighted ) {
		this.radius = radius;
		this.width = radius*2+1;
		if( weighted )
			this.weights = FactoryKernelGaussian.gaussian(2,true, 64, -1,radius);

		derivX = new double[width*width];
		derivY = new double[width*width];
	}

	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	public void setImage(T integralImage) {
		this.ii = integralImage;
	}

	@Override
	public double compute(int c_x, int c_y ) {
		SurfDescribeOps.gradient(ii,c_x,c_y,radius,scale,derivX,derivY);

		double Dx=0,Dy=0;
		if( weights == null ) {
			for( int i = 0; i < derivX.length; i++ ) {
				Dx += derivX[i];
				Dy += derivY[i];
			}
		} else {
			for( int i = 0; i < derivX.length; i++ ) {
				double w = weights.data[i];
				Dx += w*derivX[i];
				Dy += w*derivY[i];
			}
		}

		return Math.atan2(Dy,Dx);
	}

	@Override
	public Class<T> getImageType() {
		return (Class<T>)ii.getClass();
	}
}
