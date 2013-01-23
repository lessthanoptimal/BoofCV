/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.tracker.klt.KltFeature;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestPointTrackerKltPyramid extends StandardPointTracker<ImageFloat32> {

	PkltConfig<ImageFloat32,ImageFloat32> config;

	public TestPointTrackerKltPyramid() {
		super(false, true);
	}

	@Override
	public PointTracker<ImageFloat32> createTracker() {
		config = PkltConfig.createDefault(ImageFloat32.class, ImageFloat32.class);
		return FactoryPointTracker.klt(config, new ConfigGeneralDetector(200, 3, 1000, 0, true));
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
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

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
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

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
		PointTrackerKltPyramid<ImageFloat32,ImageFloat32> alg =
				(PointTrackerKltPyramid<ImageFloat32,ImageFloat32>)createTracker();

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
}
