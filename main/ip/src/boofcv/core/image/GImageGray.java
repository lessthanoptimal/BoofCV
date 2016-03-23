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

package boofcv.core.image;

import boofcv.struct.image.ImageGray;

/**
 * Generalized interface for single banded images.  Setters and getters which use Number will be much slower than
 * direct access, about 12x.  Setts and getters which use index have a negligible difference to about 25% performance
 * hit depending if the data types need to be converted or not.
 *
 * @author Peter Abeles
 */
public interface GImageGray {

	void wrap( ImageGray image );

	int getWidth();

	int getHeight();

	boolean isFloatingPoint();

	Number get( int x , int y );

	/**
	 * Set's pixel value using number.  If native type of 'num' and image are the same then there is no loss in precision.
	 * @param x pixel coordinate x-value
	 * @param y pixel coordinate y-value
	 * @param num Value of the pixel
	 */
	void set( int x , int y , Number num );

	/**
	 * get which returns a double, has no bounds checking. Still slow, but faster than the super generic get.  Also
	 * doesn't create memory on each get
	 */
	double unsafe_getD(int x, int y);

	float unsafe_getF(int x, int y );

	/**
	 * Sets pixel based on pixel value in data array
	 */
	void set( int index , float value );

	float getF( int index );

	ImageGray getImage();

	Class getImageType();
}
