/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d2;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayU8;
import georegression.struct.InvertibleTransform;
import georegression.struct.se.Se2_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestImageMotionPointTrackerKey {

	/**
	 * Give it a very simple example and see if it computes the correct motion and has the expected behavior
	 * when processing an image
	 */
	@Test
	public void process() {
		// what the initial transform should be
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Se2_F32> matcher = new DummyModelMatcher<>(computed, 5);

		GrayU8 input = new GrayU8(20,30);

		ImageMotionPointTrackerKey<GrayU8,Se2_F32> alg =
				new ImageMotionPointTrackerKey<>(tracker, matcher, null, model, 1000);
		// it calls reset in the constructor. This ensures reset() and the initial state are the same
		assertEquals(1, tracker.numReset);

		// the first time it processes an image it should always return false since no motion can be estimated
		assertFalse(alg.process(input));
		assertFalse(alg.isKeyFrame());
		assertEquals(0, tracker.numSpawn);

		// make the current frame into the keyframe
		// request that the current frame is a keyframe
		alg.changeKeyFrame();
		assertEquals(0, tracker.numDropAll);
		assertEquals(1, tracker.numSpawn);
		assertTrue(alg.isKeyFrame());

		// now it should compute some motion
		assertTrue(alg.process(input));
		assertFalse(alg.isKeyFrame());

		// no new tracks should have been spawned
		assertEquals(1, tracker.numSpawn);

		// test the newly computed results
		assertEquals(computed.getX(), alg.getKeyToCurr().getX(), 1e-8);
		assertEquals(computed.getX(), alg.getWorldToCurr().getX(), 1e-8);

		// see if reset does its job
		alg.reset();
		assertEquals(0, tracker.numDropAll);
		assertEquals(2, tracker.numReset);
		assertEquals(-1, alg.getFrameID() );
		assertEquals(0, alg.getKeyToCurr().getX(), 1e-8);
		assertEquals(0, alg.getWorldToCurr().getX(), 1e-8);
	}

	/**
	 * Test the keyframe based on the definition of the keyframe
	 */
	@Test
	public void changeKeyFrame() {
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Se2_F32> matcher = new DummyModelMatcher<>(computed, 5);

		GrayU8 input = new GrayU8(20,30);

		ImageMotionPointTrackerKey<GrayU8,Se2_F32> alg = new ImageMotionPointTrackerKey<>(tracker, matcher, null, model, 100);

		// process twice to change the transforms
		alg.process(input);
		alg.changeKeyFrame();
		alg.process(input);

		// sanity check
		Se2_F32 worldToKey = alg.getWorldToKey();
		assertEquals(0, worldToKey.getX(), 1e-8);
		assertEquals(1, tracker.numSpawn);

		// invoke the function being tested
		alg.changeKeyFrame();

		// the keyframe should be changed and new tracks spawned
		assertEquals(2, tracker.numSpawn);

		// worldToKey should now be equal to worldToCurr
		worldToKey = alg.getWorldToKey();
		assertEquals(computed.getX(), worldToKey.getX(), 1e-8);
	}

	/**
	 * See if tracks are pruned after not being in inlier set for X time
	 */
	@Test
	public void testPrune() {
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Se2_F32> matcher = new DummyModelMatcher<>(computed, 5);

		GrayU8 input = new GrayU8(20,30);

		ImageMotionPointTrackerKey<GrayU8,Se2_F32> alg = new ImageMotionPointTrackerKey<>(tracker, matcher, null, model, 5);

		// create tracks such that only some of them will be dropped
		tracker.frameID = 9;
		for( int i = 0; i < 10; i++ ) {
			PointTrack t = new PointTrack();
			AssociatedPairTrack a = new AssociatedPairTrack();
			a.lastUsed = i;
			t.cookie = a;

			tracker.list.add(t);
		}

		// update
		alg.process(input);

		// check to see how many were dropped
		assertEquals(6,tracker.numDropped);
	}

	public static class DummyTracker implements PointTracker<GrayU8>
	{
		public int numSpawn = 0;
		public int numDropped = 0;
		public int numDropAll = 0;
		public int numReset = 0;
		public long frameID = -1;

		List<PointTrack> list = new ArrayList<>();
		List<PointTrack> listSpawned = new ArrayList<>();

		@Override
		public void reset() {numReset++;frameID=-1;}

		@Override
		public long getFrameID() { return frameID; }

		@Override
		public int getTotalActive() { return 0; }

		@Override
		public int getTotalInactive() { return 0; }

		@Override
		public void process(GrayU8 image) {frameID++;}

		@Override
		public void spawnTracks() {
			numSpawn++;
			listSpawned.clear();
			for( int i = 0; i < 5; i++ ){
				PointTrack t = new PointTrack();
				listSpawned.add(t);
				list.add(t);
			}
		}

		@Override
		public void dropAllTracks() {
			numDropAll++;
		}

		@Override
		public boolean dropTrack(PointTrack track) {numDropped++;return true;}

		@Override
		public void dropTracks(Dropper dropper) {
			throw new RuntimeException("HMM");
		}

		@Override
		public List<PointTrack> getAllTracks( List<PointTrack> list ) {
			if( list == null ) list = new ArrayList<>();
			list.addAll(this.list);
			return list;
		}

		@Override
		public List<PointTrack> getActiveTracks(List<PointTrack> list) {
			return getAllTracks(list);
		}

		@Override
		public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
			if( list == null )
				list = new ArrayList<>();
			return list;
		}

		@Override
		public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public List<PointTrack> getNewTracks(List<PointTrack> list) {
			if( list == null ) list = new ArrayList<>();
			list.addAll(this.listSpawned);
			return list;
		}
	}

	public static class DummyModelMatcher<T extends InvertibleTransform> implements ModelMatcher<T,AssociatedPair> {

		T found;
		int matchSetSize;

		public DummyModelMatcher(T found, int matchSetSize) {
			this.found = found;
			this.matchSetSize = matchSetSize;
		}

		@Override
		public boolean process(List<AssociatedPair> dataSet) {
			return true;
		}

		@Override
		public T getModelParameters() {
			return found;
		}

		@Override
		public List<AssociatedPair> getMatchSet() {
			List<AssociatedPair> ret = new ArrayList<>();
			for( int i = 0; i < matchSetSize; i++ ) {
				ret.add( new AssociatedPairTrack());
			}
			return ret;
		}

		@Override
		public int getInputIndex(int matchIndex) {
			return matchIndex;
		}

		@Override
		public double getFitQuality() {
			return 0;
		}

		@Override
		public int getMinimumSize() {
			return matchSetSize;
		}

		@Override
		public void reset() {}

		@Override
		public Class<AssociatedPair> getPointType() {
			return AssociatedPair.class;
		}

		@Override
		public Class<T> getModelType() {
			return null;
		}

		public void setMotion(T se) {
			found = se;
		}
	}
}

