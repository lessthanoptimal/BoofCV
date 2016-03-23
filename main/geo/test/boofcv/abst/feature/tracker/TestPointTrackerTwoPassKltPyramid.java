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
import boofcv.alg.tracker.klt.*;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPointTrackerTwoPassKltPyramid extends StandardPointTrackerTwoPass<GrayF32> {

	PkltConfig config;

	public TestPointTrackerTwoPassKltPyramid() {
		super(false, true);
	}

	@Override
	public PointTrackerTwoPass<GrayF32> createTracker() {
		config = new PkltConfig();
		return FactoryPointTrackerTwoPass.klt(config, new ConfigGeneralDetector(200, 3, 1000, 0, true),
				GrayF32.class, GrayF32.class);
	}

	@Test
	public void allPointsInside_firstPass() {
		PointTrackerTwoPassKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerTwoPassKltPyramid<GrayF32,GrayF32>)createTracker();

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
		assertEquals(2, alg.candidateDrop.size());
		assertEquals(N-2,alg.getActiveTracks(null).size());

		alg.finishTracking();
		assertEquals(N-2,alg.getAllTracks(null).size());
	}

	@Test
	public void allPointsInside_secondPass() {
		PointTrackerTwoPassKltPyramid<GrayF32,GrayF32> alg =
				(PointTrackerTwoPassKltPyramid<GrayF32,GrayF32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		// swap in a new tracker which won't change the track states
		alg.tracker = new DummyTracker(null);
		int N = alg.active.size();
		assertTrue(N > 10);

		// no change after first pass
		alg.process(image);
		assertEquals(0, alg.candidateDrop.size());
		assertEquals(N,alg.getActiveTracks(null).size());
		// should drop tracks after the second pass
		alg.active.get(0).setPosition(-1, -2);
		alg.active.get(2).setPosition(image.width + 1, image.height);
		alg.performSecondPass();

		assertEquals(2,alg.candidateDrop.size());
		assertEquals(N-2,alg.getActiveTracks(null).size());

		alg.finishTracking();
		assertEquals(N-2,alg.getAllTracks(null).size());
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
