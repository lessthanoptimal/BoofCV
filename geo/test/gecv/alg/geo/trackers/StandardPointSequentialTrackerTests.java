/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.trackers;

import gecv.alg.geo.AssociatedPair;
import gecv.alg.geo.PointSequentialTracker;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_F64;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementations of {@link gecv.alg.geo.PointSequentialTracker}.
 *
 * @author Peter Abeles
 */
public abstract class StandardPointSequentialTrackerTests {

	public PointSequentialTracker<?> tracker;
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
	public abstract void trackUpdateDrop( PointSequentialTracker tracker );

	/**
	 * Tracker should change the position of existing tracks
	 */
	public abstract void trackUpdateChangePosition( PointSequentialTracker tracker );

	/**
	 * Creates a new tracker with the specified number of tracks initially.
	 */
	public abstract PointSequentialTracker createTracker();

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
		tracker.dropTracks();
		assertEquals(0,tracker.getActiveTracks().size());
		assertEquals(5,tracker.getDroppedTracks().size());
	}

	@Test
	public void testTrackDrop() {
		tracker = createTracker();
		addTracks(5);
		assertEquals(5,tracker.getActiveTracks().size());
		assertEquals(0,tracker.getDroppedTracks().size());

		trackUpdateDrop(tracker);

		assertEquals(0,tracker.getActiveTracks().size());
		assertEquals(5,tracker.getDroppedTracks().size());
	}

	@Test
	public void testTrackUpdate() {
		tracker = createTracker();
		addTracks(5);
		assertEquals(5,tracker.getActiveTracks().size());
		for( AssociatedPair p : tracker.getActiveTracks() ) {
			assertTrue(p.currLoc.x == p.keyLoc.x);
			assertTrue(p.currLoc.y == p.keyLoc.y);
		}
		checkUniqueFeatureID();

		// by adding a little bit of noise the features should move slightly
		ImageTestingOps.addUniform(image,rand,0,5);
		trackUpdateChangePosition(tracker);
		checkUniqueFeatureID();

		// hmm it is totally possible that some features would be dropped.  might
		// have to make this more robust in the future
		assertEquals(5,tracker.getActiveTracks().size());
		for( AssociatedPair p : tracker.getActiveTracks() ) {
			assertTrue(p.currLoc.x != p.keyLoc.x);
			assertTrue(p.currLoc.y != p.keyLoc.y);
		}
	}

	@Test
	public void testSetKeyFrame() {
		tracker = createTracker();
		addTracks(5);

		// by adding a little bit of noise the features should move slightly
		ImageTestingOps.addUniform(image,rand,0,5);
		trackUpdateChangePosition(tracker);

		// the initial location and the current location should be different
		assertEquals(5,tracker.getActiveTracks().size());
		List<Point2D_F64> currPts = new ArrayList<Point2D_F64>();
		for( AssociatedPair p : tracker.getActiveTracks() ) {
			currPts.add(p.currLoc.copy());
			assertTrue(p.currLoc.x != p.keyLoc.x);
			assertTrue(p.currLoc.y != p.keyLoc.y);
		}

		// after set to keyframe is called they should be the same
		tracker.setCurrentToKeyFrame();
		for( int i = 0; i < currPts.size(); i++ ) {
			Point2D_F64 c = currPts.get(i);
			AssociatedPair p = tracker.getActiveTracks().get(i);

			assertTrue(c.x == p.keyLoc.x);
			assertTrue(c.y == p.keyLoc.y);
		}
		checkUniqueFeatureID();
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
		List<AssociatedPair> l = tracker.getActiveTracks();

		for( int i = 0; i < l.size(); i++ ) {
			AssociatedPair a = l.get(i);
			for( int j = i+1; j < l.size(); j++ ) {
				AssociatedPair b = l.get(j);

				assertTrue(a.featureId != b.featureId);
			}
		}
	}
}
