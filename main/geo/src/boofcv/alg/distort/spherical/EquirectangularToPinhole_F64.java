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

import boofcv.alg.distort.PixelToNormalized_F64;
import boofcv.struct.calib.PinholeIntrinsic;
import boofcv.struct.distort.PixelTransform_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Renders a pinhole camera view from an equirectangular image.  When no additional rotation
 * is applied the camera will lie along the +z axis
 *
 * @author Peter Abeles
 */
public class EquirectangularToPinhole_F64 extends PixelTransform_F64 {

	EquirectangularTools_F64 tools = new EquirectangularTools_F64();
	PinholeIntrinsic pinhole;

	DenseMatrix64F direction = CommonOps.identity(3,3);
	Vector3D_F64 n = new Vector3D_F64();
	Point2D_F64 out = new Point2D_F64();

	Vector3D_F64[] vectors;

	public EquirectangularToPinhole_F64(PinholeIntrinsic pinhole) {
		this.pinhole = pinhole;

		vectors = new Vector3D_F64[pinhole.width*pinhole.height];
		for (int i = 0; i < vectors.length; i++) {
			vectors[i] = new Vector3D_F64();
		}

		// computing the 3D ray through each pixel in the pinhole camera at it's canonical
		// location
		PixelToNormalized_F64 pixelToNormalized = new PixelToNormalized_F64();
		pixelToNormalized.set(pinhole.fx,pinhole.fy,pinhole.skew,pinhole.cx,pinhole.cy);

		Point2D_F64 norm = new Point2D_F64();
		for (int pixelY = 0; pixelY < pinhole.height; pixelY++) {
			for (int pixelX = 0; pixelX < pinhole.width; pixelX++) {
				// the +0.5 is so that color which passes through pixel's center is used
				// this will be slightly less biased I think
				pixelToNormalized.compute(pixelX + 0.5, pixelY + 0.5, norm);
				Vector3D_F64 v = vectors[pixelY*pinhole.width+pixelX];

				v.set(norm.x,norm.y,1);
			}
		}
	}

	/**
	 * Specify the shame of the equirectangular image
	 *
	 * @param width equirectangular image width
	 * @param height equirectangular image height
	 */
	public void setEquirectangularShape(int width , int height ) {
		tools.configure(width, height);
	}

	/**
	 * Specifies the rotation offset from the canonical location using yaw and pitch.
	 * @param yaw Radian from -pi to pi
	 * @param pitch Radian from -pi/2 to pi/2
	 */
	public void setDirection(double yaw, double pitch ) {
		ConvertRotation3D_F64.eulerToMatrix(EulerType.YXZ,pitch,0,yaw,direction);
	}

	/**
	 * Specifies direction using a rotation matrix
	 * @param R rotation matrix
	 */
	public void setDirection( DenseMatrix64F R ) {
		this.direction.set(R);
	}

	/**
	 * Input is in pinhole camera pixel coordinates.  Output is in equirectangular coordinates
	 *
	 * @param x Pixel x-coordinate in rendered pinhole camera
	 * @param y Pixel y-coordinate in rendered pinhole camera
	 */
	@Override
	public void compute(int x, int y) {
		// grab precomputed normalized image coordinate at canonical location
		Vector3D_F64 v = vectors[y*pinhole.width+x];
		// move to requested orientation
		GeometryMath_F64.mult(direction,v,n); // TODO make faster by not using an array based matrix
		// compute pixel coordinate
		tools.normToEqui(n.x,n.y,n.z,out);

		distX = out.x;
		distY = out.y;
	}
}
