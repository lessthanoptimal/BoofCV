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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.ImageFloat32;

/**
 * Constructs the scale-space in which SIFT detects features.  An octave contains a set of scales.
 * Each octave is half the width/height of the previous octave.  Scales are computed inside an octave
 * by applying Gaussian blur.   See SIFT paper for the details.
 *
 * <p>
 * OCTAVE SCALES: The scales sampled are different from the SIFT paper.  In each octave the scales are sampled
 * across a linear function. s(i) = sigma*i.  In the paper scales are sampled using an exponential function
 * s(i) = sigma*s(i-1)
 * </p>
 *
 * <p>
 * When computing the next octave in the sequence it is seeded with the image from the second scale in the previous
 * octave.  The first octave is seeded with the input image or the input image scaled.
 * </p>
 *
 * @author Peter Abeles
 */
public class SiftImageScaleSpace {

	// number of octaves computed.  Each consecutive octave is composed of numScales images which have
	// half the resolution of the previous.
	protected int numOctaves;
	// number of scales per octave
	protected int numScales;
	// largest octave when image size is taken in account.  no octave if image has a width less than 5
	protected int actualOctaves;

	// Difference of Gaussian (DOG) features
	protected ImageFloat32 dog[];
	// Images across scale-space in this octave
	protected ImageFloat32 scale[];
	// Image Derivatives
	protected ImageFloat32 derivX[];
	protected ImageFloat32 derivY[];
	// Amount of blur which is applied
	protected float sigma;

	// ratio of pixels in each octave to the original image
	// x = x'*pixelScale, where x is original coordinate, and x' is current image.
	protected double pixelScale[];

	// the sigma for each layer in the pyramid.  Saved as an array for speed reasons
	protected double layerSigma[];

	// should the input image be doubled
	private boolean doubleInputImage;

	// The blur sigma applied to the first scale BEFORE any additional blur has been applied
	// Note that the octave's are recursively computed, so this is the blur magnitude from before
	private double priorSigmaFirstScale[];

	// Computes the image derivative
	private ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.three_F32();

	// storage for applying blur
	protected ImageFloat32 storage;

	/**
	 * Configures the scale-space.
	 *
	 * @param blurSigma Amount of blur applied to each scale inside an octaves.  Try 1.6
	 * @param numScales Number of scales per octaves.  Try 5.  Must be >= 3
	 * @param numOctaves Number of octaves to detect.  Try 4
	 * @param doubleInputImage Should the input image be doubled? Try false.
	 */
	public SiftImageScaleSpace(float blurSigma, int numScales, int numOctaves, boolean doubleInputImage)
	{
		if( numScales < 3 )
			throw new IllegalArgumentException("A minimum of 3 scales are required");
		if( numOctaves < 1 )
			throw new IllegalArgumentException("At least one octave is required");

		this.numOctaves = numOctaves;
		this.numScales = numScales;
		this.sigma = blurSigma;
		this.doubleInputImage = doubleInputImage;

		pixelScale = new double[ numOctaves ];
		priorSigmaFirstScale = new double[ numOctaves ];

		double firstScale = doubleInputImage ? 0.5 : 1;
		pixelScale[0] = firstScale;
		priorSigmaFirstScale[0] = 0;
		for( int o = 1; o < numOctaves; o++ ) {
			pixelScale[o] = pixelScale[o-1]*2;
			priorSigmaFirstScale[o] = computeScaleSigma(o-1,1);
		}

		int totalImages = numScales*numOctaves;

		scale = new ImageFloat32[totalImages];
		derivX = new ImageFloat32[totalImages];
		derivY = new ImageFloat32[totalImages];
		dog = new ImageFloat32[totalImages-numOctaves];
		for( int i = 0; i < scale.length; i++ ) {
			scale[i] = new ImageFloat32(1,1);
			derivX[i] = new ImageFloat32(1,1);
			derivY[i] = new ImageFloat32(1,1);
		}
		for( int i = 0; i < dog.length; i++ ) {
			dog[i] = new ImageFloat32(1,1);
		}
		storage = new ImageFloat32(1,1);

		layerSigma = new double[totalImages];
		for( int o = 0; o < numOctaves; o++ ) {
			for( int s = 0; s < numScales; s++ ) {
				int index = o*numScales + s;
				layerSigma[index] = computeScaleSigma(o,s);
			}
		}
	}

	/**
	 * Processes the first image and constructs the scale-space pyramid for the first level.
	 *
	 * @param input Input image
	 */
	public void constructPyramid(ImageFloat32 input) {
		// compute the first octave
		if( doubleInputImage ) {
			reshapeToInput(input.width * 2, input.height * 2);
			upSample(input,scale[1]);

			blurImage(scale[1],scale[0],sigma);
		} else {
			reshapeToInput(input.width, input.height);
			blurImage(input, scale[0], sigma);
		}
		constructRestOfOctave(0);

		// compute rest of the octaves
		actualOctaves = numOctaves;
		for( int o = 1; o < numOctaves; o++ ) {
			// use the second scale in the previous octave to seed this one
			int indexSeed = (o-1)*numScales+1;
			int indexStart = o*numScales;

			// stop computing octaves if the image is too small
			if( Math.max(scale[indexStart].width,scale[indexStart].height) < 5 ) {
				actualOctaves = o;
				break;
			}

			downSample(scale[indexSeed],scale[indexStart+1]);
			blurImage(scale[indexStart+1],scale[indexStart],sigma);

			constructRestOfOctave(o);
		}
	}

