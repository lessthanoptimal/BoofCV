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

package boofcv.alg.geo;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Convenience class which will take a point in world coordinates, translate it to camera reference frame,
 * then project onto the image plane and compute its pixels.  Supports lens distortion
 *
 * @author Peter Abeles
 */
public class WorldToCameraToPixel {

	// transform from world to camera reference frames
	private Se3_F64 worldToCamera;

	// storage for point in camera frame
	private Point3D_F64 cameraPt = new Point3D_F64();

	// transform from normalized image coordinates into pixels
	private Point2Transform2_F64 normToPixel;

	/**
	 * Specifies intrinsic camera parameters and  the transform from world to camera.
	 * @param intrinsic camera parameters
	 * @param worldToCamera transform from world to camera
	 */
	public void configure(CameraPinholeRadial intrinsic , Se3_F64 worldToCamera ) {
		configure( new LensDistortionRadialTangential(intrinsic), worldToCamera);
	}

	/**
	 * Specifies intrinsic camera parameters and  the transform from world to camera.
	 * @param distortion camera parameters
	 * @param worldToCamera transform from world to camera
	 */
	public void configure(LensDistortionNarrowFOV distortion , Se3_F64 worldToCamera ) {
		this.worldToCamera = worldToCamera;

		normToPixel = distortion.distort_F64(false,true);
	}

	/**
	 * Computes the observed location of the specified point in world coordinates in the camera pixel.  If
	 * the object can't be viewed because it is behind the camera then false is returned.
	 * @param worldPt Location of point in world frame
	 * @param pixelPt Pixel observation of point.
	 * @return True if visible (+z) or false if not visible (-z)
	 */
	public boolean transform( Point3D_F64 worldPt , Point2D_F64 pixelPt ) {
		SePointOps_F64.transform(worldToCamera,worldPt,cameraPt);

		// can't see the point
		if( cameraPt.z <= 0 )
			return false;

		normToPixel.compute(cameraPt.x/cameraPt.z, cameraPt.y/cameraPt.z, pixelPt);
		return true;
	}

	/**
	 * Computes location of 3D point in world as observed in the camera.  Point is returned if visible or null
	 * if not visible.
	 *
	 * @param worldPt Location of point on world reference frame
	 * @return Pixel coordinate of point or null if not visible
	 */
	public Point2D_F64 transform( Point3D_F64 worldPt ) {
		Point2D_F64 out = new Point2D_F64();
		if( transform(worldPt,out))
			return out;
		else
			return null;
	}
}
