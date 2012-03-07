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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDistortedPinholeCamera {

	Random rand = new Random(234);
	
	int imageWidth = 500;
	int imageHeight = 600;
	
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,600,0,imageWidth/2,0,650,imageHeight/2,0,0,1);

	/**
	 * Make sure features behind the camera are not seen
	 */
	@Test
	public void checkBehindCamera() {
		DistortedPinholeCamera alg = createCamera(0);

		// point along optical axis behind the camera
		Point3D_F64 p3 = new Point3D_F64(0,2,-10);
		Point2D_F64 found = new Point2D_F64(1,2);

		assertFalse(alg.projectPoint(p3,found));
	}

	/**
	 * Give it a few points with known positions or general locations
	 */
	@Test
	public void checkProjectKnown() {
		DistortedPinholeCamera alg = createCamera(0);
		
		Se3_F64 pose = new Se3_F64();
		pose.getT().set(0,2,0);
		alg.setCameraToWorld(pose);
		
		// point along optical axis
		Point3D_F64 p3 = new Point3D_F64(0,2,10);
		Point2D_F64 found = new Point2D_F64(1,2);

		assertTrue(alg.projectPoint(p3,found));
		
		// should be in the image's center
		assertEquals(imageWidth/2,found.x,1e-8);
		assertEquals(imageHeight/2,found.y,1e-8);
		
		// put the point out of center and see if it is above/below the center
		p3.set(0.1,2.2,10);
		assertTrue(alg.projectPoint(p3,found));
		assertTrue(found.x>imageWidth/2);
		assertTrue(found.y<imageHeight/2);

		p3.set(-0.1,1.9,10);
		assertTrue(alg.projectPoint(p3,found));
		assertTrue(found.x<imageWidth/2);
		assertTrue(found.y>imageHeight/2);
	}

	/**
	 * See if it handles the y-axis orientation correction
	 */
	@Test
	public void checkYAxis() {
		DistortedPinholeCamera alg = createCamera(0);

		Se3_F64 pose = new Se3_F64();
		pose.getT().set(0,2,0);
		alg.setCameraToWorld(pose);

		//have the point be above the optical axis
		Point3D_F64 p3 = new Point3D_F64(0,2.2,10);
		Point2D_F64 found = new Point2D_F64(1,2);

		// it should now be below
		assertTrue(alg.projectPoint(p3,found));
		assertTrue(found.y < imageHeight / 2);

		// change the y-axis direction
		alg.setyAxisDown(false);
		assertTrue(alg.projectPoint(p3,found));
		assertTrue(found.y>imageHeight/2);
	}

	/**
	 * Give it four points which should be outside the image
	 */
	@Test
	public void checkIsInside() {
		DistortedPinholeCamera alg = createCamera(0);

		Point3D_F64 p3 = new Point3D_F64(10,0,1);
		Point2D_F64 found = new Point2D_F64(1,2);
		
		assertFalse(alg.projectPoint(p3,found));
		
		p3.set(0,10,1);
		assertFalse(alg.projectPoint(p3,found));

		p3.set(0,-10,1);
		assertFalse(alg.projectPoint(p3,found));

		p3.set(-10,0,1);
		assertFalse(alg.projectPoint(p3,found));
	}

	
	private DistortedPinholeCamera createCamera( double sigma ) {
		return new DistortedPinholeCamera(rand,K,null,imageWidth,imageHeight,true,sigma);
	}
}
