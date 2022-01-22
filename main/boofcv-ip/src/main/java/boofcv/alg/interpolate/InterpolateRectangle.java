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

package boofcv.alg.interpolate;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Performs interpolation across a whole rectangular region inside the image. This can be significantly faster than
 * interpolating on a per-pixel basis.
 *
 * @author Peter Abeles
 */
public interface InterpolateRectangle<T extends ImageGray<T>> {
	/**
	 * Change the image that is being interpolated.
	 *
	 * @param image An image.
	 */
	void setImage( T image );

	/**
	 * Returns the image which is being interpolated.
	 *
	 * @return A reference to the image being interpolated.
	 */
	T getImage();

	/**
	 * Copies a grid from the source image starting at the specified coordinate
	 * into the destination image. The 'dest' image must be within the original image.
	 *
	 * @param tl_x upper left corner of the region in the image.
	 * @param tl_y upper left corner of the region in the image.
	 * @param dest Where the interpolated region is to be copied into
	 */
	void region( float tl_x, float tl_y, GrayF32 dest );

	/** Creates a copy that can be run in parallel with the original */
	InterpolateRectangle<T> copyConcurrent();

	/** Type of image it can process */
	ImageType<T> getImageType();
}
