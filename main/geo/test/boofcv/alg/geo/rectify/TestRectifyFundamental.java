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

package boofcv.alg.geo.rectify;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.TestUtilEpipolar;
import boofcv.alg.geo.UtilEpipolar;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

		// compute the rectification transforms
		RectifyFundamental alg = new RectifyFundamental();
		alg.process(F,pairs,500,500);

		DenseMatrix64F R1 = alg.getRect1();
		DenseMatrix64F R2 = alg.getRect2();

		// adjust F
		DenseMatrix64F Fadj = new DenseMatrix64F(3,3);
		DenseMatrix64F temp = new DenseMatrix64F(3,3);
		CommonOps.multTransA(R2, F, temp);
		CommonOps.mult(temp,R1,Fadj);

		Point3D_F64 epipole1 = new Point3D_F64();
		Point3D_F64 epipole2 = new Point3D_F64();

		UtilEpipolar.extractEpipoles(Fadj,epipole1,epipole2);

	}

	public void createScene() {

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,500,0,250,0,500,250,0,0,1);

		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		DenseMatrix64F E = UtilEpipolar.computeEssential(motion.getR(),motion.getT());
		F = TestUtilEpipolar.computeF(E,K);

		// randomly generate points in space
		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<AssociatedPair>();

		for(Point3D_F64 p1 : worldPts) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.keyLoc.set(p1.x/p1.z,p1.y/p1.z);
			pair.currLoc.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			GeometryMath_F64.mult(K, pair.keyLoc, pair.keyLoc);
			GeometryMath_F64.mult(K, pair.currLoc, pair.currLoc);
		}
	}
}
