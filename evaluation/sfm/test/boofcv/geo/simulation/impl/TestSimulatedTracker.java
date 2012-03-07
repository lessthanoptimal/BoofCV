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

package boofcv.geo.simulation.impl;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.geo.simulation.CameraModel;
import boofcv.geo.simulation.EnvironmentModel;
import boofcv.geo.simulation.SimPoint3D;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.geo.simulation.impl.TestBasicEnvironment.DummyCamera;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSimulatedTracker {

	/**
	 * Sees if it handles spawned tracks and updates them correctly
	 */
	@Test
	public void process_update() {

		DummyEnvironment env = new DummyEnvironment();
		DummyCamera cam = new DummyCamera();

		SimulatedTracker tracker = new SimulatedTracker(env,cam,10);

		// runs the tracker and see if it adds spawned tracks correctly
		assertEquals(0,tracker.getActiveTracks().size());

		env.maintenance();
		tracker.spawnTracks();
		assertEquals(10,tracker.getNewTracks().size());
		assertEquals(10,tracker.getActiveTracks().size());
		tracker.process(null);
		assertEquals(10,tracker.getActiveTracks().size());
		assertEquals(0,tracker.getNewTracks().size());
		assertEquals(0,tracker.getDroppedTracks().size());

		// no new tracks
		env.maintenance();
		tracker.process(null);
		assertEquals(10,tracker.getActiveTracks().size());
		assertEquals(0,tracker.getDroppedTracks().size());
		assertEquals(0,tracker.getNewTracks().size());

		// see if it updated the tick on tracks it viewed
		for( PointTrack t : tracker.getActiveTracks() ) {
			SimPoint3D p = t.getDescription();
			
			assertEquals(2,p.timeLastViewed);
		}
	}

	/**
	 * Spawns tracks then the environment drops some of them
	 */
	@Test
	public void process_drop() {
		DummyEnvironment env = new DummyEnvironment();
		DummyCamera cam = new DummyCamera();

		SimulatedTracker tracker = new SimulatedTracker(env,cam,10);

		// runs the tracker and see if it adds spawned tracks correctly
		assertEquals(0,tracker.getActiveTracks().size());

		env.maintenance();
		tracker.spawnTracks();
		assertEquals(10,tracker.getNewTracks().size());
		assertEquals(10,tracker.getActiveTracks().size());

		// drop some of the next update
		env.numDrop = 3;
		env.maintenance();
		tracker.process(null);
		assertEquals(7,tracker.getActiveTracks().size());
		assertEquals(0,tracker.getNewTracks().size());
		assertEquals(3,tracker.getDroppedTracks().size());

		// see if it updated the tick on tracks it viewed
		for( PointTrack t : tracker.getActiveTracks() ) {
			SimPoint3D p = t.getDescription();

			assertEquals(2,p.timeLastViewed);
		}
	}

	private class DummyEnvironment implements  EnvironmentModel {

		List<SimPoint3D> points = new ArrayList<SimPoint3D>();
		long tick = 0;

		public int numDrop;

		@Override
		public long getTick() {
			return tick;
		}

		@Override
		public List<SimPoint3D> getPoints() {
			return points;
		}

		@Override
		public List<SimPoint3D> requestSpawn( CameraModel camera , int numFeatures ) {
			List<SimPoint3D> spawned = new ArrayList<SimPoint3D>();

			for( int i = 0; i < numFeatures; i++ ) {
				spawned.add( new SimPoint3D());
			}
			
			points.addAll(spawned);
			return spawned;
		}

		@Override
		public void maintenance() {
			tick++;

			
			for( int i = 0; i < numDrop && points.size() > 0 ; i++ ) {
				points.remove(points.size()-1);
			}
		}
	}

}
