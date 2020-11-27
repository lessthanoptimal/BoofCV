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
import boofcv.alg.misc.ImageCoverage;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BCamera;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMaxGeoKeyFrameManager extends ChecksVisOdomKeyFrameManager {

	@Override
	public VisOdomKeyFrameManager createFrameManager() {
		return new MaxGeoKeyFrameManager();
	}

	/**
	 * Give it a simple sequence and see how it does
	 */
	@Test
	void simpleSequence() {
		simpleSequence(1);
	}

	@Test
	@Override
	void discardMultipleNewFrames() {
		simpleSequence(2);
	}

	@Test
	void configure() {
		DogArray<BCamera> cameras = createScene().cameras;
		var alg = new MaxGeoKeyFrameManager();
		alg.cameras.grow().maxFeaturesPerFrame = 234; // give it a garbage value that should be reset
		alg.minimumCoverage = 0.7;
		alg.initialize(cameras);
		assertEquals(width, alg.cameras.get(0).imageWidth);
		assertEquals(height, alg.cameras.get(0).imageHeight);
		assertEquals(0, alg.cameras.get(0).maxFeaturesPerFrame);

		// make sure these are not accidentally reset
		assertEquals(0.7, alg.minimumCoverage);
	}

	@Test
	void keepCurrentFrame_threshold() {
		VisOdomBundleAdjustment<BTrack> sba = createScene();
		sba.addFrame(0);
		var coverage = new DummyCoverage();
		var alg = new MaxGeoKeyFrameManager();
		alg.initialize(sba.cameras);
		alg.cameras.get(0).maxFeaturesPerFrame = 100;
		alg.coverage = coverage;

		alg.minimumCoverage = 0.25;
		coverage.fraction = 0.25;

		assertFalse(alg.keepCurrentFrame(sba));
		coverage.fraction = 0.24999999999;
		assertTrue(alg.keepCurrentFrame(sba));
	}

	/**
	 * There will be one frame with no connection to the best connected frame and should be dropped
	 */
	@Test
	void selectOldToDiscard_noConnection() {
		int bestConnect = 2;
		// Create a scene with 5 frames.
		VisOdomBundleAdjustment<BTrack> scene = createScene();
		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
		}
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i + 1; j < scene.frames.size; j++) {
				if (i == 1 && j == bestConnect) // no connection to best connect
					continue;
				connectFrames(i, j, 10 + i, scene);
			}
		}
		// make best connect to current be frame 2
		connectFrames(4, bestConnect, 20, scene);

		var alg = new MaxGeoKeyFrameManager();
		alg.selectOldToDiscard(scene, 1);
		assertEquals(1, alg.discardKeyIndices.size);
		assertEquals(1, alg.discardKeyIndices.get(0));
	}

	/**
	 * Drop the frame with worst connection to best connected
	 */
	@Test
	void selectOldToDiscard() {
		// Create a scene with 5 frames.
		VisOdomBundleAdjustment<BTrack> scene = createScene();
		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
		}
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i + 1; j < scene.frames.size; j++) {
				connectFrames(i, j, 10 + i, scene);
			}
		}
		// make best connect to current be frame 2
		connectFrames(4, 2, 20, scene);

		var alg = new MaxGeoKeyFrameManager();
		alg.selectOldToDiscard(scene, 1);
		assertEquals(1, alg.discardKeyIndices.size);
		assertEquals(0, alg.discardKeyIndices.get(0));
		// add to the oldest track to make it no longer the worst
		connectFrames(0, 2, 3, scene);
		alg.discardKeyIndices.reset();
		alg.selectOldToDiscard(scene, 1);
		assertEquals(1, alg.discardKeyIndices.size);
		assertEquals(1, alg.discardKeyIndices.get(0));
	}

	public static void connectFrames( int a, int b, int count, VisOdomBundleAdjustment<BTrack> scene ) {
		BFrame frameA = scene.frames.get(a);
		BFrame frameB = scene.frames.get(b);

		for (int i = 0; i < count; i++) {
			BTrack track = scene.tracks.grow();
			track.id = -1; // crash hard if it's used
			track.observations.grow().frame = frameA;
			track.observations.grow().frame = frameB;
			frameA.tracks.add(track);
			frameB.tracks.add(track);
		}
	}

	/**
	 * See if it correctly updates the max possible tracks based on what the tracker can tell it
	 */
	@Test
	void handleSpawnedTracks() {
		var scene = createScene();
		BCamera bcam = scene.getCamera(0);
		var tracker = new DummyTracker();
		var alg = new MaxGeoKeyFrameManager();
		alg.initialize(scene.cameras);

		tracker.activeTracks = 210; // should be ignored
		tracker.maxSpawn = 200;

		alg.handleSpawnedTracks(tracker, bcam);
		assertEquals(200, alg.cameras.get(0).maxFeaturesPerFrame);

		tracker.activeTracks = 220;
		tracker.maxSpawn = 0;

		alg.handleSpawnedTracks(tracker, bcam);
		assertEquals(220, alg.cameras.get(0).maxFeaturesPerFrame);

		tracker.activeTracks = 230;
		alg.handleSpawnedTracks(tracker, bcam);
		assertEquals(230, alg.cameras.get(0).maxFeaturesPerFrame);
	}

	private void simpleSequence( int newFrames ) {
		var coverage = new DummyCoverage();
		coverage.fraction = 0.5;
		VisOdomBundleAdjustment<BTrack> scene = createScene();
		var tracker = new DummyTracker();
		tracker.activeTracks = 50;
		tracker.maxSpawn = 60;
		var alg = new MaxGeoKeyFrameManager();
		alg.coverage = coverage;
		alg.minimumCoverage = 0.25;
		int maxKeyFrames = 3*newFrames;

		int frameID = 0;

		// Always add new key frames as it has not hit the max yet
		alg.initialize(scene.cameras);
		for (int i = 0; i < 3; i++) {
			alg.handleSpawnedTracks(tracker, scene.getCamera(0));
			for (int indexNew = 0; indexNew < newFrames; indexNew++) {
				scene.addFrame(frameID++);
			}
			assertEquals(0, alg.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, scene).size);
		}

		// there will now be an extra frame. The coverage threshold is high so it will not keep the current frame
		for (int indexNew = 0; indexNew < newFrames; indexNew++) {
			scene.addFrame(frameID++);
		}
		if (newFrames == 2)
			checkDiscard(alg.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, scene), frameID - 2, frameID - 1);
		else
			checkDiscard(alg.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, scene), frameID - 1);

		for (int indexNew = 0; indexNew < newFrames; indexNew++) {
			scene.removeFrame(scene.getLastFrame(), new ArrayList<>());
		}
		// cover is now low so it will select an older frame to discard
		coverage.fraction = 0.1;
		for (int indexNew = 0; indexNew < newFrames; indexNew++) {
			scene.addFrame(frameID++);
		}
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i + 1; j < scene.frames.size; j++) {
				connectFrames(i, j, 10 + i, scene);
			}
		}
		// make best connect to current be frame 0
		connectFrames(0, maxKeyFrames + newFrames - 1, 20, scene);
		if (newFrames == 2)
			checkDiscard(alg.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, scene), 1, 2);
		else
			checkDiscard(alg.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, scene), 1);
	}

	private void checkDiscard( DogArray_I32 discarded, int... expected ) {
		assertEquals(expected.length, discarded.size);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], discarded.get(i));
		}
	}

	static class DummyTracker extends PointTrackerDefault {
		public int maxSpawn;
		public int activeTracks;

		@Override
		public int getTotalActive() {
			return activeTracks;
		}

		@Override
		public int getMaxSpawn() {
			return maxSpawn;
		}
	}

	private static class DummyCoverage extends ImageCoverage {
		public double fraction;

		@Override
		public void process() {
			super.fraction = this.fraction;
		}
	}
}
