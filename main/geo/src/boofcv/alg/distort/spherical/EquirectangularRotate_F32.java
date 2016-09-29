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

import boofcv.struct.distort.PointTransform_F32;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Applies a point transform that can be used to re-render an equirectangular image with a new center.  This lets you
 * close which parts of the image should be highly distorted and which should not.
 *
 * @author Peter Abeles
 */
public class EquirectangularRotate_F32 implements PointTransform_F32 {

	EquirectangularTools_F32 tools = new EquirectangularTools_F32();

	DenseMatrix64F R = CommonOps.identity(3,3);
	Vector3D_F32 n = new Vector3D_F32();

	float longitudeCenter;
	float latitudeCenter;

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
	 * Specifies which longitude/latitude will be along the displayed image center
	 *
	 * @param longitudeCenter center longitude line. -pi to pi
	 * @param latitudeCenter center latitude line. -pi/2 to pi/2
	 */
	public void setCenter( float longitudeCenter , float latitudeCenter ) {
		this.longitudeCenter = longitudeCenter;
		this.latitudeCenter = latitudeCenter;
		ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,latitudeCenter,0,longitudeCenter,R);
	}

	@Override
	public void compute(float x, float y, Point2D_F32 out) {

		tools.equiToNorm(x,y,n);
		GeometryMath_F32.mult(R,n,n);
		tools.normToEqui(n.x,n.y,n.z,out);
	}

	public EquirectangularTools_F32 getTools() {
		return tools;
	}

	public float getLongitudeCenter() {
		return longitudeCenter;
	}

	public float getLatitudeCenter() {
		return latitudeCenter;
	}
}
