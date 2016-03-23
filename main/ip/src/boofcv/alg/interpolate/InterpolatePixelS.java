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

import boofcv.struct.image.ImageGray;


/**
 * Interface for interpolation between pixels on a per-pixel basis for a single band image.
 *
 * @author Peter Abeles
 */
public interface InterpolatePixelS<T extends ImageGray> extends InterpolatePixel<T> {

	/**
	 * Returns the interpolated pixel value at the specified location while checking to see if
	 * border conditions apply.  If the requested pixel is outside the image border it will attempt
	 * to process it using or throw a null pointer exception of a border handler has not been specified.
	 *
	 * @param x Point's x-coordinate. x &ge; 0 && x < image.width or all values if border specified
	 * @param y Point's y-coordinate. y &ge; 0 && y < image.height or all values if border specified
	 * @return Interpolated intensity value or NaN if it can't be interpolated.
	 */
	public float get(float x, float y);

	/**
	 * Returns the interpolated pixel value at the specified location while assuming it is inside
	 * the image far away from the border.  For any input point {@link #isInFastBounds} should return true.
	 *
	 * @param x Point's x-coordinate.
	 * @param y Point's y-coordinate.
	 * @return Interpolated intensity value.
	 */
	public float get_fast(float x, float y);
}
