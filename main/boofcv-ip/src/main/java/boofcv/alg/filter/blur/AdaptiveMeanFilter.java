/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.misc.ImageLambdaFilters;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Generated;

/**
 * <p>Given an estimate of image noise sigma, adaptive applies a mean filter dependent on local image statistics in
 * order to preserve edges, see [1]. This implementation uses multiple images to store intermediate results
 * to fully utilize efficient implementations of mean filters.</p>
 *
 * <pre>f(x,y) = g(x,y) - (noise)/(local noise)*[ g(x,y) - mean(x,y) ]</pre>
 *
 * Where noise is the estimated variance of pixel "noise", "local noise" is the local variance inside the region, mean
 * is the mean of the local region at (x,y). If the ratio is more than one it is set to one.
 *
 * <ol>
 *      <li> Rafael C. Gonzalez and Richard E. Woods, "Digital Image Processing" 4nd Ed. 2018.</li>
 * </ol>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateAdaptiveMeanFilter</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.blur.GenerateAdaptiveMeanFilter")
public class AdaptiveMeanFilter {
	// Note: This could be made to run WAY faster by using a histogram,
	//       then modifying it while sliding it across the image
	// Note: Add concurrent implementation

	/** Defines the symmetric rectangular region. width = 2*radius + 1 */
	@Getter @Setter int radiusX, radiusY;

	/** Defines the expected additive Gaussian pixel noise that the input image is corrupted by */
	@Getter double noiseVariance;

	public AdaptiveMeanFilter( int radiusX, int radiusY ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
	}

	public AdaptiveMeanFilter() {}

	/**
	 * Generic version for single band images. Applies filter to the input image.
	 *
	 * @param src (Input) Image. Not modified.
	 * @param dst (Output) Image. Modified.
	 */
	public <T extends ImageGray<T>> void process( T src, T dst ) {
		switch (src.getDataType()) {
			case U8 -> process((GrayU8)src, (GrayU8)dst);
			case U16 -> process((GrayU16)src, (GrayU16)dst);
			case F32 -> process((GrayF32)src, (GrayF32)dst);
			case F64 -> process((GrayF64)src, (GrayF64)dst);
			default -> throw new IllegalArgumentException("Unsupported image type: " + src.getDataType());
		}
	}

	/**
	 * Applies the filter to the src image and saves the results to the dst image. The shape of dst is modified
	 * to match src.
	 *
	 * @param src (Input) Image. Not modified.
	 * @param dst (Output) Image. Modified.
	 */
	public void process( GrayU8 src, GrayU8 dst ) {
		BoofMiscOps.checkTrue(radiusX >= 0, "Radius must not be negative");
		BoofMiscOps.checkTrue(radiusY >= 0, "Radius must not be negative");
		dst.reshape(src.width, src.height);

		int regionX = radiusX*2 + 1;
		int regionY = radiusY*2 + 1;
		var localValues = new int[regionX*regionY];

		// Apply filter to inner region
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {
			var values = (int[])w;

			// copy values of local region into an array
			int valueIndex = 0;
			int pixelRowIndex = indexCenter - radiusX - radiusY*src.stride;

			for (int y = 0; y < regionY; y++) {
				int pixelIndex = pixelRowIndex + y*src.stride;
				for (int x = 0; x < regionX; x++) {
					localValues[valueIndex++] = src.data[pixelIndex++]& 0xFF;
				}
			}

			final int N = localValues.length;
			return computeFilter(noiseVariance, localValues[N/2], values, N);
		});

		// Apply filter to image border
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {
			var values = (int[])w;

			for (int y = y0, valueIndex = 0; y < y1; y++) {
				int indexSrc = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					values[valueIndex++] = src.data[indexSrc++]& 0xFF;
				}
			}

			// Compute index of center pixel using local grid in row-major order
			int indexCenter = (cy - y0)*(x1 - x0) + cx - x0;

			// number of elements in local region
			final int N = (x1 - x0)*(y1 - y0);

			return computeFilter(noiseVariance, values[indexCenter], values, N);
		});
	}

	/**
	 * Applies the filter to the src image and saves the results to the dst image. The shape of dst is modified
	 * to match src.
	 *
	 * @param src (Input) Image. Not modified.
	 * @param dst (Output) Image. Modified.
	 */
	public void process( GrayU16 src, GrayU16 dst ) {
		BoofMiscOps.checkTrue(radiusX >= 0, "Radius must not be negative");
		BoofMiscOps.checkTrue(radiusY >= 0, "Radius must not be negative");
		dst.reshape(src.width, src.height);

		int regionX = radiusX*2 + 1;
		int regionY = radiusY*2 + 1;
		var localValues = new int[regionX*regionY];

		// Apply filter to inner region
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {
			var values = (int[])w;

			// copy values of local region into an array
			int valueIndex = 0;
			int pixelRowIndex = indexCenter - radiusX - radiusY*src.stride;

			for (int y = 0; y < regionY; y++) {
				int pixelIndex = pixelRowIndex + y*src.stride;
				for (int x = 0; x < regionX; x++) {
					localValues[valueIndex++] = src.data[pixelIndex++]& 0xFFFF;
				}
			}

			final int N = localValues.length;
			return computeFilter(noiseVariance, localValues[N/2], values, N);
		});

		// Apply filter to image border
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {
			var values = (int[])w;

			for (int y = y0, valueIndex = 0; y < y1; y++) {
				int indexSrc = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					values[valueIndex++] = src.data[indexSrc++]& 0xFFFF;
				}
			}

			// Compute index of center pixel using local grid in row-major order
			int indexCenter = (cy - y0)*(x1 - x0) + cx - x0;

			// number of elements in local region
			final int N = (x1 - x0)*(y1 - y0);

			return computeFilter(noiseVariance, values[indexCenter], values, N);
		});
	}

	/**
	 * Applies the filter to the src image and saves the results to the dst image. The shape of dst is modified
	 * to match src.
	 *
	 * @param src (Input) Image. Not modified.
	 * @param dst (Output) Image. Modified.
	 */
	public void process( GrayF32 src, GrayF32 dst ) {
		BoofMiscOps.checkTrue(radiusX >= 0, "Radius must not be negative");
		BoofMiscOps.checkTrue(radiusY >= 0, "Radius must not be negative");
		dst.reshape(src.width, src.height);

		int regionX = radiusX*2 + 1;
		int regionY = radiusY*2 + 1;
		var localValues = new float[regionX*regionY];

		// Apply filter to inner region
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {
			var values = (float[])w;

			// copy values of local region into an array
			int valueIndex = 0;
			int pixelRowIndex = indexCenter - radiusX - radiusY*src.stride;

			for (int y = 0; y < regionY; y++) {
				int pixelIndex = pixelRowIndex + y*src.stride;
				for (int x = 0; x < regionX; x++) {
					localValues[valueIndex++] = src.data[pixelIndex++];
				}
			}

			final int N = localValues.length;
			return computeFilter((float)noiseVariance, localValues[N/2], values, N);
		});

		// Apply filter to image border
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {
			var values = (float[])w;

			for (int y = y0, valueIndex = 0; y < y1; y++) {
				int indexSrc = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					values[valueIndex++] = src.data[indexSrc++];
				}
			}

			// Compute index of center pixel using local grid in row-major order
			int indexCenter = (cy - y0)*(x1 - x0) + cx - x0;

			// number of elements in local region
			final int N = (x1 - x0)*(y1 - y0);

			return computeFilter((float)noiseVariance, values[indexCenter], values, N);
		});
	}

	/**
	 * Applies the filter to the src image and saves the results to the dst image. The shape of dst is modified
	 * to match src.
	 *
	 * @param src (Input) Image. Not modified.
	 * @param dst (Output) Image. Modified.
	 */
	public void process( GrayF64 src, GrayF64 dst ) {
		BoofMiscOps.checkTrue(radiusX >= 0, "Radius must not be negative");
		BoofMiscOps.checkTrue(radiusY >= 0, "Radius must not be negative");
		dst.reshape(src.width, src.height);

		int regionX = radiusX*2 + 1;
		int regionY = radiusY*2 + 1;
		var localValues = new double[regionX*regionY];

		// Apply filter to inner region
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {
			var values = (double[])w;

			// copy values of local region into an array
			int valueIndex = 0;
			int pixelRowIndex = indexCenter - radiusX - radiusY*src.stride;

			for (int y = 0; y < regionY; y++) {
				int pixelIndex = pixelRowIndex + y*src.stride;
				for (int x = 0; x < regionX; x++) {
					localValues[valueIndex++] = src.data[pixelIndex++];
				}
			}

			final int N = localValues.length;
			return computeFilter(noiseVariance, localValues[N/2], values, N);
		});

		// Apply filter to image border
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {
			var values = (double[])w;

			for (int y = y0, valueIndex = 0; y < y1; y++) {
				int indexSrc = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					values[valueIndex++] = src.data[indexSrc++];
				}
			}

			// Compute index of center pixel using local grid in row-major order
			int indexCenter = (cy - y0)*(x1 - x0) + cx - x0;

			// number of elements in local region
			final int N = (x1 - x0)*(y1 - y0);

			return computeFilter(noiseVariance, values[indexCenter], values, N);
		});
	}

	/**
	 * Apply the filter using pixel values copied into the array
	 *
	 * @param noiseVariance Assumed image noise variance
	 * @param centerValue Value of image at center pixel
	 * @param N Length of array
	 */
	private static int computeFilter( double noiseVariance, int centerValue, int[] values, int N ) {
		// Compute local mean and variance statistics
		double localMean = 0.0;
		for (int i = 0; i < N; i++) {
			localMean += values[i];
		}
		localMean /= N;

		double localVariance = 0.0;
		for (int i = 0; i < N; i++) {
			double diff = values[i] - localMean;
			localVariance += diff*diff;
		}

		localVariance /= N;

		if (localVariance == 0.0)
			return centerValue;

		// Apply the formula. 0.5 is to round instead of floor. Works because it's always positive
		return (int)(centerValue - Math.min(1.0, noiseVariance/localVariance)*(centerValue - localMean) + 0.5);
	}

	/**
	 * Apply the filter using pixel values copied into the array
	 *
	 * @param noiseVariance Assumed image noise variance
	 * @param centerValue Value of image at center pixel
	 * @param N Length of array
	 */
	private static float computeFilter( float noiseVariance, float centerValue, float[] values, int N ) {
		// Compute local mean and variance statistics
		float localMean = 0.0f;
		for (int i = 0; i < N; i++) {
			localMean += values[i];
		}
		localMean /= N;

		float localVariance = 0.0f;
		for (int i = 0; i < N; i++) {
			double diff = values[i] - localMean;
			localVariance += diff*diff;
		}

		localVariance /= N;

		if (localVariance == 0.0f)
			return centerValue;

		// Apply the formula. 0.5 is to round instead of floor. Works because it's always positive
		return (float)(centerValue - Math.min(1.0f, noiseVariance/localVariance)*(centerValue - localMean));
	}

	/**
	 * Apply the filter using pixel values copied into the array
	 *
	 * @param noiseVariance Assumed image noise variance
	 * @param centerValue Value of image at center pixel
	 * @param N Length of array
	 */
	private static double computeFilter( double noiseVariance, double centerValue, double[] values, int N ) {
		// Compute local mean and variance statistics
		double localMean = 0.0;
		for (int i = 0; i < N; i++) {
			localMean += values[i];
		}
		localMean /= N;

		double localVariance = 0.0;
		for (int i = 0; i < N; i++) {
			double diff = values[i] - localMean;
			localVariance += diff*diff;
		}

		localVariance /= N;

		if (localVariance == 0.0)
			return centerValue;

		// Apply the formula. 0.5 is to round instead of floor. Works because it's always positive
		return (double)(centerValue - Math.min(1.0, noiseVariance/localVariance)*(centerValue - localMean));
	}

}
