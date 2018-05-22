/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
 * Base class for images with multiple bands.
 *
 * @author Peter Abeles
 */
public abstract class ImageMultiBand<T extends ImageMultiBand<T>> extends ImageBase<T> {

	/**
	 * Returns the number of bands or colors stored in this image.
	 *
	 * @return Number of bands in the image.
	 */
	public abstract int getNumBands();

	/**
	 * Changes the number of bands in the image while keeping the width and height the same. A simple reshape
	 * is done if possible, if not then new internal data is defined
	 * @param bands number of bands
	 */
	public abstract void setNumberOfBands( int bands );

	/**
	 * Reshape for MultiBand images which allows the number of bands to be changed a the same time too
	 * @param width Desired image width
	 * @param height Desired image height
	 * @param numberOfBands Desired number of bands
	 */
	public void reshape( int width , int height , int numberOfBands ) {
		reshape(width, height);
		setNumberOfBands(numberOfBands);
	}
}
