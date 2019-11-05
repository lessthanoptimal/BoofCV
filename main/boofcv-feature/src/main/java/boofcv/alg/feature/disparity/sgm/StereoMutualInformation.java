/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.ejml.UtilEjml;

/**
 * <p></p>Computes the Mutual Information error metric from a rectified stereo pair. Mutual information
 * between two images is defined as: MI(I1,I2) = H<sub>I1</sub> + H<sub>I2</sub> + H<sub>I1,I2</sub>.
 * Where H is an entropy function, e.g. H<sub>I</sub> = -sum_i P<sub>I</sub>(i)log(P<sub>I</sub>(i)),
 * where P<sub>I</sub>(i) is the probability of a pixel in image 'I' having that intensity. See [1]
 * for details.</p>
 *
 * <p>This implementation has been designed to handle images with pixel intensity greater than 8-bit.
 * That was done by allowing the number of bins in a histogram to be specified.
 * High dynamic range images are problematic because the joint entropy instead of being a 256x256 image might now be a
 * 4096x4096 image. The PDF for each image might also appear to be too flat.</p>
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
public class StereoMutualInformation {

	// Kernel that's used to apply smoothing to 1D and 2D
	Kernel1D_F32 smoothKernel;
	GrayF32 smoothWork = new GrayF32(1,1); // work space for smoothing

	// To avoid log(0) this is added to all probabilities
	float eps = UtilEjml.F_EPS;

	// Maximum intensity value a pixel of the input image can have
	int maxPixelValue;
	// Storage for pixel intensity histogram after scaling
	int[] histogramIntensity;
	// The maximum intensity value after scaling is applied. Always histogram.length-1
	int maxHistogramValue;

	GrayS32 histJoint = new GrayS32(1,1);

	// Storage for entropy. Initially the probabilities will be stored here but that is overwritten
	GrayF32 entropyJoint = new GrayF32(1,1);
	GrayF32 entropyLeft = new GrayF32(1,1);
	GrayF32 entropyRight = new GrayF32(1,1);

	public StereoMutualInformation() {
		// this is a reasonable default for 8-bit images
		configureHistogram(255,255);

		configureSmoothing(3);
	}

	/**
	 * Configures the histogram and how the input is scaled. For an 8-bit input image just pass in
	 * 0xFF for both values.
	 *
	 * @param maxPixelValue The maximum value a pixel in the input image can have
	 * @param maxHistogramValue The maximum value that the pixel can have after being scaled
	 */
	public void configureHistogram(int maxPixelValue , int maxHistogramValue ) {
		if( maxHistogramValue > maxPixelValue )
			throw new IllegalArgumentException("Maximum histogram value can't be more than max pixel value");

		this.maxPixelValue = maxPixelValue;
		this.maxHistogramValue = maxHistogramValue;
		int intensityBins = maxHistogramValue+1;
		histogramIntensity = new int[intensityBins];

		histJoint.reshape(intensityBins,intensityBins);
		entropyJoint.reshape(intensityBins,intensityBins);
		entropyLeft.reshape(intensityBins,1);
		entropyRight.reshape(intensityBins,1);
	}

	public void configureSmoothing( int radius ) {
		smoothKernel = FactoryKernelGaussian.gaussian(1,true,32,-1,radius);
	}

	/**
	 * Process the images and compute the entropy terms which will be in turn used to compute mutual information
	 * @param left Left rectified image
	 * @param right Right rectified image
	 * @param disparity Disparity from left to right
	 * @param invalid Value of disparity pixels which are invalid
	 */
	public void process(GrayU8 left , GrayU8 right , GrayU8 disparity , int invalid ) {
		// Check input to make sure it's valid
		InputSanityCheck.checkSameShape(left,right);
		if( left.isSubimage() || right.isSubimage() || disparity.isSubimage() )
			throw new IllegalArgumentException("Can't process sub images. Is this a major issue? Could be fixed");
		disparity.reshape(left);

		// Compute entropy tables
		computeJointHistogram(left, right, disparity, invalid);
		computeProbabilities();
		computeEntropy();
	}

	/**
	 * Computes the mutual information score given pixel values from left and right images
	 * @param leftValue Value in left image. I(x,y)
	 * @param rightValue Value of pixel in right image I(x-d,y)
	 * @return the mutual information score
	 */
	public float computeMI( int leftValue , int rightValue ) {
		// Scale the input value to be in the same range as the histogram
		int leftScale = scalePixelValue(leftValue);
		int rightScale = scalePixelValue(rightValue);

		// Equation 8b and 9a
		return -(entropyLeft.data[leftScale] + entropyRight.data[rightScale] - entropyJoint.unsafe_get(leftScale,rightScale));
	}

	/**
	 * Computes the joint histogram of pixel intensities (2D histogram) while skipping over pixels with
	 * no correspondences
	 */
	void computeJointHistogram(GrayU8 left, GrayU8 right, GrayU8 disparity, int invalid) {
		// zero the histogram
		ImageMiscOps.fill(histJoint,0);

		int histLength = histogramIntensity.length;

		// Compute the joint histogram
		for (int row = 0; row < left.height; row++) {
			int idx = row*left.stride;
			for (int col = 0; col < left.width; col++, idx++ ) {
				int d = disparity.data[idx]&0xFF;
				// Don't consider pixels without correspondences
				if( d == invalid )
					continue;

				int leftValue = left.data[idx]&0xFF;     // I(x,y)
				int rightValue = right.data[idx-d]&0xFF; // I(x-d,y)

				// scale the pixel intensity for the histogram
				leftValue = scalePixelValue(leftValue);
				rightValue = scalePixelValue(rightValue);

				// increment the histogram
				histJoint.data[leftValue*histLength+rightValue]++; // H(L,R) += 1
			}
		}
	}

	/**
	 * Computes the joint and image specific probabilities using the joint histogram.
	 */
	void computeProbabilities() {
		// Convert joint histogram into a joint probability
		float totalPixels = ImageStatistics.sum(histJoint);
		int histN = histJoint.width*histJoint.height;
		for (int i = 0; i < histN; i++) {
			entropyJoint.data[i] = histJoint.data[i]/totalPixels + eps;
		}

		// Compute probabilities for left and right images by summing rows and columns of the joint probability
		GImageMiscOps.fill(entropyRight,0);
		for (int row = 0; row < entropyJoint.height; row++) {
			int idx = row * entropyJoint.width;
			float sumRow = 0;
			for (int col = 0; col < entropyJoint.width; col++, idx++) {
				float v = entropyJoint.data[idx];
				sumRow += v;
				entropyRight.data[col] += v;
			}
			entropyLeft.data[row] = sumRow;
		}
	}

	/**
	 * Compute Entropy from the already computed probabilities
	 */
	void computeEntropy() {
		// Compute Joint Entropy Eq. 5
		// H = -(1/n)*log(I*G)*G
		// Supposedly this is effectively Parezen Estimation
		ConvolveImageNormalized.horizontal(smoothKernel, entropyJoint, smoothWork);
		ConvolveImageNormalized.vertical(smoothKernel, smoothWork, entropyJoint);
		PixelMath.log(entropyJoint, 0.0f, entropyJoint);
		ConvolveImageNormalized.horizontal(smoothKernel, entropyJoint, smoothWork);
		ConvolveImageNormalized.vertical(smoothKernel, smoothWork, entropyJoint);
		PixelMath.divide(entropyJoint,-entropyJoint.totalPixels(), entropyJoint);

		// Compute entropy for each image using a similar approach
		ConvolveImageNormalized.horizontal(smoothKernel, entropyLeft, smoothWork);
		PixelMath.log(smoothWork,0.0f,smoothWork);
		ConvolveImageNormalized.horizontal(smoothKernel, smoothWork, entropyLeft);
		PixelMath.divide(entropyLeft,-entropyLeft.totalPixels(), entropyLeft);

		ConvolveImageNormalized.horizontal(smoothKernel, entropyRight, smoothWork);
		PixelMath.log(smoothWork,0.0f,smoothWork);
		ConvolveImageNormalized.horizontal(smoothKernel, smoothWork, entropyRight);
		PixelMath.divide(entropyRight,-entropyRight.totalPixels(), entropyRight);
	}

	/**
	 * Scales input pixel intensity to histogram range
	 */
	final int scalePixelValue(int value ) {
		return maxHistogramValue*value/maxPixelValue;
	}

	public float getEps() {
		return eps;
	}

	public void setEps(float eps) {
		this.eps = eps;
	}
}
