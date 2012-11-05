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

package boofcv.abst.feature.trackers;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;


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
	 * The tracker should drop all the tracks on update
	 */
	public abstract void trackUpdateDrop( ImagePointTracker<T> tracker );

	/**
	 * Tracker should change the position of existing tracks
	 */
	public abstract void trackUpdateChangePosition( ImagePointTracker<T> tracker );

	/**
	 * Creates a new tracker with the specified number of tracks initially.
	 */
	public abstract ImagePointTracker<T> createTracker();

	@Test
	public void spawnTracks() {
		tracker = createTracker();

		assertEquals(0,tracker.getActiveTracks().size());
		assertTrue(tracker.getNewTracks().size() == 0 );

		tracker.spawnTracks();

		assertTrue(tracker.getActiveTracks().size()>0);
		assertTrue(tracker.getNewTracks().size() > 0 );
	}

	@Test
	public void dropAllTracks() {
		tracker = createTracker();
		addTracks(5);
		assertEquals(5,tracker.getActiveTracks().size());
		tracker.dropAllTracks();
		assertEquals(0,tracker.getActiveTracks().size());
		// tracks which have been dropped by request should not be included in this list
		assertEquals(0,tracker.getDroppedTracks().size());
	}

	@Test
	public void testUpdateTrackDrop() {
		tracker = createTracker();
		addTracks(5);
		assertEquals(5,tracker.getActiveTracks().size());
		assertEquals(0,tracker.getDroppedTracks().size());

		trackUpdateDrop(tracker);

		assertEquals(0,tracker.getActiveTracks().size());
		assertEquals(5,tracker.getDroppedTracks().size());
	}
	
	@Test
	public void testRequestDrop() {
		tracker = createTracker();
		addTracks(5);
		
		List<PointTrack> tracks = tracker.getActiveTracks();
		
		tracker.dropTrack(tracks.get(2));
		
		assertEquals(4,tracker.getActiveTracks().size());
		// tracks which have been dropped by request should not be included in this list
		assertEquals(0,tracker.getDroppedTracks().size());
	}

	@Test
	public void testTrackUpdate() {
		tracker = createTracker();
		addTracks(5);
		assertEquals(5,tracker.getActiveTracks().size());
		checkUniqueFeatureID();

		// by adding a little bit of noise the features should move slightly
		ImageTestingOps.addUniform(image,rand,0,5);
		trackUpdateChangePosition(tracker);
		checkUniqueFeatureID();

		// hmm it is totally possible that some features would be dropped.  might
		// have to make this more robust in the future
		assertEquals(5,tracker.getActiveTracks().size());
	}

	@Test
	public void reset() {
		fail("IMplement");
	}

	private void addTracks( int num ) {
		for( int i = 0; i < num; i++ ) {
			float x = rand.nextFloat()*width;
			float y = rand.nextFloat()*height;

			tracker.addTrack(x,y);
		}
	}

	/**
	 * Makes sure each feature has a unique feature number
	 */
	private void checkUniqueFeatureID() {
		List<PointTrack> l = tracker.getActiveTracks();

		for( int i = 0; i < l.size(); i++ ) {
			PointTrack a = l.get(i);
			for( int j = i+1; j < l.size(); j++ ) {
				PointTrack b = l.get(j);

				assertTrue(a.featureId != b.featureId);
			}
		}
	}
}
