/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import lombok.Getter;

/**
 * <p>
 * Generates the pyramidal scale space as described in the SIFT [1] paper.
 * </p>
 *
 * <p>
 * [1] Lowe, D. "Distinctive image features from scale-invariant keypoints". International Journal of
 * Computer Vision, 60, 2 (2004), pp.91--110.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SiftScaleSpace {

	// Storages images for all the octaves
	// Each octave is the double of the amount of blur (sigma)
	public final Octave[] octaves;

	// Blur factor for first image in the scale pyramid
	// The sigma for the first image in each octave is sigma0*(2**octave)
	double sigma0;
	// Indexes of octaves. It will create a sequence of images between these two numbers, inclusive.
	// The size of each one is 2*||octave|| for negative numbers and 1/(2*octave) for positive numbers.
	int firstOctave, lastOctave;
	// Number of scales in each octave. The amount of blur applied to each image
	// in the octave is designed so that image[numScales] will have 2x the blur of image[0].
	// Total octave images = scales + 3
	// Total DoG images = scales + 2
	@Getter int numScales;

	// scale factor difference in sigma between levels
	// sigma[i+1] = levelK*sigma[i]
	// This is 'k' in the paper
	double levelK;

	// precomputed kernels
	Kernel1D_F32 kernelSigma0;
	Kernel1D_F32[] kernelSigmaToK;

	// the input image
	GrayF32 input;

	// temporary storage used when applying gaussian blur
	GrayF32 tempBlur;

	// internal work space used when computing octaves
	GrayF32 tempImage;

	// interpolation used when scaling an image up
	InterpolatePixelS<GrayF32> interp =
			FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);

	/**
	 * Configures the scale-space
	 *
	 * @param firstOctave Initial octave. Negative numbers means it will scale up. Recommend 0 or -1.
	 * @param lastOctave Last octave, inclusive. Recommend ????
	 * @param numScales Number of scales in each octave. Recommend 3.
	 * @param sigma0 Amount of blur at the first level in the image pyramid. Recommend 1.6
	 */
	public SiftScaleSpace( int firstOctave, int lastOctave,
						   int numScales,
						   double sigma0 ) {
		BoofMiscOps.checkTrue(firstOctave >= -1);

		if (lastOctave <= firstOctave)
			throw new IllegalArgumentException("Last octave must be more than the first octave");
		if (numScales < 1)
			throw new IllegalArgumentException("Number of scales must be >= 1");

		this.firstOctave = firstOctave;
		this.lastOctave = lastOctave;
		this.numScales = numScales;
		this.sigma0 = sigma0;

		octaves = new Octave[getTotalOctaves()];
		for (int i = 0; i < octaves.length; i++) {
			octaves[i] = new Octave(numScales + 3);
		}

		tempImage = new GrayF32(1, 1);
		tempBlur = new GrayF32(1, 1);

		// each scale is K has a sigma that is K times larger than the previous
		levelK = Math.pow(2, 1.0/numScales);

		// create all the convolution kernels
		Class kernelType = FactoryKernel.getKernelType(GrayF32.class, 1);
		kernelSigma0 = (Kernel1D_F32)FactoryKernelGaussian.gaussian(kernelType, sigma0, -1);

		kernelSigmaToK = new Kernel1D_F32[numScales + 2];
		for (int i = 1; i < numScales + 3; i++) {
			double before = computeSigmaScale(0, i - 1);

			// compute the sigma that when applied to the previous scale will produce k*scale
			// k*sigma_{i-1} = conv( sigma_(i-1) , sigma)
			double sigma = before*Math.sqrt(levelK - 1.0);
			kernelSigmaToK[i - 1] = (Kernel1D_F32)FactoryKernelGaussian.gaussian(kernelType, sigma, -1);
		}
	}

	/**
	 * Returns the blur at the given octave and scale
	 */
	public double computeSigmaScale( int octave, int scale ) {
		return sigma0*Math.pow(2, octave + scale/(double)numScales);
	}

	/**
	 * Checks to see if the octave is too small to process and no more layers should be processed
	 */
	public boolean isOctaveTooSmall( int octaveIdx ) {
		Octave o = octaves[octaveIdx];
		return o.scales[0].width < 10 || o.scales[0].height < 10;
	}

	/**
	 * Computes the entire SIFT scale space given the input image
	 *
	 * @param input Input image. No prior blur should be applied to this image. Not modified.
	 */
	public void process( GrayF32 input ) {
		this.input = input;

		// NOTE: In the 2004 paper the down sample is a factor of 2. In earlier works it was 1.5 pixels

		if (firstOctave == -1) {
			// The first octave is at a "higher" resolution than the input image
			PyramidOps.scaleImageUp(input, tempImage, -2*firstOctave, interp);
			applyGaussian(tempImage, octaves[0].scales[0], kernelSigma0);
		} else {
			applyGaussian(input, octaves[0].scales[0], kernelSigma0);

			// if the first octave is at a lower resolution then down sample it
			for (int i = 1; i <= firstOctave; i++) {
				// double the blur, then down sample
				applyGaussian(octaves[0].scales[0], tempImage, kernelSigma0);
				PyramidOps.scaleDown2(tempImage, octaves[0].scales[0]);
			}
		}

		// Compute all the other images
		for (int octaveIdx = 0; octaveIdx < octaves.length; octaveIdx++) {
			Octave o = octaves[octaveIdx];
			o.reshapeToFirst();

			// Compute the blur and DoG images for all scales in the octave
			for (int i = 1; i < o.scales.length; i++) {
				applyGaussian(o.scales[i - 1], o.scales[i], kernelSigmaToK[i - 1]);
			}

			for (int i = 1; i < o.scales.length; i++) {
				PixelMath.subtract(o.scales[i], o.scales[i - 1], o.differenceOfGaussian[i - 1]);
			}

			// Create the first image in the octave using the scale which has 2x the blur of the initial image
			if (octaveIdx + 1 < octaves.length)
				PyramidOps.scaleDown2(octaves[octaveIdx].scales[numScales], octaves[octaveIdx + 1].scales[0]);
		}
	}

	/**
	 * Set of images (scales) in a single octave
	 */
	public static class Octave {
		// all the scale images across an octave
		public final GrayF32[] scales;
		// images which are the difference between the scales
		public final GrayF32[] differenceOfGaussian;

		public Octave( int numScales ) {
			scales = new GrayF32[numScales];
			differenceOfGaussian = new GrayF32[numScales - 1];

			for (int i = 0; i < scales.length; i++) {
				scales[i] = new GrayF32(1, 1);
			}
			for (int i = 0; i < differenceOfGaussian.length; i++) {
				differenceOfGaussian[i] = new GrayF32(1, 1);
			}
		}

		/**
		 * Reshapes all the images to match the first (index=0) scale, which has previously been reshaped
		 * to the appropriate size.
		 */
		public void reshapeToFirst() {
			int width = scales[0].width;
			int height = scales[0].height;

			for (int i = 1; i < scales.length; i++) {
				scales[i].reshape(width, height);
			}
			for (int i = 0; i < differenceOfGaussian.length; i++) {
				differenceOfGaussian[i].reshape(width, height);
			}
		}
	}

	/**
	 * Applies the separable kernel to the input image and stores the results in the output image.
	 */
	void applyGaussian( GrayF32 input, GrayF32 output, Kernel1D kernel ) {
		output.reshape(input.width, input.height);
		tempBlur.reshape(input.width, input.height);
		GConvolveImageOps.horizontalNormalized(kernel, input, tempBlur);
		GConvolveImageOps.verticalNormalized(kernel, tempBlur, output);
	}

	public int getNumScaleImages() {
		return numScales + 3;
	}

	public int getTotalOctaves() {
		return lastOctave - firstOctave + 1;
	}

	/**
	 * Returns the size of a pixel in the current octave relative to the size of a pixel
	 * in the input image
	 *
	 * @return pixel size to input image
	 */
	public double pixelScaleCurrentToInput( int octave ) {
		return Math.pow(2.0, octave);
	}

	public int getOriginalWidth() {
		return input.width;
	}

	public int getOriginalHeight() {
		return input.height;
	}
}
