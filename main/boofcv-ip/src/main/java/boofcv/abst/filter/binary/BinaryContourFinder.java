/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayU8;

/**
 * Interface for finding contours around binary blobs.
 * To get the points in a contour invoke {@link #loadContour} with
 * the ID of the contour you wish to load. Adjusting the max contour size is useful in
 * situations were memory is limited. Same for turning off inner contours.
 *
 * NOTE: Contours which are too small or too large are filtered out. This is different from the labeled
 * variant of this class. The difference is because there is no labeled image.
 *
 * Defaults:
 * <ul>
 *     <li>{@link ConnectRule#FOUR}</li>
 *     <li>Inner Contours Enabled</li>
 *     <li>Infinite Contour Size</li>
 * </ul>
 *
 * @see boofcv.alg.filter.binary.LinearExternalContours
 *
 * @author Peter Abeles
 */
public interface BinaryContourFinder extends BinaryContourInterface {

	/**
	 * Processes the binary image to find the contour. If you let the input be modified you really need to read up
	 * on how the contour algorithm works. Setting the outside border to zero is typical
	 *
	 * @param binary Input binary image. Not modified.
	 */
	void process(GrayU8 binary);
}

