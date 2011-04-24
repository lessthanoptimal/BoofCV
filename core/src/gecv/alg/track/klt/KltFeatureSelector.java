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

package gecv.alg.track.klt;

import gecv.struct.image.ImageFloat32;

import java.util.List;

/**
 * Automatically selects KLT features and creates descriptions of them.  There are many different ways to do this
 * for different applications.
 *
 * @author Peter Abeles
 */
public interface KltFeatureSelector {

	/**
	 * Given the image and its gradient select features for the tracker to track.
	 *
	 * @param image  Original image.
	 * @param derivX Image derivative along the x-axis.
	 * @param derivY Image derivative along the y-axis.
	 * @return List of KLT features.
	 */
	public List<KltFeature> select(ImageFloat32 image, ImageFloat32 derivX, ImageFloat32 derivY);
}
