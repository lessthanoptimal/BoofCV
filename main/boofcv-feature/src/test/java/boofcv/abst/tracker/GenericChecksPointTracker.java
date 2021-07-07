/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard tests for implementations of {@link PointTracker}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class GenericChecksPointTracker<T extends ImageGray<T>> extends BoofStandardJUnit {

	public PointTracker<T> tracker;
	Random rand = new Random(234);
	int width = 100;
	int height = 80;
	GrayF32 image = new GrayF32(width, height);
	boolean shouldDropTracks;
	boolean shouldCreateInactive;

	int count;

	protected GenericChecksPointTracker( boolean shouldCreateInactive, boolean shouldDropTracks ) {
		this.shouldCreateInactive = shouldCreateInactive;
		this.shouldDropTracks = shouldDropTracks;
	}

	@BeforeEach
	public void initStandard() {
		ImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	/**
	 * Creates a new tracker with the specified number of tracks initially.
	 */
	public abstract PointTracker<T> createTracker();

	/**
	 * The cookie for tracks should not be set
	 */
	@Test void checkCookieNull() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);

		for (PointTrack t : tracker.getActiveTracks(null)) {
			assertNull(t.cookie);
		}
	}

	@Test void checkFrameID() {
		tracker = createTracker();
		assertEquals(-1, tracker.getFrameID());
		for (int i = 0; i < 5; i++) {
			processImage((T)image);
			assertEquals(i, tracker.getFrameID());
		}
		tracker.reset();
		assertEquals(-1, tracker.getFrameID());
		for (int i = 0; i < 5; i++) {
			processImage((T)image);
			assertEquals(i, tracker.getFrameID());
		}
	}

	/**
	 * After a track has been dropped the cookie should not be modified and returned when
	 * the track is recycled.
	 *
	 * NOTE: This test will return incorrect results if a large group of tracks are predeclared
	 * and recycled tracks are put at the end of the queue
	 */
	@Test void checkCookieSaved() {
		// create tracks
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);

		tracker.getActiveTracks(null).get(0).setCookie(1);

		// drop tracks
		tracker.dropAllTracks();

		// respawn and look for a cookie that's not null
		processImage((T)image);
		tracker.spawnTracks();

		int numFound = 0;
		for (PointTrack t : tracker.getActiveTracks(null)) {
			if (t.getCookie() != null)
				numFound++;
		}
		assertEquals(1, numFound);
	}

	/**
	 * High level spawn tracks test.
	 */
	@Test void spawnTracks() {
		// Process an image and make sure no new tracks have been spawned until requested
		tracker = createTracker();
		processImage((T)image);
		assertEquals(0, tracker.getAllTracks(null).size());
		assertEquals(0, tracker.getActiveTracks(null).size());
		assertEquals(0, tracker.getNewTracks(null).size());

		// Request that new tracks be spawned and ensure that all lists have been updated
		tracker.spawnTracks();

		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertTrue(tracker.getTotalActive() > 0);
		assertEquals(tracker.getTotalActive(), tracker.getNewTracks(null).size());
		checkInside(tracker.getAllTracks(null));

		// Tweak the input image and make sure that everything has the expected size
		ImageMiscOps.addGaussian(image, rand, 2, 0, 255);
		processImage((T)image);

		int beforeEach = tracker.getAllTracks(null).size();
		int beforeActive = tracker.getActiveTracks(null).size();

		assertTrue(beforeEach > 0);
		assertTrue(beforeActive > 0);
		assertEquals(0, tracker.getNewTracks(null).size());
		checkInside(tracker.getAllTracks(null));

		// Call spawn again. There should be more tracks now
		tracker.spawnTracks();

		assertTrue(beforeEach < tracker.getAllTracks(null).size());
		assertTrue(beforeActive < tracker.getActiveTracks(null).size());
		checkInside(tracker.getAllTracks(null));

		// there should be some pre-existing tracks
		assertTrue(tracker.getActiveTracks(null).size() !=
				tracker.getNewTracks(null).size());
	}

	/**
	 * When spawn is called, make sure that it doesn't return identical tracks
	 */
	@Test void spawnTracks_NoDuplicates() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getActiveTracks(null).size() > 0);

		int activeBefore = tracker.getActiveTracks(null).size();

		// drop a track
		PointTrack dropped = tracker.getActiveTracks(null).get(0);
		tracker.dropTrack(dropped);
		double x = dropped.pixel.x;
		double y = dropped.pixel.y;

		// process the exact same image
		// I think in just about every tracker nothing should change. Might need to change this test for some
		processImage((T)image);

		// should just spawn one track
		tracker.spawnTracks();
		assertEquals(activeBefore, tracker.getActiveTracks(null).size());
		assertEquals(1, tracker.getNewTracks(null).size());

		PointTrack found = tracker.getNewTracks(null).get(0);

		assertEquals(x, found.pixel.x);
		assertEquals(y, found.pixel.y);
	}

	/**
	 * Ensures that the spawn ID is set correctly
	 */
	@Test void spawnFrame_and_lastSeenFrame() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		// no new tracks are spawned so their ID's should not be updated
		for (int frame = 0; frame < 3; frame++) {
			for (var t : tracker.getActiveTracks(null)) {
				assertEquals(frame, t.lastSeenFrameID);
				assertEquals(0, t.spawnFrameID);
			}
			processImage((T)image);
		}

		// drop half the tracks
		List<PointTrack> tracks = tracker.getActiveTracks(null);
		int expectedZero = tracks.size()/2 - 1;
		for (int i = tracks.size() - expectedZero - 1; i >= 0; i--) {
			tracker.dropTrack(tracks.get(i));
		}

		// spawn more
		processImage((T)image);
		tracker.spawnTracks();

		// count their spawn ID
		int count0 = 0;
		int count1 = 0;

		for (var t : tracker.getActiveTracks(null)) {
			assertEquals(4, t.lastSeenFrameID);
			if (t.spawnFrameID == 0)
				count0++;
			else if (t.spawnFrameID == 4)
				count1++;
			else
				throw new RuntimeException("Unexpected " + t.spawnFrameID);
		}

		assertEquals(expectedZero, count0);
		assertTrue(count1 > 5);
	}

	@Test void dropAllTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertTrue(tracker.getActiveTracks(null).size() > 0);

		tracker.dropAllTracks();

		assertEquals(0, tracker.getAllTracks(null).size());
		assertEquals(0, tracker.getActiveTracks(null).size());
		// tracks which have been dropped by request should not be included in this list
		assertEquals(0, tracker.getDroppedTracks(null).size());
	}

	/**
	 * Cause tracks to be dropped during the update
	 */
	@Test void updateThenDropTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		int BeforeEach = tracker.getAllTracks(null).size();
		int beforeActive = tracker.getActiveTracks(null).size();
		assertTrue(BeforeEach > 0);

		assertEquals(0, tracker.getDroppedTracks(null).size());

		// make the image a poor match, causing tracks to be dropped
		GImageMiscOps.fill(image, 0);
		processImage((T)image);

		int afterAll = tracker.getAllTracks(null).size();
		int afterActive = tracker.getActiveTracks(null).size();
		int afterDropped = tracker.getDroppedTracks(null).size();
		int afterInactive = tracker.getInactiveTracks(null).size();

		// make sure some change happened
		assertTrue(afterActive < beforeActive);

		// algorithm specific checks
		if (shouldDropTracks) {
			assertTrue(afterDropped > 0);
			assertTrue(afterAll < BeforeEach);
		} else {
			assertEquals(0, afterDropped);
			assertEquals(afterAll, BeforeEach);
		}

		if (shouldCreateInactive)
			assertTrue(afterInactive > 0);
		else
			assertEquals(0, afterInactive);

		// this might not be true for all trackers...
		assertEquals(0, afterActive);
		// some tracks should either be dropped or become inactive
		assertTrue(afterDropped + afterInactive > 0);
		// note that some trackers will not add any features to the dropped list since
		// it will try to respawn them
		assertEquals(BeforeEach - afterAll, tracker.getDroppedTracks(null).size());
	}

	@Test void dropTrack() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		List<PointTrack> tracks = tracker.getActiveTracks(null);

		int before = tracks.size();
		assertTrue(before > 0);
		assertTrue(tracker.dropTrack(tracks.get(0)));
		// a second request to drop the track should do nothing
		assertFalse(tracker.dropTrack(tracks.get(0)));

		// the track should be removed from the all and active lists
		assertEquals(before - 1, tracker.getAllTracks(null).size());
		assertEquals(before - 1, tracker.getActiveTracks(null).size());

		// tracks which have been dropped by request should not be included in this list
		assertEquals(0, tracker.getDroppedTracks(null).size());
	}

	@Test void trackUpdate() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		int before = tracker.getAllTracks(null).size();
		assertTrue(before > 0);
		checkUniqueFeatureID();

		// by adding a little bit of noise the features should move slightly
		ImageMiscOps.addUniform(image, rand, 0, 5);
		processImage((T)image);
		checkUniqueFeatureID();

		int after = tracker.getAllTracks(null).size();
		int dropped = tracker.getDroppedTracks(null).size();
		assertEquals(before, after + dropped);
	}

	@Test void reset() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();
		assertTrue(tracker.getAllTracks(null).size() > 0);
		assertEquals(0, tracker.getAllTracks(null).get(0).featureId);

		tracker.reset();

		// add several tracks
		ImageMiscOps.addUniform(image, rand, 0, 5);
		processImage((T)image);
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

		for (int i = 0; i < l.size(); i++) {
			PointTrack a = l.get(i);
			for (int j = i + 1; j < l.size(); j++) {
				PointTrack b = l.get(j);

				assertTrue(a.featureId != b.featureId);
			}
		}
	}

	@Test void getAllTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<>();
		assertSame(input, tracker.getAllTracks(input));

		List<PointTrack> ret = tracker.getAllTracks(null);

		//sanity check
		assertTrue(ret.size() > 0);

		checkIdentical(input, ret);
	}

	@Test void getActiveTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<>();
		assertSame(input, tracker.getActiveTracks(input));

		List<PointTrack> ret = tracker.getActiveTracks(null);

		//sanity check
		assertTrue(ret.size() > 0);

		checkIdentical(input, ret);
	}

	@Test void getInactiveTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		// create a situation where tracks might become inactive
		GImageMiscOps.fill(image, 0);
		processImage((T)image);

		List<PointTrack> input = new ArrayList<>();
		assertSame(input, tracker.getInactiveTracks(input));

		List<PointTrack> ret = tracker.getInactiveTracks(null);

		checkIdentical(input, ret);
	}

	@Test void getDroppedTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		// create a situation where tracks might be dropped
		GImageMiscOps.fill(image, 0);
		processImage((T)image);

		List<PointTrack> input = new ArrayList<>();
		assertSame(input, tracker.getDroppedTracks(input));
		if (shouldDropTracks)
			assertTrue(input.size() > 0);

		List<PointTrack> ret = tracker.getDroppedTracks(null);

		checkIdentical(input, ret);
	}

	@Test void getNewTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		List<PointTrack> input = new ArrayList<>();
		assertSame(input, tracker.getNewTracks(input));

		List<PointTrack> ret = tracker.getNewTracks(null);

		//sanity check
		assertTrue(ret.size() > 0);

		checkIdentical(input, ret);
	}

	/**
	 * Makes sure the number of active tracks makes sense
	 */
	@Test void totalCounts() {
		// create tracks
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		// These should be in agreement
		assertEquals(tracker.getActiveTracks(null).size(), tracker.getTotalActive());
		assertEquals(tracker.getInactiveTracks(null).size(), tracker.getTotalInactive());
		assertTrue(tracker.getTotalActive() > 0);

		// see if still works after the next frame is processed
		processImage((T)image);
		assertEquals(tracker.getActiveTracks(null).size(), tracker.getTotalActive());
		assertEquals(tracker.getInactiveTracks(null).size(), tracker.getTotalInactive());
		assertTrue(tracker.getTotalActive() > 0);

		// Make sure when tracks are dropped this count is reset too
		tracker.dropAllTracks();
		assertEquals(0, tracker.getTotalActive());
		assertEquals(0, tracker.getTotalInactive());
	}

	@Test void clearLists() {
		tracker = createTracker();
		processImage((T)image);

		List<PointTrack> tracks = new ArrayList<>();

		tracker.spawnTracks();

		// pass in the same list, which should be cleared every call
		assertEquals(tracker.getTotalActive(), tracker.getActiveTracks(tracks).size());
		// call twice, since it was originally empty
		assertEquals(tracker.getTotalActive(), tracker.getActiveTracks(tracks).size());
		assertEquals(tracker.getTotalInactive(), tracker.getInactiveTracks(tracks).size());
		assertEquals(tracker.getTotalActive(), tracker.getNewTracks(tracks).size());
		assertEquals(0, tracker.getDroppedTracks(tracks).size());
		assertEquals(tracker.getTotalActive() + tracker.getTotalInactive(),
				tracker.getAllTracks(tracks).size());
		assertTrue(tracker.getTotalActive() > 0);

		// see if still works after the next frame is processed
		processImage((T)image);
		assertEquals(tracker.getTotalActive(), tracker.getActiveTracks(tracks).size());
		assertEquals(tracker.getTotalInactive(), tracker.getInactiveTracks(tracks).size());
		tracks.add(new PointTrack());
		assertEquals(0, tracker.getNewTracks(tracks).size());
		assertEquals(tracker.getTotalActive() + tracker.getTotalInactive(),
				tracker.getAllTracks(tracks).size());
		assertTrue(tracker.getTotalActive() > 0);
	}

	@Test void dropTracks() {
		tracker = createTracker();
		processImage((T)image);
		tracker.spawnTracks();

		// This is the number of tracks it should process
		int expectedTotal = tracker.getTotalActive() + tracker.getTotalInactive();
		// set a cookie in one of the tracks which will be dropped
		tracker.getActiveTracks(null).get(0).setCookie(1);
		tracker.getActiveTracks(null).get(1).setCookie(2);

		count = 0;
		tracker.dropTracks(t -> {
			count++;
			return t.featureId%2 == 0;
		});
		assertEquals(expectedTotal, count);
		int afterTotal = tracker.getTotalActive() + tracker.getTotalInactive();
		assertEquals(expectedTotal/2, afterTotal);
		List<PointTrack> after = tracker.getActiveTracks(null);
		assertNotNull(after.get(0).cookie);
		assertNull(after.get(1).cookie);
	}

	@Test void imageTypeIsNotNull() {
		// Very basic sanity check
		assertNotNull(createTracker().getImageType());
	}

	@Test void canProcessImageType() {
		tracker = createTracker();

		// Create an image, fill it with random noise to make it less likely to hit an edge case
		T image = tracker.getImageType().createImage(width, height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);

		// See if it can process the image and not throw some sort of exception due to image type
		tracker.process(image);
	}

	private void checkIdentical( List<PointTrack> a, List<PointTrack> b ) {
		assertEquals(a.size(), b.size());

		for (int i = 0; i < a.size(); i++) {
			assertSame(a.get(i), b.get(i));
		}
	}

	/**
	 * Makes sure all the tracks are inside the image
	 */
	protected void checkInside( List<PointTrack> tracks ) {
		for (PointTrack t : tracks) {
			Point2D_F64 p = t.pixel;
			if (p.x < 0 || p.y < 0 || p.x > width - 1 || p.y > height - 1)
				fail("track is outside of the image: " + p.x + " " + p.y);
		}
	}

	/**
	 * Performs all the standard tracking steps
	 */
	protected void processImage( T image ) {
		tracker.process(image);
	}
}
