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

package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.GeoTestingOps;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Common testing code for algorithms which estimate motion from a set of associated observations
 * and known 3D coordinates.
 *
 * @author Peter Abeles
 */
public abstract class CommonMotionNPoint {

	protected Random rand = new Random(234);

	public abstract Se3_F64 compute( List<AssociatedPair> obs , List<Point3D_F64> locations );

	/**
	 * Test a set of random points in general position
	 * @param N number of observed point objects
	 */
	public void standardTest( int N ) {
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		// randomly generate points in space
		List<Point3D_F64> pts = GeoTestingOps.randomPoints_F32(-1, 1, -1, 1, 2, 3, N, rand);
		List<Point3D_F64> ptsB = new ArrayList<Point3D_F64>();

		// transform points into second camera's reference frame
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for(Point3D_F64 p1 : pts ) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.keyLoc.set(p1.x/p1.z,p1.y/p1.z);
			pair.currLoc.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			ptsB.add(p2);
		}

		// extract the motion
		Se3_F64 found = compute(pairs,pts);

		// see if the found motion produces the same output as the original motion
		for( int i = 0; i < pts.size(); i++ ) {
			Point3D_F64 p1 = pts.get(i);
			Point3D_F64 p2 = ptsB.get(i);

			Point3D_F64 foundPt = SePointOps_F64.transform(found, p1, null);

			assertEquals(p2.x,foundPt.x,1e-8);
			assertEquals(p2.y,foundPt.y,1e-8);
			assertEquals(p2.z,foundPt.z,1e-8);
		}
	}
}


