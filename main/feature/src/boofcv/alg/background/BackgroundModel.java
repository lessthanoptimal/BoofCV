/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Base class for background subtraction/motion detection.
 *
 * @author Peter Abeles
 */
public abstract class BackgroundModel<T extends ImageBase> {

	// type of input image
	protected ImageType<T> imageType;

	// value assigned to pixels outside the image.  Default to 0, which is background
	protected byte unknownValue = 0;

	public BackgroundModel(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	/**
	 * Resets model to its original state
	 */
	public abstract void reset();


	/**
	 * Returns the value that pixels in the segmented image are assigned if there is no background information.
	 *
	 * @return Value for unknown
	 */
	public int getUnknownValue() {
		return unknownValue & 0xff;
	}

	/**
	 * Specify the value of a segmented pixel which has no corresponding pixel in the background image.
	 * @param unknownValue Value for pixels with out a background pixel. 2 to 255, inclusive.
	 */
	public void setUnknownValue(int unknownValue) {
		if( unknownValue < 2 || unknownValue > 255 )
			throw new IllegalArgumentException("out of range. 2 to 255");
		this.unknownValue = (byte)unknownValue;
	}

	/**
	 * Type of input image it can process
	 */
	public ImageType<T> getImageType() {
		return imageType;
	}
}
