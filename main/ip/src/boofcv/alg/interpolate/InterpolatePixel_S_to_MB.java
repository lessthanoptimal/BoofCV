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

package boofcv.alg.interpolate;

import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper that allows a {@link InterpolatePixelS} to be used as a {@link InterpolatePixelMB},
 * input image has to be {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class InterpolatePixel_S_to_MB<T extends ImageGray> implements InterpolatePixelMB<T>
{
	InterpolatePixelS<T> interp;

	public InterpolatePixel_S_to_MB(InterpolatePixelS<T> interp) {
		this.interp = interp;
	}

	@Override
	public void get(float x, float y, float[] values) {
		values[0] = interp.get(x,y);
	}

	@Override
	public void get_fast(float x, float y, float[] values) {
		values[0] = interp.get_fast(x, y);
	}

	@Override
	public void setBorder(ImageBorder<T> border) {
		interp.setBorder(border);
	}

	@Override
	public ImageBorder<T> getBorder() {
		return interp.getBorder();
	}

	@Override
	public void setImage(T image) {
		interp.setImage(image);
	}

	@Override
	public T getImage() {
		return interp.getImage();
	}

	@Override
	public boolean isInFastBounds(float x, float y) {
		return interp.isInFastBounds(x,y);
	}

	@Override
	public int getFastBorderX() {
		return interp.getFastBorderX();
	}

	@Override
	public int getFastBorderY() {
		return interp.getFastBorderY();
	}

	@Override
	public ImageType<T> getImageType() {
		return interp.getImageType();
	}
}
