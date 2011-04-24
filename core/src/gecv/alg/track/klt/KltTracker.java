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

/**
 * <p>
 * A Kanade-Lucas-Tomasi (KLT) [1,2,3,4] point feature tracker for a single layer gray scale image.  It tracks point features
 * across a sequence of images by having each feature individually follow the image's gradient.  Feature locations
 * are estimated to within sub-pixel accuracy.
 * </p>
 * <p/>
 * <p>
 * Citations:<br>
 * <br>
 * [1] Bruce D. Lucas and Takeo Kanade.  An Iterative Image Registration Technique with an
 * Application to Stereo Vision.  International Joint Conference on Artificial Intelligence,
 * pages 674-679, 1981.<br>
 * [2] Carlo Tomasi and Takeo Kanade. Detection and Tracking of Point Features. Carnegie
 * Mellon University Technical Report CMU-CS-91-132, April 1991.<br>
 * [3] Jianbo Shi and Carlo Tomasi. Good Features to Track. IEEE Conference on Computer
 * Vision and Pattern Recognition, pages 593-600, 1994.<br>
 * [4] Stan Birchfield, http://www.ces.clemson.edu/~stb/klt/
 * </p>
 *
 * @author Peter Abeles
 */
public class KltTracker {

	// input image
	protected ImageFloat32 image;
	// image gradient
	protected ImageFloat32 derivX, derivY;

	// specifies the type of tracking fault which occurred
	protected KltTrackFault fault;

	public void setImage(ImageFloat32 image) {

	}

	public void setImageDerivative(ImageFloat32 derivX, ImageFloat32 derivY) {

	}

	public boolean getRequiresDerivative() {
		return false;
	}

	public boolean track(KltFeature feature) {
		return true;
	}

	public KltTrackFault getFault() {
		return fault;
	}
}
