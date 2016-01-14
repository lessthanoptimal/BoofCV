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

package boofcv.alg.geo.pose;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Common testing code for algorithms which estimate motion from a set of associated observations
 * and known 3D coordinates.
 *
 * @author Peter Abeles
 */
public abstract class ChecksMotionNPoint extends CommonMotionNPoint {

	public abstract Se3_F64 compute( List<AssociatedPair> obs , List<Point3D_F64> locations );


	/**
	 * Test a set of random points in general position
	 * @param N number of observed point objects
	 */
	public void testNoMotion( int N ) {
		Se3_F64 motion = new Se3_F64();

		checkMotion(N, motion,false);
	}

	/**
	 * Test a set of random points in general position
	 * @param N number of observed point objects
	 */
	public void standardTest( int N ) {
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		motion.getT().set(0.1,-0.1,0.01);

		checkMotion(N, motion,false);
	}

	/**
	 * Test a set of random points in general position
	 * @param N number of observed point objects
	 */
	public void planarTest( int N ) {
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		motion.getT().set(0.1,-0.1,0.01);

		checkMotion(N, motion,true);
	}

	private void checkMotion(int N, Se3_F64 motion, boolean planar ) {
		generateScene(N, motion, planar);

		// extract the motion
		Se3_F64 found = compute(assocPairs,worldPts);

		// see if the found motion produces the same output as the original motion
		for( int i = 0; i < worldPts.size(); i++ ) {
			Point3D_F64 p1 = worldPts.get(i);
			Point3D_F64 p2 = cameraPts.get(i);

			Point3D_F64 foundPt = SePointOps_F64.transform(found, p1, null);

			assertEquals(p2.x,foundPt.x,1e-6);
			assertEquals(p2.y,foundPt.y,1e-6);
			assertEquals(p2.z,foundPt.z,1e-6);
		}
	}

}


