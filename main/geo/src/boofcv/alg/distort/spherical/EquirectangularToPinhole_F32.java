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

import boofcv.alg.distort.PixelToNormalized_F32;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform_F32;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Renders a pinhole camera view from an equirectangular image.  When no additional rotation
 * is applied the camera will lie along the +z axis.  Before use you must invoke the following functions:
 * <ul>
 *     <li>{@link #setPinhole}</li>
 *     <li>{@link #setEquirectangularShape}</li>
 * </ul>
 *
 *
 *
 * @author Peter Abeles
 */
public class EquirectangularToPinhole_F32 extends PixelTransform_F32 {

	// function for doing match on equirectangular images
	EquirectangularTools_F32 tools = new EquirectangularTools_F32();

	// camera model without distortion
	CameraPinhole pinhole;

	// rotation matrix
	DenseMatrix64F direction = CommonOps.identity(3,3);

	// storage for intermediate variables
	Vector3D_F32 n = new Vector3D_F32();
	Point2D_F32 out = new Point2D_F32();

	// storage for precomputed pointing vectors for each pixel in pinhole camera
	Vector3D_F32[] vectors = new Vector3D_F32[0];

	/**
	 * Specifies the pinhole camera
	 * @param pinhole intrinsic parameters of pinhole camera
	 */
	public void setPinhole( CameraPinhole pinhole ) {
		this.pinhole = pinhole;

		if( vectors.length < pinhole.width*pinhole.height ) {
			vectors = new Vector3D_F32[pinhole.width * pinhole.height];
			for (int i = 0; i < vectors.length; i++) {
				vectors[i] = new Vector3D_F32();
			}
		}

		// computing the 3D ray through each pixel in the pinhole camera at it's canonical
		// location
		PixelToNormalized_F32 pixelToNormalized = new PixelToNormalized_F32();
		pixelToNormalized.set(pinhole.fx,pinhole.fy,pinhole.skew,pinhole.cx,pinhole.cy);

		Point2D_F32 norm = new Point2D_F32();
		for (int pixelY = 0; pixelY < pinhole.height; pixelY++) {
			for (int pixelX = 0; pixelX < pinhole.width; pixelX++) {
				pixelToNormalized.compute(pixelX, pixelY, norm);
				Vector3D_F32 v = vectors[pixelY*pinhole.width+pixelX];

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
	public void setDirection(float yaw, float pitch ) {
		ConvertRotation3D_F32.eulerToMatrix(EulerType.YXZ,pitch,0,yaw,direction);
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
		Vector3D_F32 v = vectors[y*pinhole.width+x];
		// move to requested orientation
		GeometryMath_F32.mult(direction,v,n); // TODO make faster by not using an array based matrix
		// compute pixel coordinate
		tools.normToEqui(n.x,n.y,n.z,out);

		distX = out.x;
		distY = out.y;
	}
}
