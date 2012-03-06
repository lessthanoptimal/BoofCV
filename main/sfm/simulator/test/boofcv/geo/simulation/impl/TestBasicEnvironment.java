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
import boofcv.geo.simulation.SimPoint3D;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBasicEnvironment {

	Random rand = new Random(234);

	/**
	 * The camera is not in the world coordinate system.  Make sure it spawns points correctly
	 */
	@Test
	public void spawn()
	{
		Se3_F64 pose = new Se3_F64();
		pose.getT().set(2,0,0);
		RotationMatrixGenerator.eulerXYZ(0,Math.PI,0,pose.getR());

		DummyCamera cam = new DummyCamera();
		cam.pose = pose;
		
		BasicEnvironment alg = new BasicEnvironment(rand,6,0,5);

		List<SimPoint3D> spawned = alg.requestSpawn(cam,1);
		
		assertEquals(1,spawned.size());
		SimPoint3D p = spawned.get(0);

		assertEquals(2,p.world.getX(),1e-8);
		assertEquals(0,p.world.getY(),1e-8);
		assertEquals(-5, p.world.getZ(), 1e-8);
	}

	/**
	 * Drops old unused features
	 */
	@Test
	public void dropOld() {
		int discardNum = 5;
		DummyCamera cam = new DummyCamera();
		BasicEnvironment alg = new BasicEnvironment(rand,discardNum,0,5);

		assertEquals(0, alg.getPoints().size());
		alg.requestSpawn(cam, 5);
		for( int i = 0; i < discardNum; i++ ) {
			assertEquals(5,alg.getPoints().size());
			alg.maintenance();
		}
		assertEquals(5,alg.getPoints().size());
		alg.maintenance();
		assertEquals(0,alg.getPoints().size());
	}

	/**
	 * Ensures that recycled data has been correctly reset
	 */
	@Test
	public void recycleData() {
		DummyCamera cam = new DummyCamera();
		BasicEnvironment alg = new BasicEnvironment(rand,1,0,5);

		alg.requestSpawn(cam, 5);
		for( SimPoint3D p : alg.getPoints() ) {
			p.trackData = 1;
		}
		// make it drop the new points
		alg.maintenance();
		alg.maintenance();
		assertEquals(0,alg.getPoints().size());

		// spawn more and see if track data is null
		alg.requestSpawn(cam, 5);
		for( SimPoint3D p : alg.getPoints() ) {
			assertTrue(p.id >= 5);
			assertTrue(p.trackData == null);
		}
	}
	
	public static class DummyCamera implements CameraModel {

		public Se3_F64 pose = new Se3_F64();
		
		@Override
		public boolean projectPoint(Point3D_F64 world, Point2D_F64 pixel) {
			return true;
		}

		@Override
		public void setCameraToWorld(Se3_F64 pose) {
			this.pose = pose;
		}

		@Override
		public Se3_F64 getCameraToWorld() {
			return pose;
		}
	}
}
