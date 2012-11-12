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

import boofcv.alg.misc.ImageTestingOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementations of {@link boofcv.abst.feature.tracker.ImagePointTracker}.
 *
 * @author Peter Abeles
 */
public abstract class StandardImagePointTracker <T extends ImageSingleBand> {

	public ImagePointTracker<T> tracker;
	Random rand = new Random(234);
	int width = 100;
	int height = 80;
	ImageFloat32 image = new ImageFloat32(width,height);

	@Before
	public void initStandard() {
		ImageTestingOps.randomize(image,rand,0,100);
	}

	/**
	 * Creates a new tracker with the specified number of tracks initially.
	 */
	public abstract ImagePointTracker<T> createTracker();

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

	@Test
	public void spawnTracks() {
		tracker = createTracker();
		tracker.process((T)image);
		assertEquals(0,tracker.getAllTracks(null).size());
		assertEquals(0,tracker.getActiveTracks(null).size());
		assertTrue(tracker.getNewTracks(null).size() == 0 );

		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertTrue(tracker.getActiveTracks(null).size()>0);
		assertTrue(tracker.getActiveTracks(null).size() ==
				tracker.getNewTracks(null).size() );

		ImageTestingOps.addGaussian(image,rand,1,0,255);
		tracker.process((T)image);

		int beforeAll = tracker.getAllTracks(null).size();
		int beforeActive = tracker.getActiveTracks(null).size();

		assertTrue(beforeAll > 0);
		assertTrue(beforeActive>0);

		tracker.spawnTracks();

		assertTrue(beforeAll < tracker.getAllTracks(null).size());
		assertTrue(beforeActive < tracker.getActiveTracks(null).size());

		// there should be some pre-existing tracks
		assertTrue(tracker.getActiveTracks(null).size() !=
				tracker.getNewTracks(null).size() );
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
		GeneralizedImageOps.fill(image, 0);
		tracker.process((T) image);

		int afterAll = tracker.getAllTracks(null).size();
		int afterActive = tracker.getActiveTracks(null).size();

		assertTrue(afterActive < beforeActive);

		assertTrue(afterAll <= beforeAll );
		assertEquals(0,tracker.getActiveTracks(null).size());
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
		ImageTestingOps.addUniform(image,rand,0,5);
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
		ImageTestingOps.addUniform(image,rand,0,5);
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
}
