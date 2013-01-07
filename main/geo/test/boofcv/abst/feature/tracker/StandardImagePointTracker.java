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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementations of {@link PointTrackerSpawn}.
 *
 * @author Peter Abeles
 */
public abstract class StandardImagePointTracker <T extends ImageSingleBand> {

	public PointTrackerSpawn<T> tracker;
	Random rand = new Random(234);
	int width = 100;
	int height = 80;
	ImageFloat32 image = new ImageFloat32(width,height);
	boolean shouldDropTracks;
	boolean shouldCreateInactive;

	protected StandardImagePointTracker(boolean shouldCreateInactive, boolean shouldDropTracks) {
		this.shouldCreateInactive = shouldCreateInactive;
		this.shouldDropTracks = shouldDropTracks;
	}

	@Before
	public void initStandard() {
		ImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	/**
	 * Creates a new tracker with the specified number of tracks initially.
	 */
	public abstract PointTrackerSpawn<T> createTracker();

	/**
	 * The cookie for tracks should not be set
	 */
	@Test
	public void checkCookieNull() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);

		for( PointTrack t : tracker.getActiveTracks(null) ) {
			assertTrue(t.cookie==null);
		}
	}

	/**
	 * After a track has been dropped the cookie should not be modified and returned when
	 * the track is recycled.
	 *
	 * NOTE: This test will return incorrect results if a large group of tracks are predeclared
	 * and recycled tracks are put at the end of the queue
	 */
	@Test
	public void checkCookieSaved() {
		// create tracks
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);

		tracker.getActiveTracks(null).get(0).setCookie(1);

		// drop tracks
		tracker.dropAllTracks();

		// respawn and look for a cookie that's not null
		tracker.process((T)image);
		tracker.spawnTracks();

		int numFound = 0;
		for( PointTrack t : tracker.getActiveTracks(null) ) {
			if( t.getCookie() != null )
				numFound++;
		}
		assertEquals(1,numFound);
	}

	/**
	 * High level spawn tracks test.
	 */
	@Test
	public void spawnTracks() {
		// Process an image and make sure no new tracks have been spawned until requested
		tracker = createTracker();
		tracker.process((T)image);
		assertEquals(0,tracker.getAllTracks(null).size());
		assertEquals(0,tracker.getActiveTracks(null).size());
		assertTrue(tracker.getNewTracks(null).size() == 0 );

		// Request that new tracks be spawned and ensure that all lists have been updated
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertTrue(tracker.getActiveTracks(null).size()>0);
		assertTrue(tracker.getActiveTracks(null).size() ==
				tracker.getNewTracks(null).size() );

		// Tweak the input image and make sure that everything has the expected size
		ImageMiscOps.addGaussian(image,rand,1,0,255);
		tracker.process((T)image);

		int beforeAll = tracker.getAllTracks(null).size();
		int beforeActive = tracker.getActiveTracks(null).size();

		assertTrue(beforeAll > 0);
		assertTrue(beforeActive>0);
		assertTrue(tracker.getNewTracks(null).size() == 0 );

		// Call spawn again.  There should be more tracks now
		tracker.spawnTracks();

		assertTrue(beforeAll < tracker.getAllTracks(null).size());
		assertTrue(beforeActive < tracker.getActiveTracks(null).size());

		// there should be some pre-existing tracks
		assertTrue(tracker.getActiveTracks(null).size() !=
				tracker.getNewTracks(null).size() );
	}

	/**
	 * When spawn is called, make sure that it doesn't return identical tracks
	 */
	@Test
	public void spawnTracks_NoDuplicates() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getActiveTracks(null).size()>0);

		// drop a track
		PointTrack dropped = tracker.getActiveTracks(null).get(0);
		tracker.dropTrack(dropped);
		double x = dropped.x;
		double y = dropped.y;

		// no change to the image
		// I think in just about every tracker nothing should change. Might need to change test for some
		tracker.process((T)image);

		// should just spawn one track
		tracker.spawnTracks();
		assertEquals(1,tracker.getNewTracks(null).size());

		PointTrack found = tracker.getNewTracks(null).get(0);

		assertTrue(x == found.x);
		assertTrue(y == found.y);
	}

	@Test
	public void dropAllTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertTrue(tracker.getActiveTracks(null).size() > 0);

		tracker.dropAllTracks();

		assertEquals(0,tracker.getAllTracks(null).size());
		assertEquals(0,tracker.getActiveTracks(null).size());
		// tracks which have been dropped by request should not be included in this list
		assertEquals(0,tracker.getDroppedTracks(null).size());
	}

	/**
	 * Cause tracks to be dropped during the update
	 */
	@Test
	public void testUpdateTrackDrop() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		int beforeAll = tracker.getAllTracks(null).size();
		int beforeActive = tracker.getActiveTracks(null).size();
		assertTrue(beforeAll > 0);

		assertEquals(0,tracker.getDroppedTracks(null).size());

		// make the image a poor match, causing tracks to be dropped
		GImageMiscOps.fill(image, 0);
		tracker.process((T) image);

		int afterAll = tracker.getAllTracks(null).size();
		int afterActive = tracker.getActiveTracks(null).size();
		int afterDropped =  tracker.getDroppedTracks(null).size();
		int afterInactive =  tracker.getInactiveTracks(null).size();

		// make sure some change happened
		assertTrue(afterActive < beforeActive);

		// algorithm specific checks
		if( shouldDropTracks ) {
			assertTrue(afterDropped>0);
			assertTrue(afterAll < beforeAll );
		} else  {
			assertEquals(0,afterDropped);
			assertTrue(afterAll == beforeAll );
		}

		if( shouldCreateInactive )
			assertTrue(afterInactive>0);
		else
			assertEquals(0,afterInactive);

		// this might not be true for all trackers...
		assertEquals(0,afterActive);
		// some tracks should either be dropped or become inactive
		assertTrue(afterDropped+afterInactive>0);
		// note that some trackers will not add any features to the dropped list since
		// it will try to respawn them
		assertEquals(beforeAll-afterAll,tracker.getDroppedTracks(null).size());
	}
	
	@Test
	public void testRequestDrop() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		List<PointTrack> tracks = tracker.getActiveTracks(null);

		int before = tracks.size();
		assertTrue(before > 0);
		tracker.dropTrack(tracks.get(0));
		
		assertEquals(before-1,tracker.getActiveTracks(null).size());

		// tracks which have been dropped by request should not be included in this list
		assertEquals(0,tracker.getDroppedTracks(null).size());
	}

	@Test
	public void testTrackUpdate() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		int before = tracker.getAllTracks(null).size();
		assertTrue(before > 0);
		checkUniqueFeatureID();

		// by adding a little bit of noise the features should move slightly
		ImageMiscOps.addUniform(image,rand,0,5);
		tracker.process((T)image);
		checkUniqueFeatureID();

		int after = tracker.getAllTracks(null).size();
		int dropped = tracker.getDroppedTracks(null).size();
		assertEquals(before , after+dropped);
	}

	@Test
	public void reset() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertEquals(0, tracker.getAllTracks(null).get(0).featureId);

		tracker.reset();

		// add several tracks
		ImageMiscOps.addUniform(image,rand,0,5);
		tracker.process((T)image);
		tracker.spawnTracks();

		// old tracks should be discarded
		assertTrue(tracker.getAllTracks(null).size() > 0);
		// checks to see if feature ID counter was reset
		assertEquals(0, tracker.getAllTracks(null).get(0).featureId);
	}


	/**
	 * Makes sure each feature has a unique feature number
	 */
	private void checkUniqueFeatureID() {
		List<PointTrack> l = tracker.getActiveTracks(null);

		for( int i = 0; i < l.size(); i++ ) {
			PointTrack a = l.get(i);
			for( int j = i+1; j < l.size(); j++ ) {
				PointTrack b = l.get(j);

				assertTrue(a.featureId != b.featureId);
			}
		}
	}

	@Test
	public void getAllTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<PointTrack>();
		assertTrue(input == tracker.getAllTracks(input));

		List<PointTrack> ret = tracker.getAllTracks(null);

		//sanity check
		assertTrue(ret.size() > 0 );

		checkIdentical(input,ret);
	}

	@Test
	public void getActiveTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<PointTrack>();
		assertTrue( input == tracker.getActiveTracks(input));

		List<PointTrack> ret = tracker.getActiveTracks(null);

		//sanity check
		assertTrue(ret.size() > 0 );

		checkIdentical(input, ret);
	}

	@Test
	public void getInactiveTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		// create a situation where tracks might become inactive
		GImageMiscOps.fill(image, 0);
		tracker.process((T) image);

		List<PointTrack> input = new ArrayList<PointTrack>();
		assertTrue( input == tracker.getInactiveTracks(input));

		List<PointTrack> ret = tracker.getInactiveTracks(null);

		checkIdentical(input, ret);
	}

	@Test
	public void getDroppedTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		// create a situation where tracks might be dropped
		GImageMiscOps.fill(image, 0);
		tracker.process((T) image);

		List<PointTrack> input = new ArrayList<PointTrack>();
		assertTrue( input == tracker.getDroppedTracks(input));

		List<PointTrack> ret = tracker.getDroppedTracks(null);

		checkIdentical(input, ret);
	}

	@Test
	public void getNewTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<PointTrack>();
		assertTrue( input == tracker.getNewTracks(input));

		List<PointTrack> ret = tracker.getNewTracks(null);

		//sanity check
		assertTrue(ret.size() > 0 );

		checkIdentical(input, ret);
	}

	private void checkIdentical( List<PointTrack> a , List<PointTrack> b ){
		assertEquals(a.size(),b.size());

		for( int i = 0; i < a.size(); i++ ) {
			assertTrue(a.get(i) == b.get(i));
		}
	}
}
