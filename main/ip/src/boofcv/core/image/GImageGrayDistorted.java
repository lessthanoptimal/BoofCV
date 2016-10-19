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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class GImageGrayDistorted<T extends ImageGray> implements GImageGray {

	PixelTransform2_F32 transform;
	InterpolatePixelS<T> interpolate;

	int inputWidth,inputHeight;

	public GImageGrayDistorted(PixelTransform2_F32 transform,
							   InterpolatePixelS<T> interpolate) {
		this.transform = transform;
		this.interpolate = interpolate;
	}

	@Override
	public void wrap(ImageGray image) {
		interpolate.setImage((T)image);

		inputWidth = image.getWidth();
		inputHeight = image.getHeight();
	}

	@Override
	public int getWidth() {
		return inputWidth;
	}

	@Override
	public int getHeight() {
		return inputHeight;
	}

	@Override
	public boolean isFloatingPoint() {
		return true;
	}

	@Override
	public Number get(int x, int y) {
		transform.compute(x,y);
		return interpolate.get(transform.distX, transform.distY);
	}

	@Override
	public void set(int x, int y, Number num) {
		throw new IllegalArgumentException("set is not supported");
	}

	@Override
	public double unsafe_getD(int x, int y) {
		transform.compute(x,y);
		return interpolate.get(transform.distX, transform.distY);
	}

	@Override
	public float unsafe_getF(int x, int y) {
		transform.compute(x,y);
		return interpolate.get(transform.distX, transform.distY);
	}

	@Override
	public void set(int index, float value) {
		throw new IllegalArgumentException("set is not supported");
	}

	@Override
	public float getF(int index) {
		throw new IllegalArgumentException("getF is not supported");
	}

	@Override
	public ImageGray getImage() {
		throw new IllegalArgumentException("getImage() is not supported");
	}

	@Override
	public Class getImageType() {
		throw new IllegalArgumentException("getImageType() is not supported");
	}
}
