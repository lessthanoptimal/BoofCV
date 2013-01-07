/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;

import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Point feature tracker that automatically selects and creates tracks upon request.
 * </p>
 *
 * @author Peter Abeles
 */
public interface PointTrackerSpawn<T extends ImageBase> extends PointTracker<T> {

	/**
	 * Automatically selects new features in the image to track. Returned tracks must
	 * be unique and not duplicates of any existing tracks.  This includes both active
	 * and inactive tracks.
	 *
	 * NOTE: This function may or may not also modify the active and inactive lists.
	 */
	public void spawnTracks();

}

