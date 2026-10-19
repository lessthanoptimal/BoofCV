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

import georegression.struct.se.Se3_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ejml.data.DMatrixRMaj;

import java.util.List;
import java.util.Map;

/// Used to project 2D points onto 3D, typically used with 2D fiducials / markers. It takes in inputs of
/// 2D points that implicitly lie on the z=0 plane. It then applies a SE3 transform to convert it into
/// a 3D points.
public class CROperatorPlane extends CROperatorAbstractSE3 implements CROperatorBackwardsJac<double[]> {

	// location of 2D point, the input
	private double x, y;

	public CROperatorPlane( boolean homogeneous, RotationNumerics rotate ) {
		super(homogeneous, rotate);
	}

	@Override public Map<String, String> getSettings() {
		return Map.of("homogeneous", "" + homogeneous);
	}

	@Override public void setInput( int which, CRArray<double[]> input ) {
		switch (which) {
			case 0 -> {
				// 2D location of point on the plane. z=0 is implicit
				x = input.d(0);
				y = input.d(1);
			}

			case 1 -> saveInputSE3(input);
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		}
	}

	@Override public void setSettings( Map<String, String> settings ) {
		settings.forEach(( key, value ) -> {
			switch (key) {
				case "homogeneous" -> homogeneous = Boolean.parseBoolean(value);
				default -> throw new IllegalArgumentException("Unknown key: " + key);
			}
		});
	}

	@Override public void forward( CRArray<double[]> output ) {
		copyStateToSE3();

		// Apply rigid body transformation
		final DMatrixRMaj R = srcToDst.R;
		output.d(0, R.data[0]*x + R.data[1]*y + srcToDst.T.x);
		output.d(1, R.data[3]*x + R.data[4]*y + srcToDst.T.y);
		output.d(2, R.data[6]*x + R.data[7]*y + srcToDst.T.z);

		if (homogeneous) {
			output.d(3, 1);
		}
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		copyStateToSE3();

		if (!jacobianInputs.get(0).isInvalid()) {
			workJac.rows = getOutputSize();
			workJac.cols = 2;
			math.zero(workJac);

			final DMatrixRMaj R = srcToDst.R;
			workJac.d(0, 0, R.data[0]);
			workJac.d(0, 1, R.data[1]);
			workJac.d(1, 0, R.data[3]);
			workJac.d(1, 1, R.data[4]);
			workJac.d(2, 0, R.data[6]);
			workJac.d(2, 1, R.data[7]);
			math.mult(dout, workJac, jacobianInputs.get(0));
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			rotate.matrixJacobians(rotationJacs);
			workJac.rows = getOutputSize();
			workJac.cols = getSe3Size();
			math.zero(workJac);

			// Jacobian of translation is eye(3)
			workJac.d(0, 0, 1);
			workJac.d(1, 1, 1);
			workJac.d(2, 2, 1);

			// Fx = Tx + R(0,:)*[x,y,0]'  How a single output (x-axis in this case) is computed
			for (int paramIdx = 0; paramIdx < rotationJacs.size; paramIdx++) {
				DMatrixRMaj jac = rotationJacs.get(paramIdx);
				addToJac(paramIdx, jac, x, y, 0);
			}

			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	/// Returns the transform from the fiducial to world reference frame
	public Se3_F64 getPlaneToWorld() {return srcToDst;}

	@Override public int getInputCount() {return 2;}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case 0 -> 2;
			case 1 -> getSe3Size();
			default -> throw new IllegalStateException("Unexpected value: " + which);
		};
	}

	@Override public int getOutputSize() {return homogeneous ? 4 : 3;}
}
