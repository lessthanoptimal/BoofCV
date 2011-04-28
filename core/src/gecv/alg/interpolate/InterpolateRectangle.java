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

package gecv.alg.interpolate;

import gecv.struct.image.ImageBase;

/**
 * Performs interpolation across a whole rectangular region inside the image.  This can be significantly faster than
 * interpolating on a per-pixel basis.
 *
 * @author Peter Abeles
 */
public interface InterpolateRectangle<T extends ImageBase> {

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
	 * There is a certain amount of overhead involved with repeat calls to get.  If a grid
	 * of points is being copied then this can be avoided by calling this function.  Here
	 * it is assumed that a set of points needs to be copied that are all one pixel apart.
	 *
	 * @param tl_x	  upper left corner of the region in the image.
	 * @param tl_y	  upper left corner of the region in the image.
	 * @param results   The subregion's data
	 * @param regWidth  subregion width
	 * @param regHeight subregion height
	 */
	// TODO change the output into an image
	public void region(float tl_x, float tl_y, float[] results, int regWidth, int regHeight);
}
