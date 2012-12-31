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

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleGradient;


/**
 * <p>
 * Common base class for integral image region orientation algorithms.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationIntegralBase<II extends ImageSingleBand,G extends GradientValue>
		implements OrientationIntegral<II>
{
	// integral image transform of input image
	protected II ii;

	// the scale at which the feature was detected
	protected double scale=1;

	// size of the area being considered in wavelets samples
	protected int radius;
	protected int width;

	// optional weights
	protected Kernel2D_F64 weights;

	// size of sample kernels
	protected int sampleWidth;

	// how often the image is sampled
	protected double period;

	// used to sample the image when it's on the image's border
	protected SparseScaleGradient<II,G> g;

	Class<II> integralType;
	/**
	 * Configure orientation estimation.
	 *
	 * @param radius Radius of the region being considered in terms of samples. Typically 6.
	 * @param period How often the image is sampled.  This number is scaled.  Typically 1.   
	 * @param sampleWidth How wide of a kernel should be used to sample. Try 4
	 * @param weightSigma Sigma for weighting.  zero for unweighted.
	 */
	public OrientationIntegralBase(int radius, double period, 
								   int sampleWidth , double weightSigma ,
								   Class<II> integralType) {
		this.radius = radius;
		this.period = period;
		this.sampleWidth = sampleWidth;
		this.width = radius*2+1;
		this.integralType = integralType;
		if( weightSigma != 0 )
			this.weights = FactoryKernelGaussian.gaussian(2,true, 64, weightSigma,radius);

		g = (SparseScaleGradient<II,G>)SurfDescribeOps.createGradient(false, sampleWidth, integralType);
	}
	
	@Override
	public void setScale(double scale) {
		this.scale = scale;
		g.setScale(scale);
	}

	@Override
	public void setImage(II integralImage) {
		this.ii = integralImage;
		g.setImage(ii);
	}

	@Override
	public Class<II> getImageType() {
		return integralType;
	}
}
