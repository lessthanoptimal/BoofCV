/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.pose;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.Intersection3D_F64;
import georegression.struct.EulerType;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BaseChecksPnP {
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,490,0,320,240,640,480).fsetRadial(0.1,-0.05);
	Random rand = new Random(234);

	Se3_F64 worldToCamera0;
	Se3_F64 worldToCamera1 ;

	boolean worldPointsZZero = false;

	public BaseChecksPnP() {
		// the world is directly in front of the camera
		worldToCamera0 = new Se3_F64();
		worldToCamera0.getT().set(0,0,5);

		// in front of the camera and rotated
		worldToCamera1 = new Se3_F64();
		worldToCamera1.getT().set(2, 5, 10);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.2, -0.34, 0.7, worldToCamera1.getR());

		// an early version of this test had it behind the camera.
		// no idea how anything passed that test since most algorithms assume that the world view is visible
	}

	public List<Point2D3D> createObservations( Se3_F64 worldToCamera , int total ) {
		return createObservations(worldToCamera,4,total);
	}

	public List<Point2D3D> createObservations( Se3_F64 worldToCamera , double nominalZ , int total ) {

		Se3_F64 cameraToWorld = worldToCamera.invert(null);

		// transform from pixel coordinates to normalized pixel coordinates, which removes lens distortion
		Point2Transform2_F64 pixelToNorm = LensDistortionOps.narrow(intrinsic).undistort_F64(true,false);

		List<Point2D3D> observations = new ArrayList<>();

		Point2D_F64 norm = new Point2D_F64();

		PlaneNormal3D_F64 plane3D = new PlaneNormal3D_F64();
		plane3D.n.set(0,0,1);
		GeometryMath_F64.mult(worldToCamera.R,plane3D.n,plane3D.n);
		plane3D.p.set(worldToCamera.T.x,worldToCamera.T.y,worldToCamera.T.z);

		LineParametric3D_F64 ray = new LineParametric3D_F64();
		Point3D_F64 cameraPt = new Point3D_F64();

		for (int i = 0; i < total; i++) {
			// randomly pixel a point inside the image
			double x = rand.nextDouble()*intrinsic.width;
			double y = rand.nextDouble()*intrinsic.height;

			pixelToNorm.compute(x,y,norm);

			// Randomly pick a depth and compute 3D coordinate

			if( worldPointsZZero ) {
				// find the world plane in the image
				ray.slope.set(norm.x,norm.y,1);
				Intersection3D_F64.intersect(plane3D,ray,cameraPt);
			} else {
				cameraPt.z = rand.nextDouble() + nominalZ;
				cameraPt.x = norm.x * cameraPt.z;
				cameraPt.y = norm.y * cameraPt.z;
			}

			// Change the point's reference frame from camera to world
			Point3D_F64 worldPt = new Point3D_F64();

			SePointOps_F64.transform(cameraToWorld, cameraPt, worldPt);

			if( worldPointsZZero ) {
				if( Math.abs(worldPt.z) > 1e-8 )
					throw new RuntimeException("Bug");
				worldPt.z = 0; // make it exactly zero
			}

			// Save the perfect noise free observation
			Point2D3D o = new Point2D3D();
			o.getLocation().set(worldPt);
			o.getObservation().set(norm.x,norm.y);

			observations.add(o);
		}

		return observations;
	}
}
