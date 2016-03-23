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


import boofcv.alg.interpolate.array.PolynomialNevilleFixed_F32;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Polynomial interpolation using {@link PolynomialNevilleFixed_F32 Neville's} algorithm.
 * First interpolation is performed along the horizontal axis, centered at the specified x-coordinate.
 * Then a second pass is done along the vertical axis using the output from the first pass.
 * </p>
 *
 * <p>
 * The code is unoptimized and the algorithm is relatively expensive.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class PolynomialPixel<T extends ImageGray> implements InterpolatePixelS<T> {
	// for reading pixels outside the image border
	protected ImageBorder<T> border;
	// the image that is being interpolated
	protected T image;

	protected int M;
	// if even need to add one to initial coordinate to make sure
	// the point interpolated is bounded inside the interpolation points
	protected int offM;

	// temporary arrays used in the interpolation
	protected float horiz[];
	protected float vert[];

	// the minimum and maximum pixel intensity values allowed
	protected float min;
	protected float max;

	protected PolynomialNevilleFixed_F32 interp1D;

	public PolynomialPixel(int maxDegree, float min, float max) {
		this.M = maxDegree;
		this.min = min;
		this.max = max;
		horiz = new float[maxDegree];
		vert = new float[maxDegree];

		if( maxDegree % 2 == 0 ) {
			offM = 1;
		} else {
			offM = 0;
		}

		interp1D = new PolynomialNevilleFixed_F32(maxDegree);
	}

	@Override
	public void setBorder(ImageBorder<T> border) {
		this.border = border;
	}

	@Override
	public void setImage(T image) {
		if( border != null )
			border.setImage(image);
		this.image = image;
	}

	@Override
	public T getImage() {
		return image;
	}

	@Override
	public boolean isInFastBounds(float x, float y) {
		float x0 = x - M/2 + offM;
		float x1 = x0 + M;
		float y0 = y - M/2 + offM;
		float y1 = y0 + M;

		return (x0 >= 0 && y0 >= 0 && x1 <= image.width-1 && y1 <= image.height-1 );
	}

	@Override
	public int getFastBorderX() {
		return M;
	}

	@Override
	public int getFastBorderY() {
		return M;
	}

	@Override
	public ImageBorder<T> getBorder() {
		return border;
	}
}
