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

package boofcv.alg.interpolate;

import boofcv.struct.border.ImageBorder;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;

/**
 * Applies distortion to a coordinate then samples the image
 * with interpolation.
 *
 * @author Peter Abeles
 */
public class InterpolatePixelDistortS<T extends ImageGray<T>>
		implements InterpolatePixelS<T> {
	protected InterpolatePixelS<T> interpolate;
	protected Point2Transform2_F32 distorter;

	Point2D_F32 p = new Point2D_F32();

	public InterpolatePixelDistortS( InterpolatePixelS<T> interpolate,
									 Point2Transform2_F32 distorter ) {
		this.interpolate = interpolate;
		this.distorter = distorter;
	}

	@Override
	public float get( float x, float y ) {
		distorter.compute(x, y, p);
		return interpolate.get(p.x, p.y);
	}

	@Override
	public float get_fast( float x, float y ) {
		distorter.compute(x, y, p);
		return interpolate.get_fast(p.x, p.y);
	}

	@Override
	public InterpolatePixelS<T> copy() {
		throw new RuntimeException("Implement");
	}

	@Override
	public void setBorder( ImageBorder<T> border ) {
		interpolate.setBorder(border);
	}

	@Override
	public ImageBorder<T> getBorder() {
		return interpolate.getBorder();
	}

	@Override
	public void setImage( T image ) {
		interpolate.setImage(image);
	}

	@Override
	public T getImage() {
		return interpolate.getImage();
	}

	@Override
	public boolean isInFastBounds( float x, float y ) {
		return interpolate.isInFastBounds(x, y);
	}

	@Override
	public int getFastBorderX() {
		return interpolate.getFastBorderX();
	}

	@Override
	public int getFastBorderY() {
		return interpolate.getFastBorderY();
	}

	@Override
	public ImageType<T> getImageType() {
		return interpolate.getImageType();
	}

	public InterpolatePixelS<T> getInterpolate() {
		return interpolate;
	}

	public Point2Transform2_F32 getDistorter() {
		return distorter;
	}
}
