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

package boofcv.alg.interpolate;

import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

/**
 *  Performs nearest neighbor interpolation to extract values between pixels in an image.
 *
 * @author Peter Abeles
 */
public abstract class NearestNeighborPixelMB<T extends ImageMultiBand> implements InterpolatePixelMB<T> {
	protected ImageBorder<T> border;
	protected T orig;
	protected int stride;
	protected int width;
	protected int height;

	@Override
	public void setBorder(ImageBorder<T> border) {
		this.border = border;
	}

	@Override
	public void setImage(T image) {
		if( border != null )
			border.setImage(image);
		this.orig = image;
		this.stride = orig.getStride();
		this.width = orig.getWidth();
		this.height = orig.getHeight();
	}

	@Override
	public T getImage() {
		return orig;
	}

	@Override
	public boolean isInFastBounds(float x, float y) {
		return( x >= 0 && y >= 0 && x <= width-1 && y <= height-1 );
	}

	@Override
	public int getFastBorderX() {
		return 0;
	}

	@Override
	public int getFastBorderY() {
		return 0;
	}

	@Override
	public ImageBorder<T> getBorder() {
		return border;
	}

	@Override
	public ImageType<T> getImageType() {
		return orig.getImageType();
	}
}
