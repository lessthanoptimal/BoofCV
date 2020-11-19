/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Extracts the RGB color from an image
 *
 * @author Peter Abeles
 */
public interface LookUpColorRgb<T extends ImageBase<T>> {
	/** Sets the input image */
	void setImage( T image );

	/**
	 * Returns the RGB value at the specified pixel. It can be assumed that the coordinate will always be
	 * inside the image.
	 *
	 * @param x x-axis pixel coordinate
	 * @param y y-axis pixel coordinate
	 * @return RGB in 24-bit format
	 */
	int lookupRgb( int x, int y );

	/**
	 * Returns the input image type
	 */
	ImageType<T> getImageType();
}
