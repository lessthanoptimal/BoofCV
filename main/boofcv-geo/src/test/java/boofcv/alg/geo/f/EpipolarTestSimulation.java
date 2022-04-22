/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPair3D;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Standardized checks for computing fundamental matrices
 *
 * @author Peter Abeles
 */
public abstract class EpipolarTestSimulation extends BoofStandardJUnit {

	// create a reasonable calibration matrix
	protected CameraPinhole intrinsic = new CameraPinhole(60,80,0.01,200,150,400,300);
	protected DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic,(DMatrixRMaj)null);

	protected Se3_F64 a_to_b;
	protected List<Point3D_F64> pointsInA;
	protected List<AssociatedPair> pairs;
	protected List<AssociatedPair3D> pairsPointing;
	protected List<Point2D_F64> currentObs;

	public void init( int N , boolean isFundamental ) {
		// define the camera's motion
		a_to_b = define_a_to_b();

		// randomly generate points in space
		pointsInA = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<>();
		pairsPointing = new ArrayList<>();
		currentObs = new ArrayList<>();

		for(Point3D_F64 p1 : pointsInA) {
			Point3D_F64 p2 = SePointOps_F64.transform(a_to_b, p1, null);

			var pair = new AssociatedPair();
			pair.p1.setTo(p1.x/p1.z,p1.y/p1.z);
			pair.p2.setTo(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			var pair3D = new AssociatedPair3D();
			pair3D.p1.setTo(p1);
			pair3D.p2.setTo(p2);
			pairsPointing.add(pair3D);

			if( isFundamental ) {
				GeometryMath_F64.mult(K, pair.p1, pair.p1);
				GeometryMath_F64.mult(K, pair.p2, pair.p2);
			}

			currentObs.add(pair.p2);
		}
	}

	protected Se3_F64 define_a_to_b() {
		return SpecialEuclideanOps_F64.eulerXyz(0.1,-0.1,0.01, 0.05, -0.03, 0.02,null);
	}

	public List<AssociatedPair> randomPairs( int N ) {
		List<AssociatedPair> ret = new ArrayList<>();

		while( ret.size() < N ) {
			AssociatedPair p = pairs.get(rand.nextInt(pairs.size()));
			if( !ret.contains(p) )
				ret.add(p);
		}

		return ret;
	}

	/**
	 * Random observations as pointing vectors
	 */
	public List<AssociatedPair3D> randomPairsPointing( int N ) {
		List<AssociatedPair3D> ret = new ArrayList<>();

		while( ret.size() < N ) {
			AssociatedPair3D p = pairsPointing.get(rand.nextInt(pairs.size()));
			if( !ret.contains(p) )
				ret.add(p);
		}

		return ret;
	}
}
