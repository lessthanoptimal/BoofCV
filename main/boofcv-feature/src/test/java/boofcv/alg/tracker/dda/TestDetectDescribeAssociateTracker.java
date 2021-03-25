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

package boofcv.alg.tracker.dda;

import boofcv.abst.feature.associate.AbstractAssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTrack;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestDetectDescribeAssociateTracker extends BoofStandardJUnit {

	private DetectDescribeAssociateTracker<GrayF32, TupleDesc_F64> createAlgorithm() {
		var detector = new DummyDetector(5);
		var associate = new DummyAssoc(5);

		return new DetectDescribeAssociateTracker<>(detector,associate,new ConfigTrackerDda());
	}

	@Test
	void dropExcessiveInactiveTracks() {
		var alg = createAlgorithm();
		alg.maxInactiveTracks = 20;
		addTracks(alg.tracksAll,50);
		var unassociatedIdx = new DogArray_I32();
		for (int i = 0; i < 30; i++) {
			unassociatedIdx.add(i);
			alg.tracksInactive.add(alg.tracksAll.get(i));
		}

		alg.dropExcessiveInactiveTracks(unassociatedIdx);
		assertEquals(40,alg.tracksAll.size);
		assertEquals(20,alg.tracksInactive.size());
	}

	@Test
	void addNewTrack() {
		var alg = createAlgorithm();
		alg.frameID = 7;
		alg.featureID = 8;

		alg.addNewTrack(9,1,2,new TupleDesc_F64(3));

		assertEquals(1,alg.tracksAll.size);
		assertEquals(1,alg.tracksActive.size());
		assertEquals(0,alg.tracksInactive.size());

		PointTrack t = alg.tracksAll.get(0);
		assertEquals(7,t.spawnFrameID);
		assertEquals(7,t.lastSeenFrameID);
		assertEquals(8,t.featureId);
		assertEquals(9,t.detectorSetId);
		assertEquals(1,t.pixel.x, UtilEjml.TEST_F64);
		assertEquals(2,t.pixel.y, UtilEjml.TEST_F64);
	}

	private void addTracks(DogArray<PointTrack> l , int num ) {
		for( int i = 0; i < num; i++ ) {
			l.grow();
		}
	}

	private static class DummyAssoc extends AbstractAssociateDescription2D<TupleDesc_F64> {
		int N;

		public DummyAssoc(int N ) {
			this.N = N;
		}

		@Override
		public FastAccess<AssociatedIndex> getMatches() {
			DogArray<AssociatedIndex> queue = new DogArray<>(N, AssociatedIndex::new);

			for( int i = 0; i < N; i++ ) {
				queue.grow().setTo(i,i,1);
			}

			return queue;
		}

		@Override public boolean uniqueSource() {return true;}
		@Override public boolean uniqueDestination() {return true;}
		@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;};
	}

	private static class DummyDetector extends DetectDescribePointAbstract<GrayF32,TupleDesc_F64> {
		int N;
		private DummyDetector(int n) {N = n;}
		@Override public int getNumberOfFeatures() {return N;}
		@Override public Point2D_F64 getLocation(int featureIndex) {return new Point2D_F64(2,2);}
		@Override public int getNumberOfSets() {return 1;}
		@Override public int getSet(int index) {return 0;}
		@Override public TupleDesc_F64 createDescription() {return new TupleDesc_F64(1);}
		@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;};
	}
}
