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

package boofcv.alg.filter.binary;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.HistogramStatistics;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * Weakly typed version of {@link ThresholdImageOps}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"Duplicates", "unchecked", "rawtypes"})
public class GThresholdImageOps {

	/**
	 * <p>
	 * Computes the variance based threshold using Otsu's method from an input image. Internally it uses
	 * {@link #computeOtsu(int[], int, int)} and {@link boofcv.alg.misc.GImageStatistics#histogram(ImageGray, double, int[])}
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image. (inclusive)
	 * @param maxValue The maximum value of a pixel in the image. (inclusive)
	 * @return Selected threshold.
	 */
	public static double computeOtsu( ImageGray input, double minValue, double maxValue ) {

		int range = (int)(1 + maxValue - minValue);
		int[] histogram = new int[range];

		GImageStatistics.histogram(input, minValue, histogram);

		// Total number of pixels
		int total = input.width*input.height;

		return computeOtsu(histogram, range, total) + minValue;
	}

	/**
	 * <p>
	 * Computes the variance based threshold using a modified Otsu method from an input image. Internally it uses
	 * {@link #computeOtsu2(int[], int, int)} and {@link boofcv.alg.misc.GImageStatistics#histogram(ImageGray, double, int[])}
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image. (inclusive)
	 * @param maxValue The maximum value of a pixel in the image. (inclusive)
	 * @return Selected threshold.
	 */
	public static int computeOtsu2( ImageGray input, int minValue, int maxValue ) {

		int range = 1 + maxValue - minValue;
		int[] histogram = new int[range];

		GImageStatistics.histogram(input, minValue, histogram);

		// Total number of pixels
		int total = input.width*input.height;

		return computeOtsu2(histogram, range, total) + minValue;
	}

	/**
	 * Computes the variance based Otsu threshold from a histogram directly. The threshold is selected by minimizing the
	 * spread of both foreground and background pixel values.
	 *
	 * @param histogram Histogram of pixel intensities.
	 * @param length Number of elements in the histogram.
	 * @param totalPixels Total pixels in the image
	 * @return Selected threshold
	 */
	// original code from http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html
	//                    Dr. Andrew Greensted
	// modifications to reduce overflow
	public static int computeOtsu( int[] histogram, int length, int totalPixels ) {

		// NOTE: ComputeOtsu is not used here since that will create memory.

		double dlength = length;
		double sum = 0;
		for (int i = 0; i < length; i++)
			sum += (i/dlength)*histogram[i];

		double sumB = 0;
		int wB = 0;

		double varMax = 0;
		int threshold = 0;

		for (int i = 0; i < length; i++) {
			wB += histogram[i];               // Weight Background
			if (wB == 0) continue;

			int wF = totalPixels - wB;        // Weight Foreground
			if (wF == 0) break;

			sumB += (i/dlength)*histogram[i];

			double mB = sumB/wB;            // Mean Background
			double mF = (sum - sumB)/wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (double)wB*(double)wF*(mB - mF)*(mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = i;
			}
		}

		return threshold;
	}

	/**
	 * Computes a modified modified Otsu threshold which maximizes the distance from the distributions means. In
	 * extremely sparse histograms with the values clustered at the two means Otsu will select a threshold which
	 * is at the lower peak and in binary data this can cause a total failure.
	 *
	 * @param histogram Histogram of pixel intensities.
	 * @param length Number of elements in the histogram.
	 * @param totalPixels Total pixels in the image
	 * @return Selected threshold
	 */
	public static int computeOtsu2( int[] histogram, int length, int totalPixels ) {

		// NOTE: ComputeOtsu is not used here since that will create memory.

		double dlength = length;
		double sum = 0;
		for (int i = 0; i < length; i++)
			sum += (i/dlength)*histogram[i];

		double sumB = 0;
		int wB = 0;

		double variance = 0;

		double selectedMB = 0;
		double selectedMF = 0;

		int i;
		for (i = 0; i < length; i++) {
			wB += histogram[i];               // Weight Background
			if (wB == 0) continue;

			int wF = totalPixels - wB;        // Weight Foreground
			if (wF == 0) break;

			double f = i/dlength;
			sumB += f*histogram[i];

			double mB = sumB/wB;            // Mean Background
			double mF = (sum - sumB)/wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (double)wB*(double)wF*(mB - mF)*(mB - mF);

			// Check if new maximum found
			if (varBetween > variance) {
				variance = varBetween;
				selectedMB = mB;
				selectedMF = mF;
			}
		}

		// select a threshold which maximizes the distance between the two distributions. In pathological
		// cases there's a dead zone where all the values are equally good and it would select a value with a low index
		// arbitrarily. Then if you scaled the threshold it would reject everything
		return (int)(length*(selectedMB + selectedMF)/2.0 + 0.5);
	}

