/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BCamera;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * This key frame manager performs its maintenance at a constant fixed rate independent of observations.
 *
 * @author Peter Abeles
 */
public class TickTockKeyFrameManager implements VisOdomKeyFrameManager {
	/**
	 * The period at which the current frame is turned into a new keyframe
	 */
	public int keyframePeriod = 1;

	// list of frames to discard
	private final DogArray_I32 keyframeIndexes = new DogArray_I32();

	public TickTockKeyFrameManager( int keyframePeriod ) {
		this.keyframePeriod = keyframePeriod;
	}

	public TickTockKeyFrameManager() {
	}

	/**
	 * No need to configure or initialize anything
	 */
	@Override
	public void initialize( FastAccess<BCamera> cameras ) {}

	@Override
	public DogArray_I32 selectFramesToDiscard( PointTracker<?> tracker, int maxKeyFrames, int newFrames, VisOdomBundleAdjustment<?> sba ) {
		keyframeIndexes.reset();
		// Add key frames until it hits the max
		if (sba.frames.size <= maxKeyFrames)
			return keyframeIndexes;

		// See if the current keyframe should be removed from the list and prevent it from becoming a real keyframe
		boolean removeCurrent = tracker.getFrameID()%keyframePeriod != 0;
		maxKeyFrames += removeCurrent ? newFrames : 0;

		// Remove older keyframes until it has the correct number of keyframes
		for (int i = 0; i < sba.frames.size - maxKeyFrames; i++) {
			keyframeIndexes.add(i);
		}

		// Now remove the current frames. This is done at the end to ensure the order of key frame indexes
		if (removeCurrent) {
			for (int i = newFrames - 1; i >= 0; i--) {
				keyframeIndexes.add(sba.frames.size - 1 - i);
			}
		}

		return keyframeIndexes;
	}

	/**
	 * Tracker information is ignored
	 */
	@Override
	public void handleSpawnedTracks( PointTracker<?> tracker, BCamera camera ) {}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {}
}
