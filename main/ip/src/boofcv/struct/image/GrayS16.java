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

package boofcv.struct.image;

/**
 * <p>
 * Image with a pixel type of signed 16-bit integer.
 * </p>
 *
 * @author Peter Abeles
 */
public class GrayS16 extends GrayI16<GrayS16> {
	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public GrayS16(int width, int height) {
		super(width, height);
	}

	/**
	 * Creates an image with no data declared and the width/height set to zero.
	 */
	public GrayS16() {
	}

	@Override
	public int unsafe_get(int x, int y) {
		return data[getIndex(x, y)];
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.S16;
	}

	@Override
	public GrayS16 createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new GrayS16();
		return new GrayS16(imgWidth, imgHeight);
	}
}
