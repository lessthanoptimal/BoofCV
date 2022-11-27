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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.misc.ImageLambdaFilters;
import boofcv.struct.image.GrayU8;

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
 * @author Peter Abeles
 */
public class AdaptiveMeanFilter {
	int radiusX, radiusY;
	int regionX = radiusX*2 + 1;
	int regionY = radiusY*2 + 1;

	int[] localValues = new int[regionX*regionY];

	public void process( GrayU8 src, double noiseVariance, GrayU8 dst ) {
		InputSanityCheck.checkReshape(src, dst);

		// TODO modified so that this will work with threads

		// Apply filter to inner region
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, localValues, ( indexCenter, w ) -> {
			int[] values = (int[])w;

			// copy values of local region into an array
			int valueIndex = 0;
			int pixelRowIndex = indexCenter - regionX - regionY*src.stride;

			for (int y = 0; y < regionY; y++) {
				int pixelIndex = pixelRowIndex + y*src.stride;
				for (int x = 0; x < regionX; x++) {
					localValues[valueIndex++] = src.data[pixelIndex++] & 0xFF;
				}
			}

			final int N = localValues.length;
			return computeFilter(noiseVariance, localValues[N/2], values, N);
		});

		// Apply filter to image border
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, localValues, ( cx, cy, x0, y0, x1, y1, w ) -> {
			int[] values = (int[])w;

			for (int y = y0, valueIndex = 0; y < y1; y++) {
				int indexSrc = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					values[valueIndex++] = src.data[indexSrc++] & 0xFF;
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

		// Apply the formula. 0.5 is to round instead of floor. Works because it's always positive
		return (int)(centerValue - Math.min(1.0, noiseVariance/localVariance)*(centerValue - localMean) + 0.5);
	}
}
