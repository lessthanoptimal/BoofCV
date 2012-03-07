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

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.geo.simulation.CameraModel;
import boofcv.geo.simulation.EnvironmentModel;
import boofcv.geo.simulation.SimPoint3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Simulated tracker that examines all the points to see what is visible in the camera.
 * It keeps track of a point as long as it is continuously visible from the point of time
 * it was spawned.
 *
 * @author Peter Abeles
 */
public class SimulatedTracker implements ImagePointTracker {
	// features which are currently visible
	List<PointTrack> current = new ArrayList<PointTrack>();
	// features which were visible last time process was called
	List<PointTrack> previous = new ArrayList<PointTrack>();
	// features that were visible but are not any more
	List<PointTrack> dropped = new ArrayList<PointTrack>();
	// features that were visible but are not any more
	List<PointTrack> spawned = new ArrayList<PointTrack>();

	EnvironmentModel environment;
	CameraModel camera;

	Stack<PointTrack> unused = new Stack<PointTrack>();
	Point2D_F64 pixel = new Point2D_F64();

	// the maximum number of tracks it tries to maintain on each spawn
	int targetTracks;
	
	// number of visible tracks
	int numVisible;

	public SimulatedTracker(EnvironmentModel environment,
							CameraModel camera , int targetTracks ) {
		this.environment = environment;
		this.camera = camera;
		this.targetTracks = targetTracks;
	}

	@Override
	public void process(ImageBase image) {
		// swap current and previous
		List<PointTrack> temp = current;
		current = previous;
		previous = temp;

		// add new visible tracks
		current.clear();
		dropped.clear();
		spawned.clear();
		
		List<SimPoint3D> simPoints = environment.getPoints();

		long currentTick = environment.getTick();
		
		for( SimPoint3D p : simPoints ) {
			if( camera.projectPoint(p.world,pixel) ) {
				PointTrack track = p.getTrackData();
				if( track == null ) {
					// this can happen if a track was visible, then was not, but became visible
					// again.  Simply ignore these tracks
				} else {
					track.set(pixel);
					p.timeLastViewed = currentTick;
					current.add(track);
				}
			}
		}
		// save the number of visible tracks.  can't use current.size()
		// since that can be changed.
		numVisible = current.size();

		// if a track was visible and no longer is it is then dropped
		for( PointTrack p : previous ) {
			if( !current.contains(p) ) {
				// this marks the track as being dropped by the tracker
				// but the point will still be around in the simulated world
				SimPoint3D s = p.getDescription();
				s.trackData = null;
				dropped.add(p);
				unused.add(p);
			}
		}
	}

	@Override
	public boolean addTrack(double x, double y) {
		throw new RuntimeException("Not supported");
	}

	@Override
	public void spawnTracks() {
		int numSpawn = targetTracks - numVisible;

		List<SimPoint3D> simPoints = environment.requestSpawn(camera,numSpawn);

		long currentTick = environment.getTick();

		for( SimPoint3D p : simPoints ) {
			if( camera.projectPoint(p.world,pixel) ) {
				PointTrack track = p.getTrackData();
				if( track != null ) {
					throw new RuntimeException("Bug, wasn't cleaned up");
				} else {
					if( unused.isEmpty() ) {
						track = new PointTrack();
					} else {
						track = unused.pop();
					}
					track.set(pixel);
					track.setDescription(p);
					p.trackData = track;
					p.timeLastViewed = currentTick;
					current.add(track);
					spawned.add(track);
				}
			}
		}
	}

	@Override
	public void dropTracks() {
		unused.addAll(current);
		dropped.addAll(current);
		current.clear();
	}

	@Override
	public void dropTrack(PointTrack track) {
		current.remove(track);
		dropped.add(track);
		unused.add(track);
	}

	@Override
	public List<PointTrack> getActiveTracks() {
		return current;
	}

	@Override
	public List<PointTrack> getDroppedTracks() {
		return dropped;
	}

	@Override
	public List<PointTrack> getNewTracks() {
		return spawned;
	}
}