	/**
	 * <p>
	 * Computes Li's Minimum Cross Entropy thresholding from an input image. Internally it uses
	 * {@link #computeLi(int[], int)} and {@link boofcv.alg.misc.GImageStatistics#histogram(ImageGray, double, int[])}
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image. (inclusive)
	 * @param maxValue The maximum value of a pixel in the image. (inclusive)
	 * @return Selected threshold.
	 */
	public static double computeLi( ImageGray input, double minValue, double maxValue ) {

		int range = (int)(1 + maxValue - minValue);
		int[] histogram = new int[range];

		GImageStatistics.histogram(input, minValue, histogram);

		return computeLi(histogram, range) + minValue;
	}

	/**
	 * Implements Li's Minimum Cross Entropy thresholding method This
	 * implementation is based on the iterative version (Ref. 2) of the
	 * algorithm. 1) Li C.H. and Lee C.K. (1993) "Minimum Cross Entropy
	 * Thresholding" Pattern Recognition, 26(4): 617-625 2) Li C.H. and Tam
	 * P.K.S. (1998) "An Iterative Algorithm for Minimum Cross Entropy
	 * Thresholding"Pattern Recognition Letters, 18(8): 771-776 3) Sezgin M. and
	 * Sankur B. (2004) "Survey over Image Thresholding Techniques and
	 * Quantitative Performance Evaluation" Journal of Electronic Imaging, 13(1):
	 * 146-165 http://citeseer.ist.psu.edu/sezgin04survey.html
	 *
	 * Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
	 * Ported from Imagej code (https://imagej.nih.gov/ij/developer/source/) to
	 * BoofCV by Nico Stuurman
	 *
	 * @param histogram Histogram of pixel intensities.
	 * @param length Number of elements in the histogram.
	 * @return Selected threshold
	 */
	public static int computeLi( int[] histogram, int length ) {
		// This function has been released by various authors under a public domain license

		int threshold;
		int old_thresh;
		double mean_back;       // mean of the background pixels at a given threshold
		double mean_obj;        // mean of the object pixels at a given threshold
		double tolerance = 0.5; // threshold tolerance
		double temp;

		// Calculate the mean gray-level and set the threshold initially to this
		int new_thresh = (int)(HistogramStatistics.mean(histogram, length) + 0.5);

		do {
			old_thresh = new_thresh;
			threshold = (int)(old_thresh + 0.5);
			// Calculate the means of background and object pixels 

			// Background
			long sum_back = 0; // sum of the background pixels at a given threshold
			long num_back = 0; // number of background pixels at a given threshold
			for (int ih = 0; ih <= threshold; ih++) {
				sum_back += ih*histogram[ih];
				num_back += histogram[ih];
			}
			mean_back = (num_back == 0 ? 0.0 : (sum_back/(double)num_back));

			// Object
			long sum_obj = 0; // sum of the object pixels at a given threshold
			long num_obj = 0; // number of object pixels at a given threshold
			for (int ih = threshold + 1; ih < length; ih++) {
				sum_obj += ih*histogram[ih];
				num_obj += histogram[ih];
			}
			mean_obj = (num_obj == 0 ? 0.0 : (sum_obj/(double)num_obj));

			/* Calculate the new threshold: Equation (7) in Ref. 2 */
			//new_thresh = simple_round ( ( mean_back - mean_obj ) / ( Math.log ( mean_back ) - Math.log ( mean_obj ) ) );
			//simple_round ( double x ) {
			// return ( int ) ( IS_NEG ( x ) ? x - .5 : x + .5 );
			//}
			//
			//#define IS_NEG( x ) ( ( x ) < -DBL_EPSILON ) 
			//DBL_EPSILON = 2.220446049250313E-16
			temp = (mean_back - mean_obj)/(Math.log(mean_back) - Math.log(mean_obj));

			if (temp < -UtilEjml.EPS) {
				new_thresh = (int)(temp - 0.5);
			} else {
				new_thresh = (int)(temp + 0.5);
			}
			//  Stop the iterations when the difference between the
			// new and old threshold values is less than the tolerance
		} while (Math.abs(new_thresh - old_thresh) > tolerance);

		return threshold;
	}

