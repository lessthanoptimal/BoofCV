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

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrackerDefault;
import boofcv.alg.misc.ImageCoverage;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.factory.geo.FactoryMultiView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMaxGeoKeyFrameManager {
	final int width = 100;
	final int height = 200;
	BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleSparseMetric(null);

	/**
	 * Give it a simple sequence and see how it does
	 */
	@Test
	void simpleSequence() {
		var coverage = new DummyCoverage();
		coverage.fraction = 0.5;
		var scene = new VisOdomBundleAdjustment<>(sba, BTrack::new);
		var tracker = new DummyTracker();
		tracker.activeTracks = 50;
		tracker.maxSpawn = 60;
		var alg = new MaxGeoKeyFrameManager();
		alg.coverage = coverage;
		alg.minimumCoverage = 0.25;
		alg.maxKeyFrames = 3;

		// Always add new key frames as it has not hit the max yet
		alg.configure(width,height);
		alg.handleSpawnedTracks(tracker);
		scene.addFrame(0);
		assertEquals(0,alg.selectFramesToDiscard(tracker,scene).size);
		scene.addFrame(1);
		alg.handleSpawnedTracks(tracker);
		assertEquals(0,alg.selectFramesToDiscard(tracker,scene).size);
		scene.addFrame(2);
		alg.handleSpawnedTracks(tracker);
		assertEquals(0,alg.selectFramesToDiscard(tracker,scene).size);
		// there will now be an extra frame. The coverage threshold is high so it will not keep the current frame
		scene.addFrame(3);
		assertEquals(3,alg.selectFramesToDiscard(tracker,scene).get(0));
		// cover is now low so it will select an older frame to discard
		coverage.fraction = 0.1;
		scene.addFrame(4);
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i+1; j < scene.frames.size; j++) {
				connectFrames(i,j,10+i,scene);
			}
		}
		// make best connect to current be frame 1
		connectFrames(0,3,20,scene);
		assertEquals(1,alg.selectFramesToDiscard(tracker,scene).get(0));
	}

	@Test
	void configure() {
		var alg = new MaxGeoKeyFrameManager();
		alg.minimumCoverage = 0.7;
		alg.maxKeyFrames = 6;
		alg.maxFeaturesPerFrame = 101;

		alg.configure(width,height);
		assertEquals(width,alg.imageWidth);
		assertEquals(height,alg.imageHeight);
		assertEquals(0,alg.maxFeaturesPerFrame);

		// make sure these are not accidentally reset
		assertEquals(0.7,alg.minimumCoverage);
		assertEquals(6,alg.maxKeyFrames);
	}

	@Test
	void keepCurrentFrame_threshold() {
		var coverage = new DummyCoverage();
		var alg = new MaxGeoKeyFrameManager();
		alg.maxFeaturesPerFrame = 100;
		alg.coverage = coverage;

		alg.minimumCoverage = 0.25;
		coverage.fraction = 0.25;

		assertFalse(alg.keepCurrentFrame());
		coverage.fraction = 0.24999999999;
		assertTrue(alg.keepCurrentFrame());
	}

	/**
	 * There will be one frame with no connection to the best connected frame and should be dropped
	 */
	@Test
	void selectOldToDiscard_noConnection() {
		int bestConnect = 2;
		// Create a scene with 5 frames.
		var scene = new VisOdomBundleAdjustment<>(sba, BTrack::new);
		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
		}
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i+1; j < scene.frames.size; j++) {
				if( i == 1 && j == bestConnect ) // no connection to best connect
					continue;
				connectFrames(i,j,10+i,scene);
			}
		}
		// make best connect to current be frame 2
		connectFrames(4,bestConnect,20,scene);

		var alg = new MaxGeoKeyFrameManager();
		assertEquals(1,alg.selectOldToDiscard(scene));
	}

	/**
	 * Drop the frame with worst connection to best connected
	 */
	@Test
	void selectOldToDiscard() {
		// Create a scene with 5 frames.
		var scene = new VisOdomBundleAdjustment<>(sba, BTrack::new);
		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
		}
		// connect all the frames to each other
		for (int i = 0; i < scene.frames.size; i++) {
			for (int j = i+1; j < scene.frames.size; j++) {
				connectFrames(i,j,10+i,scene);
			}
		}
		// make best connect to current be frame 2
		connectFrames(4,2,20,scene);

		var alg = new MaxGeoKeyFrameManager();
		assertEquals(0,alg.selectOldToDiscard(scene));
		// add to the oldest track to make it no longer the worst
		connectFrames(0,2,3,scene);
		assertEquals(1,alg.selectOldToDiscard(scene));
	}

	public static void connectFrames( int a , int b , int count , VisOdomBundleAdjustment<BTrack> scene ) {
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
		var tracker = new DummyTracker();
		var alg = new MaxGeoKeyFrameManager();

		tracker.activeTracks = 210; // should be ignored
		tracker.maxSpawn = 200;

		alg.handleSpawnedTracks(tracker);
		assertEquals(200, alg.maxFeaturesPerFrame);

		tracker.activeTracks = 220;
		tracker.maxSpawn = 0;

		alg.handleSpawnedTracks(tracker);
		assertEquals(220, alg.maxFeaturesPerFrame);

		tracker.activeTracks = 230;
		alg.handleSpawnedTracks(tracker);
		assertEquals(230, alg.maxFeaturesPerFrame);
	}

	private static class DummyTracker extends PointTrackerDefault
	{
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