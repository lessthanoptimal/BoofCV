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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.transform.ii.DerivativeIntegralImage;
import boofcv.alg.transform.ii.IntegralImageOps; //CONCURRENT_REMOVE_LINE
import boofcv.alg.transform.ii.IntegralKernel;
//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;

//CONCURRENT_INLINE import static boofcv.alg.feature.detect.intensity.impl.ImplIntegralImageFeatureIntensity.computeHessian;

/**
 * <p>Routines for computing the intensity of the fast hessian features in an image.</p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplIntegralImageFeatureIntensity</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.feature.detect.intensity.impl.GenerateImplIntegralImageFeatureIntensity")
public class ImplIntegralImageFeatureIntensity {

//CONCURRENT_OMIT_BEGIN
	/**
	 * Brute force approach which is easy to validate through visual inspection.
	 */
	public static void hessianNaive( GrayF32 integral, int skip, int size, GrayF32 intensity ) {
		final int w = intensity.width;
		final int h = intensity.height;

		// get convolution kernels for the second order derivatives
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(size, null);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(size, null);
		IntegralKernel kerXY = DerivativeIntegralImage.kernelDerivXY(size, null);

		float norm = 1.0f/(size*size);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				int xx = x*skip;
				int yy = y*skip;

				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
	}
//CONCURRENT_OMIT_END

	/**
	 * Only computes the fast hessian along the border using a brute force approach
	 */
	public static void hessianBorder( GrayF32 integral, int skip, int size,
									  GrayF32 intensity,
									  @Nullable IntegralKernel storageKerXX,
									  @Nullable IntegralKernel storageKerYY,
									  @Nullable IntegralKernel storageKerXY ) {
		final int w = intensity.width;
		final int h = intensity.height;

		// get convolution kernels for the second order derivatives
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(size, storageKerXX);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(size, storageKerYY);
		IntegralKernel kerXY = DerivativeIntegralImage.kernelDerivXY(size, storageKerXY);

		int radiusFeature = size/2;
		final int borderOrig = radiusFeature + 1 + (skip - (radiusFeature + 1)%skip);
		final int border = borderOrig/skip;

		float norm = 1.0f/(size*size);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y -> {
		for (int y = 0; y < h; y++) {
			int yy = y*skip;
			for (int x = 0; x < border; x++) {
				int xx = x*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
			for (int x = w - border; x < w; x++) {
				int xx = x*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
		//CONCURRENT_ABOVE });

		//CONCURRENT_BELOW BoofConcurrency.loopFor(border, w - border, x -> {
		for (int x = border; x < w - border; x++) {
			int xx = x*skip;

			for (int y = 0; y < border; y++) {
				int yy = y*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
			for (int y = h - border; y < h; y++) {
				int yy = y*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
		//CONCURRENT_ABOVE });
	}

	//CONCURRENT_OMIT_BEGIN
	public static void computeHessian( GrayF32 integral, GrayF32 intensity, IntegralKernel kerXX, IntegralKernel kerYY, IntegralKernel kerXY, float norm, int y, int yy, int x, int xx ) {
		float Dxx = IntegralImageOps.convolveSparse(integral, kerXX, xx, yy);
		float Dyy = IntegralImageOps.convolveSparse(integral, kerYY, xx, yy);
		float Dxy = IntegralImageOps.convolveSparse(integral, kerXY, xx, yy);

		Dxx *= norm;
		Dxy *= norm;
		Dyy *= norm;

		float det = Dxx*Dyy - 0.81f*Dxy*Dxy;

		intensity.set(x, y, det);
	}
//CONCURRENT_OMIT_END

	/**
	 * Optimizes intensity for the inner image.
	 */
	public static void hessianInner( GrayF32 integral, int skip, int size,
									 GrayF32 intensity ) {
		final int w = intensity.width;
		final int h = intensity.height;

		float norm = 1.0f/(size*size);

		int blockSmall = size/3;
		int blockLarge = size - blockSmall - 1;
		int radiusFeature = size/2;
		int radiusSkinny = blockLarge/2;

		int blockW2 = 2*blockSmall;
		int blockW3 = 3*blockSmall;


		int rowOff1 = blockSmall*integral.stride;
		int rowOff2 = 2*rowOff1;
		int rowOff3 = 3*rowOff1;

		// make sure it starts on the correct pixel
		final int borderOrig = radiusFeature + 1 + (skip - (radiusFeature + 1)%skip);
		final int border = borderOrig/skip;
		final int lostPixel = borderOrig - radiusFeature - 1;
		final int endY = h - border;
		final int endX = w - border;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(border, endY, y -> {
		for (int y = border; y < endY; y++) {

			// pixel location in original input image
			int yy = y*skip;

			// index for output
			int indexDst = intensity.startIndex + y*intensity.stride + border;

			// indexes for Dxx
			int indexTop = integral.startIndex + (yy - radiusSkinny - 1)*integral.stride + lostPixel;
			int indexBottom = indexTop + blockLarge*integral.stride;

			// indexes for Dyy
			int indexL = integral.startIndex + (yy - radiusFeature - 1)*integral.stride + (radiusFeature - radiusSkinny) + lostPixel;
			int indexR = indexL + blockLarge;

			// indexes for Dxy
			int indexY1 = integral.startIndex + (yy - blockSmall - 1)*integral.stride + (radiusFeature - blockSmall) + lostPixel;
			int indexY2 = indexY1 + blockSmall*integral.stride;
			int indexY3 = indexY2 + integral.stride;
			int indexY4 = indexY3 + blockSmall*integral.stride;

			for (int x = border; x < endX; x++, indexDst++) {
				float Dxx = integral.data[indexBottom + blockW3] - integral.data[indexTop + blockW3] - integral.data[indexBottom] + integral.data[indexTop];
				Dxx -= 3*(integral.data[indexBottom + blockW2] - integral.data[indexTop + blockW2] - integral.data[indexBottom + blockSmall] + integral.data[indexTop + blockSmall]);

				float Dyy = integral.data[indexR + rowOff3] - integral.data[indexL + rowOff3] - integral.data[indexR] + integral.data[indexL];
				Dyy -= 3*(integral.data[indexR + rowOff2] - integral.data[indexL + rowOff2] - integral.data[indexR + rowOff1] + integral.data[indexL + rowOff1]);

				int x3 = blockSmall + 1;
				int x4 = x3 + blockSmall;

				float Dxy = integral.data[indexY2 + blockSmall] - integral.data[indexY1 + blockSmall] - integral.data[indexY2] + integral.data[indexY1];
				Dxy -= integral.data[indexY2 + x4] - integral.data[indexY1 + x4] - integral.data[indexY2 + x3] + integral.data[indexY1 + x3];
				Dxy += integral.data[indexY4 + x4] - integral.data[indexY3 + x4] - integral.data[indexY4 + x3] + integral.data[indexY3 + x3];
				Dxy -= integral.data[indexY4 + blockSmall] - integral.data[indexY3 + blockSmall] - integral.data[indexY4] + integral.data[indexY3];

				Dxx *= norm;
				Dxy *= norm;
				Dyy *= norm;

				intensity.data[indexDst] = Dxx*Dyy - 0.81f*Dxy*Dxy;

				indexTop += skip;
				indexBottom += skip;
				indexL += skip;
				indexR += skip;
				indexY1 += skip;
				indexY2 += skip;
				indexY3 += skip;
				indexY4 += skip;
			}
		}
		//CONCURRENT_ABOVE });
	}

//CONCURRENT_OMIT_BEGIN

	/**
	 * Brute force approach which is easy to validate through visual inspection.
	 */
	public static void hessianNaive( GrayS32 integral, int skip, int size,
									 GrayF32 intensity ) {
		final int w = intensity.width;
		final int h = intensity.height;

		// get convolution kernels for the second order derivatives
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(size, null);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(size, null);
		IntegralKernel kerXY = DerivativeIntegralImage.kernelDerivXY(size, null);

		float norm = 1.0f/(size*size);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				int xx = x*skip;
				int yy = y*skip;

				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
	}
//CONCURRENT_OMIT_END

	/**
	 * Only computes the fast hessian along the border using a brute force approach
	 */
	public static void hessianBorder( GrayS32 integral, int skip, int size,
									  GrayF32 intensity,
									  @Nullable IntegralKernel storageKerXX,
									  @Nullable IntegralKernel storageKerYY,
									  @Nullable IntegralKernel storageKerXY ) {
		final int w = intensity.width;
		final int h = intensity.height;

		// get convolution kernels for the second order derivatives
		IntegralKernel kerXX = DerivativeIntegralImage.kernelDerivXX(size, storageKerXX);
		IntegralKernel kerYY = DerivativeIntegralImage.kernelDerivYY(size, storageKerYY);
		IntegralKernel kerXY = DerivativeIntegralImage.kernelDerivXY(size, storageKerXY);

		int radiusFeature = size/2;
		final int borderOrig = radiusFeature + 1 + (skip - (radiusFeature + 1)%skip);
		final int border = borderOrig/skip;

		float norm = 1.0f/(size*size);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, h, y -> {
		for (int y = 0; y < h; y++) {
			int yy = y*skip;
			for (int x = 0; x < border; x++) {
				int xx = x*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
			for (int x = w - border; x < w; x++) {
				int xx = x*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
		//CONCURRENT_ABOVE });

		//CONCURRENT_BELOW BoofConcurrency.loopFor(border, w - border, x -> {
		for (int x = border; x < w - border; x++) {
			int xx = x*skip;

			for (int y = 0; y < border; y++) {
				int yy = y*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
			for (int y = h - border; y < h; y++) {
				int yy = y*skip;
				computeHessian(integral, intensity, kerXX, kerYY, kerXY, norm, y, yy, x, xx);
			}
		}
		//CONCURRENT_ABOVE });
	}

	//CONCURRENT_OMIT_BEGIN
	public static void computeHessian( GrayS32 integral, GrayF32 intensity, IntegralKernel kerXX, IntegralKernel kerYY, IntegralKernel kerXY, float norm, int y, int yy, int x, int xx ) {
		float Dxx = IntegralImageOps.convolveSparse(integral, kerXX, xx, yy);
		float Dyy = IntegralImageOps.convolveSparse(integral, kerYY, xx, yy);
		float Dxy = IntegralImageOps.convolveSparse(integral, kerXY, xx, yy);

		Dxx *= norm;
		Dxy *= norm;
		Dyy *= norm;

		float det = Dxx*Dyy - 0.81f*Dxy*Dxy;

		intensity.set(x, y, det);
	}
//CONCURRENT_OMIT_END

	/**
	 * Optimizes intensity for the inner image.
	 */
	public static void hessianInner( GrayS32 integral, int skip, int size,
									 GrayF32 intensity ) {
		final int w = intensity.width;
		final int h = intensity.height;

		float norm = 1.0f/(size*size);

		int blockSmall = size/3;
		int blockLarge = size - blockSmall - 1;
		int radiusFeature = size/2;
		int radiusSkinny = blockLarge/2;

		int blockW2 = 2*blockSmall;
		int blockW3 = 3*blockSmall;


		int rowOff1 = blockSmall*integral.stride;
		int rowOff2 = 2*rowOff1;
		int rowOff3 = 3*rowOff1;

		// make sure it starts on the correct pixel
		final int borderOrig = radiusFeature + 1 + (skip - (radiusFeature + 1)%skip);
		final int border = borderOrig/skip;
		final int lostPixel = borderOrig - radiusFeature - 1;
		final int endY = h - border;
		final int endX = w - border;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(border, endY, y -> {
		for (int y = border; y < endY; y++) {

			// pixel location in original input image
			int yy = y*skip;

			// index for output
			int indexDst = intensity.startIndex + y*intensity.stride + border;

			// indexes for Dxx
			int indexTop = integral.startIndex + (yy - radiusSkinny - 1)*integral.stride + lostPixel;
			int indexBottom = indexTop + blockLarge*integral.stride;

			// indexes for Dyy
			int indexL = integral.startIndex + (yy - radiusFeature - 1)*integral.stride + (radiusFeature - radiusSkinny) + lostPixel;
			int indexR = indexL + blockLarge;

			// indexes for Dxy
			int indexY1 = integral.startIndex + (yy - blockSmall - 1)*integral.stride + (radiusFeature - blockSmall) + lostPixel;
			int indexY2 = indexY1 + blockSmall*integral.stride;
			int indexY3 = indexY2 + integral.stride;
			int indexY4 = indexY3 + blockSmall*integral.stride;

			for (int x = border; x < endX; x++, indexDst++) {
				float Dxx = integral.data[indexBottom + blockW3] - integral.data[indexTop + blockW3] - integral.data[indexBottom] + integral.data[indexTop];
				Dxx -= 3*(integral.data[indexBottom + blockW2] - integral.data[indexTop + blockW2] - integral.data[indexBottom + blockSmall] + integral.data[indexTop + blockSmall]);

				float Dyy = integral.data[indexR + rowOff3] - integral.data[indexL + rowOff3] - integral.data[indexR] + integral.data[indexL];
				Dyy -= 3*(integral.data[indexR + rowOff2] - integral.data[indexL + rowOff2] - integral.data[indexR + rowOff1] + integral.data[indexL + rowOff1]);

				int x3 = blockSmall + 1;
				int x4 = x3 + blockSmall;

				float Dxy = integral.data[indexY2 + blockSmall] - integral.data[indexY1 + blockSmall] - integral.data[indexY2] + integral.data[indexY1];
				Dxy -= integral.data[indexY2 + x4] - integral.data[indexY1 + x4] - integral.data[indexY2 + x3] + integral.data[indexY1 + x3];
				Dxy += integral.data[indexY4 + x4] - integral.data[indexY3 + x4] - integral.data[indexY4 + x3] + integral.data[indexY3 + x3];
				Dxy -= integral.data[indexY4 + blockSmall] - integral.data[indexY3 + blockSmall] - integral.data[indexY4] + integral.data[indexY3];

				Dxx *= norm;
				Dxy *= norm;
				Dyy *= norm;

				intensity.data[indexDst] = Dxx*Dyy - 0.81f*Dxy*Dxy;

				indexTop += skip;
				indexBottom += skip;
				indexL += skip;
				indexR += skip;
				indexY1 += skip;
				indexY2 += skip;
				indexY3 += skip;
				indexY4 += skip;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
