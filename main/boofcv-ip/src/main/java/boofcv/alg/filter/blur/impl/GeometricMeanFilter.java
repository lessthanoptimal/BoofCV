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

package boofcv.alg.filter.blur.impl;

import boofcv.alg.filter.misc.ImageLambdaFilters;
import boofcv.struct.image.*;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.alg.filter.misc.ImageLambdaFilters_MT;
//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of Geometric Mean filter as describes in [1] with modifications to avoid numerical issues.
 *
 * <p>
 * Compute a scale factor so that the average pixel intensity will be one. As a result, after scaling,
 * the product of pixel values will also be close to 1, avoiding over and under flow issues.
 * </p>
 *
 * <p>NOTE: This could be made MUCH faster using a similar technique to how the mean filter is done by
 * separating x and y axis computations. This has not been done yet since it's not yet a bottleneck
 * in any application. Maybe you would want to add it?</p>
 *
 * <ol>
 *      <li> Rafael C. Gonzalez and Richard E. Woods, "Digital Image Processing" 4nd Ed. 2018.</li>
 * </ol>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateGeometricMeanFilter</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.blur.impl.GenerateGeometricMeanFilter")
public class GeometricMeanFilter {

	/**
	 * Applies filter with type determined at runtime.
	 */
	public static <T extends ImageGray<T>> void filter( T src, int radiusX, int radiusY, double mean, T dst ) {
		switch (src.getDataType()) {
			case U8 -> filter((GrayU8)src, radiusX, radiusY, mean, (GrayU8)dst);
			case U16 -> filter((GrayU16)src, radiusX, radiusY, mean, (GrayU16)dst);
			case F32 -> filter((GrayF32)src, radiusX, radiusY, (float)mean, (GrayF32)dst);
			case F64 -> filter((GrayF64)src, radiusX, radiusY, mean, (GrayF64)dst);
			default -> throw new IllegalArgumentException("Unsupported data type: " + src.getDataType());
		}
	}

