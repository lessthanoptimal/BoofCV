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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Implements all the functions but does nothing. Primarily for testing
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway"})
public class AbstractInterpolatePixelS<T extends ImageGray<T>> implements InterpolatePixelS<T> {
	protected T image;

	// @formatter:off
	@Override public void setBorder( ImageBorder<T> border ) {}
	@Override public ImageBorder<T> getBorder() {return null;}
	@Override public void setImage( T image ) {this.image = image;}
	@Override public T getImage() {return image;}
	@Override public boolean isInFastBounds( float x, float y ) {return false;}
	@Override public int getFastBorderX() {return 0;}
	@Override public int getFastBorderY() {return 0;}
	@Override public ImageType<T> getImageType() {return image != null ? image.getImageType() : null;}
	@Override public float get( float x, float y ) {return 0;}
	@Override public float get_fast( float x, float y ) {return 0;}
	@Override public InterpolatePixelS<T> copy() {return this;}
	// @formatter:on
}
