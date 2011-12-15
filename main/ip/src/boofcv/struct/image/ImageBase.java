/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import georegression.struct.point.Point2D_I32;

import java.io.Serializable;

/**
 * Base class for all image types.
 *
 * @author Peter Abeles
 */
public abstract class ImageBase<T extends ImageBase> implements Serializable, Cloneable {
	/**
	 * Index of the first pixel in the data array
	 */
	public int startIndex;
	/**
	 * How many elements need to be skipped over to go one row down.
	 */
	public int stride;

	/**
	 * Number of columns in the image.
	 */
	public int width;
	/**
	 * Number of rows in the image.
	 */
	public int height;

	public abstract T subimage(int x0, int y0, int x1, int y1);

	public abstract void reshape(int width, int height);

	/**
	 * If this matrix is a sub-image or not.
	 *
	 * @return true if it is a subimage, otherwise false.
	 */
	public boolean isSubimage() {
		return startIndex != 0 || width != stride;
	}

	public final boolean isInBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public int getIndex(int x, int y) {
		return startIndex + y * stride + x;
	}

	public final int getWidth() {
		return width;
	}

	public final void setWidth(int width) {
		this.width = width;
	}

	public final int getHeight() {
		return height;
	}

	public final void setHeight(int height) {
		this.height = height;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getStride() {
		return stride;
	}

	public void setStride(int stride) {
		this.stride = stride;
	}

	public Point2D_I32 indexToPixel( int index )
	{
		index -= startIndex;
		return new Point2D_I32( index % stride , index / stride );
	}
}
