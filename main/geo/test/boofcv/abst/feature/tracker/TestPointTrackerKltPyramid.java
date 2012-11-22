/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestPointTrackerKltPyramid extends StandardImagePointTracker<ImageFloat32> {

	PkltConfig<ImageFloat32,ImageFloat32> config;

	public TestPointTrackerKltPyramid() {
		super(false, true);
	}

	@Override
	public ImagePointTracker<ImageFloat32> createTracker() {
		config = PkltConfig.createDefault(ImageFloat32.class, ImageFloat32.class);
		return FactoryPointSequentialTracker.klt(config,1,1);
	}

	/**
	 * Checks to see if tracks are correctly recycled by process and spawn
	 */
	@Test
	public void checkRecycle_Process_Spawn() {
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int max = alg.config.maxFeatures;
		int total = alg.active.size();

		assertTrue( total > 0 );
		assertEquals(0,alg.dropped.size());
		assertEquals(max-total,alg.unused.size());

		// drastically change the image causing tracks to be dropped
		GImageMiscOps.fill(image, 0);
		alg.process(image);

		int difference = total - alg.active.size();
		assertEquals(difference,alg.dropped.size());
		assertEquals(max-alg.active.size(),alg.unused.size());
	}

	@Test
	public void checkRecycleDropAll() {
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		assertTrue( alg.active.size() > 0 );

		alg.dropAllTracks();

		assertEquals( 0, alg.active.size());
		assertEquals( 0, alg.dropped.size());
		assertEquals(alg.config.maxFeatures, alg.unused.size());
	}

	@Test
	public void checkRecycleDropTrack() {
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

		alg.process(image);
		alg.spawnTracks();

		int before = alg.active.size();
		assertTrue( before > 2 );

		PyramidKltFeature f = alg.active.get(2);

		alg.dropTrack((PointTrack)f.cookie);

		assertEquals( before-1, alg.active.size());
		assertEquals(alg.config.maxFeatures-alg.active.size(),alg.unused.size());
	}
}
