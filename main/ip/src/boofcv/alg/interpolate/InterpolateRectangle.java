/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

/**
 * Performs interpolation across a whole rectangular region inside the image.  This can be significantly faster than
 * interpolating on a per-pixel basis.
 *
 * @author Peter Abeles
 */
public interface InterpolateRectangle<T extends ImageGray> {

	/**
	 * Change the image that is being interpolated.
	 *
	 * @param image An image.
	 */
	public void setImage(T image);

	/**
	 * Returns the image which is being interpolated.
	 *
	 * @return A reference to the image being interpolated.
	 */
	public T getImage();

	/**
	 * Copies a grid from the source image starting at the specified coordinate
	 * into the destination image.  The 'dest' image must be within the original image.
	 *
	 * @param tl_x	  upper left corner of the region in the image.
	 * @param tl_y	  upper left corner of the region in the image.
	 * @param dest Where the interpolated region is to be copied into
	 */
	public void region(float tl_x, float tl_y, GrayF32 dest );
//	public void region(float tl_x, float tl_y, float[] results, int regWidth, int regHeight);
}
