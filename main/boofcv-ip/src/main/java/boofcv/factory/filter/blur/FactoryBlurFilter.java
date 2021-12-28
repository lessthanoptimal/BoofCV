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

package boofcv.factory.filter.blur;

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating different blur image filters.
 *
 * @author Peter Abeles
 */
public class FactoryBlurFilter {

	/**
	 * Creates a median filter for the specified image type.
	 *
	 * @param type Image type.
	 * @param radius Size of the filter.
	 * @return Median image filter.
	 */
	public static <T extends ImageBase<T>> BlurStorageFilter<T> median( ImageType<T> type, int radius ) {
		return new BlurStorageFilter<>("median", type, radius);
	}

	public static <T extends ImageGray<T>> BlurStorageFilter<T> median( Class<T> type, int radius ) {
		return median(ImageType.single(type), radius);
	}

	/**
	 * Creates a mean filter for the specified image type.
	 *
	 * @param type Image type.
	 * @param radius Size of the filter.
	 * @return mean image filter.
	 */
	public static <T extends ImageBase<T>> BlurStorageFilter<T> mean( ImageType<T> type, int radius ) {
		return new BlurStorageFilter<>("mean", type, radius);
	}

	public static <T extends ImageBase<T>> BlurStorageFilter<T> mean( ImageType<T> type, int radiusX, int radiusY ) {
		return new BlurStorageFilter<>("mean", type, radiusX, radiusY);
	}

	public static <T extends ImageGray<T>> BlurStorageFilter<T> mean( Class<T> type, int radius ) {
		return mean(ImageType.single(type), radius);
	}

	public static <T extends ImageGray<T>> BlurStorageFilter<T> mean( Class<T> type, int radiusX, int radiusY ) {
		return mean(ImageType.single(type), radiusX, radiusY);
	}

	public static <T extends ImageBase<T>> BlurStorageFilter<T> meanB( ImageType<T> type, int radiusX, int radiusY,
																	   @Nullable ImageBorder<T> border ) {
		BlurStorageFilter<T> filter = new BlurStorageFilter<>("meanB", type, radiusX, radiusY);
		filter.setBorder(border);
		return filter;
	}

	/**
	 * Creates a Gaussian filter for the specified image type.
	 *
	 * @param type Image type.
	 * @param radius Size of the filter.
	 * @return mean image filter.
	 */
	public static <T extends ImageBase<T>> BlurStorageFilter<T> gaussian( ImageType<T> type, double sigma, int radius ) {
		return new BlurStorageFilter<>("gaussian", type, sigma, radius, sigma, radius);
	}

	public static <T extends ImageGray<T>> BlurStorageFilter<T> gaussian( Class<T> type, double sigma, int radius ) {
		return gaussian(ImageType.single(type), sigma, radius);
	}

	public static <T extends ImageBase<T>> BlurStorageFilter<T>
	gaussian( ImageType<T> type, double sigmaX, int radiusX, double sigmaY, int radiusY ) {
		return new BlurStorageFilter<T>("gaussian", type, sigmaX, radiusX, sigmaY, radiusY);
	}

	public static <T extends ImageGray<T>> BlurStorageFilter<T>
	gaussian( Class<T> type, double sigmaX, int radiusX, double sigmaY, int radiusY ) {
		return gaussian(ImageType.single(type), sigmaX, radiusX, sigmaY, radiusY);
	}
}
