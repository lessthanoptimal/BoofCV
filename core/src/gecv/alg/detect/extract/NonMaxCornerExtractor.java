/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.extract;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;

/**
 * Extracts features from the intensity image by finding local maximums.  Previously found corners are automatically
 * excluded and not added again.  Features below an intensity threshold are automatically ignored and no two
 * features can be closer than the minSeparation apart.
 *
 * @author Peter Abeles
 */
public interface NonMaxCornerExtractor {

	/**
	 * Sets the minimum distance two features can be.  This is the local region which is searched in non-max
	 * suppression.
	 *
	 * @param minSeparation How close two features can be.
	 */
	public void setMinSeparation(int minSeparation);

	/**
	 * The minimum intensity a feature can have.
	 *
	 * @param thresh Minimum intensity a feature can have to be valid.
	 */
	public void setThresh(float thresh);

	/**
	 * Detects corners in the image while excluding corners which are already contained in the corners list.
	 *
	 * @param intensityImage Feature intensity image. Can be modified.
	 * @param corners		Where found corners are stored.  Corners which are already in the list will not be added twice.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner corners);
}
