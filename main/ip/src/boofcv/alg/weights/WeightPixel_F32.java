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

package boofcv.alg.weights;

/**
 * Used to get the weight for a pixel from a kernel in 2D space.  The kernel is assumed to be centered around 0
 * and has the specified radius.  The kernel has a width of radius*2 + 1.  The information stored inside
 * this interface could be represented as a 2D array, but the interface allows a simple return to be used
 * if the value is constant.
 *
 * @author Peter Abeles
 */
public interface WeightPixel_F32 {

	/**
	 * <p>
	 * Faster way to access the weight.  Refers to the index in a row major matrix.<br>
	 * <br>
	 * x = (index % widthX) - radiusX <br>
	 * y = (index / widthX) - radiusY <br>
	 * </p>
	 *
	 * @param index index of grid element
	 * @return the weight
	 */
	public float weightIndex( int index );

	/**
	 * Access the weight using coordinates.
	 *
	 * @param x x-coordinate: range = -radius to radius, inclusive
	 * @param y y-coordinate: range = -radius to radius, inclusive
	 * @return the weight
	 */
	public float weight( int x , int y );

	/**
	 * Change the kernel's size
	 *
	 * @param radiusX Radius along x-axis
	 * @param radiusY Radius along y-axis
	 */
	public void setRadius( int radiusX , int radiusY );

	/**
	 * Returns the kernel's radius along the x-axis
	 * @return Radius of kernel
	 */
	public int getRadiusX();

	/**
	 * Returns the kernel's radius along the y-axis
	 * @return Radius of kernel
	 */
	public int getRadiusY();
}
