/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

import lombok.Data;

/**
 * Specifies the width and height of an image
 *
 * @author Peter Abeles
 */
@Data
public class ImageDimension {
	public int width;
	public int height;

	public ImageDimension( int width, int height ) {
		this.width = width;
		this.height = height;
	}

	public ImageDimension() {}

	public void setTo( ImageDimension src ) {
		this.width = src.width;
		this.height = src.height;
	}

	public void setTo( int width, int height ) {
		this.width = width;
		this.height = height;
	}

	public boolean isIdentical( ImageDimension a ) {
		return width == a.width && height == a.height;
	}

	@Override
	public String toString() {
		return "ImageDimension{" +
				"width=" + width +
				", height=" + height +
				'}';
	}

	/**
	 * Returns the value of largest side. I.e. max(width,height)
	 */
	public int getMaxLength() {
		return Math.max(width, height);
	}
}
