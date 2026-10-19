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
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsGrad;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/// Applies rigid body motion to a point in 3D or homogeneous coordinates.
public class CROperatorSE3 extends CROperatorAbstractSE3
		implements CROperatorBackwardsGrad<double[]>, CROperatorBackwardsJac<double[]> {
	Point4D_F64 point = new Point4D_F64();

	// Convert R into CRMatrix
	private final CRMatrix<double[]> wrapR = new CRMatrix<>(srcToDst.R.data, 0, 3, 3);

	// storage for 4x4 matrix
	private final DMatrixRMaj m44 = new DMatrixRMaj(4, 4);

	private final DMatrixRMaj wrapJac = new DMatrixRMaj();

	// wraps the dout matrix
	private final DMatrixRMaj wrapDOut = new DMatrixRMaj();

	public CROperatorSE3( boolean homogeneous, RotationNumerics rotate ) {
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

		// Apply rigid body transformation
		final DMatrixRMaj R = srcToDst.R;
		output.d(0, R.data[0]*x + R.data[1]*y + R.data[2]*z + srcToDst.T.x*w);
		output.d(1, R.data[3]*x + R.data[4]*y + R.data[5]*z + srcToDst.T.y*w);
		output.d(2, R.data[6]*x + R.data[7]*y + R.data[8]*z + srcToDst.T.z*w);

		if (homogeneous) {
			output.d(3, w);
		}
	}

	@Override public void gradient( CRArray<double[]> dout, List<CRArray<double[]>> gradientInputs ) {
		copyStateToSE3();

		double dout0 = dout.d(0);
		double dout1 = dout.d(1);
		double dout2 = dout.d(2);

		// Point in other coordinate system
		double x = point.x;
		double y = point.y;
		double z = point.z;
		double w = homogeneous ? point.w : 1.0;

		// inplace dot product of partial relative to inputs and dout
		if (!gradientInputs.get(0).isInvalid()) {
			CRArray<double[]> gradient = gradientInputs.get(0);
			final DMatrixRMaj R = srcToDst.R;
			gradient.d(0, R.data[0]*dout0 + R.data[3]*dout1 + R.data[6]*dout2);
			gradient.d(1, R.data[1]*dout0 + R.data[4]*dout1 + R.data[7]*dout2);
			gradient.d(2, R.data[2]*dout0 + R.data[5]*dout1 + R.data[8]*dout2);

			if (homogeneous) {
				double dout3 = dout.d(3);
				gradient.d(3, srcToDst.T.x*dout0 + srcToDst.T.y*dout1 + srcToDst.T.z*dout2 + dout3);
			}
		}

		if (!gradientInputs.get(1).isInvalid()) {
			rotate.matrixJacobians(rotationJacs);
			CRArray<double[]> gradient = gradientInputs.get(1);
			double[] dp = gradient.a;
			int dpIdx = gradient.offset;

			// Jacobian of translation is eye(3)
			// Jacobians of rotation parameters is computed external, but is composed of n 3x3 matrices

			dp[dpIdx++] = w*dout0;
			dp[dpIdx++] = w*dout1;
			dp[dpIdx++] = w*dout2;
			for (int paramIdx = 0; paramIdx < rotationJacs.size; paramIdx++) {
				DMatrixRMaj jac = rotationJacs.get(paramIdx);
				dp[dpIdx++] = inplaceMult(jac, x, y, z, dout0, dout1, dout2);
			}
		}
	}

	/// Takes the previously computed Jacobian for rotation matrix (3x3) multiplies by the point, then does a vector
	/// dot product against dout
	private double inplaceMult( DMatrixRMaj jR, double x, double y, double z,
								double d0, double d1, double d2 ) {
		double dxd0 = jR.data[0]*x + jR.data[1]*y + jR.data[2]*z;
		double dxd1 = jR.data[3]*x + jR.data[4]*y + jR.data[5]*z;
		double dxd2 = jR.data[6]*x + jR.data[7]*y + jR.data[8]*z;

		return dxd0*d0 + dxd1*d1 + dxd2*d2;
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		copyStateToSE3();

		// Point in other coordinate system
		double x = point.x;
		double y = point.y;
		double z = point.z;
		double w = homogeneous ? point.w : 1.0;

		if (!jacobianInputs.get(0).isInvalid()) {
			CRMatrix<double[]> jacobian = jacobianInputs.get(0);

			// Jacobian relative to inputs is the rotation matrix
			if (homogeneous) {
				computeInputPartial(dout, jacobian);
			} else {
				wrapR.data = srcToDst.R.data;
				math.mult(dout, wrapR, jacobian);
			}
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			rotate.matrixJacobians(rotationJacs);
			workJac.reshape(getOutputSize(), getInputSize(1));
			math.zero(workJac);

			// Jacobian of translation is eye(3)*w
			workJac.d(0, 0, w);
			workJac.d(1, 1, w);
			workJac.d(2, 2, w);

			// Fx = Tx + R(0,:)*[x,y,z]'  How a single output (x-axis in this case) is computed
			for (int paramIdx = 0; paramIdx < rotationJacs.size; paramIdx++) {
				DMatrixRMaj jac = rotationJacs.get(paramIdx);
				addToJac(paramIdx, jac, x, y, z);
			}

			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	/// Computes partial relative to inputs. Only a little bit tricky because there is no clean way to do the linear
	/// algebra required and easily concatenate the translational component.
	private void computeInputPartial( CRMatrix<double[]> dout, @NotNull CRMatrix<double[]> jacobianInput ) {
		// Jacobian relative to inputs is the rotation matrix with translation = [R, T]
		CommonOps_DDRM.insert(srcToDst.R, m44, 0, 0);
		m44.set(0, 3, srcToDst.T.x);
		m44.set(1, 3, srcToDst.T.y);
		m44.set(2, 3, srcToDst.T.z);
		m44.set(3, 3, 1);

		// wrap dout so that it is compatible with EJML
		wrapDOut.data = dout.data;
		wrapDOut.numRows = dout.rows;
		wrapDOut.numCols = dout.cols;

		wrapJac.data = jacobianInput.data;
		wrapJac.numRows = jacobianInput.rows;
		wrapJac.numCols = jacobianInput.cols;

		CommonOps_DDRM.mult(wrapDOut, m44, wrapJac);
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