	/**
	 * <p>
	 * Computes Huang's Minimum fyzzy thresholding from an input image. Internally it uses
	 * {@link #computeHuang(int[], int)} and {@link boofcv.alg.misc.GImageStatistics#histogram(ImageGray, double, int[])}
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image. (inclusive)
	 * @param maxValue The maximum value of a pixel in the image. (inclusive)
	 * @return Selected threshold.
	 */
	public static double computeHuang( ImageGray input, double minValue, double maxValue ) {

		int range = (int)(1 + maxValue - minValue);
		int[] histogram = new int[range];

		GImageStatistics.histogram(input, minValue, histogram);

		return computeHuang(histogram, range) + minValue;
	}

	/**
	 * Implements Huang's fuzzy thresholding method Uses Shannon's entropy
	 * function (one can also use Yager's entropy function) Huang L.-K. and Wang
	 * M.-J.J. (1995) "Image Thresholding by Minimizing the Measures of
	 * Fuzziness" Pattern Recognition, 28(1): 41-51 M. Emre Celebi 06.15.2007
	 *
	 * Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
	 * Ported from Imagej code (https://imagej.nih.gov/ij/developer/source/) to
	 * BoofCV by Nico Stuurman
	 *
	 * @param histogram Histogram of pixel intensities.
	 * @param length Number of elements in the histogram.
	 * @return Selected threshold
	 */
	public static int computeHuang( int[] histogram, int length ) {
		// This function has been released by various authors under a public domain license

		// Determine the first non-zero bin
		int first_bin = 0;
		for (int ih = 0; ih < length; ih++) {
			if (histogram[ih] != 0) {
				first_bin = ih;
				break;
			}
		}

		// Determine the last non-zero bin
		int last_bin = length - 1;
		for (int ih = last_bin; ih >= first_bin; ih--) {
			if (histogram[ih] != 0) {
				last_bin = ih;
				break;
			}
		}
		double term = 1.0/(double)(last_bin - first_bin);
		double[] mu_0 = new double[length];
		{
			long sum_pix = 0, num_pix = 0;
			for (int ih = first_bin; ih < length; ih++) {
				sum_pix += ih*histogram[ih];
				num_pix += histogram[ih];
				// NUM_PIX cannot be zero !
				mu_0[ih] = sum_pix/(double)num_pix;
			}
		}

		double[] mu_1 = new double[length];
		{
			long sum_pix = 0, num_pix = 0;
			for (int ih = last_bin; ih >= 0; ih--) { // original: (ih = last_bin; ih > 0; ih--)
				sum_pix += ih*histogram[ih];
				num_pix += histogram[ih];
				// NUM_PIX cannot be zero !
				mu_1[ih] = sum_pix/(double)num_pix; // original: mu_1[ih -1] = sum_pix/(double) num_pix
			}
		}

		/* Determine the threshold that minimizes the fuzzy entropy */
		int threshold = -1;
		double min_ent = Double.MAX_VALUE; // min entropy
		for (int it = first_bin; it <= last_bin; it++) {
			double ent = 0.0;  // entropy
			for (int ih = first_bin; ih <= it; ih++) {
				// Equation (4) in Ref. 1
				double mu_x = 1.0/(1.0 + term*Math.abs(ih - mu_0[it]));
				if (!((mu_x < 1e-06) || (mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += histogram[ih]*(-mu_x*Math.log(mu_x) - (1.0 - mu_x)*Math.log(1.0 - mu_x));
				}
			}

			for (int ih = it + 1; ih <= last_bin; ih++) {
				// Equation (4) in Ref. 1
				double mu_x = 1.0/(1.0 + term*Math.abs(ih - mu_1[it]));
				if (!((mu_x < 1e-06) || (mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += histogram[ih]*(-mu_x*Math.log(mu_x) - (1.0 - mu_x)*Math.log(1.0 - mu_x));
				}
			}
			/* No need to divide by NUM_ROWS * NUM_COLS * LOG(2) ! */
			if (ent < min_ent) {
				min_ent = ent;
				threshold = it;
			}
		}
		return threshold;
	}

	/**
	 * <p>
	 * Computes a threshold which maximizes the entropy between the foreground and background regions. See
	 * {@link #computeEntropy(int[], int, int)} for more details.
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image. (inclusive)
	 * @param maxValue The maximum value of a pixel in the image. (inclusive)
	 * @return Selected threshold.
	 * @see boofcv.alg.misc.GImageStatistics#histogram(ImageGray, double, int[])
	 */
	public static double computeEntropy( ImageGray input, double minValue, double maxValue ) {

		int range = (int)(1 + maxValue - minValue);
		int[] histogram = new int[range];

		GImageStatistics.histogram(input, minValue, histogram);

		// Total number of pixels
		int total = input.width*input.height;

		return computeEntropy(histogram, range, total) + minValue;
	}

	/**
	 * <p>
	 * Computes a threshold which maximizes the entropy between the foreground and background regions. See [1]
	 * for algorithmic details, which cites [2].
	 * </p>
	 *
	 * <p>
	 * [1] E.R. Davies "Machine Vision Theory Algorithms Practicalities" 3rd Ed. 2005. pg. 124<br>
	 * [2] Hannah, Ian, Devesh Patel, and Roy Davies. "The use of variance and entropic thresholding methods
	 * for image segmentation." Pattern Recognition 28.8 (1995): 1135-1143.
	 * </p>
	 *
	 * @param histogram Histogram of pixel intensities.
	 * @param length Number of elements in the histogram.
	 * @param totalPixels Total pixels in the image
	 * @return Selected threshold
	 */
	public static int computeEntropy( int[] histogram, int length, int totalPixels ) {

		// precompute p[i]*ln(p[i]) and handle special case where p[i] = 0
		double[] p = new double[length];
		for (int i = 0; i < length; i++) {
			int h = histogram[i];
			if (h == 0) {
				p[i] = 0;
			} else {
				p[i] = h/(double)totalPixels;
				p[i] *= Math.log(p[i]);
			}
		}

		double bestScore = 0;
		int bestIndex = 0;
		int countF = 0;

		for (int i = 0; i < length; i++) {
			countF += histogram[i];
			double sumF = countF/(double)totalPixels;

			if (sumF == 0 || sumF == 1.0) continue;

			double sumB = 1.0 - sumF;

			double HA = 0;
			for (int j = 0; j <= i; j++) {
				HA += p[j];
			}
			HA /= sumF;

			double HB = 0;
			for (int j = i + 1; j < length; j++) {
				HB += p[j];
			}
			HB /= sumB;

			double entropy = Math.log(sumF) + Math.log(sumB) - HA - HB;

			if (entropy > bestScore) {
				bestScore = entropy;
				bestIndex = i;
			}
		}

		return bestIndex;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values &le;
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values &gt;
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality &le; is used, otherwise if false then &gt; is used.
	 * @return binary image.
	 */
	public static <T extends ImageGray<T>>
	GrayU8 threshold( T input, @Nullable GrayU8 output,
					  double threshold, boolean down ) {
		if (input instanceof GrayF32) {
			return ThresholdImageOps.threshold((GrayF32)input, output, (float)threshold, down);
		} else if (input instanceof GrayU8) {
			return ThresholdImageOps.threshold((GrayU8)input, output, (int)threshold, down);
		} else if (input instanceof GrayU16) {
			return ThresholdImageOps.threshold((GrayU16)input, output, (int)threshold, down);
		} else if (input instanceof GrayS16) {
			return ThresholdImageOps.threshold((GrayS16)input, output, (int)threshold, down);
		} else if (input instanceof GrayS32) {
			return ThresholdImageOps.threshold((GrayS32)input, output, (int)threshold, down);
		} else if (input instanceof GrayF64) {
			return ThresholdImageOps.threshold((GrayF64)input, output, threshold, down);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the average value of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) &gt; T(x,y) * scale ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results. If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace. Can be null
	 * @param work2 (Optional) Internal workspace. Can be null
	 * @return binary image.
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localMean( T input, GrayU8 output,
					  ConfigLength width, double scale, boolean down,
					  @Nullable T work1, @Nullable T work2, @Nullable GrowArray work3 ) {
		if (input instanceof GrayF32) {
			return ThresholdImageOps.localMean((GrayF32)input, output, width, (float)scale, down,
					(GrayF32)work1, (GrayF32)work2, (GrowArray<DogArray_F32>)work3);
		} else if (input instanceof GrayU8) {
			return ThresholdImageOps.localMean((GrayU8)input, output, width, (float)scale, down,
					(GrayU8)work1, (GrayU8)work2, (GrowArray<DogArray_I32>)work3);
		} else if (input instanceof GrayU16) {
			return ThresholdImageOps.localMean((GrayU16)input, output, width, (float)scale, down,
					(GrayU16)work1, (GrayU16)work2, (GrowArray<DogArray_I32>)work3);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the gaussian weighted sum of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) &gt; T(x,y) * scale ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results. If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace. Can be null
	 * @param work2 (Optional) Internal workspace. Can be null
	 * @return binary image.
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localGaussian( T input, @Nullable GrayU8 output,
						  ConfigLength width, double scale, boolean down,
						  @Nullable T work1, @Nullable ImageGray work2 ) {
		if (input instanceof GrayF32) {
			return ThresholdImageOps.localGaussian((GrayF32)input, output, width, (float)scale, down,
					(GrayF32)work1, (GrayF32)work2);
		} else if (input instanceof GrayU8) {
			return ThresholdImageOps.localGaussian((GrayU8)input, output, width, (float)scale, down,
					(GrayU8)work1, (GrayU8)work2);
		} else if (input instanceof GrayU16) {
			return ThresholdImageOps.localGaussian((GrayU16)input, output, width, (float)scale, down,
					(GrayU16)work1, (GrayU16)work2);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * {@link FactoryThresholdBinary#localOtsu(ConfigLength, double, boolean, boolean, double, Class)}
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localOtsu( T input, @Nullable GrayU8 output, boolean otsu2, ConfigLength width, double tuning, double scale, boolean down ) {
		InputToBinary<T> alg = FactoryThresholdBinary.localOtsu(width, scale, down, otsu2, tuning, input.getImageType().getImageClass());

		if (output == null)
			output = new GrayU8(input.width, input.height);

		alg.process(input, output);

		return output;
	}

	/**
	 * Applies {@link ThresholdNiblackFamily Niblack} thresholding to the input image.
	 * Intended for use with text image.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param k Positive parameter used to tune threshold. Try 0.3
	 * @param down Should it threshold up or down.
	 * @return binary image
	 * @see ThresholdNiblackFamily
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localNiblack( T input, @Nullable GrayU8 output, ConfigLength width, float k, boolean down ) {
		return niblackFamily(input, output, width, k, down, ThresholdNiblackFamily.Variant.NIBLACK);
	}

	/**
	 * Applies {@link ThresholdNiblackFamily Sauvola} thresholding to the input image.
	 * Intended for use with text image.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param k Positive parameter used to tune threshold. Try 0.3
	 * @param down Should it threshold up or down.
	 * @return binary image
	 * @see ThresholdNiblackFamily
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localSauvola( T input, @Nullable GrayU8 output, ConfigLength width, float k, boolean down ) {
		return niblackFamily(input, output, width, k, down, ThresholdNiblackFamily.Variant.SAUVOLA);
	}

	/**
	 * Applies {@link ThresholdNiblackFamily Wolf} thresholding to the input image.
	 * Intended for use with text image.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param k Positive parameter used to tune threshold. Try 0.3
	 * @param down Should it threshold up or down.
	 * @return binary image
	 * @see ThresholdNiblackFamily
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localWolf( T input, @Nullable GrayU8 output, ConfigLength width, float k, boolean down ) {
		return niblackFamily(input, output, width, k, down, ThresholdNiblackFamily.Variant.WOLF_JOLION);
	}

	protected static <T extends ImageGray<T>>
	GrayU8 niblackFamily( T input, @Nullable GrayU8 output, ConfigLength width, float k, boolean down,
						  ThresholdNiblackFamily.Variant variant) {
		InputToBinary<GrayF32> alg;

		if (BoofConcurrency.USE_CONCURRENT) {
			alg = new ThresholdNiblackFamily_MT(width, k, down, variant);
		} else {
			alg = new ThresholdNiblackFamily(width, k, down, variant);
		}

		if (output == null)
			output = new GrayU8(input.width, input.height);

		if (input instanceof GrayF32) {
			alg.process((GrayF32)input, output);
		} else {
			GrayF32 conv = new GrayF32(input.width, input.height);
			GConvertImage.convert(input, conv);
			alg.process(conv, output);
		}

		return output;
	}

	/**
	 * Applies {@link boofcv.alg.filter.binary.ThresholdNick NICK} thresholding to the input image.
	 * Intended for use with text image.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param k Positive parameter used to tune threshold. Try -0.1 to -0.2
	 * @param down Should it threshold up or down.
	 * @return binary image
	 * @see boofcv.alg.filter.binary.ThresholdNick
	 */
	public static <T extends ImageGray<T>>
	GrayU8 localNick( T input, @Nullable GrayU8 output, ConfigLength width, float k, boolean down ) {
		InputToBinary<GrayF32> alg =
				BoofConcurrency.USE_CONCURRENT ?
						new ThresholdNick_MT(width, k, down) : new ThresholdNick(width, k, down);

		if (output == null)
			output = new GrayU8(input.width, input.height);

		if (input instanceof GrayF32) {
			alg.process((GrayF32)input, output);
		} else {
			GrayF32 conv = new GrayF32(input.width, input.height);
			GConvertImage.convert(input, conv);
			alg.process(conv, output);
		}

		return output;
	}

	/**
	 * Applies a threshold to an image by computing the min and max values in a regular grid across
	 * the input image. See {@link ThresholdBlockMinMax} for the details.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param textureThreshold If the min and max values are within this threshold the pixel will be set to 1.
	 * @return Binary image
	 */
	public static <T extends ImageGray<T>>
	GrayU8 blockMinMax( T input, @Nullable GrayU8 output, ConfigLength width, double scale, boolean down, double textureThreshold ) {
		InputToBinary<T> alg = FactoryThresholdBinary.blockMinMax(
				width, scale, down, true, textureThreshold, (Class)input.getClass());

		if (output == null)
			output = new GrayU8(input.width, input.height);

		alg.process(input, output);

		return output;
	}

	/**
	 * Applies a threshold to an image by computing the mean values in a regular grid across
	 * the input image. See {@link ThresholdBlockMean} for the details.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @return Binary image
	 */
	public static <T extends ImageGray<T>>
	GrayU8 blockMean( T input, @Nullable GrayU8 output, ConfigLength width, double scale, boolean down ) {
		InputToBinary<T> alg = FactoryThresholdBinary.blockMean(width, scale, down, true,
				(Class<T>)input.getClass());

		if (output == null)
			output = new GrayU8(input.width, input.height);

		alg.process(input, output);

		return output;
	}

	/**
	 * Applies a threshold to an image by computing the Otsu threshold in a regular grid across
	 * the input image. See {@link ThresholdBlockOtsu} for the details.
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param tuning Tuning parameter. 0 = regular Otsu
	 * @param down Should it threshold up or down.
	 * @return Binary image
	 */
	public static <T extends ImageGray<T>>
	GrayU8 blockOtsu( T input, @Nullable GrayU8 output, boolean otsu2, ConfigLength width, double tuning, double scale, boolean down ) {
		InputToBinary<T> alg = FactoryThresholdBinary.blockOtsu(width, scale, down, true, otsu2, tuning,
				(Class)input.getClass());

		if (output == null)
			output = new GrayU8(input.width, input.height);

		alg.process(input, output);

		return output;
	}
}
