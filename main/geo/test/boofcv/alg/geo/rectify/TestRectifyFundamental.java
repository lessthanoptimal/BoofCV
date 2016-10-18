/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.rectify;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyFundamental {

	int N = 30;
	Random rand = new Random(234);
	List<AssociatedPair> pairs;

	Se3_F64 motion;
	DenseMatrix64F F;

	/**
	 * Checks to see that the epipoles go to infinity after applying the transforms
	 */
	@Test
	public void checkEpipoles() {
		createScene();

		// extract eipoles
		Point3D_F64 epipole1 = new Point3D_F64();
		Point3D_F64 epipole2 = new Point3D_F64();

		MultiViewOps.extractEpipoles(F, epipole1, epipole2);

		// compute the rectification transforms
		RectifyFundamental alg = new RectifyFundamental();
		alg.process(F,pairs,500,520);

		DenseMatrix64F R1 = alg.getRect1();
		DenseMatrix64F R2 = alg.getRect2();

		// sanity check

		assertTrue(Math.abs(epipole1.z) > 1e-8);
		assertTrue(Math.abs(epipole2.z) > 1e-8);

		// see if epipoles are projected to infinity
		GeometryMath_F64.mult(R1,epipole1,epipole1);
		GeometryMath_F64.mult(R2,epipole2,epipole2);

		assertEquals(0, epipole1.z, 1e-12);
		assertEquals(0, epipole2.z, 1e-12);
	}

	/**
	 * See if the transform align an observation to the same y-axis
	 */
	@Test
	public void alignY() {
		createScene();

		RectifyFundamental alg = new RectifyFundamental();
		alg.process(F,pairs,500,520);

		// unrectified observations
		AssociatedPair unrect = pairs.get(0);

		// rectified observations
		Point2D_F64 r1 = new Point2D_F64();
		Point2D_F64 r2 = new Point2D_F64();

		GeometryMath_F64.mult(alg.getRect1(),unrect.p1,r1);
		GeometryMath_F64.mult(alg.getRect2(),unrect.p2,r2);

		assertEquals(r1.y,r2.y,1e-8);
	}

	public void createScene() {

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,500,0,250,0,520,270,0,0,1);

		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,-0.01, 0.1, 0.05, null));
		motion.getT().set(-0.5,0.1,-0.05);

		DenseMatrix64F E = MultiViewOps.createEssential(motion.getR(), motion.getT());
		F = MultiViewOps.createFundamental(E, K);

		// randomly generate points in space
		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<>();

		for(Point3D_F64 p1 : worldPts) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.set(p1.x/p1.z,p1.y/p1.z);
			pair.p2.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			GeometryMath_F64.mult(K, pair.p1, pair.p1);
			GeometryMath_F64.mult(K, pair.p2, pair.p2);
		}
	}
}
