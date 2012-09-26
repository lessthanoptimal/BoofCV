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

package boofcv.alg.geo;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class PerspectiveOps {

	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , DenseMatrix64F K , Point3D_F64 X ) {
		Point3D_F64 X_cam = new Point3D_F64();

		SePointOps_F64.transform(worldToCamera,X,X_cam);

		Point2D_F64 norm = new Point2D_F64(X_cam.x/X_cam.z,X_cam.y/X_cam.z);
		Point2D_F64 pixel = new Point2D_F64();

		GeometryMath_F64.mult(K,norm,pixel);

		return pixel;
	}

	/**
	 * Computes the image coordinate of a point given its 3D location and the camera matrix.
	 *
	 * @param worldToCamera 3x4 camera matrix for transforming a 3D point from world to image coordinates.
	 * @param X Point in 3D world coordinates.
	 * @return Point in 2D image coordinates.
	 */
	public static Point2D_F64 renderPixel( DenseMatrix64F worldToCamera , Point3D_F64 X ) {
		DenseMatrix64F P = worldToCamera;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		Point2D_F64 pixel = new Point2D_F64();

		pixel.x = x/z;
		pixel.y = y/z;

		return pixel;
	}
}
