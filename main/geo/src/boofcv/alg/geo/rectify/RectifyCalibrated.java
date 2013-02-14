/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.rectify;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

/**
 * <p>
 * Rectifies a stereo pair with known camera calibration using a simple algorithm described in [1]
 * such that the epipoles project to infinity along the x-axis. After rectification both images will
 * have the same intrinsic calibration matrix, same extrinsic rotation matrix, but the optical centers
 * are not modified.
 * </p>
 *
 * <p>
 * The calibration matrix is the standard upper triangular matrix used throughout the library.  A single
 * calibration matrix is computed for both images by averaging the two original and setting the skew
 * to zero.
 * </p>
 *
 * <p>
 * The rectified view is chosen such that it will be most similar to the first camera.  This is done by making
 * the original z-axis and the rectified z-axis similar.
 * </p>
 *
 * <p>
 * [1] A. Fusiello, E. Trucco, and A. Verri, "A Compact Algorithm for Rectification of Stereo Pairs"
 * Machine Vision and Applications, 2000
 * </p>
 *
 * @author Peter Abeles
 */
public class RectifyCalibrated {

	// rectifying transform for left and right images
	DenseMatrix64F rect1 = new DenseMatrix64F(3,3);
	DenseMatrix64F rect2 = new DenseMatrix64F(3,3);

	// direction of new coordinate system axises
	Vector3D_F64 v1 = new Vector3D_F64();
	Vector3D_F64 v2 = new Vector3D_F64();
	Vector3D_F64 v3 = new Vector3D_F64();

	// Camera calibration matrix.
	SimpleMatrix K = new SimpleMatrix(3,3);

	// rotation matrix of rectified cameras
	DenseMatrix64F rectifiedR;

	/**
	 * Computes rectification transforms for both cameras and optionally a single calibration
	 * matrix.
	 *
	 * @param K1 Calibration matrix for first camera.
	 * @param worldToCamera1 Location of the first camera.
	 * @param K2 Calibration matrix for second camera.
	 * @param worldToCamera2 Location of the second camera.
	 */
	public void process( DenseMatrix64F K1 , Se3_F64 worldToCamera1 ,
						 DenseMatrix64F K2 , Se3_F64 worldToCamera2 )
	{
		SimpleMatrix sK1 = SimpleMatrix.wrap(K1);
		SimpleMatrix sK2 = SimpleMatrix.wrap(K2);
		SimpleMatrix R1 = SimpleMatrix.wrap(worldToCamera1.getR());
		SimpleMatrix R2 = SimpleMatrix.wrap(worldToCamera2.getR());
		SimpleMatrix T1 = new SimpleMatrix(3,1,true,
				worldToCamera1.getT().x,worldToCamera1.getT().y,worldToCamera1.getT().z);
		SimpleMatrix T2 = new SimpleMatrix(3,1,true,
				worldToCamera2.getT().x,worldToCamera2.getT().y,worldToCamera2.getT().z);

		//  P = K*[R|T]
		SimpleMatrix KR1 = sK1.mult(R1);
		SimpleMatrix KR2 = sK2.mult(R2);

		// compute optical centers in world reference frame
		// c = -R'*T
		SimpleMatrix c1 = R1.transpose().mult(T1.scale(-1));
		SimpleMatrix c2 = R2.transpose().mult(T2.scale(-1));

		// new coordinate system axises
		selectAxises(R1, c1, c2);

		// new extrinsic parameters, rotation matrix with rows of camera 1's coordinate system in
		// the world frame
		SimpleMatrix RR = new SimpleMatrix(3,3,true,
				v1.x,v1.y,v1.z,
				v2.x,v2.y,v2.z,
				v3.x,v3.y,v3.z);

		// new calibration matrix that is an average of the original
		K = sK1.plus(sK2).scale(0.5);
		K.set(0,1,0);// set skew to zero

		// new projection rotation matrices
		SimpleMatrix KRR = K.mult(RR);

		// rectification transforms
		rect1.set(KRR.mult(KR1.invert()).getMatrix());
		rect2.set(KRR.mult(KR2.invert()).getMatrix());

		rectifiedR = RR.getMatrix();
	}

	/**
	 * Selects axises of new coordinate system
	 */
	private void selectAxises(SimpleMatrix R, SimpleMatrix c1, SimpleMatrix c2) {
		// --------- Compute the new x-axis
		v1.set(c2.get(0) - c1.get(0), c2.get(1) - c1.get(1), c2.get(2) - c1.get(2));
		v1.normalize();

		// --------- Compute the new y-axis
		//   cross product of old z axis and new x axis
		// According to the paper [1] this choice is arbitrary, however it is not.  By selecting
		// the original axis the similarity with the first view is maximized.  The other extreme
		// would be to make it perpendicular, resulting in an unusable rectification.

		// extract old z-axis from rotation matrix
		Vector3D_F64 oldZ = new Vector3D_F64(R.get(2,0), R.get(2,1), R.get(2,2));
		GeometryMath_F64.cross(oldZ, v1, v2);
		v2.normalize();

		// ---------- Compute the new z-axis
		// simply the process product of the first two
		GeometryMath_F64.cross(v1,v2,v3);
		v3.normalize();
	}

	/**
	 * Rectification transform for first camera
	 */
	public DenseMatrix64F getRect1() {
		return rect1;
	}

	/**
	 * Rectification transform for second camera
	 */
	public DenseMatrix64F getRect2() {
		return rect2;
	}

	/**
	 * If a single calibration matrix was requested then this returns it.
	 *
	 * @return Calibration matrix for both cameras
	 */
	public DenseMatrix64F getCalibrationMatrix() {
		return K.getMatrix();
	}

	/**
	 * Rotation matrix of rectified coordinate system
	 *
	 * @return Rotation matrix
	 */
	public DenseMatrix64F getRectifiedRotation() {
		return rectifiedR;
	}
}
