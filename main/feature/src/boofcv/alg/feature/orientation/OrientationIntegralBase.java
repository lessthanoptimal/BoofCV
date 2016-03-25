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

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.SurfDescribeOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleGradient;


/**
 * <p>
 * Common base class for integral image region orientation algorithms.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class OrientationIntegralBase<II extends ImageGray,G extends GradientValue>
		implements OrientationIntegral<II>
{
	// integral image transform of input image
	protected II ii;

	// the scale at which the feature was detected
	protected double scale=1;

	// size of the area being considered in wavelets samples
	protected int sampleRadius;
	protected int sampleWidth;

	// optional weights
	protected Kernel2D_F64 weights;

	// size of sample kernels
	protected int kernelWidth;

	// how often the image is sampled
	protected double period;

	protected double objectRadiusToScale;

	// used to sample the image when it's on the image's border
	protected SparseScaleGradient<II,G> g;

	Class<II> integralType;
	/**
	 * Configure orientation estimation.
	 *  @param sampleRadius The radius of samples that it will do.  Typically 6.
	 * @param period How often the image is sampled in pixels at canonical size. Internally, this value
	 *               is scaled by scaledPeriod = period*objectRadius/sampleRadius.  Typically 1.
	 * @param kernelWidth How wide of a kernel should be used to sample. Try 4
	 * @param weightSigma Sigma for weighting.  Set to zero for unweighted, negative to use sampleRadius.
	 * @param assignDefaultRadius If true it will set the object's radius to a scale of 1
	 */
	public OrientationIntegralBase(double objectRadiusToScale, int sampleRadius, double period,
								   int kernelWidth, double weightSigma,
								   boolean assignDefaultRadius, Class<II> integralType) {
		this.objectRadiusToScale = objectRadiusToScale;
		this.sampleRadius = sampleRadius;
		this.period = period;
		this.kernelWidth = kernelWidth;
		this.sampleWidth = sampleRadius *2+1;
		this.integralType = integralType;
		if( weightSigma != 0 )
			this.weights = FactoryKernelGaussian.gaussian(2,true, 64, weightSigma, sampleRadius);

		g = (SparseScaleGradient<II,G>)SurfDescribeOps.createGradient(false, integralType);
		if( assignDefaultRadius )
			setObjectRadius(1.0/objectRadiusToScale);
	}
	
	@Override
	public void setObjectRadius(double radius) {
		this.scale = radius* objectRadiusToScale;
		g.setWidth(scale * kernelWidth);
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
