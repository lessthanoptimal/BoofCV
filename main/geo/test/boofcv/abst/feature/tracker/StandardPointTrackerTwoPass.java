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

import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Standard tests for {@link PointTrackerTwoPass}
 *
 * @author Peter Abeles
 */
public abstract class StandardPointTrackerTwoPass<T extends ImageGray>
		extends StandardPointTracker<T>
{

	PointTrackerTwoPass<T> tracker2;

	protected StandardPointTrackerTwoPass(boolean shouldCreateInactive, boolean shouldDropTracks) {
		super(shouldCreateInactive, shouldDropTracks);
	}

	@Override
	public abstract PointTrackerTwoPass<T> createTracker();

	/**
	 * Tests basic functionality for process(), performSecondPass(), and finishTracking()
	 */
	@Test
	public void process() {
		tracker2 = createTracker();
		tracker2.process((T)image);
		tracker2.finishTracking();
		tracker2.spawnTracks();

		int allBefore = tracker2.getActiveTracks(null).size();
		int activeBefore = tracker2.getAllTracks(null).size();

		assertTrue(allBefore > 0 );
		assertTrue(activeBefore > 0 );

		tracker2.process((T)image);
		List<PointTrack> tracks = tracker2.getAllTracks(null);
		// since the image is the same the tracks should all be the same
		assertEquals(allBefore, tracks.size());
		assertEquals(activeBefore, tracker2.getActiveTracks(null).size());

		// The bad hints below should cause stuff to break
		for( PointTrack t : tracks ) {
			tracker2.setHint(0,0,t);
		}
		tracker2.performSecondPass();
		// make sure that lists which shouldn't change haven't changed
		assertEquals(allBefore, tracker2.getAllTracks(null).size());
		assertEquals(0, tracker2.getDroppedTracks(null).size());
		assertEquals(0, tracker2.getNewTracks(null).size());
		// active list can change
		assertTrue(activeBefore != tracker2.getActiveTracks(null).size());

		// now everything can change
		tracker2.finishTracking();
		checkInside(tracker2.getAllTracks(null));
		if( shouldDropTracks ) {
			assertTrue(allBefore > tracker2.getAllTracks(null).size());
			assertTrue( tracker2.getDroppedTracks(null).size() > 0 );
		}
		assertTrue(activeBefore > tracker2.getActiveTracks(null).size());
		checkInside(tracker2.getAllTracks(null));
	}

	/**
	 * Should be possible to do more than one second pass. Very minimal test, just checks number of elements in lists.
	 */
	@Test
	public void performSecondPass_multiple() {
		tracker2 = createTracker();
		tracker2.process((T) image);
		tracker2.finishTracking();
		tracker2.spawnTracks();

		int allBefore = tracker2.getActiveTracks(null).size();
		int activeBefore = tracker2.getAllTracks(null).size();

		tracker2.process((T) image);
		tracker2.performSecondPass();
		tracker2.performSecondPass();

		// since the same image was processed twice nothing should change
		tracker2.finishTracking();
		checkInside(tracker2.getAllTracks(null));
		assertEquals(allBefore, tracker2.getAllTracks(null).size());
		assertEquals(activeBefore, tracker2.getActiveTracks(null).size());
	}


	/**
	 * Makes sure it can take a hint before process is called and not just for the second pass
	 */
	@Test
	public void hintBeforeProcess() {
		tracker2 = createTracker();
		tracker2.process((T)image);
		tracker2.finishTracking();
		tracker2.spawnTracks();

		int allBefore = tracker2.getActiveTracks(null).size();
		int activeBefore = tracker2.getAllTracks(null).size();

		assertTrue(allBefore > 0 );
		assertTrue(activeBefore > 0 );

		// provide bad hints before process is called
		List<PointTrack> tracks = tracker2.getAllTracks(null);
		for( PointTrack t : tracks ) {
			tracker2.setHint(0,0,t);
		}
		// process should do bad stuff now
		tracker2.process((T)image);
		// make sure that lists which shouldn't change haven't changed
		assertEquals(allBefore, tracker2.getAllTracks(null).size());
		assertEquals(0, tracker2.getDroppedTracks(null).size());
		assertEquals(0, tracker2.getNewTracks(null).size());
		assertTrue(activeBefore != tracker2.getActiveTracks(null).size());
		checkInside(tracker2.getAllTracks(null));

		// and finalize the bad tracking
		tracker2.finishTracking();
		if( shouldDropTracks ) {
			assertTrue(allBefore > tracker2.getAllTracks(null).size());
			assertTrue( tracker2.getDroppedTracks(null).size() > 0 );
		}
		assertTrue(activeBefore > tracker2.getActiveTracks(null).size());
		checkInside(tracker2.getAllTracks(null));

	}

	/**
	 * Calling process and finishTracking should be equivalent to process in PointTracker
	 */
	@Override
	protected void processImage( T image ) {
		PointTrackerTwoPass<T> tracker = (PointTrackerTwoPass<T>)this.tracker;

		tracker.process(image);
		tracker.finishTracking();
	}
}
