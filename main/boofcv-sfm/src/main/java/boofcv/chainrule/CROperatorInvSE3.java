/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ejml.data.DMatrixRMaj;

import java.util.List;
import java.util.Map;

/// Applies inverse of a rigid body motion to a point in 3D or homogeneous coordinates.
public class CROperatorInvSE3 extends CROperatorAbstractSE3 implements CROperatorBackwardsJac<double[]> {
	Point4D_F64 point = new Point4D_F64();

	public CROperatorInvSE3( boolean homogeneous, RotationNumerics rotate ) {
		super(homogeneous, rotate);
	}

	@Override public Map<String, String> getSettings() {
		return Map.of("homogeneous", "" + homogeneous);
	}

	@Override public void setSettings( Map<String, String> settings ) {
		settings.forEach(( key, value ) -> {
			switch (key) {
				case "homogeneous" -> homogeneous = Boolean.parseBoolean(value);
				default -> throw new IllegalArgumentException("Unknown key: " + key);
			}
		});
	}

	@Override public void setInput( int which, CRArray<double[]> src ) {
		switch (which) {
			case 0 -> point.setTo(src.d(0), src.d(1), src.d(2), homogeneous ? src.d(3) : 1.0);
			case 1 -> saveInputSE3(src);
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		}
	}

	@Override public void forward( CRArray<double[]> output ) {
		copyStateToSE3();

		// location of the point in other coordinates
		double x = point.x;
		double y = point.y;
		double z = point.z;
		double w = homogeneous ? point.w : 1.0;

		// Construct inv(P) = [R' | -R'*T]
		final DMatrixRMaj R = srcToDst.R;
		double P11 = R.data[0], P12 = R.data[3], P13 = R.data[6];
		double P21 = R.data[1], P22 = R.data[4], P23 = R.data[7];
		double P31 = R.data[2], P32 = R.data[5], P33 = R.data[8];

		Vector3D_F64 T = srcToDst.T;
		double P14 = -(P11*T.x + P12*T.y + P13*T.z);
		double P24 = -(P21*T.x + P22*T.y + P23*T.z);
		double P34 = -(P31*T.x + P32*T.y + P33*T.z);
		// the bottom row is implicitly [0 0 0 1]

		output.d(0, P11*x + P12*y + P13*z + P14*w);
		output.d(1, P21*x + P22*y + P23*z + P24*w);
		output.d(2, P31*x + P32*y + P33*z + P34*w);

		if (homogeneous) {
			output.d(3, w);
		}
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		copyStateToSE3();

		// Point in other coordinate system
		double x = point.x;
		double y = point.y;
		double z = point.z;
		double w = homogeneous ? point.w : 1.0;

		if (!jacobianInputs.get(0).isInvalid()) {
			workJac.reshape(getOutputSize(), getInputSize(0));
			math.zero(workJac);

			insertTransposed(srcToDst.R, workJac);

			if (homogeneous) {
				final DMatrixRMaj R = srcToDst.R;
				final Vector3D_F64 T = srcToDst.T;
				double P14 = -(R.data[0]*T.x + R.data[3]*T.y + R.data[6]*T.z);
				double P24 = -(R.data[1]*T.x + R.data[4]*T.y + R.data[7]*T.z);
				double P34 = -(R.data[2]*T.x + R.data[5]*T.y + R.data[8]*T.z);

				workJac.d(0, 3, P14);
				workJac.d(1, 3, P24);
				workJac.d(2, 3, P34);
				workJac.d(3, 3, 1);
			}

			math.mult(dout, workJac, jacobianInputs.get(0));
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			rotate.matrixJacobians(rotationJacs);
			workJac.reshape(getOutputSize(), getInputSize(1));
			math.zero(workJac);

			// Jacobian of translation is -R*eye(3)*w
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					workJac.d(row, col, -srcToDst.R.unsafe_get(col, row)*w);
				}
			}

			// R'*x + -R'*T*w
			for (int paramIdx = 0; paramIdx < rotationJacs.size; paramIdx++) {
				final Vector3D_F64 T = srcToDst.T;
				final DMatrixRMaj R = rotationJacs.get(paramIdx);
				for (int row = 0; row < 3; row++) {
					double sumLeft = 0;
					sumLeft += R.unsafe_get(0, row)*x;
					sumLeft += R.unsafe_get(1, row)*y;
					sumLeft += R.unsafe_get(2, row)*z;

					double sumRight = 0;
					sumRight += R.unsafe_get(0, row)*T.x*w;
					sumRight += R.unsafe_get(1, row)*T.y*w;
					sumRight += R.unsafe_get(2, row)*T.z*w;

					workJac.d(row, 3 + paramIdx, sumLeft - sumRight);
				}
			}

			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	private void insertTransposed( DMatrixRMaj src, CRMatrix<double[]> dst ) {
		workJac.d(0, 0, src.data[0]);
		workJac.d(0, 1, src.data[3]);
		workJac.d(0, 2, src.data[6]);
		workJac.d(1, 0, src.data[1]);
		workJac.d(1, 1, src.data[4]);
		workJac.d(1, 2, src.data[7]);
		workJac.d(2, 0, src.data[2]);
		workJac.d(2, 1, src.data[5]);
		workJac.d(2, 2, src.data[8]);
	}

	@Override public void normalizeAfterStep( int which, CRArray<double[]> params ) {
		if (which != 1)
			return;
		rotate.normalizeParams(params.a, params.offset + 3);
	}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case 0 -> homogeneous ? 4 : 3;
			case 1 -> getSe3Size();
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		};
	}

	@Override public int getInputCount() {return 2;}

	@Override public int getOutputSize() {return homogeneous ? 4 : 3;}
}
