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
 * Given two rectified images compute the corresponding dense disparity image.  Input images are assumed
 * to be rectified along the x-axis.  Disparity goes from left to right image, thus the x coordinates of pixels
 * in the left and right images have the following relationship x_right = x_left + disparity, and y_right = y_left..
 * To speed up calculations only a limited range of disparities are considered from minDisparity to maxDisparity.
 * Image borders are not processed and the border size is specified by functions below.
 * </p>
 *
 * <p>
 * DISPARITY IMAGE FORMAT: Returned disparity image is offset from the true value by minDisparity.  This
 * is done to maximize storage efficiency.  To get the actual disparity value simply extract the value
 * of a pixel and add minDisparity to it.  Invalid pixels are indicated by having a value greater than
 * (maxDisparity - minDisparity).
 * </p>
 *
 * @see StereoDisparitySparse
 *
 * @author Peter Abeles
 */
public interface StereoDisparity<Image extends ImageGray, Disparity extends ImageGray> {

	/**
	 * Computes stereo disparity.
	 *
	 * @param imageLeft Input left rectified image.
	 * @param imageRight Input right rectified image.
	 */
	public void process( Image imageLeft , Image imageRight );

	/**
	 * Return the computed disparity image.  See comments in class description on disparity image format.
	 *
	 * @return Output disparity from left to right image.
	 */
	public Disparity getDisparity();

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
	 * Type of input images it can process
	 *
	 * @return Input image type
	 */
	public Class<Image> getInputType();

	/**
	 * Type of disparity image it can write to.
	 *
	 * @return Output image type
	 */
	public Class<Disparity> getDisparityType();

}
