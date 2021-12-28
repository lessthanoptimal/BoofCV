/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.struct.image.ImageBase;

/**
 * Extension of {@link FiducialDetector} which allows for trackers. A tracker will use previous observations
 * to improve results in the current observations. Past history can be removed by calling {@link #reset()}
 *
 * @author Peter Abeles
 */
public interface FiducialTracker<T extends ImageBase<T>> extends FiducialDetector<T> {

	/**
	 * Detects and tracks fiducials inside the image. Since it is a tracker it is assumed that a sequence
	 * of images is being processed. Order of images will matter.
	 *
	 * @param input Input image. Not modified.
	 */
	@Override
	void detect( T input );

	/**
	 * Removes all past history from the tracker and sets it back into its current state.
	 */
	void reset();
}
