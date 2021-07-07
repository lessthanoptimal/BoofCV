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

package boofcv.alg.geo.rectify;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyFundamental extends BoofStandardJUnit {

	int N = 30;
	List<AssociatedPair> pairs;

	Se3_F64 motion;
	DMatrixRMaj F;

	@Test void rotateEpipole() {
		Point3D_F64 epipole = new Point3D_F64(1250,210,0.5);

		int cx = 400,cy=350;
		SimpleMatrix R = RectifyFundamental.rotateEpipole(epipole,cx,cy);

		Point3D_F64 work = epipole.copy();
		work.scale(1.0/work.z);

		SimpleMatrix x = new SimpleMatrix(new double[][]{{work.x-cx},{work.y-cy},{1}});
		SimpleMatrix found = R.mult(x);

		assertTrue(Math.abs(found.get(0))>0);
		assertEquals(0,Math.abs(found.get(1)), UtilEjml.TEST_F64);
		assertEquals(1,Math.abs(found.get(2)), UtilEjml.TEST_F64);
	}

	/**
	 * Checks to see that the epipoles go to infinity after applying the transforms
	 */
	@Test void checkEpipoles() {
		createScene();

		// extract eipoles
		Point3D_F64 epipole1 = new Point3D_F64();
		Point3D_F64 epipole2 = new Point3D_F64();

		MultiViewOps.extractEpipoles(F, epipole1, epipole2);

		// compute the rectification transforms
		RectifyFundamental alg = new RectifyFundamental();
		alg.process(F,pairs,500,520);

		DMatrixRMaj R1 = alg.getRect1();
		DMatrixRMaj R2 = alg.getRect2();

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
	@Test void alignY() {
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

		DMatrixRMaj K = new DMatrixRMaj(3,3,true,500,0,250,0,520,270,0,0,1);

		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().setTo(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,-0.01, 0.1, 0.05, null));
		motion.getT().setTo(-0.5,0.1,-0.05);

		DMatrixRMaj E = MultiViewOps.createEssential(motion.getR(), motion.getT(), null);
		F = MultiViewOps.createFundamental(E, K);

		// randomly generate points in space
		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<>();

		for(Point3D_F64 p1 : worldPts) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.setTo(p1.x/p1.z,p1.y/p1.z);
			pair.p2.setTo(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			GeometryMath_F64.mult(K, pair.p1, pair.p1);
			GeometryMath_F64.mult(K, pair.p2, pair.p2);
		}
	}
}
