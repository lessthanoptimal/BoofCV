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
import lombok.Getter;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.List;
import java.util.Map;

/// Applies a rotation or its transpose to a 3D or homogenous point
public class CROperatorSO3 implements CROperatorBackwardsJac<double[]> {
	Point4D_F64 point = new Point4D_F64();

	/// If true then input and outputs are in homogeneous coordinates
	@Getter protected boolean homogeneous;

	/// If true then rotation matrix is transposed
	@Getter protected boolean transpose;

	// Storage for rotation parameters and matrix
	@Getter private final double[] params;
	private final DMatrixRMaj R = new DMatrixRMaj(3, 3);
	protected final DogArray<DMatrixRMaj> jacs = new DogArray<>(() -> new DMatrixRMaj(3, 3));
	private final MathCRMatrixD math = new MathCRMatrixD();
	private final CRMatrix<double[]> workJac = CRMatrix.createD(5, 5);

	// rotation parameterization of numerics
	protected RotationNumerics rotate;

	public CROperatorSO3( boolean homogeneous, boolean transpose, RotationNumerics rotate ) {
		this.homogeneous = homogeneous;
		this.transpose = transpose;
		this.rotate = rotate;
		this.params = new double[rotate.length()];
	}

	@Override public Map<String, String> getSettings() {
		return Map.of("homogeneous", Boolean.toString(homogeneous), "transpose", Boolean.toString(transpose));
	}

	@Override public void setSettings( Map<String, String> settings ) {
		settings.forEach(( key, value ) -> {
			switch (key) {
				case "homogeneous" -> homogeneous = Boolean.parseBoolean(value);
				case "transpose" -> transpose = Boolean.parseBoolean(value);
				default -> throw new IllegalArgumentException("Unknown key: " + key);
			}
		});
	}

	@Override public void forward( CRArray<double[]> output ) {
		// location of the point in other coordinates
		double x = point.x;
		double y = point.y;
		double z = point.z;
		double w = homogeneous ? point.w : 1.0;

		rotate.set(params, 0);
		rotate.toMatrix(R);

		// Apply rotation
		if (transpose) {
			output.d(0, R.data[0]*x + R.data[3]*y + R.data[6]*z);
			output.d(1, R.data[1]*x + R.data[4]*y + R.data[7]*z);
			output.d(2, R.data[2]*x + R.data[5]*y + R.data[8]*z);
		} else {
			output.d(0, R.data[0]*x + R.data[1]*y + R.data[2]*z);
			output.d(1, R.data[3]*x + R.data[4]*y + R.data[5]*z);
			output.d(2, R.data[6]*x + R.data[7]*y + R.data[8]*z);
		}

		if (homogeneous) {
			output.d(3, w);
		}
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		rotate.set(params, 0);

		if (!jacobianInputs.get(0).isInvalid()) {
			workJac.reshape(getOutputSize(), getInputSize(0));
			math.zero(workJac);
			rotate.toMatrix(R);

			if (transpose) {
				for (int row = 0; row < 3; row++) {
					for (int col = 0; col < 3; col++) {
						workJac.d(col, row, R.unsafe_get(row, col));
					}
				}
			} else {
				for (int row = 0; row < 3; row++) {
					for (int col = 0; col < 3; col++) {
						workJac.d(row, col, R.unsafe_get(row, col));
					}
				}
			}

			if (homogeneous)
				workJac.d(3, 3, 1);

//            System.out.println(workJac.toStringD("%.3f"));
			math.mult(dout, workJac, jacobianInputs.get(0));
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			workJac.reshape(getOutputSize(), getInputSize(1));
			math.zero(workJac);
			rotate.matrixJacobians(jacs);
			if (transpose) {
				for (int idxJac = 0; idxJac < jacs.size; idxJac++) {
					DMatrixRMaj jac = jacs.get(idxJac);
					for (int col = 0; col < 3; col++) {
						double sum = 0;
						for (int row = 0; row < 3; row++) {
							sum += jac.unsafe_get(row, col)*point.get(row);
						}
						workJac.d(col, idxJac, sum);
					}
				}
			} else {
				for (int idxJac = 0; idxJac < jacs.size; idxJac++) {
					DMatrixRMaj jac = jacs.get(idxJac);
					for (int row = 0; row < 3; row++) {
						double sum = 0;
						for (int col = 0; col < 3; col++) {
							sum += jac.unsafe_get(row, col)*point.get(col);
						}
						workJac.d(row, idxJac, sum);
					}
				}
			}

//            System.out.println(workJac.toStringD("%.3f"));
			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	@Override public void normalizeAfterStep( int which, CRArray<double[]> params ) {
		if (which != 1)
			return;
		rotate.normalizeParams(params.a, params.offset);
	}

	@Override public void setInput( int which, CRArray<double[]> src ) {
		switch (which) {
			case 0 -> point.setTo(src.d(0), src.d(1), src.d(2), homogeneous ? src.d(3) : 1.0);
			case 1 -> src.write(params, 0, params.length);
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		}
	}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case 0 -> homogeneous ? 4 : 3;
			case 1 -> params.length;
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		};
	}

	@Override public int getInputCount() {return 2;}

	@Override public int getOutputSize() {return homogeneous ? 4 : 3;}

	@Override public Class<double[]> getArrayType() {return double[].class;}
}
