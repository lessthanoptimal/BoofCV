/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.filter.blur;

import gecv.abst.filter.blur.impl.BlurStorageFilter;
import gecv.abst.filter.blur.impl.MedianImageFilter;
import gecv.struct.image.ImageBase;

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
	public static <T extends ImageBase> MedianImageFilter<T> median( Class<T> type , int radius ) {
		return new MedianImageFilter<T>(type,radius);
	}

	/**
	 * Creates a mean filter for the specified image type.
	 *
	 * @param type Image type.
	 * @param radius Size of the filter.
	 * @return mean image filter.
	 */
	public static <T extends ImageBase> BlurStorageFilter<T> mean( Class<T> type , int radius ) {
		return new BlurStorageFilter<T>("mean",type,radius);
	}

	/**
	 * Creates a Gaussian filter for the specified image type.
	 *
	 * @param type Image type.
	 * @param radius Size of the filter.
	 * @return mean image filter.
	 */
	public static <T extends ImageBase> BlurStorageFilter<T> gaussian( Class<T> type , int radius ) {
		return new BlurStorageFilter<T>("gaussian",type,radius);
	}
}