	/**
	 * Applies the geometric mean blur operator.
	 *
	 * @param src Input image
	 * @param radiusX Region's radius along x-axis
	 * @param radiusY Region's radius along y-axis
	 * @param mean Mean of input image. Used to scale pixel values so that they average 1.0
	 * @param dst Output image with results
	 */
	public static void filter( GrayU8 src, int radiusX, int radiusY, double mean, GrayU8 dst ) {
		dst.reshape(src.width, src.height);

		// Width and height of kernel
		int kx = radiusX*2 + 1;
		int ky = radiusY*2 + 1;

		// What power the product is multiplied by
		double power = 1.0/(kx*ky);

		// apply to the inner image
		//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
			int indexRow = indexCenter - radiusX - src.stride*radiusY;

			double product = 1.0;

			for (int y = 0; y < ky; y++) {
				int indexPixel = indexRow + src.stride*y;
				for (int x = 0; x < kx; x++) {
					product *= (src.data[indexPixel++] & 0xFF)/mean;
				}
			}

			return (int)(mean*Math.pow(product, power) + 0.5);
		});

		// Apply to image edge with an adaptive region size
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {
			double product = 1.0;

			for (int y = y0; y < y1; y++) {
				int indexPixel = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					product *= (src.data[indexPixel++] & 0xFF)/mean;
				}
			}

			// + 0.5 so that it rounds to nearest integer
			return (int)(mean*Math.pow(product, 1.0/((x1 - x0)*(y1 - y0))) + 0.5);
		});
	}

	/**
	 * Applies the geometric mean blur operator.
	 *
	 * @param src Input image
	 * @param radiusX Region's radius along x-axis
	 * @param radiusY Region's radius along y-axis
	 * @param mean Mean of input image. Used to scale pixel values so that they average 1.0
	 * @param dst Output image with results
	 */
	public static void filter( GrayU16 src, int radiusX, int radiusY, double mean, GrayU16 dst ) {
		dst.reshape(src.width, src.height);

		// Width and height of kernel
		int kx = radiusX*2 + 1;
		int ky = radiusY*2 + 1;

		// What power the product is multiplied by
		double power = 1.0/(kx*ky);

		// apply to the inner image
		//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
			int indexRow = indexCenter - radiusX - src.stride*radiusY;

			double product = 1.0;

			for (int y = 0; y < ky; y++) {
				int indexPixel = indexRow + src.stride*y;
				for (int x = 0; x < kx; x++) {
					product *= (src.data[indexPixel++] & 0xFFFF)/mean;
				}
			}

			return (int)(mean*Math.pow(product, power) + 0.5);
		});

		// Apply to image edge with an adaptive region size
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {
			double product = 1.0;

			for (int y = y0; y < y1; y++) {
				int indexPixel = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					product *= (src.data[indexPixel++] & 0xFFFF)/mean;
				}
			}

			// + 0.5 so that it rounds to nearest integer
			return (int)(mean*Math.pow(product, 1.0/((x1 - x0)*(y1 - y0))) + 0.5);
		});
	}

	/**
	 * Applies the geometric mean blur operator.
	 *
	 * @param src Input image
	 * @param radiusX Region's radius along x-axis
	 * @param radiusY Region's radius along y-axis
	 * @param mean Mean of input image. Used to scale pixel values so that they average 1.0
	 * @param dst Output image with results
	 */
	public static void filter( GrayF32 src, int radiusX, int radiusY, float mean, GrayF32 dst ) {
		dst.reshape(src.width, src.height);

		// Width and height of kernel
		int kx = radiusX*2 + 1;
		int ky = radiusY*2 + 1;

		// What power the product is multiplied by
		float power = 1.0f/(kx*ky);

		// apply to the inner image
		//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
			int indexRow = indexCenter - radiusX - src.stride*radiusY;

			float product = 1.0f;

			for (int y = 0; y < ky; y++) {
				int indexPixel = indexRow + src.stride*y;
				for (int x = 0; x < kx; x++) {
					product *= (src.data[indexPixel++])/mean;
				}
			}

			return (float)(mean*Math.pow(product, power));
		});

		// Apply to image edge with an adaptive region size
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {
			float product = 1.0f;

			for (int y = y0; y < y1; y++) {
				int indexPixel = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					product *= (src.data[indexPixel++])/mean;
				}
			}

			// + 0.5 so that it rounds to nearest integer
			return (float)(mean*Math.pow(product, 1.0f/((x1 - x0)*(y1 - y0))));
		});
	}

	/**
	 * Applies the geometric mean blur operator.
	 *
	 * @param src Input image
	 * @param radiusX Region's radius along x-axis
	 * @param radiusY Region's radius along y-axis
	 * @param mean Mean of input image. Used to scale pixel values so that they average 1.0
	 * @param dst Output image with results
	 */
	public static void filter( GrayF64 src, int radiusX, int radiusY, double mean, GrayF64 dst ) {
		dst.reshape(src.width, src.height);

		// Width and height of kernel
		int kx = radiusX*2 + 1;
		int ky = radiusY*2 + 1;

		// What power the product is multiplied by
		double power = 1.0/(kx*ky);

		// apply to the inner image
		//CONCURRENT_BELOW ImageLambdaFilters_MT.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
		ImageLambdaFilters.filterRectCenterInner(src, radiusX, radiusY, dst, null, ( indexCenter, w ) -> {
			int indexRow = indexCenter - radiusX - src.stride*radiusY;

			double product = 1.0;

			for (int y = 0; y < ky; y++) {
				int indexPixel = indexRow + src.stride*y;
				for (int x = 0; x < kx; x++) {
					product *= (src.data[indexPixel++])/mean;
				}
			}

			return (double)(mean*Math.pow(product, power));
		});

		// Apply to image edge with an adaptive region size
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, ( cx, cy, x0, y0, x1, y1, w ) -> {
			double product = 1.0;

			for (int y = y0; y < y1; y++) {
				int indexPixel = src.startIndex + y*src.stride + x0;
				for (int x = x0; x < x1; x++) {
					product *= (src.data[indexPixel++])/mean;
				}
			}

			// + 0.5 so that it rounds to nearest integer
			return (double)(mean*Math.pow(product, 1.0/((x1 - x0)*(y1 - y0))));
		});
	}

}
