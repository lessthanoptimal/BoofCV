/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageSingleBand;


/**
 * Interface for interpolation between pixels on a per-pixel basis.  If a whole rectangular region needs
 * to be interpolated then {@link InterpolateRectangle} should be considered for performance reasons.
 *
 * @author Peter Abeles
 */
public interface InterpolatePixel<T extends ImageSingleBand> {

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
	 * Returns the intensity value of the image at the specified coordinate.
	 * This value is computed using interpolation.  Bounds checking is performed
	 * to make sure a point that can be interpolated inside the image is requested.
	 * If a point can't be interpolated then Float.NaN is returned.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return Interpolated intensity value or NaN if it can't be interpolated.
	 */
	public float get(float x, float y);

	/**
	 * Returns the intensity value of the image at the specified coordinate using
	 * interpolation.  No bounds checks are done to see if it is inside the image
	 * and the image border might not be handled.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return Interpolated intensity value.
	 */
	public float get_unsafe(float x, float y);

	/**
	 * Is the requested pixel inside the image bounds in which get_unsafe() can be called without throwing
	 * an exception?
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return  true if get_unsafe() can be called.
	 */
	public boolean isInSafeBounds( float x , float y );

	/**
	 * Border around the image that {@link #get_unsafe(float, float)} cannot be called.
	 *
	 * @return Border size in pixels
	 */
	public int getUnsafeBorderX();

	/**
	 * Border around the image that {@link #get_unsafe(float, float)} cannot be called.
	 *
	 * @return Border size in pixels
	 */
	public int getUnsafeBorderY();

}
