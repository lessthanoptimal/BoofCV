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

package boofcv.abst.feature.disparity;

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Computes the disparity between two rectified images at specified points only.
 * </p>
 *
 * <p>
 * NOTE: Unlike for dense images, the returned disparity is the actual disparity.  No need to add minDisparity
 * to the returned value.
 * </p>
 *
 * @see StereoDisparity
 *
 * @author Peter Abeles
 */
public interface StereoDisparitySparse<Image extends ImageGray> {

	/**
	 * Sets the input images that are to be processed.
	 *
	 * @param imageLeft Input left rectified image.
	 * @param imageRight Input right rectified image.
	 */
	public void setImages( Image imageLeft , Image imageRight );

	/**
	 * Calculates the disparity at the specified point.  Returns true if a valid
	 * correspondence was found between the two images.
	 *
	 * @param x center of region x-axis
	 * @param y center of region y-axis
	 * @return true if a correspondence was found
	 */
	public boolean process( int x  , int y );

	/**
	 * The found disparity at the selected point
	 *
	 * @return disparity.
	 */
	public double getDisparity();

	/**
	 * Border around the image's x-axis which is not processed.
	 * @return border x-axis
	 */
	public int getBorderX();

	/**
	 * Border around the image's y-axis which is not processed.
	 * @return border y-axis
	 */
	public int getBorderY();

	/**
	 * The minimum disparity which will be checked for.
	 *
	 * @return Minimum disparity.
	 */
	public int getMinDisparity();

	/**
	 * The maximum disparity which will be checked for.
	 *
	 * @return Maximum disparity.
	 */
	public int getMaxDisparity();

	/**
	 * Type of input images it can process
	 *
	 * @return Input image type
	 */
	public Class<Image> getInputType();
}
