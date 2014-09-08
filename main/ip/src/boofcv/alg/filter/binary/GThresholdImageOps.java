/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.impl.ThresholdSauvola;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.*;


/**
 * Weakly typed version of {@link ThresholdImageOps}.
 *
 * @author Peter Abeles
 */
public class GThresholdImageOps {

	/**
	 * <p>
	 * Computes the variance based threshold using Otsu's method from an input image. Internally it uses
	 * {@link #computeOtsu(int[], int, int)} and {@link boofcv.alg.misc.GImageStatistics#histogram(boofcv.struct.image.ImageSingleBand, int, int[])}
	 * </p>
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (exclusive)
	 * @return Selected threshold.
	 */
	public static int computeOtsu( ImageSingleBand input , int minValue , int maxValue ) {

		int range = maxValue - minValue;
		int histogram[] = new int[ range ];

		GImageStatistics.histogram(input,minValue,histogram);

		// Total number of pixels
		int total = input.width*input.height;

		return computeOtsu(histogram,range,total)+minValue;
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
	public static int computeOtsu( int histogram[] , int length , int totalPixels ) {

		double sum = 0;
		for (int i=0 ; i< length ; i++)
			sum += i*histogram[i];

		double sumB = 0;
		int wB = 0;

		double varMax = 0;
		int threshold = 0;

		for (int i=0 ; i<length ; i++) {
			wB += histogram[i];               // Weight Background
			if (wB == 0) continue;

			int wF = totalPixels - wB;         // Weight Foreground
			if (wF == 0) break;

			sumB += i*histogram[i];

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

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
	 * <p>
	 * Computes a threshold which maximizes the entropy between the foreground and background regions.  See
	 * {@link #computeEntropy(int[], int, int)} for more details.
	 * </p>
	 *
	 * @see boofcv.alg.misc.GImageStatistics#histogram(boofcv.struct.image.ImageSingleBand, int, int[])
	 *
	 * @param input Input gray-scale image
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (exclusive)
	 * @return Selected threshold.
	 */
	public static int computeEntropy( ImageSingleBand input , int minValue , int maxValue ) {

		int range = maxValue - minValue;
		int histogram[] = new int[ range ];

		GImageStatistics.histogram(input,minValue,histogram);

		// Total number of pixels
		int total = input.width*input.height;

		return computeEntropy(histogram, range, total)+minValue;
	}

	/**
	 * <p>
	 * Computes a threshold which maximizes the entropy between the foreground and background regions.  See [1]
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
	public static int computeEntropy( int histogram[] , int length , int totalPixels ) {

		// precompute p[i]*ln(p[i]) and handle special case where p[i] = 0
		double p[] = new double[length];
		for (int i = 0; i < length; i++) {
			int h = histogram[i];
			if( h == 0 ) {
				p[i] = 0;
			} else {
				p[i] = h/(double)totalPixels;
				p[i] *= Math.log(p[i]);
			}
		}

		double bestScore = 0;
		int bestIndex = 0;
		int countF = 0;

		for (int i=0 ; i<length ; i++) {
			countF += histogram[i];
			double sumF = countF/(double)totalPixels;

			if( sumF == 0 || sumF == 1.0 ) continue;

			double sumB = 1.0-sumF;

			double HA = 0;
			for (int j = 0; j <= i; j++) {
				HA += p[j];
			}
			HA/=sumF;

			double HB = 0;
			for (int j = i+1; j < length; j++) {
				HB += p[j];
			}
			HB/=sumB;

			double entropy = Math.log(sumF) + Math.log(sumB)  - HA - HB;

			if( entropy > bestScore ) {
				bestScore = entropy;
				bestIndex = i;
			}
		}

		return bestIndex;
	}

	/**
	 * Applies a global threshold across the whole image.  If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0.  If 'down' is false, then pixels with values >=
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then >= is used.
	 * @return binary image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 threshold( T input , ImageUInt8 output ,
						  double threshold , boolean down )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.threshold((ImageFloat32)input,output,(float)threshold,down);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.threshold((ImageUInt8)input,output,(int)threshold,down);
		} else if( input instanceof ImageUInt16) {
			return ThresholdImageOps.threshold((ImageUInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt16) {
			return ThresholdImageOps.threshold((ImageSInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt32 ) {
			return ThresholdImageOps.threshold((ImageSInt32)input,output,(int)threshold,down);
		} else if( input instanceof ImageFloat64 ) {
			return ThresholdImageOps.threshold((ImageFloat64)input,output,threshold,down);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using an adaptive threshold that is computed using a local square region centered
	 * on each pixel.  The threshold is equal to the average value of the surrounding pixels plus the bias.
	 * If down is true then b(x,y) = I(x,y) <= T(x,y) + bias ? 1 : 0.  Otherwise
	 * b(x,y) = I(x,y) >= T(x,y) + bias ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results.  If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image.  If null it will be declared internally.
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace.  Can be null
	 * @param work2 (Optional) Internal workspace.  Can be null
	 * @return binary image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 adaptiveSquare( T input , ImageUInt8 output ,
							   int radius , double bias , boolean down, T work1 , T work2 )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.adaptiveSquare((ImageFloat32) input, output, radius, (float) bias, down,
					(ImageFloat32) work1, (ImageFloat32) work2);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.adaptiveSquare((ImageUInt8) input, output, radius, (int) bias, down,
					(ImageUInt8) work1, (ImageUInt8) work2);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using an adaptive threshold that is computed using a local square region centered
	 * on each pixel.  The threshold is equal to the gaussian weighted sum of the surrounding pixels plus the bias.
	 * If down is true then b(x,y) = I(x,y) <= T(x,y) + bias ? 1 : 0.  Otherwise
	 * b(x,y) = I(x,y) >= T(x,y) + bias ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results.  If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image.  If null it will be declared internally.
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace.  Can be null
	 * @param work2 (Optional) Internal workspace.  Can be null
	 * @return binary image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 adaptiveGaussian( T input , ImageUInt8 output ,
								 int radius , double bias , boolean down ,
								 T work1 , ImageSingleBand work2 )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.adaptiveGaussian((ImageFloat32) input, output, radius, (float) bias, down,
					(ImageFloat32) work1, (ImageFloat32) work2);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.adaptiveGaussian((ImageUInt8) input, output, radius, (int) bias, down,
					(ImageUInt8) work1, (ImageUInt8) work2);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Applies {@link boofcv.alg.filter.binary.impl.ThresholdSauvola Sauvola} thresholding to the input image.
	 * Intended for use with text image.
	 *
	 * @see boofcv.alg.filter.binary.impl.ThresholdSauvola
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image.  If null it will be declared internally.
	 * @param radius Radius of local region.  Try 15
	 * @param k Positive parameter used to tune threshold.  Try 0.3
	 * @param down Should it threshold up or down.
	 * @return binary image
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 adaptiveSauvola( T input , ImageUInt8 output , int radius , float k , boolean down )
	{
		ThresholdSauvola alg = new ThresholdSauvola(radius,k, down);

		if( output == null )
			output = new ImageUInt8(input.width,input.height);

		if( input instanceof ImageFloat32 ) {
			alg.process((ImageFloat32)input,output);
		} else {
			ImageFloat32 conv = new ImageFloat32(input.width,input.height);
			GConvertImage.convert(input, conv);
			alg.process(conv,output);
		}

		return output;
	}

}
