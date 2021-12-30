/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.jacobians;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Quaternion_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Jacobian for 4-tuple encoded {@link Quaternion_F64 Quaternion} (w,x,y,z).
 *
 * @author Peter Abeles
 */
public class JacobianSo3Quaternions implements JacobianSo3 {

	private final Quaternion_F64 quat = new Quaternion_F64();

	private final DMatrixRMaj R = new DMatrixRMaj(3, 3);
	private final DMatrixRMaj[] jacR = new DMatrixRMaj[4];

	public JacobianSo3Quaternions() {
		for (int i = 0; i < jacR.length; i++) {
			jacR[i] = new DMatrixRMaj(3, 3);
		}
	}

	@Override
	public void getParameters( DMatrixRMaj R, double[] parameters, int offset ) {
		ConvertRotation3D_F64.matrixToQuaternion(R, quat);
		parameters[offset] = quat.w;
		parameters[offset + 1] = quat.x;
		parameters[offset + 2] = quat.y;
		parameters[offset + 3] = quat.z;
	}

	@Override
	public void setParameters( double[] parameters, int offset ) {
		quat.w = parameters[offset];
		quat.x = parameters[offset + 1];
		quat.y = parameters[offset + 2];
		quat.z = parameters[offset + 3];

		// has to be the unit quaternion and there is nothing restricting the values of each parameter
		quat.normalize();
		ConvertRotation3D_F64.quaternionToMatrix(quat, R);

		computeJacobians();
	}

	protected void computeJacobians() {
		double w = quat.w;
		double x = quat.x;
		double y = quat.y;
		double z = quat.z;

		double r2 = w*w + x*x + y*y + z*z;
		double r = Math.sqrt(r2);

		// @formatter:off
		w /= r; x /= r; y /= r; z /= r;

		double d_r = -2/r2;

		// compute the rotation matrix divided by r2. This avoid a 1.0/r4 computation
		double R00 = w*w + x*x - y*y - z*z;
		double R01 = 2.0*(x*y - w*z);
		double R02 = 2.0*(x*z + w*y);

		double R10 = 2.0*(x*y + w*z);
		double R11 = w*w - x*x + y*y - z*z;
		double R12 = 2.0*(y*z - w*x);

		double R20 = 2.0*(x*z - w*y);
		double R21 = 2.0*(y*z + w*x);
		double R22 = w*w - x*x - y*y + z*z;

		// Compute parial for each variable
		//---------- Partial w
		DMatrixRMaj Rw = jacR[0];
		Rw.data[0] =  2*w/r + R00*w*d_r;
		Rw.data[1] = -2*z/r + R01*w*d_r;
		Rw.data[2] =  2*y/r + R02*w*d_r;
		Rw.data[3] =  2*z/r + R10*w*d_r;
		Rw.data[4] =  2*w/r + R11*w*d_r;
		Rw.data[5] = -2*x/r + R12*w*d_r;
		Rw.data[6] = -2*y/r + R20*w*d_r;
		Rw.data[7] =  2*x/r + R21*w*d_r;
		Rw.data[8] =  2*w/r + R22*w*d_r;

		//---------- Partial x
		DMatrixRMaj Rx = jacR[1];
		Rx.data[0] =  2*x/r + R00*x*d_r;
		Rx.data[1] =  2*y/r + R01*x*d_r;
		Rx.data[2] =  2*z/r + R02*x*d_r;
		Rx.data[3] =  2*y/r + R10*x*d_r;
		Rx.data[4] = -2*x/r + R11*x*d_r;
		Rx.data[5] = -2*w/r + R12*x*d_r;
		Rx.data[6] =  2*z/r + R20*x*d_r;
		Rx.data[7] =  2*w/r + R21*x*d_r;
		Rx.data[8] = -2*x/r + R22*x*d_r;

		//---------- Partial y
		DMatrixRMaj Ry = jacR[2];
		Ry.data[0] = -2*y/r + R00*y*d_r;
		Ry.data[1] =  2*x/r + R01*y*d_r;
		Ry.data[2] =  2*w/r + R02*y*d_r;
		Ry.data[3] =  2*x/r + R10*y*d_r;
		Ry.data[4] =  2*y/r + R11*y*d_r;
		Ry.data[5] =  2*z/r + R12*y*d_r;
		Ry.data[6] = -2*w/r + R20*y*d_r;
		Ry.data[7] =  2*z/r + R21*y*d_r;
		Ry.data[8] = -2*y/r + R22*y*d_r;

		//---------- Partial z
		DMatrixRMaj Rz = jacR[3];
		Rz.data[0] = -2*z/r + R00*z*d_r;
		Rz.data[1] = -2*w/r + R01*z*d_r;
		Rz.data[2] =  2*x/r + R02*z*d_r;
		Rz.data[3] =  2*w/r + R10*z*d_r;
		Rz.data[4] = -2*z/r + R11*z*d_r;
		Rz.data[5] =  2*y/r + R12*z*d_r;
		Rz.data[6] =  2*x/r + R20*z*d_r;
		Rz.data[7] =  2*y/r + R21*z*d_r;
		Rz.data[8] =  2*z/r + R22*z*d_r;
		// @formatter:on
	}

	@Override
	public int getParameterLength() {
		return 4;
	}

	@Override
	public DMatrixRMaj getRotationMatrix() {
		return R;
	}

	@Override
	public DMatrixRMaj getPartial( int param ) {
		return jacR[param];
	}
}