	/**
	 * Computes the image derivative for each layer in the pyramid.
	 */
	public void computeDerivatives() {
		int maxScales = actualOctaves*numScales;
		for( int i = 0; i < maxScales; i++ ) {
			ImageFloat32 input = scale[i];
			ImageFloat32 dx = derivX[i];
			ImageFloat32 dy = derivY[i];

			dx.reshape(input.width,input.height);
			dy.reshape(input.width,input.height);

			gradient.process(input,dx,dy);
		}
	}

	/**
	 * Returns the amount of blur which has been applied to the image in total at the specified scale
	 * in the current octave
	 */
	public double computeScaleSigma( int octave , int scale ) {

		// amount of blur applied prior to the first scale in this octave
		double a = priorSigmaFirstScale[octave];

		// amount of blur applied to this scale relative to a at the specified level
		double b = pixelScale[octave]*sigma*(scale+1);

		// Compute the net effect of convolving b on top of the a
		return Math.sqrt(a*a + b*b);
	}

	/**
	 * Applies the specified amount of blur to the input image and stores the results in
	 * the output image
	 */
	private void blurImage( ImageFloat32 input , ImageFloat32 output , double sigma ) {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigma, -1);

		storage.reshape(input.width,input.height);
		ConvolveNormalized.horizontal(kernel, input, storage);
		ConvolveNormalized.vertical(kernel,storage,output);
	}

	/**
	 * Compute difference of Gaussian feature intensity across scale space
	 */
	public void computeFeatureIntensity() {
		int indexDog = 0;
		for( int o = 0; o < actualOctaves; o++ ) {
			for( int i = 1; i < numScales; i++ , indexDog++ ) {
				int indexScale = o*numScales + i;

				PixelMath.subtract(scale[indexScale],scale[indexScale-1],dog[indexDog]);

				// NOTE: In SIFT paper it states you don't need to do this adjustment.  However, since the difference
				// between scales is not a constant factor in this implementation you do need to do it.

				// compute adjustment to make it better approximate of the Laplacian of Gaussian detector
				double k = (i+1)/(double)i;
				double adjustment = k-1;
				PixelMath.divide(dog[indexDog], (float) adjustment, dog[indexDog]);
			}
		}
	}

	/**
	 * Using the first scale as seed, construct the rest of the image pyramid in one octave.
	 * The amount of blur for each scale is a multiple of sigma.  To improve runtime performance
	 * the previous scale is convolved and the amount of blur is adjusted accordingly.
	 */
	private void constructRestOfOctave( int octave ) {

		int indexScales = octave*numScales+1;
		for( int i = 1; i < numScales; i++ , indexScales++ ) {
			// sigmaA is the amount of blur already applied
			double sigmaA = sigma*i;
			// sigmaB is the desired amount of blur at this scale
			double sigmaB = sigma*(i+1);

			// compute the amount of blur which needs to be applied to get sigmaB
			double amount = Math.sqrt(sigmaB*sigmaB - sigmaA*sigmaA);

			// apply the blur
			blurImage(scale[indexScales-1],scale[indexScales],amount);
		}
	}

	/**
	 * Down samples an image by copying every other pixel, starting with pixel 1.
	 */
	protected static void downSample( ImageFloat32 from , ImageFloat32 to ) {

		for( int y = 0; y < to.height; y++ ) {
			for( int x = 0; x < to.width; x++ ) {
				to.unsafe_set(x,y,from.unsafe_get(x*2+1,y*2+1));
			}
		}
	}

	/**
	 * Up-samples the input image.  Doubling its size.
	 */
	protected static void upSample( ImageFloat32 from , ImageFloat32 to ) {

		for( int y = 0; y < from.height; y++ ) {
			int yy = y*2;
			int xx = 0;
			for( int x = 0; x < from.width; x++ ) {
				float v = from.unsafe_get(x,y);

				to.unsafe_set(xx, yy, v);
				to.unsafe_set(xx,yy+1,v);
				xx++;
				to.unsafe_set(xx,yy,v);
				to.unsafe_set(xx,yy+1,v);
				xx++;
			}
		}
	}

	/**
	 * Reshapes all images to their appropriate sizes according to the input image
	 */
	private void reshapeToInput(int width, int height) {
		int indexScales = 0;
		int indexDog = 0;
		for( int o = 0; o < numOctaves; o++ ) {
			for( int n = 0; n < numScales; n++ , indexScales++ ) {
				scale[indexScales].reshape(width,height);
			}
			for( int n = 0; n < numScales-1; n++ , indexDog++ ) {
				dog[indexDog].reshape(width,height);
			}

			width /= 2;
			height /= 2;
		}
	}

	public int getNumOctaves() {
		return numOctaves;
	}

	public int getNumScales() {
		return numScales;
	}

	public ImageFloat32 getPyramidLayer(int index) {
		return scale[index];
	}

	public ImageFloat32 getDerivativeX(int index) {
		return derivX[index];
	}

	public ImageFloat32 getDerivativeY(int index) {
		return derivY[index];
	}

	/**
	 * Given the scale, return the index of the layer in the scale-space that is the closest
	 * match.
	 */
	public int scaleToImageIndex(double sigma) {
		// figure out which octave it is in
		int index = -1;
		double bestScore = Double.MAX_VALUE;

		// find the closest match
		for( int i = 0; i < layerSigma.length; i++ ) {
			double error = Math.abs(sigma - layerSigma[i]);

			if( error < bestScore ) {
				bestScore = error;
				index = i;
			}
		}

		return index;
	}

	/**
	 * Image pixel scale factor for the specific image in the pyramid.
	 */
	public double imageIndexToPixelScale(int imageIndex) {
		return pixelScale[ imageIndex/numScales ];
	}
}
