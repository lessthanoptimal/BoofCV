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

package boofcv.alg.sfm.overhead;

import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Data structure for an overhead orthogonal view with known metric properties.  Each pixel in the overhead view corresponds
 * to a square of known size and location in the world.  The each size of the square regions is specified
 * by cellSize and the origin by (centerX,centerY).
 * </p>
 *
 * <p>
 * The overhead +x axis corresponds to the world's +z axis and the image's +y axis corresponds to the world's -x axis.
 * The user specify the origin by changing centerX and centerY parameters.  It is common to set centerX = 0 ,
 * centerY = output.height*cellSize/2.0.
 * </p>
 *
 * <p>
 * overhead pixels to world coordinates: (x,y) = (x_p,y_p)*cellSize - (centerX,centerY)<br>
 * world coordinates to overhead pixels: (x_p,y_p) = [(x,y) - (centerX,centerY)]/cellSize<br>
 * </p>
 *
 * <p>
 * Notes:
 * <ul>
 *     <li>When rendering the overhead image objects to the left will appear on the right and the other way around.  This
 * is an artifact that in image's it's standard for +y to point down (clock-wise of +x), while on 2D maps the standard
 * is +y being counter-clock-wise of +x.</li>
 * </ul>
 * </p>

 *
 * @author Peter Abeles
 */
public class OverheadView<T extends ImageBase> {

	/**
	 * Image containing the overhead view.
	 */
	public T image;

	/**
	 * X-coordinate of camera center in the overhead image in world units.
	 */
	public double centerX;

	/**
	 * Y-coordinate of camera center in the overhead image in world units.
	 */
	public double centerY;

	/**
	 * Size of each cell in the overhead image in world units.
	 */
	public double cellSize;

	public OverheadView() {
	}

	public OverheadView(T image, double centerX, double centerY, double cellSize) {
		this.image = image;
		this.centerX = centerX;
		this.centerY = centerY;
		this.cellSize = cellSize;
	}

	public T getImage() {
		return image;
	}

	public void setImage(T image) {
		this.image = image;
	}

	public double getCenterX() {
		return centerX;
	}

	public void setCenterX(double centerX) {
		this.centerX = centerX;
	}

	public double getCenterY() {
		return centerY;
	}

	public void setCenterY(double centerY) {
		this.centerY = centerY;
	}

	public double getCellSize() {
		return cellSize;
	}

	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}
}
