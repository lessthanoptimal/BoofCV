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

package boofcv.alg.similar;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTrackerDefault;
import boofcv.alg.similar.SimilarImagesFromTracks.Frame;
import boofcv.alg.similar.SimilarImagesFromTracks.Match;
import boofcv.alg.structure.GenericLookUpSimilarImagesChecks;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestSimilarImagesFromTracks extends GenericLookUpSimilarImagesChecks {
	/**
	 * Fill in the algorithm with dummy values to test the contracts
	 */
	@Override public SimilarImagesFromTracks createFullyLoaded() {
		int numFeatures = 11;
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		alg.initialize(200, 210);

		alg.frames.resize(5);
		for (int i = 0; i < alg.frames.size; i++) {
			SimilarImagesFromTracks.Frame f = alg.frames.get(i);
			f.frameID = "" + i;
			alg.frameMap.put(f.frameID, f);
			f.initActive(numFeatures);
			for (int j = 0; j < i; j++) {
				Match m = alg.connections.grow();
				m.init(numFeatures);
				SimilarImagesFromTracks.Frame r = alg.frames.get(j);
				f.related.add(r);
				r.related.add(f);
				f.matches.add(m);
				r.matches.add(m);
				m.frameSrc = f;
				m.frameDst = r;
				for (int k = 0; k < m.size(); k++) {
					// Randomize the values make it obvious if the feature src/dst order is respected
					m.src[k] = rand.nextInt();
					m.dst[k] = rand.nextInt();
				}
			}
		}

		return alg;
	}

	private SimilarImagesFromTracks<PointTrack> createTracker() {
		return new SimilarImagesFromTracks<>(t -> t.featureId, ( t, pixel ) -> pixel.setTo(t.pixel));
	}

	@Test void processFrame() {
		final int numFrames = 20;

		var tracker = new MockTracker();
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		alg.initialize(200, 210);
		for (int i = 0; i < numFrames; i++) {
			tracker.process(null);
			alg.processFrame(tracker.getActiveTracks(null), tracker.frameID);
			tracker.offsetID += 5; // this will cause 5 tracks to not match
		}

		DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);
		DogArray<AssociatedIndex> associated = new DogArray<>(AssociatedIndex::new);
		List<String> imageIds = alg.getImageIDs();
		assertEquals(numFrames, imageIds.size());
		List<String> similarIds = new ArrayList<>();
		for (int targetIdx = alg.searchRadius; targetIdx < numFrames - alg.searchRadius; targetIdx++) {
			String targetId = "" + targetIdx;

			alg.lookupPixelFeats(targetId, features);
			assertEquals(tracker.numTracks, features.size);
			for (int i = 0; i < features.size(); i++) {
				assertEquals(i, features.get(i).x, UtilEjml.TEST_F64);
				assertEquals(i + 1, features.get(i).y, UtilEjml.TEST_F64);
			}

			alg.findSimilar("" + targetIdx, ( id ) -> true, similarIds);
			// 6 is expected and not 10 because every time 5 less tracks out of 20 match.
			assertEquals(6, similarIds.size());
			for (String similarId : similarIds) {
				int similarIdx = Integer.parseInt(similarId);
				// the expected number of matches is dependent on how many frames apart they are
				int expected = 20 - 5*Math.abs(similarIdx - targetIdx);
				assertTrue(alg.lookupAssociated(similarId, associated));
				assertEquals(expected, associated.size());
			}
		}
	}

	@Test void initialize() {
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		alg.initialize(50, 10);
		assertEquals(50, alg.imageWidth);
		assertEquals(10, alg.imageHeight);

		alg.connections.grow();
		alg.frames.grow();
		alg.frameMap.put("asdasd", alg.frames.grow());

		alg.initialize(51, 12);
		assertEquals(51, alg.imageWidth);
		assertEquals(12, alg.imageHeight);
		assertEquals(0, alg.connections.size);
		assertEquals(0, alg.frames.size);
		assertEquals(0, alg.frameMap.size());
	}

	@Test void createFrameSaveObservations() {
		var tracker = new MockTracker();
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		assertEquals(0, alg.frames.size);

		tracker.process(null);
		alg.createFrameSaveObservations(tracker.getActiveTracks(null), tracker.getFrameID());

		assertEquals(1, alg.frameMap.size());
		assertEquals(1, alg.frames.size);
		Frame frame = alg.frames.get(0);
		assertEquals("0", frame.frameID);
		assertSame(frame, alg.frameMap.get(frame.frameID));
		assertEquals(tracker.numTracks, frame.featureCount());

		var foundPixel = new Point2D_F64();
		for (int i = 0; i < frame.featureCount(); i++) {
			assertEquals(i, frame.getID(i));
			frame.getPixel(i, foundPixel);
			assertEquals(i, foundPixel.x, UtilEjml.TEST_F64);
			assertEquals(i + 1, foundPixel.y, UtilEjml.TEST_F64);
		}

		// see if it can handle adding another frame
		tracker.process(null);
		alg.createFrameSaveObservations(tracker.getActiveTracks(null), tracker.getFrameID());
		assertEquals(2, alg.frameMap.size());
		assertEquals(2, alg.frames.size);
		frame = alg.frames.get(1);
		assertEquals("1", frame.frameID);
		assertSame(frame, alg.frameMap.get(frame.frameID));
		assertEquals(tracker.numTracks, frame.featureCount());
	}

	/**
	 * No common features so no match should be created.
	 */
	@Test void findRelatedPastFrames_no_common() {
		var tracker = new MockTracker();
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		// create two frames with common observations
		for (int i = 0; i < 2; i++) {
			tracker.process(null);
			alg.createFrameSaveObservations(tracker.getActiveTracks(null), tracker.getFrameID());
		}
		// make the most recent incompatible with the previous
		Frame current = alg.frames.get(1);
		current.id_to_index.clear();
		for (int i = 0; i < current.featureCount(); i++) {
			current.ids[i] = 100 + i;
			current.id_to_index.put(current.ids[i], i);
		}

		alg.findRelatedPastFrames(current);
		for (int i = 0; i < alg.frames.size; i++) {
			assertEquals(0, alg.frames.get(i).matches.size());
		}
	}

	/**
	 * All but one observation match
	 */
	@Test void findRelatedPastFrames_matched() {
		var tracker = new MockTracker();
		SimilarImagesFromTracks<PointTrack> alg = createTracker();

		// create two frames with common observations
		for (int i = 0; i < 2; i++) {
			tracker.process(null);
			alg.createFrameSaveObservations(tracker.getActiveTracks(null), tracker.getFrameID());
		}
		// make the most recent incompatible with the previous
		Frame current = alg.frames.get(1);
		current.id_to_index.remove(15);
		current.ids[15] = 115;
		current.id_to_index.put(115, 15);
		// randomize the ID order to make things more interesting. This will mess up which observation they point to
		PrimitiveArrays.shuffle(current.ids, 0, current.ids.length, rand);

		alg.findRelatedPastFrames(current);
		for (int i = 0; i < alg.frames.size; i++) {
			Frame f = alg.frames.get(i);
			assertEquals(1, f.matches.size());
			Match matches = alg.connections.get(0);
			assertEquals(19, matches.size());
			assertNotSame(matches.frameSrc, matches.frameDst);
			assertTrue(matches.frameSrc == f || matches.frameDst == f);
			// checks to see if the ids are consistent is done in the system check above
		}
	}

	/**
	 * Nake sure that this configuration is used correctly internally
	 */
	@Test void minimumCommonTracks() {
		var tracker = new MockTracker();
		tracker.numTracks = 20;
		SimilarImagesFromTracks<PointTrack> alg = createTracker();
		alg.initialize(30, 40);
		// every frame there is one less track in common. Since there are 20 tracks, 18 means it should
		// be matched with 2 frames within its radius
		alg.minimumCommonTracks.setFixed(18);
		for (int i = 0; i < 6; i++) {
			tracker.process(null);
			tracker.offsetID++; // this is what causes N-1 features to be in common between frames
			alg.processFrame(tracker.getActiveTracks(null), tracker.getFrameID());

			if (i <= 2)
				continue;
			// Check the most recently added frame
			SimilarImagesFromTracks.Frame frame = alg.frames.getTail();
			assertEquals(2, frame.related.size());
		}

		// double check that it's matching them symmetrically
		assertEquals(4, alg.frames.get(3).related.size());
	}

	@Test void getImageIDs() {
		SimilarImagesFromTracks<PointTrack> alg = createTracker();
		for (int i = 0; i < 5; i++) {
			alg.frames.grow().frameID = "" + i;
		}
		List<String> found = alg.getImageIDs();
		assertEquals(5, found.size());
		for (int i = 0; i < 5; i++) {
			assertEquals("" + i, found.get(i));
		}
	}

	@Nested
	class CheckFrame {
		/**
		 * Did some hackery to force the value of no-matches ot be -1
		 */
		@Test void valueOfNoMatch() {
			var frame = new Frame();
			assertEquals(-1, frame.id_to_index.get(345354));
		}

		@Test void getPixel() {
			var frame = new Frame();
			frame.initActive(5);

			frame.observations[2] = 3;
			frame.observations[3] = 6;

			var found = new Point2D_F64();

			frame.getPixel(1, found);
			assertEquals(3, found.x, UtilEjml.TEST_F64);
			assertEquals(6, found.y, UtilEjml.TEST_F64);
		}
	}

	private static class MockTracker extends PointTrackerDefault<GrayU8> {
		long offsetID = 0;
		int numTracks = 20;
		DogArray<PointTrack> tracks = new DogArray<>(PointTrack::new);

		@Override
		public void process( GrayU8 image ) {
			tracks.reset();
			for (int i = 0; i < numTracks; i++) {
				PointTrack t = tracks.grow();
				t.featureId = offsetID + i;
				t.pixel.setTo(i, i + 1);
			}

			super.process(image);
		}

		@Override
		public int getTotalActive() {
			return tracks.size;
		}

		@Override
		public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
			if (list == null)
				list = new ArrayList<>();
			else
				list.clear();
			list.addAll(tracks.toList());
			return list;
		}
	}
}
