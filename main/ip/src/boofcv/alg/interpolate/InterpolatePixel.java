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

import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;


/**
 * Interface for interpolation between pixels on a per-pixel basis.  If a whole rectangular region needs
 * to be interpolated then {@link boofcv.alg.interpolate.InterpolateRectangle} should be considered for performance reasons.
 *
 * @author Peter Abeles
 */
public interface InterpolatePixel<T extends ImageBase> {

	/**
	 * Set's the class used to "read" pixels outside the image border.
	 *
	 * @param border Class for reading outside the image border
	 */
	void setBorder( ImageBorder<T> border );

	/**
	 * Returns the class which handles the image border
	 */
	ImageBorder<T> getBorder();

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
	 * Is the requested pixel inside the image boundary for which fast unsafe interpolation can be performed.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return  true if get_fast() can be called.
	 */
	public boolean isInFastBounds(float x, float y);

	/**
	 * Border around the image that fast interpolation cannot be called.
	 *
	 * @return Border size in pixels
	 */
	public int getFastBorderX();

	/**
	 * Border around the image that fast interpolation cannot be called.
	 *
	 * @return Border size in pixels
	 */
	public int getFastBorderY();

	/**
	 * Type of image it can process
	 */
	public ImageType<T> getImageType();

}
