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

import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static boofcv.alg.sfm.d3.structure.TestMaxGeoKeyFrameManager.connectFrames;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksVisOdomKeyFrameManager extends BoofStandardJUnit {
	int width = 100;
	int height = 200;

	public abstract VisOdomKeyFrameManager createFrameManager();

	@Test
	void discardMultipleFramesWithNothing() {
		VisOdomBundleAdjustment<BTrack> scene = createScene();
		var tracker = new TestMaxGeoKeyFrameManager.DummyTracker();
		tracker.activeTracks = 50;
		tracker.maxSpawn = 60;
		VisOdomKeyFrameManager alg = createFrameManager();
		alg.initialize(scene.cameras);

		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
			DogArray_I32 discard = alg.selectFramesToDiscard(tracker, 5, 1, scene);
			assertEquals(0, discard.size);
			alg.handleSpawnedTracks(tracker, scene.cameras.getTail());
		}
		// connect the two most recent frames
		scene.addFrame(5);
		connectFrames(4, 5, 10, scene);

		// tell it to only keep 3
		DogArray_I32 discard = alg.selectFramesToDiscard(tracker, 3, 1, scene);
		// there should only be 3 dropped in this order
		assertEquals(3, discard.size);
		for (int i = 0; i < 3; i++) {
			assertEquals(i, discard.get(i));
		}
	}

	// friendly reminder that this needs to be manually implemented for every class
	abstract void discardMultipleNewFrames();

	public VisOdomBundleAdjustment<BTrack> createScene() {
		VisOdomBundleAdjustment<BTrack> scene = new VisOdomBundleAdjustment<>(BTrack::new);
		scene.addCamera(new CameraPinholeBrown(0, 0, 0, 0, 0, width, height));
		return scene;
	}
}
