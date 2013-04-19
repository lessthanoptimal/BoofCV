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

package boofcv.abst.sfm;

/**
 * <p>
 * Generalized interface for sensors which allow pixels in an image to be converted into
 * 3D world coordinates.  3D points are returned in homogeneous coordinates so that
 * points at infinity can be handled.  To convert points into 3D coordinates simply divide
 * each number by 'w'.
 * </p>
 *
 * <p>
 * Homogeneous to 3D coordinates:<br>
 * 3D: (x',y',z') = (x/w, y/w , z/w)
 * </p>
 *
 * Examples of sensors: stereo cameras, flash LADAR, and structured light.
 *
 * @author Peter Abeles
 */
public interface ImagePixelTo3D {

	/**
	 * Estimate the location of the pixel in 3D camera coordinates.
	 *
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @return true if a position could be estimated and false if not.
	 */
	public boolean process( double x , double y );

	/**
	 * Found x-coordinate of point in camera coordinate system.
	 *
	 * @return x-coordinate
	 */
	public double getX();

	/**
	 * Found y-coordinate of point in camera coordinate system.
	 *
	 * @return y-coordinate
	 */
	public double getY();
	/**
	 * Found z-coordinate of point in camera coordinate system.
	 *
	 * @return z-coordinate
	 */
	public double getZ();

	/**
	 * Found w-coordinate of point in camera coordinate system.  If a point
	 * is at infinity then this value will be zero.
	 *
	 * @return w-coordinate
	 */
	public double getW();
}
