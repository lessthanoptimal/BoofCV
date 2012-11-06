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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.InvertibleTransform;
import georegression.struct.se.Se2_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
* @author Peter Abeles
*/
public class TestImageMotionPointKey {

	/**
	 * Give it a very simple example and see if it computes the correct motion and has the expected behavior
	 * when processing an image
	 */
	@Test
	public void process() {
		// what the initial transform should be
		Se2_F32 initial = new Se2_F32(1,2,3);
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Se2_F32> matcher = new DummyModelMatcher<Se2_F32>(computed,5);

		ImageUInt8 input = new ImageUInt8(20,30);

		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(tracker,matcher,null,model);

		// specify an initial transform
		alg.setInitialTransform(initial);

		// the first time it processes an image it should always return false since no motion can be estimated
		assertFalse(alg.process(input));
		assertEquals(0, tracker.numSpawn);

		// make the current frame into the keyframe
		// request that the current frame is a keyframe
		alg.changeKeyFrame();
		assertEquals(1, tracker.numSpawn);

		// now it should compute some motion
		assertTrue(alg.process(input));

		// no new tracks should have been spawned
		assertEquals(1, tracker.numSpawn);

		// test the newly computed results
		Se2_F32 keyToCurr = alg.getKeyToCurr();
		assertEquals(computed.getX(), keyToCurr.getX(), 1e-8);

		// see if this transform was correctly computed
		Se2_F32 worldToCurr = initial.concat(keyToCurr, null);
		Se2_F32 found = alg.getWorldToCurr();
		assertEquals(worldToCurr.getX(), found.getX(), 1e-8);
	}

	@Test
	public void changeWorld() {
		Se2_F32 oldToNew = new Se2_F32(1,2,0);
		Se2_F32 model = new Se2_F32();

		// the world frame will initially be the identify matrix
		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(null,null,null,model);

		// change it to this frame
		alg.changeWorld(oldToNew);

		// see of all the other ones are updated correctly
		Se2_F32 worldToCurr = alg.getWorldToCurr();
		Se2_F32 worldToKey = alg.getWorldToKey();

		// since they were initially the identity this test should work
		assertEquals(oldToNew.getX(), worldToCurr.getX(), 1e-8);
		assertEquals(oldToNew.getX(), worldToKey.getX(), 1e-8);
	}

	/**
	 * Test the keyframe based on the definition of the keyframe
	 */
	@Test
	public void changeKeyFrame() {
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Se2_F32> matcher = new DummyModelMatcher<Se2_F32>(computed,5);

		ImageUInt8 input = new ImageUInt8(20,30);

		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(tracker,matcher,null,model);

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

	public static class DummyTracker implements ImagePointTracker<ImageUInt8>
	{
		public int numSpawn = 0;
		public int numDropped = 0;

		List<PointTrack> list = new ArrayList<PointTrack>();
		List<PointTrack> listSpawned = new ArrayList<PointTrack>();

		@Override
		public void reset() {}

		@Override
		public void process(ImageUInt8 image) {}

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
		public void dropAllTracks() {}

		@Override
		public void dropTrack(PointTrack track) {numDropped++;}

		@Override
		public List<PointTrack> getAllTracks( List<PointTrack> list ) {
			if( list == null ) list = new ArrayList<PointTrack>();
			list.addAll(this.list);
			return list;
		}

		@Override
		public List<PointTrack> getActiveTracks(List<PointTrack> list) {
			return getAllTracks(list);
		}

		@Override
		public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
			return new ArrayList<PointTrack>();
		}

		@Override
		public List<PointTrack> getNewTracks(List<PointTrack> list) {
			if( list == null ) list = new ArrayList<PointTrack>();
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
		public T getModel() {
			return found;
		}

		@Override
		public List<AssociatedPair> getMatchSet() {
			List<AssociatedPair> ret = new ArrayList<AssociatedPair>();
			for( int i = 0; i < matchSetSize; i++ ) {
				ret.add( new AssociatedPair());
			}
			return ret;
		}

		@Override
		public int getInputIndex(int matchIndex) {
			return matchIndex;
		}

		@Override
		public double getError() {
			return 0;
		}

		@Override
		public int getMinimumSize() {
			return matchSetSize;
		}

		public void setMotion(T se) {
			found = se;
		}
	}
}
