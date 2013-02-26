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

package boofcv.struct.image;

import boofcv.core.image.GeneralizedImageOps;

/**
 * Specifies the type of image data structure.
 *
 * @author Peter Abeles
 */
public class ImageDataType<T extends ImageBase> {

	Family family;
	ImageTypeInfo dataType;

	public ImageDataType(Family family, ImageTypeInfo dataType) {
		this.family = family;
		this.dataType = dataType;
	}

	public ImageTypeInfo getDataType() {
		return dataType;
	}

	/**
	 * Creates a new image.  If its a single band image then the numBands parameter is ignored.
	 *
	 * @param width Number of colums in the image.
	 * @param height Number of rows in the image.
	 * @param numBands Number of bands.  Ignored for single band images.
	 * @return New instance of the image.
	 */
	public T createImage( int width , int height , int numBands ) {
		switch( family ) {
			case SINGLE_BAND:
				return (T)GeneralizedImageOps.createSingleBand(dataType.getImageClass(),width,height);

			case MULTI_SPECTRAL:
				return (T)new MultiSpectral(dataType.getImageClass(),width,height,numBands);

			default:
				throw new IllegalArgumentException("Type not yet supported");
		}
	}

	public Family getFamily() {
		return family;
	}

	public static enum Family
	{
		SINGLE_BAND,
		MULTI_SPECTRAL,
		INTERLEAVED
	}
}
