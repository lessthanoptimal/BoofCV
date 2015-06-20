/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.LensDistortionOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
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
	Se3_F64 worldToCamera;

	// storage for point in camera frame
	Point3D_F64 cameraPt = new Point3D_F64();

	// transform from normalized image coordinates into pixels
	PointTransform_F64 normToPixel;

	public void configure( IntrinsicParameters intrinsic , Se3_F64 worldToCamera ) {
		this.worldToCamera = worldToCamera;

		normToPixel = LensDistortionOps.distortTransform(intrinsic).distort_F64(false,true);
	}

	public boolean transform( Point3D_F64 worldPt , Point2D_F64 pixelPt ) {
		SePointOps_F64.transform(worldToCamera,worldPt,cameraPt);

		// can't see the point
		if( cameraPt.z <= 0 )
			return false;

		normToPixel.compute(cameraPt.x/cameraPt.z, cameraPt.y/cameraPt.z, pixelPt);
		return true;
	}

	public Point2D_F64 transform( Point3D_F64 worldPt ) {
		Point2D_F64 out = new Point2D_F64();
		if( transform(worldPt,out))
			return out;
		else
			return null;
	}
}
