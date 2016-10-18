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

package boofcv.alg.geo.h;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates two views of a planar scene for checking homography related algorithms
 *
 * @author Peter Abeles
 */
public class CommonHomographyChecks {

	protected Random rand = new Random(234234);

	// create a reasonable calibration matrix
	protected DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,-200,0,80,-150,0,0,1);

	protected Se3_F64 motion;
	protected List<Point3D_F64> pts;
	protected List<AssociatedPair> pairs;
	protected double d=3; // distance plane is from camera

	protected DenseMatrix64F solution = new DenseMatrix64F(3,3);

	public void createScene( int numPoints , boolean isPixels ) {
		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02,null));
		motion.getT().set(0.1,-0.1,0.01);

		// randomly generate points in space
		pts = createRandomPlane(rand,d,numPoints);

		// transform points into second camera's reference frame
		pairs = new ArrayList<>();
		for(Point3D_F64 p1 : pts ) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.set(p1.x/p1.z,p1.y/p1.z);
			pair.p2.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			if( isPixels ) {
				GeometryMath_F64.mult(K, pair.p1, pair.p1);
				GeometryMath_F64.mult(K, pair.p2,pair.p2);
			}
		}
	}

	/**
	 * Creates a set of random points along the (X,Y) plane
	 */
	public static List<Point3D_F64> createRandomPlane( Random rand , double d , int N )
	{
		List<Point3D_F64> ret = new ArrayList<>();

		for( int i = 0; i < N; i++ ) {
			double x = (rand.nextDouble()-0.5)*2;
			double y = (rand.nextDouble()-0.5)*2;

			ret.add( new Point3D_F64(x,y,d));
		}

		return ret;
	}
}
