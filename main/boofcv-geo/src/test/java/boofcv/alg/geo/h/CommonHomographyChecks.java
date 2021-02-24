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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPair3D;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates two views of a planar scene for checking homography related algorithms
 *
 * @author Peter Abeles
 */
public class CommonHomographyChecks extends BoofStandardJUnit {
	// create a reasonable calibration matrix
	protected DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 60, 0.01, 200, 0, 80, 150, 0, 0, 1);

	protected Se3_F64 motion = SpecialEuclideanOps_F64.eulerXyz(0.1, -0.1, 0.01, 0.05, -0.03, 0.02, null);
	protected List<Point3D_F64> pts;
	protected List<AssociatedPair> pairs2D;
	protected List<AssociatedPair3D> pairs3D;
	protected double d = 3; // distance plane is from camera

	protected DMatrixRMaj solution = new DMatrixRMaj(3, 3);

	public void createScene( int numPoints, boolean isPixels ) {
		// randomly generate points in space
		pts = createRandomPlane(rand, d, numPoints);

		// transform points into second camera's reference frame
		pairs2D = new ArrayList<>();
		for (Point3D_F64 p1 : pts) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.setTo(p1.x/p1.z, p1.y/p1.z);
			pair.p2.setTo(p2.x/p2.z, p2.y/p2.z);
			pairs2D.add(pair);

			if (isPixels) {
				GeometryMath_F64.mult(K, pair.p1, pair.p1);
				GeometryMath_F64.mult(K, pair.p2, pair.p2);
			}
		}

		pairs3D = new ArrayList<>();
		for (int i = 0; i < pairs2D.size(); i++) {
			AssociatedPair p2 = pairs2D.get(i);
			AssociatedPair3D p3 = new AssociatedPair3D();

			// same point but in homogenous coordinates and Z isn't always 1
			double z = 1.0 + rand.nextGaussian()*0.1;
			p3.p1.setTo(p2.p1.x*z, p2.p1.y*z, z);
			p3.p2.setTo(p2.p2.x*z, p2.p2.y*z, z);

			pairs3D.add(p3);
		}
	}

	protected DMatrixRMaj computeH( boolean isPixel ) {
		if (isPixel)
			return MultiViewOps.createHomography(motion.R, motion.T, 1, new Vector3D_F64(0, 0, -1), K);
		else
			return MultiViewOps.createHomography(motion.R, motion.T, 1, new Vector3D_F64(0, 0, -1));
	}

	/**
	 * Creates a set of random points along the (X,Y) plane
	 */
	public static List<Point3D_F64> createRandomPlane( Random rand, double d, int N ) {
		List<Point3D_F64> ret = new ArrayList<>();

		for (int i = 0; i < N; i++) {
			double x = (rand.nextDouble() - 0.5)*2;
			double y = (rand.nextDouble() - 0.5)*2;

			ret.add(new Point3D_F64(x, y, d));
		}

		return ret;
	}

	/**
	 * Create a random set of points which line on the x-y plane at distance 'd'
	 */
	public static List<Point4D_F64> createRandomPlaneH( Random rand, double d, int N ) {
		List<Point4D_F64> ret = new ArrayList<>();

		for (int i = 0; i < N; i++) {
			double x = (rand.nextDouble() - 0.5)*2;
			double y = (rand.nextDouble() - 0.5)*2;

			// make it more interesting by not having the same W for all the point
			double scale = rand.nextDouble() + 0.1;
			if (rand.nextBoolean())
				scale *= -1;

			ret.add(new Point4D_F64(scale*x, scale*y, scale*d, scale));
		}

		return ret;
	}
}
