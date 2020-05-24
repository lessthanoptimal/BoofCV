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

package boofcv.alg.tracker.combined;

import boofcv.abst.feature.associate.AssociateDescriptionSets;
import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"unchecked", "rawtypes"})
class TestCombinedTrackerScalePoint {

	@Test
	void reset() {
		CombinedTrackerScalePoint alg = new CombinedTrackerScalePoint();

		addTracks(alg.tracksDormant, 1);
		addTracks(alg.tracksPureKlt, 2);
		addTracks(alg.tracksReactivated, 4);
		addTracks(alg.tracksSpawned, 8);
		addTracks(alg.tracksUnused, 2);

		alg.totalTracks = 5;

		alg.reset();

		assertEquals(0,alg.tracksDormant.size());
		assertEquals(0,alg.tracksPureKlt.size());
		assertEquals(0,alg.tracksReactivated.size());
		assertEquals(0,alg.tracksSpawned.size());
		assertEquals(9,alg.tracksUnused.size());
		assertEquals(0,alg.totalTracks);
	}

	@Test
	void dropAllTracks() {
		CombinedTrackerScalePoint alg = new CombinedTrackerScalePoint();

		addTracks(alg.tracksDormant,1);
		addTracks(alg.tracksPureKlt,2);
		addTracks(alg.tracksReactivated,4);
		addTracks(alg.tracksSpawned,8);
		addTracks(alg.tracksUnused, 2);
		alg.totalTracks = 5;

		alg.dropAllTracks();

		assertEquals(0,alg.tracksDormant.size());
		assertEquals(0,alg.tracksPureKlt.size());
		assertEquals(0,alg.tracksReactivated.size());
		assertEquals(0,alg.tracksSpawned.size());
		assertEquals(9,alg.tracksUnused.size());
		assertEquals(5,alg.totalTracks);
	}

	@Test
	void updateTracks() {
		CombinedTrackerScalePoint alg = new CombinedTrackerScalePoint();

		alg.trackerKlt = new DummyKlt();

		addTracks(alg.tracksPureKlt,8);

		// this should be cleared
		alg.tracksSpawned.add(1);

		alg.updateTracks(null,null,null,null);

		// tracks after 5 should be dropped
		assertEquals(5, alg.tracksPureKlt.size());
		assertEquals(3, alg.tracksDormant.size());
		assertEquals(0, alg.tracksUnused.size());
		assertEquals(0, alg.tracksSpawned.size());
	}

	@Test
	void associateAllToDetected() {
		CombinedTrackerScalePoint alg = new CombinedTrackerScalePoint();

		alg.detectedDesc = new FastArray(BrightFeature.class);
		alg.knownDesc = new FastArray(BrightFeature.class);
		alg.associate = new DummyAssoc(20);
		alg.detector = new DummyDetector(20);
		alg.trackerKlt = new DummyKlt();

		addTracks(alg.tracksDormant,3);
		addTracks(alg.tracksReactivated,4);
		addTracks(alg.tracksPureKlt,8);

		alg.associateAllToDetected();

		// all dormant should be reactivated
		assertEquals(0,alg.tracksDormant.size());
		assertEquals(7,alg.tracksReactivated.size());
		assertEquals(8,alg.tracksPureKlt.size());

		// make sure the KLT tracks haven't been changed
		for( Object a : alg.tracksPureKlt ) {
			CombinedTrack c = (CombinedTrack)a;
			assertEquals(0,c.pixel.x,1e-8);
			assertEquals(0,c.pixel.y,1e-8);
		}
		// the others should have
		for( Object a : alg.tracksReactivated ) {
			CombinedTrack c = (CombinedTrack)a;
			assertTrue(0 != c.pixel.x);
			assertTrue(0 != c.pixel.y);
		}
	}

	private void addTracks( List l , int num ) {
		for( int i = 0; i < num; i++ ) {
			CombinedTrack t =new CombinedTrack();
			t.track = new PyramidKltFeature(2,5);
			l.add( t );
		}
	}

	private static class DummyKlt extends PyramidKltForCombined {
		int count = 0;

		@Override
		public boolean performTracking(  PyramidKltFeature feature ) {
			return count++ < 5;
		}

		@Override
		public void setInputs(ImagePyramid image , ImageGray[] derivX , ImageGray[] derivY ) {}

		@Override
		public void setDescription( float x , float y , PyramidKltFeature ret ) {}
	}


	private static class DummyAssoc extends AssociateDescriptionSets {
		int N;

		public DummyAssoc(int N ) {
			super(null,BrightFeature.class);
			this.N = N;
		}

		@Override
		public void initialize(int numberOfSets) {}

		@Override
		public FastAccess<AssociatedIndex> getMatches() {
			FastQueue<AssociatedIndex> queue = new FastQueue<>(N, AssociatedIndex::new);

			for( int i = 0; i < N; i++ ) {
				queue.grow().setAssociation(i,i,1);
			}

			return queue;
		}
	}

	private static class DummyDetector extends DetectDescribePointAbstract {

		int N;

		private DummyDetector(int n) {N = n;}
		@Override public int getNumberOfFeatures() {return N;}
		@Override public Point2D_F64 getLocation(int featureIndex) {return new Point2D_F64(2,2);}
		@Override public int getNumberOfSets() {return 1;}
		@Override public int getSet(int index) {return 0;}
	}
}
