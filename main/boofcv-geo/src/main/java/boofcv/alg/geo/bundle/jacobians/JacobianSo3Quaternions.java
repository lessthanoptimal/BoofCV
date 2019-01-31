/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
 * TODO Analytic Jacobian and not numerical
 *
 * @author Peter Abeles
 */
public class JacobianSo3Quaternions extends JacobianSo3Numerical {

	private Quaternion_F64 quat = new Quaternion_F64();

	@Override
	public void getParameters(DMatrixRMaj R, double[] parameters, int offset) {
		ConvertRotation3D_F64.matrixToQuaternion(R,quat);
		parameters[offset  ] = quat.w;
		parameters[offset+1] = quat.x;
		parameters[offset+2] = quat.y;
		parameters[offset+3] = quat.z;
	}

	@Override
	public void computeRotationMatrix(double[] parameters, int offset, DMatrixRMaj R) {
		quat.w = parameters[offset  ];
		quat.x = parameters[offset+1];
		quat.y = parameters[offset+2];
		quat.z = parameters[offset+3];

		// has to be the unit quaternion and there is nothing restricting the values of each parameter
		quat.normalize();
		ConvertRotation3D_F64.quaternionToMatrix(quat,R);
	}

	@Override
	public int getParameterLength() {
		return 4;
	}
}
