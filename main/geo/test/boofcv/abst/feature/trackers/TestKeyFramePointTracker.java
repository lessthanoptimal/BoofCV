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

package boofcv.abst.feature.trackers;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.struct.image.ImageBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
* @author Peter Abeles
*/
@SuppressWarnings("unchecked")
public class TestKeyFramePointTracker {

	@Test
	public void process() {
		Dummy tracker = new Dummy(0,2,5);
		KeyFramePointTracker alg = new KeyFramePointTracker(tracker);

		assertEquals(0, alg.getActiveTracks(null).size());
		assertEquals(0, alg.getActivePairs(null).size());

		alg.spawnTracks(null);
		alg.setKeyFrame();
		assertEquals(5, alg.getActivePairs(null).size());

		alg.process(null);

		assertEquals(3, alg.getActiveTracks(null).size());
		assertEquals(3, alg.getActivePairs(null).size());
		assertEquals(1, tracker.numCalledSpawn);
	}

	@Test
	public void setKeyFrame() {
		Dummy tracker = new Dummy(0,0,5);
		KeyFramePointTracker alg = new KeyFramePointTracker(tracker);

		assertEquals(0, alg.getActivePairs(null).size());

		alg.spawnTracks(null);
		alg.setKeyFrame();

		assertEquals(5, alg.getActiveTracks(null).size());
		assertEquals(5, alg.getActivePairs(null).size());
		assertEquals(1, tracker.numCalledSpawn);
	}

	@Test
	public void spawnTracks() {
		Dummy tracker = new Dummy(0,0,5);
		KeyFramePointTracker alg = new KeyFramePointTracker(tracker);

		alg.spawnTracks(null);

		assertEquals(5, alg.getActivePairs(null).size());
		assertEquals(1, tracker.numCalledSpawn);
	}

	@Test
	public void dropTrack() {
		Dummy tracker = new Dummy(0,0,5);
		KeyFramePointTracker alg = new KeyFramePointTracker(tracker);

		alg.spawnTracks(null);
		alg.setKeyFrame();
		alg.dropTrack(tracker.active.get(0));

		assertEquals(4, alg.getActiveTracks(null).size());
		assertEquals(4, alg.getActivePairs(null).size());
	}

	@Test
	public void reset() {
		Dummy tracker = new Dummy(0,0,5);
		KeyFramePointTracker alg = new KeyFramePointTracker(tracker);

		alg.spawnTracks(null);
		alg.setKeyFrame();
		assertEquals(5, alg.getActivePairs(null).size());

		alg.reset();

		assertEquals(0, alg.getActivePairs(null).size());
		assertEquals(1,tracker.numCalledDrop);
	}

	private static class Dummy implements ImagePointTracker<ImageBase> {

		List<PointTrack> spawned = new ArrayList<PointTrack>();
		List<PointTrack> active = new ArrayList<PointTrack>();
		List<PointTrack> dropped = new ArrayList<PointTrack>();

		int numCalledSpawn = 0;
		int numCalledProcess = 0;
		int numCalledDrop= 0;

		int numDrop;
		int numToSpawn;

		public Dummy() {
		}

		public Dummy( int numActive , int numDrop , int numToSpawn ) {
			for( int i = 0; i < numActive; i++ ) {
				active.add( new PointTrack());
			}
			this.numDrop = numDrop;
			this.numToSpawn = numToSpawn;
		}

		@Override
		public void reset() {
		}

		@Override
		public void process(ImageBase image) {
			numCalledProcess++;

			for( int i = 0; i < numDrop; i++ ) {
				dropped.add( active.remove(i) );
			}
		}

		@Override
		public boolean addTrack(double x, double y) {
			return false;
		}

		@Override
		public void spawnTracks() {
			for( int i = 0; i < numToSpawn; i++ ) {
				spawned.add( new PointTrack() );
			}
			active.addAll(spawned);

			numCalledSpawn++;
		}

		@Override
		public void dropAllTracks() {
			numCalledDrop++;
		}

		@Override
		public void dropTrack(PointTrack track) {
			active.remove(track);
		}

		@Override
		public List<PointTrack> getAllTracks(List<PointTrack> list) {
			return null;
		}

		@Override
		public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
			return active;
		}

		@Override
		public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
			return dropped;
		}

		@Override
		public List<PointTrack> getNewTracks( List<PointTrack> list ) {
			return spawned;
		}
	}
}
