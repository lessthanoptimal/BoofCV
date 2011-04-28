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
 * Interface for interpolation between pixels on a per-pixel basis.  If a whole rectangular region needs
 * to be interpolated then {@link InterpolateRectangle} should be considered for performance reasons.
 *
 * @author Peter Abeles
 */
public interface InterpolatePixel<T extends ImageBase> {

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
	 * Returns true of the point is inside the image.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return True if it is inside the image and false if it is not.
	 */
	public boolean inBounds(float x, float y);

	/**
	 * Returns the intensity value of the image at the specified coordinate.
	 * This value is computed using interpolation.  Bounds checking is performed
	 * to make sure a point that can be interpolated inside the image is requested.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return Interpolated intensity value.
	 */
	public float get(float x, float y);

	/**
	 * Returns the intensity value of the image at the specified coordinate.
	 * This value is computed using interpolation.  No checks are done to make
	 * sure
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return Interpolated intensity value.
	 */
	public float get_unsafe(float x, float y);

	/**
	 * Edge conditions are often a problem for interpolation algorithms.  This returns
	 * the border around the image that unsafe will fail on.
	 *
	 * @return [top,right,bottom,left] in pixels
	 */
	public int[] getBorderOffsets();
}
