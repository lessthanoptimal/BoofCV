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

package boofcv.alg.distort.spherical;

import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Transforms the equirectangular image as if the input image was taken by the camera at the same location but with
 * a rotation.    Includes a built in function to center the camera at a particular location to minimize the distortion.
 *
 * @author Peter Abeles
 */
public class EquirectangularRotate_F64 implements PointTransform_F64 {

	EquirectangularTools_F64 tools = new EquirectangularTools_F64();

	DenseMatrix64F R = CommonOps.identity(3,3);
	Vector3D_F64 n = new Vector3D_F64();

	double longitudeCenter;
	double latitudeCenter;

	/**
	 * Specifies the image's width and height
	 *
	 * @param width Image width
	 * @param height Image height
	 */
	public void setImageShape( int width , int height ) {
		tools.configure(width, height);
	}

	/**
	 * Applies a rotation matrix which will center the view at the specified longitude/latitude
	 *
	 * @param longitudeCenter center longitude line. -pi to pi
	 * @param latitudeCenter center latitude line. -pi/2 to pi/2
	 */
	public void setCenter( double longitudeCenter , double latitudeCenter ) {
		this.longitudeCenter = longitudeCenter;
		this.latitudeCenter = latitudeCenter;
		ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,latitudeCenter,0,longitudeCenter,R);
	}

	/**
	 * Allows an arbitrary rotation matrix to be applied
	 * @param R rotation matrix. 3x3
	 */
	public void setRotation( DenseMatrix64F R ) {
		this.R.set(R);
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {

		tools.equiToNorm(x,y,n);
		GeometryMath_F64.mult(R,n,n);
		tools.normToEqui(n.x,n.y,n.z,out);
	}

	public EquirectangularTools_F64 getTools() {
		return tools;
	}

	public double getLongitudeCenter() {
		return longitudeCenter;
	}

	public double getLatitudeCenter() {
		return latitudeCenter;
	}
}
