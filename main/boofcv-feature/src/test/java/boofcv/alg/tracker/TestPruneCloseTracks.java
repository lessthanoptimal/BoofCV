/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker;

import boofcv.abst.tracker.PointTrack;
import boofcv.misc.BoofMiscOps;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestPruneCloseTracks extends BoofStandardJUnit {
	int width = 14;
	int height = 10;

	@Test
	void negative() {
		var alg = PruneCloseTracks.prunePointTrack(2);
		alg.init(10, 20);
		var tracks = new ArrayList<PointTrack>();

		// space them out far enough so that non of them should be dropped
		tracks.add(new PointTrack(0, 3, 4));
		tracks.add(new PointTrack(0, 0, 0));
		tracks.add(new PointTrack(0, 6, 6));

		var dropped = new ArrayList<PointTrack>();
		alg.process(tracks, dropped);

		assertEquals(0, dropped.size());
	}

	@Test
	void positive() {
		var alg = PruneCloseTracks.prunePointTrack(2);
		alg.init(10, 20);

		var tracks = new ArrayList<PointTrack>();
		tracks.add(new PointTrack(3, 3, 4));
		tracks.add(new PointTrack(9, 0, 0));

		// Add a point surrounding (3,3) and see if it's always dropped
		// sometimes it won't be in the same cell
		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				tracks.add(new PointTrack(3 + x, 3 + y, 4));

				var dropped = new ArrayList<PointTrack>();
				alg.process(tracks, dropped);

				assertEquals(3, tracks.size());
				assertEquals(1, dropped.size(), x + " " + y);

				tracks.remove(2);
			}
		}
	}

	/**
	 * Call multiple times and see if it produces the same output
	 */
	@Test
	void multipleCalls() {
		List<PointTrack> tracks = createRandom(50, width, height);

		var alg = PruneCloseTracks.prunePointTrack(2);
		alg.init(width, height);
		var found0 = new ArrayList<PointTrack>();
		var found1 = new ArrayList<PointTrack>();

		alg.process(tracks, found0);
		alg.process(tracks, found1);

		assertTrue(found0.size() > 5);
		assertEquals(found0.size(), found1.size());
		for (int i = 0; i < found0.size(); i++) {
			assertSame(found0.get(i), found1.get(i));
		}
	}

	/**
	 * Two tracks will be in conflict with each other and the ambiguity resolver doesn't help. Does it use trackID?
	 */
	@Test
	void handleAmbiguousRemove() {
		List<PointTrack> tracks = createRandom(3, width, height);
		tracks.get(0).featureId = 3;
		tracks.get(1).featureId = 2;
		tracks.get(2).featureId = 1;

		// set the radius so large it will cover the entire image
		var alg = PruneCloseTracks.prunePointTrack(100);
		alg.init(width, height);
		alg.ambiguityResolver = ( a, b ) -> 0; // useless resolver
		var found = new ArrayList<PointTrack>();
		alg.process(tracks, found);

		// only one should be left
		assertEquals(2, found.size());

		assertTrue(found.contains(tracks.get(0)));
		assertTrue(found.contains(tracks.get(1)));
	}

	/**
	 * Makes sure what's dropped is independent of the track's order in the list
	 */
	@Test
	void checkOrderIndependent() {
		var alg = PruneCloseTracks.prunePointTrack(2);
		alg.init(width, height);
		// let's give it a horrible resolver that tells us nothing so that it uses the default resolution
		alg.ambiguityResolver = ( o1, o2 ) -> 0;

		for (int trial = 0; trial < 5; trial++) {
			List<PointTrack> tracks = createRandom(200, width, height);

			var found0 = new ArrayList<PointTrack>();
			var found1 = new ArrayList<PointTrack>();

			alg.process(tracks, found0);
			Collections.shuffle(tracks, rand);
			alg.process(tracks, found1);

			// sanity check
			assertFalse(BoofMiscOps.containsDuplicates(found0));
			assertTrue(found0.size() > 5 && found0.size() < tracks.size());
			// should be the same size and contain the same elements
			assertEquals(found0.size(), found1.size());
			for (var a : found0) {
				assertTrue(found1.contains(a));
			}
		}
	}

	/**
	 * Compare results to brute force algorithm. This also tests the behavior of the default
	 * resolver
	 */
	@Test
	void compareBruteForce() {
		List<PointTrack> tracks = createRandom(200, width, height);
		var alg = PruneCloseTracks.prunePointTrack(2);
		alg.init(width, height);

		var expected = bruteForce(tracks, 2);
		var found = new ArrayList<PointTrack>();
		alg.process(tracks, found);

		assertEquals(expected.size(), found.size());
		for (var a : expected) {
			assertTrue(found.contains(a));
		}
	}

	List<PointTrack> bruteForce( List<PointTrack> tracks, int radius ) {
		List<PointTrack> dropped = new ArrayList<>();
		for (int i = 0; i < tracks.size(); i++) {
			PointTrack a = tracks.get(i);
			boolean keep = true;
			for (int j = 0; j < tracks.size(); j++) {
				if (i == j)
					continue;
				PointTrack b = tracks.get(j);
				double d = Math.max(Math.abs(a.pixel.x - b.pixel.x), Math.abs(a.pixel.y - b.pixel.y));
				if (d < radius) {
					if (a.featureId > b.featureId) {
						keep = false;
						break;
					}
				}
			}
			if (!keep) {
				dropped.add(tracks.get(i));
			}
		}
		return dropped;
	}

	List<PointTrack> createRandom( int total, int width, int height ) {
		var tracks = new ArrayList<PointTrack>();

		for (int i = 0; i < total; i++) {
			PointTrack t = new PointTrack();
			t.pixel.x = rand.nextDouble()*0.9999*width; // make setting it to the width impossible
			t.pixel.y = rand.nextDouble()*0.9999*height;
			t.featureId = i;
			t.spawnFrameID = rand.nextInt(100);
			tracks.add(t);
		}

		return tracks;
	}
}
