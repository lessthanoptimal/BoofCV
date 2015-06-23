/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

/**
 * Types of adjustments that can be done to an undistorted image.
 *
 * @author Peter Abeles
 */
public enum AdjustmentType {
	/**
	 * Don't adjust the view for visibility
	 */
	NONE,
	/**
	 * The undistorted view will contain all the pixels in the original distorted image. No information is lost but
	 * you have black borders to deal with
	 */
	FULL_VIEW,
	/**
	 * The undistorted view will be entirely filled with pixels from the distorted view.  There will be no black
	 * regions around the border.  This is accomplished by expanding the image.  The advantage is that there are no
	 * edge conditions when image processing due to the black border, but you will discard information.
	 */
	EXPAND
}
