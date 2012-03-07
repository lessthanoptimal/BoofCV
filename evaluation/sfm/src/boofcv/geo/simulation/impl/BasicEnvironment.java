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

import boofcv.geo.simulation.CameraModel;
import boofcv.geo.simulation.EnvironmentModel;
import boofcv.geo.simulation.SimPoint3D;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Creates tracks only when requested in front of a camera.  Discards points if they have not been
 * viewed in X frames.
 *
 * @author Peter Abeles
 */
public class BasicEnvironment implements EnvironmentModel {

	// used to randomly select point location
	private Random rand;

	// list of all points in the simulated world
	private List<SimPoint3D> points = new ArrayList<SimPoint3D>();
	// number of times maintenance has been called
	private long tick;
	
	// discard points that have not been viewed after this amount of time
	private long discardTicks;

	// sigma that points are generated around
	private double spreadSigma;

	// how far in front of the camera should points be generated
	private double pointGenerateDistance;

	// simulated points which are to be recycled
	private Stack<SimPoint3D> unused = new Stack<SimPoint3D>();

	// provide each track with a unique track ID
	private long lastTrackID;

	public BasicEnvironment(Random rand, long discardTicks,
							double spreadSigma, double pointGenerateDistance) {
		this.rand = rand;
		this.discardTicks = discardTicks;
		this.spreadSigma = spreadSigma;
		this.pointGenerateDistance = pointGenerateDistance;
	}

	@Override
	public long getTick() {
		return tick;
	}

	@Override
	public List<SimPoint3D> getPoints() {
		return points;
	}

	@Override
	public List<SimPoint3D> requestSpawn(CameraModel camera, int numFeatures) {
		
		Se3_F64 cameraToWorld = camera.getWorldToCamera();
		
		List<SimPoint3D> spawned = new ArrayList<SimPoint3D>();
		
		for( int i = 0; i < numFeatures; i++ ) {
			SimPoint3D s;
			if( unused.size() > 0 ) {
				s = unused.pop();
			} else {
				s = new SimPoint3D();
			}
			s.id = lastTrackID++;
			s.timeLastViewed = tick;
			s.trackData = null;

			Point3D_F64 p = s.world;
			p.x = rand.nextGaussian()*spreadSigma;
			p.y = rand.nextGaussian()*spreadSigma;
			p.z = rand.nextGaussian()*spreadSigma + pointGenerateDistance;

			SePointOps_F64.transform(cameraToWorld,p,p);
			
			spawned.add(s);
			points.add(s);
		}
		
		return spawned;
	}

	@Override
	public void maintenance() {
		// discard old points which are no longer being used
		for( int i = points.size()-1; i >= 0; i-- ) {
			SimPoint3D p = points.get(i);
			
			if( tick - p.timeLastViewed >= discardTicks ) {
				points.remove(i);
				unused.add(p);
			}
		}
		
		tick++;
	}
}
