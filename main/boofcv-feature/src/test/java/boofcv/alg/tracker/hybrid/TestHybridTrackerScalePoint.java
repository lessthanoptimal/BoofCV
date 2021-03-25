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

package boofcv.alg.tracker.hybrid;

import boofcv.abst.feature.associate.AbstractAssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings({"unchecked", "rawtypes"})
class TestHybridTrackerScalePoint extends BoofStandardJUnit {

	int width=100,height=120;

	private HybridTrackerScalePoint<GrayF32, GrayF32, TupleDesc_F64> createAlgorithm() {
		var trackerKlt = new DummyKlt();
		var detector = new DummyDetector(5);
		var associate = new DummyAssoc(5);

		return new HybridTrackerScalePoint(trackerKlt,detector,associate,10);
	}

	@Test void reset() {
		HybridTrackerScalePoint alg = createAlgorithm();

		addTracks(alg.tracksAll, 1);
		addTracks(alg.tracksActive.toList(), 1);
		addTracks(alg.tracksInactive.toList(), 2);
		addTracks(alg.tracksSpawned, 8);
		addTracks(alg.tracksDropped, 2);
		alg.frameID = 30;
		alg.totalTracks = 5;

		alg.reset();

		assertEquals(0,alg.tracksAll.size());
		assertEquals(0,alg.tracksActive.size());
		assertEquals(0,alg.tracksInactive.size());
		assertEquals(0,alg.tracksSpawned.size());
		assertEquals(0,alg.tracksDropped.size());
		assertEquals(-1,alg.frameID);
		assertEquals(0,alg.totalTracks);
	}

	@Test void dropAllTracks() {
		HybridTrackerScalePoint alg = createAlgorithm();

		addTracks(alg.tracksAll, 1);
		addTracks(alg.tracksActive.toList(), 1);
		addTracks(alg.tracksInactive.toList(), 2);
		addTracks(alg.tracksSpawned, 8);
		addTracks(alg.tracksDropped, 2);

		alg.dropAllTracks();

		assertEquals(0,alg.tracksAll.size());
		assertEquals(0,alg.tracksActive.size());
		assertEquals(0,alg.tracksInactive.size());
		assertEquals(0,alg.tracksSpawned.size());
		assertEquals(0,alg.tracksDropped.size());
	}

	@Test void dropTrackByAllIndex() {
		HybridTrackerScalePoint alg = createAlgorithm();

		alg.trackerKlt = new DummyKlt();

		addTracks(alg.tracksAll,8);
		alg.tracksActive.addAll(alg.tracksAll);

		HybridTrack track = alg.dropTrackByAllIndex(6);
		assertEquals(7, alg.tracksAll.size());
		assertEquals(7, alg.tracksActive.size());
		assertEquals(0, alg.tracksInactive.size());
		// tracks dropped by tracker are added to the dropped list. That might not be the case here
		assertEquals(0, alg.tracksDropped.size());

		assertFalse(alg.tracksAll.contains(track));
	}

	@Test void updateTracks() {
		HybridTrackerScalePoint alg = createAlgorithm();

		alg.trackerKlt = new DummyKlt();

		addTracks(alg.tracksAll,8);
		alg.tracksActive.addAll(alg.tracksAll);

		// this should be cleared
		alg.tracksSpawned.add(1);

		alg.updateTracks(new DummyPyramid(),null,null);

		// tracks after 5 should be dropped
		assertEquals(8, alg.tracksAll.size());
		assertEquals(5, alg.tracksActive.size());
		assertEquals(3, alg.tracksInactive.size());
		assertEquals(0, alg.tracksDropped.size());
		assertEquals(0, alg.tracksSpawned.size());
	}

	@Test void dropExcessiveInactiveTracks() {
		var alg = createAlgorithm();
		alg.maxInactiveTracks = 20;
		addTracks(alg.tracksAll,50);
		for (int i = 0; i < 30; i++) {
			alg.tracksInactive.add(alg.tracksAll.get(i));
		}

		alg.dropExcessiveInactiveTracks();
		assertEquals(40,alg.tracksAll.size);
		assertEquals(20,alg.tracksInactive.size);
	}

	@Test void pruneActiveTracksWhichAreTooClose() {
		var alg = createAlgorithm();
		alg.imageWidth = width;
		alg.imageHeight = height;
		addTracks(alg.tracksAll,8);
		// assign all tracks to the same pixel
		for( var t : alg.tracksAll.toList() ) {
			t.pixel.setTo(5,5);
		}
		alg.pruneActiveTracksWhichAreTooClose();

		// only one should survive since they have the same location
		addTracks(alg.tracksAll,1);
	}

	private void addTracks( DogArray<HybridTrack<TupleDesc_F64>> l , int num ) {
		for( int i = 0; i < num; i++ ) {
			HybridTrack t = l.grow();
			t.trackKlt = new PyramidKltFeature(2,5);
		}
	}

	private void addTracks( List<HybridTrack> l , int num ) {
		for( int i = 0; i < num; i++ ) {
			HybridTrack t =new HybridTrack();
			t.trackKlt = new PyramidKltFeature(2,5);
			l.add( t );
		}
	}

	private static class DummyKlt extends PyramidKltForHybrid<GrayF32,GrayF32> {
		int count = 0;

		@Override public boolean performTracking(  PyramidKltFeature feature ) {return count++ < 5;}
		@Override public void setInputs(ImagePyramid<GrayF32> image , GrayF32[] derivX , GrayF32[] derivY ) {}
		@Override public void setDescription( float x , float y , PyramidKltFeature ret ) {}
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

	private class DummyPyramid extends PyramidDiscrete<GrayF32> {
		public DummyPyramid() {
			super(ImageType.SB_F32, true,ConfigDiscreteLevels.levels(2));
			initialize(width,height);
		}

		@Override public void process(GrayF32 input) {}
		@Override public double getSampleOffset(int layer) {return 0;}
		@Override public double getSigma(int layer) {return 0;}
		@Override public <IP extends ImagePyramid<GrayF32>> IP copyStructure() {return null;}
	}
}
