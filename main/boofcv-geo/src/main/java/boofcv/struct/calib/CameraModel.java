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

package boofcv.struct.calib;

import boofcv.struct.image.ImageDimension;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Common class for camera models
 *
 * @author Peter Abeles
 */
public abstract class CameraModel implements Serializable {
	/** image shape (units: pixels) */
	public int width, height;

	public int getWidth() {
		return width;
	}

	public void setWidth( int width ) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight( int height ) {
		this.height = height;
	}

	public ImageDimension getDimension( @Nullable ImageDimension dimension ) {
		if (dimension == null)
			dimension = new ImageDimension();
		dimension.setTo(width, height);
		return dimension;
	}

	/**
	 * Creates a new camera model with zero values of the same type os this one
	 */
	public abstract <T extends CameraModel> T createLike();

	/**
	 * Prints a summary of this model to stdout
	 */
	public void print() {
		System.out.println(this);
	}
}
