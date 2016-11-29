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

import boofcv.struct.distort.PixelTransform2_F32;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Base class for all distortions from an equirectangular image.  The output image precomputes pointing vectors from
 * a canonical view.  The source pixel is then computed by rotating each vector and computing the longitude and
 * latitude.
 *
 * @author Peter Abeles
 */
public abstract class EquirectangularDistortBase_F32 extends PixelTransform2_F32 {
	// function for doing match on equirectangular images
	EquirectangularTools_F32 tools = new EquirectangularTools_F32();

	int outWidth;

	// rotation matrix
	DenseMatrix64F R = CommonOps.identity(3,3);

	// storage for intermediate variables
	Vector3D_F32 n = new Vector3D_F32();
	Point2D_F32 out = new Point2D_F32();

	// storage for precomputed pointing vectors for each pixel in pinhole camera
	Point3D_F32[] vectors = new Point3D_F32[0];

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
	 * @param roll Radian from -pi to pi
	 */
	public void setDirection(float yaw, float pitch, float roll ) {
		ConvertRotation3D_F32.eulerToMatrix(EulerType.YZX,pitch,yaw,roll,R);
	}

	/**
	 * Specifies direction using a rotation matrix
	 * @param R rotation matrix
	 */
	public void setDirection( DenseMatrix64F R ) {
		this.R.set(R);
	}

	/**
	 * Declares storage for precomputed pointing vectors to output image
	 *
	 * @param width output image width
	 * @param height output image height
	 */
	protected void declareVectors( int width , int height ) {
		this.outWidth = width;

		if( vectors.length < width*height ) {
			Point3D_F32[] tmp = new Point3D_F32[width*height];

			System.arraycopy(vectors,0,tmp,0,vectors.length);
			for (int i = vectors.length; i < tmp.length; i++) {
				tmp[i] = new Point3D_F32();
			}
			vectors = tmp;
		}
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
		Point3D_F32 v = vectors[y*outWidth+x];
		// move to requested orientation
		GeometryMath_F32.mult(R,v,n); // TODO make faster by not using an array based matrix

		// compute pixel coordinate
		tools.normToEquiFV(n.x,n.y,n.z,out);

		distX = out.x;
		distY = out.y;
	}

	public EquirectangularTools_F32 getTools() {
		return tools;
	}

	public DenseMatrix64F getRotation() {
		return R;
	}
}
