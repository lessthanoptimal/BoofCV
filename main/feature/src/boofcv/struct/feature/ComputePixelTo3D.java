/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import boofcv.struct.image.ImageBase;

/**
 * Returns the 3D coordinate of a point in camera coordinates.  Abstraction for optical
 * sensors with ranging capability, e.g. stereo vision or structured light.
 *
 * @author Peter Abeles
 */
public interface ComputePixelTo3D<T extends ImageBase> {

	/**
	 * Specify input images used to compute range.  If the input images
	 * are assumed to be rectified or not is implementation specific.
	 *
	 * @param leftImage Left input image.
	 * @param rightImage Right input image.
	 */
	public void setImages( T leftImage , T rightImage );

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
}
