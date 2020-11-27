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

import boofcv.abst.tracker.PointTrackerDefault;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestTickTockKeyFrameManager extends ChecksVisOdomKeyFrameManager {
	int maxKeyFrames = 5;

	@Test
	void alwaysAddBeforeMax() {
		var tracker = new DummyTracker();
		VisOdomBundleAdjustment<BTrack> scene = createScene();

		var alg = new TickTockKeyFrameManager();
		alg.keyframePeriod = 10000; // don't want it to add a new keyframe right afterwards

		// not used but call it just in case that changes in the future
		alg.initialize(scene.cameras);
		// add the initial set of frames
		for (int i = 0; i < 5; i++) {
			DogArray_I32 discard = alg.selectFramesToDiscard(tracker, maxKeyFrames, 1, scene);
			assertEquals(0, discard.size);
			scene.addFrame(i);
			tracker.process(null);
		}
		// add one more frame. It should now want to discard the current frame
		scene.addFrame(6);
		tracker.process(null);
		DogArray_I32 discard = alg.selectFramesToDiscard(tracker, maxKeyFrames, 1, scene);
		assertEquals(1, discard.size);
		assertEquals(5, discard.get(0));
	}

	/**
	 * See if it will periodically save the current frame
	 */
	@Test
	void savePeriodically() {
		var tracker = new DummyTracker();
		VisOdomBundleAdjustment<BTrack> scene = createScene();

		var alg = new TickTockKeyFrameManager();
		alg.keyframePeriod = 3;
		// add the initial set of frames
		for (int i = 0; i < 5; i++) {
			tracker.process(null);
			scene.addFrame(i);
		}

		for (int i = 0; i < 10; i++) {
			tracker.process(null);
			scene.addFrame(i + 5);
			DogArray_I32 discard = alg.selectFramesToDiscard(tracker, maxKeyFrames, 1, scene);
			assertEquals(1, discard.size);
			long id = tracker.getFrameID();
			if (id%3 == 0) {
				assertEquals(0, discard.get(0));
			} else {
				assertEquals(5, discard.get(0));
			}
			// remove the frame to make it realistic
			VisOdomBundleAdjustment.BFrame frame = scene.frames.get(discard.get(0));
			scene.removeFrame(frame, new ArrayList<>());
		}
	}

	/**
	 * Tell it there are two new frames and see if it discard that many
	 */
	@Test
	@Override
	void discardMultipleNewFrames() {
		var tracker = new DummyTracker();
		VisOdomBundleAdjustment<BTrack> scene = createScene();

		int maxKeyFrames = 10;

		var alg = new TickTockKeyFrameManager();
		alg.keyframePeriod = 3;
		// add the initial set of frames
		for (int i = 0; i < 5; i++) {
			tracker.process(null);
			scene.addFrame(i*2);
			scene.addFrame(i*2 + 1);
		}

		for (int i = 0; i < 10; i++) {
			tracker.process(null);
			scene.addFrame((i + 5)*2);
			scene.addFrame((i + 5)*2 + 1);
			DogArray_I32 discard = alg.selectFramesToDiscard(tracker, maxKeyFrames, 2, scene);
			assertEquals(2, discard.size);
			long id = tracker.getFrameID();
			if (id%3 == 0) {
				assertEquals(0, discard.get(0));
				assertEquals(1, discard.get(1));
			} else {
				assertEquals(10, discard.get(0));
				assertEquals(11, discard.get(1));
			}
			// remove the frame to make it realistic
			for (int idxDiscard = 0; idxDiscard < discard.size; idxDiscard++) {
				VisOdomBundleAdjustment.BFrame frame = scene.frames.get(discard.get(0));
				scene.removeFrame(frame, new ArrayList<>());
			}
		}
	}

	@Override
	public VisOdomKeyFrameManager createFrameManager() {
		return new TickTockKeyFrameManager();
	}

	private static class DummyTracker extends PointTrackerDefault {}
}
