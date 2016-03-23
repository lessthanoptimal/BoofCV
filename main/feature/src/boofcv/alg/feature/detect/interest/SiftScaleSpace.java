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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;

/**
 *<p>
 * Generates the pyramidal scale space as described in the SIFT [1] paper.  This is, for the most part,
 * is intended to be a faithful reproduction of the original work.
 * </p>
 *
 * Known Deviations From Original SIFT:
 * <ul>
 * <li>No prior blur is applied to input image.  Nor should you apply any.</li>
 * </ul>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints".  International Journal of
 * Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
public class SiftScaleSpace {
	// all the scale images across an octave
	GrayF32 octaveImages[];
	// images which are the difference between the scales
	GrayF32 differenceOfGaussian[];

	// Blur factor for first image in the scale pyramid
	// The sigma for the first image in each octave is sigma0*(2**octave)
	double sigma0;
	// Indexes of octaves.  It will create a sequence of images between these two numbers, inclusive.
	// The size of each one is 2*||octave|| for negative numbers and 1/(2*octave) for positive numbers.
	int firstOctave,lastOctave;
	// Number of scales in each octave.
	// Total octave images = scales + 3
	// Total DoG images = scales + 2
	int numScales;

	// scale factor difference in sigma between levels
	// sigma[i+1] = levelK*sigma[i]
	// This is 'k' in the paper
	double levelK;

	// precomputed kernels
	Kernel1D_F32 kernelSigma0;
	Kernel1D_F32 kernelSigmaToK[];

	// the input image
	GrayF32 input;

	// the current octave being examined
	int currentOctave;

	// temporary storage used when applying gaussian blur
	GrayF32 tempBlur;

	// internal work space used when computing octaves
	GrayF32 tempImage0;
	GrayF32 tempImage1;

	// interpolation used when scaling an image up
	InterpolatePixelS<GrayF32> interp =
			FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);

	/**
	 * Configures the scale-space
	 *
	 * @param firstOctave Initial octave.  Negative numbers means it will scale up.  Recommend 0 or -1.
	 * @param lastOctave  Last octave, inclusive.  Recommend ????
	 * @param numScales Number of scales in each octave.  Recommend 3.
	 * @param sigma0 Amount of blur at the first level in the image pyramid.  Recommend 1.6
	 */
	public SiftScaleSpace(int firstOctave , int lastOctave ,
						  int numScales ,
						  double sigma0  )
	{
		if( lastOctave <= firstOctave )
			throw new IllegalArgumentException("Last octave must be more than the first octave");
		if( numScales < 1 )
			throw new IllegalArgumentException("Number of scales must be >= 1");

		this.firstOctave = firstOctave;
		this.lastOctave = lastOctave;
		this.numScales = numScales;
		this.sigma0 = sigma0;

		octaveImages = new GrayF32[numScales + 3];
		differenceOfGaussian = new GrayF32[numScales + 2];
		for (int i = 1; i < octaveImages.length; i++) {
			octaveImages[i] = new GrayF32(1,1);
			differenceOfGaussian[i-1] = new GrayF32(1,1);
		}
		tempImage0 = new GrayF32(1,1);
		tempImage1 = new GrayF32(1,1);
		tempBlur = new GrayF32(1,1);

		// each scale is K has a sigma that is K times larger than the previous
		levelK = Math.pow(2,1.0/numScales);

		// create all the convolution kernels
		Class kernelType = FactoryKernel.getKernelType(GrayF32.class, 1);
		kernelSigma0 = (Kernel1D_F32) FactoryKernelGaussian.gaussian(kernelType, sigma0, -1);

		kernelSigmaToK = new Kernel1D_F32[numScales+2];
		for (int i = 1; i < numScales + 3; i++) {
			double before = computeSigmaScale(0, i - 1);

			// compute the sigma that when applied to the previous scale will produce k*scale
			// k*sigma_{i-1} = conv( sigma_(i-1) , sigma)
			double sigma = before*Math.sqrt(levelK-1.0);
			kernelSigmaToK[i-1] = (Kernel1D_F32)FactoryKernelGaussian.gaussian(kernelType, sigma, -1);
		}

//		for (int octave = firstOctave; octave <= lastOctave; octave++) {
//			for (int level = 0; level < numScales+3; level++) {
//				double sigma = computeSigmaScale(octave,level);
//				double adjustedSigma = sigma/Math.pow(2,octave);
//				System.out.printf("%2d %2d =  %7.3f   %7.3f\n",octave,level,sigma,adjustedSigma);
//			}
//		}
	}

	/**
	 * Computes the effective amount of blur at the given scale in the current octave.
	 */
	public double computeSigmaScale( int scale ) {
		return computeSigmaScale(currentOctave,scale);
	}

	/**
	 * Returns the blur at the given octave and scale
	 */
	public double computeSigmaScale(int octave, int scale) {
		return sigma0*Math.pow(2,octave+scale/(double)numScales);
	}

	/**
	 *
	 * @param input Input image.  No prior blur should be applied to this image. Not modified.
	 */
	public void initialize( GrayF32 input ) {
		this.input = input;
		currentOctave = firstOctave;

		if( firstOctave < 0 ) {
			PyramidOps.scaleImageUp(input,tempImage1,-2*firstOctave,interp);
			tempImage0.reshape(tempImage1.width, tempImage1.height);
			applyGaussian(tempImage1, tempImage0, kernelSigma0);
		} else {
			tempImage0.reshape(input.width, input.height);
			applyGaussian(input, tempImage0, kernelSigma0);
			
			for (int i = 0; i < firstOctave; i++) {
				tempImage1.reshape(tempImage0.width, tempImage0.height);
				// first image in the next octave will have 2x the blur as the first image in the prior octave
				applyGaussian(tempImage0, tempImage1, kernelSigma0);
				// next octave has half the spacial resolution
				PyramidOps.scaleDown2(tempImage1, tempImage0);
			}
		}

		computeOctaveScales();
	}


	/**
	 * Computes the next octave.  If the last octave has already been computed false is returned.
	 * @return true if an octave was computed or false if the last one was already reached
	 */
	public boolean computeNextOctave() {
		currentOctave += 1;
		if( currentOctave > lastOctave ) {
			return false;
		}

		if( octaveImages[numScales].width <= 5 || octaveImages[numScales].height <= 5)
			return false;

		// the 2nd image from the top of the stack has 2x the sigma as the first
		PyramidOps.scaleDown2(octaveImages[numScales], tempImage0);
		computeOctaveScales();
		return true;
	}

	/**
	 * Computes all the scale images in an octave.  This includes DoG images.
	 */
	private void computeOctaveScales() {
		octaveImages[0] = tempImage0;
		for (int i = 1; i < numScales+3; i++) {
			octaveImages[i].reshape(tempImage0.width, tempImage0.height);
			applyGaussian(octaveImages[i - 1], octaveImages[i], kernelSigmaToK[i-1]);
		}

		for (int i = 1; i < numScales+3; i++) {
			differenceOfGaussian[i-1].reshape(tempImage0.width, tempImage0.height);
			PixelMath.subtract(octaveImages[i],octaveImages[i - 1],differenceOfGaussian[i-1]);
		}
	}

	public GrayF32 getImageScale(int scaleIndex ) {
		return octaveImages[scaleIndex];
	}

	public GrayF32 getDifferenceOfGaussian(int dogIndex ) {
		return differenceOfGaussian[dogIndex];
	}

	/**
	 * Applies the separable kernel to the input image and stores the results in the output image.
	 */
	void applyGaussian(GrayF32 input, GrayF32 output, Kernel1D kernel) {
		tempBlur.reshape(input.width, input.height);
		GConvolveImageOps.horizontalNormalized(kernel, input, tempBlur);
		GConvolveImageOps.verticalNormalized(kernel, tempBlur,output);
	}

	public int getNumScales() {
		return numScales;
	}

	public int getNumScaleImages() {
		return numScales+3;
	}

	public int getCurrentOctave() {
		return currentOctave;
	}

	public int getTotalOctaves() {
		return lastOctave-firstOctave+1;
	}

	/**
	 * Returns the size of a pixel in the current octave relative to the size of a pixel
	 * in the input image
	 * @return pixel size to input image
	 */
	public double pixelScaleCurrentToInput() {
		return Math.pow(2.0,currentOctave);
	}
}
