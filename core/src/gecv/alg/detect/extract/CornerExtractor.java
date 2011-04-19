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
 * An interface used to get new corners.  This abstraction is done to make it easier
 * to attach more complex data structures to corners, but have the corner detection
 * algorithm unaware.
 *
 * @author Peter Abeles
 */
public interface CornerExtractor {

	/**
	 * Process a feature intensity image to extract the point features
	 *
	 * @param intenImg	Feature intensity image.
	 * @param features	List of feature indexes that were found.  Set to null if not provided.
	 * @param numFeatures Number of features that were set in the feature list.
	 * @param corners	 Where the corners that it found are written to.
	 */
	public void process(ImageFloat32 intenImg, int features[], int numFeatures,
						QueueCorner corners);
}
