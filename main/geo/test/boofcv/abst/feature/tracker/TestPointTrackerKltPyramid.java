/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.tracker.klt.*;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestPointTrackerKltPyramid extends StandardPointTracker<GrayF32> {

	PkltConfig config;

	public TestPointTrackerKltPyramid() {
		super(false, true);
	}

	@Override
	public PointTracker<GrayF32> createTracker() {
		config = new PkltConfig();
		return FactoryPointTracker.klt(config, new ConfigGeneralDetector(200, 3, 1000, 0, true),
				GrayF32.class, GrayF32.class);
	}

	/**
	 * Checks to see if tracks are correctly recycled by process and spawn
	 */
	@Test
	public void checkRecycle_Process_Spawn() {
		PointTrackerKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerKltPyramid<GrayF32,GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int total = alg.active.size();

		assertTrue(total > 0);
		assertEquals(0,alg.dropped.size());

		// drastically change the image causing tracks to be dropped
		GImageMiscOps.fill(image, 0);
		alg.process(image);

		int difference = total - alg.active.size();
		assertEquals(difference,alg.dropped.size());
		assertEquals(difference,alg.unused.size());
	}

	@Test
	public void checkRecycleDropAll() {
		PointTrackerKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerKltPyramid<GrayF32,GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int numSpawned = alg.active.size();
		assertTrue( numSpawned > 0 );

		alg.dropAllTracks();

		assertEquals( 0, alg.active.size());
		assertEquals( 0, alg.dropped.size());
		assertEquals( numSpawned, alg.unused.size());
	}

	@Test
	public void checkRecycleDropTrack() {
		PointTrackerKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerKltPyramid<GrayF32,GrayF32>)createTracker();

		assertEquals(0,alg.unused.size());

		alg.process(image);
		alg.spawnTracks();

		int before = alg.active.size();
		assertTrue( before > 2 );

		PyramidKltFeature f = alg.active.get(2);

		alg.dropTrack((PointTrack)f.cookie);

		assertEquals( before-1, alg.active.size());
		assertEquals(1,alg.unused.size());
	}

	@Test
	public void addTrack() {
		PointTrackerKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerKltPyramid<GrayF32,GrayF32>)createTracker();

		alg.process(image);
		PointTrack track = alg.addTrack(10,20.5);
		assertTrue(track != null );
		assertEquals(10,track.x,1e-5);
		assertEquals(20.5,track.y,1e-5);

		PyramidKltFeature desc = track.getDescription();
		assertEquals(10,desc.x,1e-5);
		assertEquals(20.5,desc.y,1e-5);

		for(KltFeature f : desc.desc ) {
			assertTrue(f.Gxx != 0 );
		}
	}

	/**
	 * The center of tracks should all be inside the image after process() has been called
	 */
	@Test
	public void process_allPointsInside() {
		PointTrackerKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerKltPyramid<GrayF32,GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		// swap in a new tracker which won't change the track states
		alg.tracker = new DummyTracker(null);
		int N = alg.active.size();
		assertTrue(N>10);
		// put two tracks outside of the image, but still close enough to be tracked by KLT
		alg.active.get(0).setPosition(-1,-2);
		alg.active.get(2).setPosition(image.width+1,image.height);

		// process it again, location's wont change so two tracks should be dropped since they are outside
		alg.process(image);
		assertEquals(2, alg.getDroppedTracks(null).size());
		assertEquals(N-2,alg.getActiveTracks(null).size());

	}

	/**
	 * Don't change the track state
	 */
	private static class DummyTracker extends PyramidKltTracker {

		public DummyTracker(KltTracker tracker) {
			super(tracker);
		}

		@Override
		public boolean setDescription(PyramidKltFeature feature) {
			return true;
		}

		@Override
		public KltTrackFault track(PyramidKltFeature feature) {
			return KltTrackFault.SUCCESS;
		}
	}
}
