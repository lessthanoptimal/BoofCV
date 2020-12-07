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

package boofcv.abst.tracker;

import boofcv.abst.distort.FDistort;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.tracker.PointTrackerKltPyramid.PointTrackMod;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.tracker.klt.*;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestPointTrackerKltPyramid extends GenericChecksPointTracker<GrayF32> {

	ConfigPKlt config;

	public TestPointTrackerKltPyramid() {
		super(false, true);
	}

	@Override public PointTracker<GrayF32> createTracker() {
		config = new ConfigPKlt();
		config.maximumTracks.setFixed(0);
		return createKLT(config);
	}

	/**
	 * Checks to see if tracks are correctly recycled by process and spawn
	 */
	@Test void checkRecycle_Process_Spawn() {
		var alg = (PointTrackerKltPyramid<GrayF32, GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int total = alg.active.size();

		assertTrue(total > 0);
		assertEquals(0, alg.dropped.size());

		// drastically change the image causing tracks to be dropped
		GImageMiscOps.fill(image, 0);
		alg.process(image);

		int difference = total - alg.active.size();
		assertEquals(difference, alg.dropped.size());
		assertEquals(difference, alg.unused.size());
	}

	@Test void checkRecycleDropAll() {
		var alg = (PointTrackerKltPyramid<GrayF32, GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int numSpawned = alg.active.size();
		assertTrue(numSpawned > 0);

		alg.dropAllTracks();

		assertEquals(0, alg.active.size());
		assertEquals(0, alg.dropped.size());
		assertEquals(numSpawned, alg.unused.size());
	}

	@Test void checkRecycleDropTrack() {
		var alg = (PointTrackerKltPyramid<GrayF32, GrayF32>)createTracker();

		assertEquals(0, alg.unused.size());

		alg.process(image);
		alg.spawnTracks();

		int before = alg.active.size();
		assertTrue(before > 2);

		PyramidKltFeature f = alg.active.get(2);

		alg.dropTrack((PointTrack)f.cookie);

		assertEquals(before - 1, alg.active.size());
		assertEquals(1, alg.unused.size());
	}

	@Test void addTrack() {
		var alg = (PointTrackerKltPyramid<GrayF32, GrayF32>)createTracker();

		alg.process(image);
		PointTrack track = alg.addTrack(10, 20.5);
		assertTrue(track != null);
		assertEquals(10, track.pixel.x, 1e-5);
		assertEquals(20.5, track.pixel.y, 1e-5);

		PyramidKltFeature desc = track.getDescription();
		assertEquals(10, desc.x, 1e-5);
		assertEquals(20.5, desc.y, 1e-5);

		for (KltFeature f : desc.desc) {
			assertTrue(f.Gxx != 0);
		}
	}

	/**
	 * The center of tracks should all be inside the image after process() has been called
	 */
	@Test void process_allPointsInside() {
		var alg = (PointTrackerKltPyramid<GrayF32, GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		// swap in a new tracker which won't change the track states
		alg.tracker = new DummyTracker(null);
		int N = alg.active.size();
		assertTrue(N > 10);
		// put two tracks outside of the image, but still close enough to be tracked by KLT
		alg.active.get(0).setPosition(-1, -2);
		alg.active.get(2).setPosition(image.width + 1, image.height);

		// process it again, location's wont change so two tracks should be dropped since they are outside
		alg.process(image);
		assertEquals(2, alg.getDroppedTracks(null).size());
		assertEquals(N - 2, alg.getActiveTracks(null).size());
	}

	@Test void validateRightLeft() {
		var config = new ConfigPKlt();
		// disable almost all error checking so that without R to L check it will pass
		config.config.maxIterations = 2;        // limit how far it can move. Reduces out of bound errors
		config.config.maxPerPixelError = 10000; // disable dropping due to error
		config.config.driftFracTol = 10000;     // disable features drifting as a source of them being dropped
		config.maximumTracks.setFixed(0);

		// Strict tolerance on check
		config.toleranceFB = 1e-3;

		PointTracker<GrayF32> alg = createKLT(config);

		alg.process(image);
		alg.spawnTracks();

		int originalTotal = alg.getActiveTracks(null).size();
		// sanity check
		assertTrue(originalTotal > 90);

		// process the same image again, it should drop very few tracks
		alg.process(image);
		assertEquals(0, alg.getDroppedTracks(null).size());

		// randomize the image, virtually everything should have been dropped
		GImageMiscOps.fillUniform(image, rand, 0, 255);
		alg.process(image);
		assertTrue(alg.getActiveTracks(null).size() < originalTotal/30);
	}

	private PointTrackerKltPyramid<GrayF32, GrayF32> createKLT( ConfigPKlt config ) {
		var configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 200;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 1000;

		return FactoryPointTracker.klt(config, configDetector, GrayF32.class, GrayF32.class);
	}

	/**
	 * Shift the image and see if it still tracks with FB turned on
	 */
	@Test void validateRightLeft_shifted() {
		var config = new ConfigPKlt();
		config.templateRadius = 3;
		config.toleranceFB = 0.1;
		config.pyramidLevels = ConfigDiscreteLevels.levels(3);
		config.maximumTracks.setFixed(0);

		PointTracker<GrayF32> alg = createKLT(config);

		alg.process(image);
		alg.spawnTracks();

		int originalTotal = alg.getActiveTracks(null).size();
		// sanity check
		assertTrue(originalTotal > 90);

		// Shift the image by 2 pixels exactly. Description should be almost the same
		GrayF32 shifted = image.createSameShape();
		new FDistort(image, shifted).affine(1, 0, 0, 1, 2, 0.0).borderExt().apply();

		alg.process(shifted);

		// only tracks at the border should be dropped
		assertTrue(alg.getActiveTracks(null).size() > originalTotal*0.8);

		// one last time for good measure
		alg.process(image);
		assertTrue(alg.getActiveTracks(null).size() > originalTotal*0.8);
	}

	@Test void pruneClose() {
		var config = new ConfigPKlt();
		config.pruneClose = true;

		PointTrackerKltPyramid<GrayF32, GrayF32> alg = createKLT(config);

		// create 10 tracks all in the same location
		for (int i = 0; i < 10; i++) {
			var t = new PyramidKltFeature(1, 1);
			var p = new PointTrackMod();
			p.featureId = i;
			t.x = t.y = 12;
			t.cookie = p;
			alg.active.add(t);
		}

		alg.input = new GrayF32(50, 50);
		alg.pruneCloseTracks();

		// only oe will be saved
		assertEquals(1, alg.active.size());
		assertEquals(9, alg.dropped.size());
		// save the oldest track
		assertEquals(0, ((PointTrackMod)alg.active.get(0).cookie).featureId);
	}

	/**
	 * There was a bug where it didn't just the number of layers in the gradient if the pyramid changed the number
	 * of layers.
	 */
	@Test void dynamicPyramidLayers() {
		var config = new ConfigPKlt();
		config.pyramidLevels = ConfigDiscreteLevels.minSize(5);

		PointTrackerKltPyramid<GrayF32, GrayF32> alg = createKLT(config);
		alg.process(new GrayF32(100, 100));
		alg.addTrack(1, 2);
		assertEquals(alg.currPyr.basePyramid.layers.length, alg.currPyr.derivX.length);
		assertEquals(alg.currPyr.basePyramid.getWidth(0), alg.currPyr.derivX[0].width);
		alg.reset();
		alg.process(new GrayF32(300, 300));
		int N = alg.currPyr.basePyramid.layers.length;
		assertEquals(((PyramidKltFeature)alg.addTrack(1, 2).getDescription()).desc.length, N);
		assertEquals(N, alg.currPyr.derivX.length);
		assertEquals(alg.currPyr.basePyramid.getWidth(0), alg.currPyr.derivX[0].width);
	}

	/**
	 * Don't change the track state
	 */
	private static class DummyTracker extends PyramidKltTracker {

		public DummyTracker( KltTracker tracker ) {
			super(tracker);
		}

		@Override
		public boolean setDescription( PyramidKltFeature feature ) {
			return true;
		}

		@Override
		public KltTrackFault track( PyramidKltFeature feature ) {
			return KltTrackFault.SUCCESS;
		}
	}
}
