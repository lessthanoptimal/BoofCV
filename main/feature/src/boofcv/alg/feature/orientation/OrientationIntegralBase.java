/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.orientation;

import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.ImageBase;


/**
 * <p>
 * Common base class for integral image region orientation algorithms.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationIntegralBase<T extends ImageBase>
		implements OrientationIntegral<T>
{
	// integral image transform of input image
	protected T ii;

	// the scale at which the feature was detected
	protected double scale=1;

	// size of the area being considered in wavelets samples
	protected int radius;
	protected int width;

	// optional weights
	protected Kernel2D_F64 weights;

	/**
	 *
	 * @param radius Radius of the region being considered in terms of samples. Typically 6.
	 * @param weighted If edge intensities are weighted using a Gaussian kernel.
	 */
	public OrientationIntegralBase(int radius, boolean weighted) {
		this.radius = radius;
		this.width = radius*2+1;
		if( weighted )
			this.weights = FactoryKernelGaussian.gaussian(2,true, 64, -1,radius);
	}

	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	public void setImage(T integralImage) {
		this.ii = integralImage;
	}
}
