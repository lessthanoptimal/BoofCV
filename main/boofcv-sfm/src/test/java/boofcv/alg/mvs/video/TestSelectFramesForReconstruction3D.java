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

package boofcv.alg.mvs.video;

import boofcv.abst.feature.associate.AssociateDescription2DDefault;
import boofcv.abst.feature.describe.DescribeRegionPointDefault;
import boofcv.abst.tracker.PointTrackerDefault;
import boofcv.misc.ModelGeneratorDefault;
import boofcv.misc.ModelMatcherDefault;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class TestSelectFramesForReconstruction3D extends BoofStandardJUnit {
	int width = 100;
	int height = 90;

	GrayF32 dummy = new GrayF32(width, height);

	// how many matches Mock associate should return
	int numMatches = 0;

	/** Every frame will have significant motion */
	@Test void moving3D() {
		var alg = new MockFrameSelector<GrayF32>();
		alg.config.minTranslation.setFixed(0); // disable this check to make the test easier to write

		alg.config.minimumPairs = 0;
		alg.is3D = true;
		alg.isClearlyNot3D = false;
		alg.isStatic = false;

		// Can't call init because of null checks
		alg.width = width;
		alg.height = height;
		alg.forceKeyFrame = true;

		// Process 4 frames
		alg.next(dummy);
		alg.next(dummy);
		alg.next(dummy);
		alg.next(dummy);

		assertEquals(4, alg.callsCopy);
		assertEquals(3, alg.callsCreatePairs);
		assertEquals(4, alg.callsTracking);

		DogArray_I32 selected = alg.getSelectedFrames();
		assertTrue(selected.isEquals(0, 1, 2, 3));
	}

	/** Runs but not every frame will meet the criteria to be 3D */
	@Test void pausingAndResuming3D() {
		var alg = new MockFrameSelector<GrayF32>();
		alg.config.minTranslation.setFixed(0); // disable this check to make the test easier to write

		alg.config.minimumPairs = 0;
		// Can't call init because of null checks
		alg.width = width;
		alg.height = height;
		alg.forceKeyFrame = true;

		// Process 4 frames
		alg.next(dummy);
		alg.isStatic = true;
		alg.is3D = true; // 3D check should not be called. Sanity check here
		alg.next(dummy);
		alg.isStatic = false;
		alg.isClearlyNot3D = true;
		alg.next(dummy);
		alg.isClearlyNot3D = false;
		alg.next(dummy);

		assertEquals(4, alg.callsCopy);
		assertEquals(3, alg.callsCreatePairs);
		assertEquals(4, alg.callsTracking);

		DogArray_I32 selected = alg.getSelectedFrames();
		assertTrue(selected.isEquals(0, 3));
	}

	@Test void createPairsWithKeyFrameTracking() {
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();

		// Offset the index by one with a point which will not be matched to make this non-trivial
		alg.currentFrame.locations.grow().setTo(0, 0);
		alg.currentFrame.trackID_to_index.put(100, 0);

		// Create matching pairs with an easy to compute value
		for (int i = 0; i < 4; i++) {
			alg.currentFrame.locations.grow().setTo(i, i + 1);
			alg.currentFrame.trackID_to_index.put(20 + i, i + 1);
			alg.keyFrame.locations.grow().setTo(i, i + 1);
			alg.keyFrame.trackID_to_index.put(20 + i, i);
		}

		// Create the pairs
		alg.createPairsWithKeyFrameTracking(alg.keyFrame, alg.currentFrame);

		// Check results
		assertEquals(4, alg.pairs.size);
		for (int i = 0; i < 4; i++) {
			AssociatedPair p = alg.pairs.get(i);
			int idx = (int)p.p1.x;
			assertEquals(idx + 1, p.p1.y, UtilEjml.TEST_F64);
			assertEquals(idx, p.p2.x, UtilEjml.TEST_F64);
			assertEquals(idx + 1, p.p2.y, UtilEjml.TEST_F64);
		}
	}

	@Test void isSceneStatic() {
		double tol = 2.0;
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();
		alg.config.motionInlierPx = tol;

		// these will have a motion of 2.0 along the x-axis
		for (int i = 0; i < 30; i++) {
			alg.pairs.grow().setTo(0, 1, 2, 1);
		}

		alg.config.motionInlierPx = tol*1.01;
		assertTrue(alg.isSceneStatic());

		alg.config.motionInlierPx = tol*0.99;
		assertFalse(alg.isSceneStatic());
	}

	@Test void isSceneClearlyNot3D() {
		double tol = 2.0;
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();
		alg.config.motionInlierPx = tol;
		// It will generate a model that translate by 2.0 along x-axis
		alg.setComputeHomography(new MockModelGenerator(2.0));
		// these will have a motion of 4.0 along the x-axis
		for (int i = 0; i < 30; i++) {
			alg.pairs.grow().setTo(0, 1, 4, 1);
		}

		alg.config.motionInlierPx = tol*1.01;
		assertTrue(alg.isSceneClearlyNot3D());

		// The tolerance should be just under now
		alg.config.motionInlierPx = tol*0.99;
		assertFalse(alg.isSceneClearlyNot3D());
	}

	/** See if it correctly identifies a scene as 3D which really is 3D */
	@Test void isScene3D_actually3D() {
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();

		alg.setRobust3D(new DummyRobust(50));
		alg.setRobustH(new DummyRobust(20));

		assertTrue(alg.isScene3D());
	}

	@Test void isScene3D_Not3D() {
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();

		alg.setRobust3D(new DummyRobust(50));
		alg.setRobustH(new DummyRobust(50));

		assertFalse(alg.isScene3D());
	}

	/** Tracking is good, until one frame when it drops, then the next frame there's good association again */
	@Test void checkSkippedBadFrame() {
		SelectFramesForReconstruction3D<GrayF32> alg = createMockAlg();
		alg.config.skipEvidenceRatio = 1.5;
		for (int i = 0; i < 30; i++) {
			alg.pairs.grow().setTo(0, 1, 4, 1);
		}
		numMatches = 45; // it will be just at the threshold
		assertFalse(alg.checkSkippedBadFrame());
		numMatches = 46; // 30*1.5=45
		assertTrue(alg.checkSkippedBadFrame());
	}

	private <T extends ImageBase<T>> SelectFramesForReconstruction3D<T> createMockAlg() {
		var alg = new SelectFramesForReconstruction3D<T>(new MockDescriptor<>());
		alg.config.minimumPairs = 20;
		alg.setTracker(new MockTracker<>());
		alg.setAssociate(new MockAssociate());
		alg.setRobustH(new DummyRobust(0));
		alg.setRobust3D(new DummyRobust(0));
		alg.setCompareFit(new RelativeBetter.MaximizeSoftRatio(1.0));

		return alg;
	}

	/** Used to check high level logic for deciding when to add a frame */
	private class MockFrameSelector<T extends ImageBase<T>> extends SelectFramesForReconstruction3D<T> {
		boolean isStatic = false;
		boolean isClearlyNot3D = false;
		boolean is3D = false;

		int callsTracking = 0;
		int callsCopy = 0;
		int callsCreatePairs = 0;

		public MockFrameSelector() {
			super(new MockDescriptor<>());
			associate = new MockAssociate();
		}
		@Override protected void performTracking( T frame ) {callsTracking++;}
		@Override protected void copyTrackResultsIntoCurrentFrame( T image ) {callsCopy++;}
		@Override protected void createPairsWithKeyFrameTracking( Frame keyFrame, Frame current ) {callsCreatePairs++;}
		@Override protected boolean isSceneStatic() {return isStatic;}
		@Override protected boolean isSceneClearlyNot3D() {return isClearlyNot3D;}
		@Override protected boolean isScene3D() {return is3D;}
	}

	private static class MockTracker<T extends ImageBase<T>> extends PointTrackerDefault<T> {}

	private class MockAssociate extends AssociateDescription2DDefault<TupleDesc_F64> {
		@Override public FastAccess<AssociatedIndex> getMatches() {
			DogArray<AssociatedIndex> ret = new DogArray<>(AssociatedIndex::new);
			for (int i = 0; i < numMatches; i++) {
				ret.grow();
			}
			return ret;
		}
	}

	private static class MockModelGenerator extends ModelGeneratorDefault<Homography2D_F64, AssociatedPair> {
		double delta;

		public MockModelGenerator( double delta ) { this.delta = delta; }

		@Override public boolean generate( List<AssociatedPair> dataSet, Homography2D_F64 output ) {
			output.reset();
			output.a13 += delta;
			return true;
		}
	}

	private static class MockDescriptor<T extends ImageBase<T>> extends DescribeRegionPointDefault<T, TupleDesc_F64> {
		@Override public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(1);
		}

		@Override public Class<TupleDesc_F64> getDescriptionType() { return TupleDesc_F64.class; }
	}

	@SuppressWarnings("rawtypes")
	private static class DummyRobust extends ModelMatcherDefault {
		double score;

		public DummyRobust( double score ) { this.score = score; }

		@Override public List getMatchSet() {
			List set = new ArrayList();
			for (int i = 0; i < 50; i++) {
				set.add(i);
			}
			return set;
		}

		@Override public double getFitQuality() { return score; }
	}
}
