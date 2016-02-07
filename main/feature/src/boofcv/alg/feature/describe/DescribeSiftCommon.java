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

package boofcv.alg.feature.describe;

import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.metric.UtilAngle;

/**
 * Base class for {@link DescribePointSift SIFT} descriptors.  Provides common functionality between sparse and
 * dense computations.
 *
 * @author Peter Abeles
 */
public class DescribeSiftCommon {

	// width of a subregion, in samples
	protected int widthSubregion;
	// width of the outer grid, in sub-regions
	protected int widthGrid;

	// number of bins in the orientation histogram
	protected int numHistogramBins;
	protected double histogramBinWidth;

	// maximum value of an element in the descriptor
	protected double maxDescriptorElementValue;

	// weight applied across the entire image
	protected float gaussianWeight[];

	/**
	 * Configures the descriptor.
	 *
	 * @param widthSubregion Width of sub-region in samples.  Try 4
	 * @param widthGrid Width of grid in subregions.  Try 4.
	 * @param numHistogramBins Number of bins in histogram.  Try 8
	 * @param weightingSigmaFraction Sigma for Gaussian weighting function is set to this value * region width.  Try 0.5
	 * @param maxDescriptorElementValue Helps with non-affine changes in lighting. See paper.  Try 0.2
	 */
	public DescribeSiftCommon(int widthSubregion, int widthGrid,
							  int numHistogramBins , double weightingSigmaFraction , double maxDescriptorElementValue )
	{
		this.widthSubregion = widthSubregion;
		this.widthGrid = widthGrid;
		this.numHistogramBins = numHistogramBins;
		this.maxDescriptorElementValue = maxDescriptorElementValue;

		this.histogramBinWidth = 2.0*Math.PI/numHistogramBins;

		// number of samples wide the descriptor window is
		int descriptorWindow = widthSubregion*widthGrid;
		double weightSigma = descriptorWindow*weightingSigmaFraction;
		gaussianWeight = createGaussianWeightKernel(weightSigma,descriptorWindow/2);
	}

	/**
	 * Adjusts the descriptor.  This adds lighting invariance and reduces the affects of none-affine changes
	 * in lighting.
	 *
	 * 1) Apply L2 normalization
	 * 2) Clip using max descriptor value
	 * 3) Apply L2 normalization again
	 */
	public static void normalizeDescriptor(TupleDesc_F64 descriptor , double maxDescriptorElementValue ) {
		// normalize descriptor to unit length
		UtilFeature.normalizeL2(descriptor);

		// clip the values
		for (int i = 0; i < descriptor.size(); i++) {
			double value = descriptor.value[i];
			if( value > maxDescriptorElementValue ) {
				descriptor.value[i] = maxDescriptorElementValue;
			}
		}

		// normalize again
		UtilFeature.normalizeL2(descriptor);
	}

	/**
	 * Creates a gaussian weighting kernel with an even number of elements along its width
	 */
	protected static float[] createGaussianWeightKernel( double sigma , int radius ) {
		Kernel2D_F32 ker = FactoryKernelGaussian.gaussian2D_F32(sigma,radius,false,false);
		float maxValue = KernelMath.maxAbs(ker.data,4*radius*radius);
		KernelMath.divide(ker,maxValue);
		return ker.data;
	}

	/**
	 * Applies trilinear interpolation across the descriptor
	 */
	protected void trilinearInterpolation( float weight , float sampleX , float sampleY , double angle , TupleDesc_F64 descriptor )
	{
		for (int i = 0; i < widthGrid; i++) {
			double weightGridY = 1.0 - Math.abs(sampleY-i);
			if( weightGridY <= 0) continue;
			for (int j = 0; j < widthGrid; j++) {
				double weightGridX = 1.0 - Math.abs(sampleX-j);
				if( weightGridX <= 0 ) continue;
				for (int k = 0; k < numHistogramBins; k++) {
					double angleBin = k*histogramBinWidth;
					double weightHistogram = 1.0 - UtilAngle.dist(angle,angleBin)/histogramBinWidth;
					if( weightHistogram <= 0 ) continue;

					int descriptorIndex = (i*widthGrid + j)*numHistogramBins + k;
					descriptor.value[descriptorIndex] += weight*weightGridX*weightGridY*weightHistogram;
				}
			}
		}
	}

	/**
	 * Number of elements in the descriptor.
	 */
	public int getDescriptorLength() {
		return widthGrid*widthGrid*numHistogramBins;
	}

	/**
	 * Radius of descriptor in pixels.  Width is radius*2
	 */
	public int getCanonicalRadius() {
		return widthGrid*widthSubregion/2;
	}
}
