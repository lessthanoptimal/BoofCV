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

package boofcv.abst.feature.disparity;

import boofcv.struct.image.ImageSingleBand;

/**
 * Given two rectified image compute corresponding dense disparity image.  Input images are assumed
 * to be rectified (epipoles are at infinity) along the x-axis, with the left image being to the
 * left (minus x-axis) of the right image.  A disparity of zero indicates no difference between
 * the two pixels or no corresponding match could be found.  Disparity goes from let to right image.
 *
 * @author Peter Abeles
 */
public interface StereoDisparity<Image extends ImageSingleBand, Disparity extends ImageSingleBand> {

	/**
	 * Computes stereo disparity.
	 *
	 * @param imageLeft Input left rectified image.
	 * @param imageRight Input right rectified image.
	 * @param output Output disparity from left to right image.
	 */
	public void process( Image imageLeft , Image imageRight , Disparity output );


	public int getMaxDisparity();

	public Class<Image> getInputType();

	public Class<Disparity> getDisparityType();

}
