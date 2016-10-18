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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import georegression.struct.affine.Affine2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageMotionPtkSmartRespawn {

	Random rand = new Random(234);

	GrayF32 input = new GrayF32(50,60);

	// the found feature match set
	List<AssociatedPair> matchSet = new ArrayList<>();

	/**
	 * Test the positive case when nothing should go wrong
	 */
	@Test
	public void nominal() {
		DummyMotion motion = new DummyMotion();

		ImageMotionPtkSmartRespawn<GrayF32,Affine2D_F64> alg =
				new ImageMotionPtkSmartRespawn<>(motion, 10, 0.5, 0.5);

		// first pass causes tracks to be spawned
		assertTrue(alg.process(input));
		assertTrue(alg.previousWasKeyFrame);
		assertEquals(0,motion.numReset);
		assertEquals(0,motion.numResetTransform);
		assertEquals(1,motion.numChangeKeyFrame);
		assertEquals(1,motion.numProcess);
		// add a set of features that won't trigger any faults
		matchSet = createGoodFeatureList(100);
		// nothing should go wrong here
		assertTrue(alg.process(input));
		assertFalse(alg.previousWasKeyFrame);
		assertEquals(0,motion.numReset);
		assertEquals(0,motion.numResetTransform);
		assertEquals(1,motion.numChangeKeyFrame);
		assertEquals(2,motion.numProcess);
		// one last one of nothing should go wrong
		assertTrue(alg.process(input));
		assertFalse(alg.previousWasKeyFrame);
		assertEquals(0,motion.numReset);
		assertEquals(0,motion.numResetTransform);
		assertEquals(1,motion.numChangeKeyFrame);
		assertEquals(3,motion.numProcess);
	}

	/**
	 * Checks to see if a new key-frame is triggered when the number of inliers drops too low
	 */
	@Test
	public void absoluteMinimumTracks() {
		DummyMotion motion = new DummyMotion();

		ImageMotionPtkSmartRespawn<GrayF32,Affine2D_F64> alg =
				new ImageMotionPtkSmartRespawn<>(motion, 20, 0.5, 0.5);

		// first pass causes tracks to be spawned
		assertTrue(alg.process(input));
		assertEquals(1,motion.numChangeKeyFrame);
		// give it too few tracks
		matchSet = createGoodFeatureList(10);
		assertTrue(alg.process(input));
		assertEquals(2,motion.numChangeKeyFrame);
		// give it a good number of tracks, no new keyframe
		matchSet = createGoodFeatureList(100);
		assertTrue(alg.process(input));
		assertEquals(2,motion.numChangeKeyFrame);
		// process once to set relative thresholds
		assertTrue(alg.process(input));
		assertEquals(2,motion.numChangeKeyFrame);
		// decrease the number of features
		matchSet = createGoodFeatureList(10);
		assertTrue(alg.process(input));
		assertEquals(3,motion.numChangeKeyFrame);
	}

	/**
	 * Checks to see if a new key-frame is triggered when the relative number of inliers drops too low
	 */
	@Test
	public void relativeMinimumTracks() {
		DummyMotion motion = new DummyMotion();

		ImageMotionPtkSmartRespawn<GrayF32,Affine2D_F64> alg =
				new ImageMotionPtkSmartRespawn<>(motion, 20, 0.5, 0.5);

		// first pass causes tracks to be spawned
		assertTrue(alg.process(input));
		assertEquals(1,motion.numChangeKeyFrame);
		// give a good number of tracks
		matchSet = createGoodFeatureList(100);
		assertTrue(alg.process(input));
		assertEquals(1, motion.numChangeKeyFrame);
		// drop the track count just above the relative threshold
		matchSet = createGoodFeatureList(50);
		assertTrue(alg.process(input));
		assertEquals(1, motion.numChangeKeyFrame);
		// now trigger the relative threshold
		matchSet = createGoodFeatureList(49);
		assertTrue(alg.process(input));
		assertEquals(2, motion.numChangeKeyFrame);
	}

	/**
	 * Checks to see if a new key-frame is triggered when relative coverage area drops too low
	 */
	@Test
	public void relativeCoverageArea() {
		DummyMotion motion = new DummyMotion();

		ImageMotionPtkSmartRespawn<GrayF32,Affine2D_F64> alg =
				new ImageMotionPtkSmartRespawn<>(motion, 20, 0.5, 0.5);

		// first pass causes tracks to be spawned
		assertTrue(alg.process(input));
		assertEquals(1,motion.numChangeKeyFrame);
		// give a good number of tracks with good coverage
		matchSet = createGoodFeatureList(100);
		assertTrue(alg.process(input));
		assertEquals(1, motion.numChangeKeyFrame);
		// enough tracks, but horrible coverage
		matchSet = createBadCoverageList(100);
		assertTrue(alg.process(input));
		assertEquals(2, motion.numChangeKeyFrame);
	}

	private List<AssociatedPair> createGoodFeatureList( int num ) {
		List<AssociatedPair> ret = new ArrayList<>();

		// maximize the area
		ret.add( createPair(0,0));
		ret.add( createPair(input.width,0));
		ret.add( createPair(input.width,input.height));
		ret.add( createPair(0,input.height));

		for( int i = 4; i < num; i++ ) {
			int x = rand.nextInt(input.width);
			int y = rand.nextInt(input.height);
			ret.add( createPair(x,y));
		}

		return ret;
	}

	private List<AssociatedPair> createBadCoverageList( int num ) {
		List<AssociatedPair> ret = new ArrayList<>();

		// maximize the area
		ret.add( createPair(0,0));

		for( int i = 4; i < num; i++ ) {
			ret.add( createPair(1,1));
		}

		return ret;
	}

	private AssociatedPair createPair( int x , int y ) {
		AssociatedPair p = new AssociatedPairTrack();
		p.p1.set(x,y);
		p.p2.set(x,y);

		return p;
	}

	private class DummyMotion extends ImageMotionPointTrackerKey<GrayF32,Affine2D_F64>
	{
		int numReset = 0;
		int numProcess = 0;
		int numChangeKeyFrame = 0;
		int numResetTransform = 0;

		Affine2D_F64 worldToCurr = new Affine2D_F64();
		boolean isKey = false;

		@Override
		public void reset() {
			numReset++;
		}

		@Override
		public boolean process(GrayF32 frame) {
			numProcess++;
			return true;
		}

		@Override
		public void changeKeyFrame() {
			numChangeKeyFrame++;
		}

		@Override
		public ModelMatcher<Affine2D_F64, AssociatedPair> getModelMatcher() {
			return new Matcher();
		}

		@Override
		public PointTracker<GrayF32> getTracker() {
			return new Tracker();
		}

		@Override
		public void resetTransforms() {
			numResetTransform++;
		}

		@Override
		public Affine2D_F64 getWorldToCurr() {
			return worldToCurr;
		}

		@Override
		public boolean isKeyFrame() {
			return isKey;
		}
	}

	private class Matcher implements ModelMatcher<Affine2D_F64, AssociatedPair>
	{

		@Override
		public boolean process(List<AssociatedPair> dataSet) {return false;}

		@Override
		public Affine2D_F64 getModelParameters() {return null;}

		@Override
		public List<AssociatedPair> getMatchSet() {
			return matchSet;
		}

		@Override
		public int getInputIndex(int matchIndex) {return 0;}

		@Override
		public double getFitQuality() {return 0;}

		@Override
		public int getMinimumSize() {return 0;}
	}

	private class Tracker implements PointTracker<GrayF32>
	{

		@Override
		public void process(GrayF32 image) {}

		@Override
		public void reset() {}

		@Override
		public void dropAllTracks() {}

		@Override
		public boolean dropTrack(PointTrack track) {return false;}

		@Override
		public List<PointTrack> getAllTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public List<PointTrack> getActiveTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public List<PointTrack> getDroppedTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public List<PointTrack> getNewTracks(List<PointTrack> list) {
			return new ArrayList<>();
		}

		@Override
		public void spawnTracks() {}
	}

}
