/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3.structure;

import boofcv.abst.tracker.PointTracker;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;

/**
 * Decides when new key frames should be created and when an old key frame should be removed
 *
 * @author Peter Abeles
 */
public interface VisOdomKeyFrameManager extends VerbosePrint {

	/**
	 * Specifies the size of the image and resets the manager to its initial state
	 */
	void configure( int imageWidth , int imageHeight );

	/**
	 * Selects frames to discard from the scene graph. The most recent is assumed to be the current tracker frame.
	 *
	 * @param tracker Feature tracker
	 * @param sba scene graph
	 * @return Returns a list of frames to discard. They are in sequential order from least to greatest.
	 */
	GrowQueue_I32 selectFramesToDiscard( PointTracker<?> tracker , VisOdomBundleAdjustment<?> sba);

	/**
	 * After the current frame becomes a keyframe new tracks are spawned from it. This passes in that new information
	 */
	void handleSpawnedTracks( PointTracker<?> tracker );
}
