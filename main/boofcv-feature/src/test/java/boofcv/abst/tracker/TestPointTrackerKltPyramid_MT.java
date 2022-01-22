/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.tracker.PointTrackerKltPyramid.PointTrackMod;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPointTrackerKltPyramid_MT extends GenericChecksPointTracker<GrayF32> {
	ConfigPKlt config;

	public TestPointTrackerKltPyramid_MT() {
		super(false, true);
	}

	@Override public PointTracker<GrayF32> createTracker() {
		config = new ConfigPKlt();
		config.toleranceFB = 2; // this has been made parallel too
		config.maximumTracks.setFixed(0);
		config.concurrentMinimumTracks = 0; // we want it to always be threaded for stress testing
		return createKLT(config);
	}

	private PointTrackerKltPyramid<GrayF32, GrayF32> createKLT( ConfigPKlt config ) {
		var configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 200;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 1000;

		// make sure threads are turned on
		BoofConcurrency.USE_CONCURRENT = true;
		var tracker = FactoryPointTracker.klt(config, configDetector, GrayF32.class, GrayF32.class);
		if (tracker instanceof PointTrackerKltPyramid_MT)
			return tracker;
		throw new RuntimeException("Did not create concurrent implementation");
	}

	/**
	 * See if concurrent and single thread version produce identical reuslts
	 */
	@Test void compareToSingleThread() {
		var config = new ConfigPKlt();
		config.toleranceFB = 2;
		config.maximumTracks.setFixed(0);
		config.concurrentMinimumTracks = 0;

		var configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 200;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 200;


		// Create the two different versions
		BoofConcurrency.USE_CONCURRENT = true;
		var trackerMulti = FactoryPointTracker.klt(config, configDetector, GrayF32.class, GrayF32.class);
		BoofConcurrency.USE_CONCURRENT = false;
		var trackerSingle = FactoryPointTracker.klt(config, configDetector, GrayF32.class, GrayF32.class);

		trackerMulti.process(image);
		trackerSingle.process(image);
		trackerMulti.spawnTracks();
		trackerSingle.spawnTracks();
		checkIdentical(trackerSingle, trackerMulti);
		for (int i = 0; i < 2; i++) {
			ImageMiscOps.addGaussian(image, rand, 40, 0, 255);
			trackerMulti.process(image);
			trackerSingle.process(image);
			checkIdentical(trackerSingle, trackerMulti);
		}
	}

	private void checkIdentical(PointTrackerKltPyramid<?,?> trackerA, PointTrackerKltPyramid<?,?> trackerB) {
		assertTrue(trackerA.active.size() > 10);
		assertEquals(trackerA.active.size(), trackerB.active.size());
		assertEquals(trackerA.dropped.size(), trackerB.dropped.size());
		assertEquals(trackerA.spawned.size(), trackerB.spawned.size());

		for (int i = 0; i < trackerA.active.size(); i++) {
			PyramidKltFeature a = trackerA.active.get(i);
			PyramidKltFeature b = trackerA.active.get(i);

			PointTrackMod pa = a.getCookie();
			PointTrackMod pb = b.getCookie();

			assertEquals(pa.featureId, pb.featureId);
			assertEquals(a.x, b.x, UtilEjml.TEST_F64);
			assertEquals(a.y, b.y, UtilEjml.TEST_F64);
			assertEquals(a.desc[0].Gxx, b.desc[0].Gxx, UtilEjml.TEST_F64);
		}
	}
}
