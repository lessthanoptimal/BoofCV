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

import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ddogleg.chainrule.MathCRMatrixD;

import java.util.List;

/// Projects a camera-frame point with the camera model used by "Bundle Adjustment in the Large", the same as
/// [boofcv.alg.geo.bundle.cameras.BundlePinholeSnavely]. The camera looks down the negative z-axis.
///
///     n = (X/−Z, Y/−Z),   r = 1 + k1·|n|² + k2·|n|⁴,   pixel = f·r·n
///
/// * input[0] = point (X, Y, Z) in the camera frame
/// * input[1] = intrinsics (f, k1, k2)
public class CROperatorCameraSnavely implements CROperatorBackwardsJac<double[]> {
	public static final int IN_POINT = 0;
	public static final int IN_CAMERA = 1;

	// input point
	double X, Y, Z;
	// input intrinsics
	double f, k1, k2;

	final CRMatrix<double[]> workJac = CRMatrix.createD(2, 3);
	final MathCRMatrixD math = new MathCRMatrixD();

	@Override public void setInput( int which, CRArray<double[]> input ) {
		switch (which) {
			case IN_POINT -> {
				X = input.d(0);
				Y = input.d(1);
				Z = input.d(2);
			}
			case IN_CAMERA -> {
				f = input.d(0);
				k1 = input.d(1);
				k2 = input.d(2);
			}
			default -> throw new IllegalArgumentException("Unknown input " + which);
		}
	}

	@Override public void forward( CRArray<double[]> output ) {
		// n = (X/−Z, Y/−Z)
		double nx = -X/Z;
		double ny = -Y/Z;
		// r = 1 + k1·|n|² + k2·|n|⁴
		double n2 = nx*nx + ny*ny;
		double r = 1.0 + (k1 + k2*n2)*n2;
		// pixel = f·r·n
		output.d(0, f*r*nx);
		output.d(1, f*r*ny);
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		double nx = -X/Z;
		double ny = -Y/Z;
		double n2 = nx*nx + ny*ny;
		double r = 1.0 + (k1 + k2*n2)*n2;

		if (!jacobianInputs.get(IN_POINT).isInvalid()) {
			// ∂r/∂n = 2·(k1 + 2·k2·|n|²)·n
			double drdn = 2.0*(k1 + 2.0*k2*n2);
			// ∂pixel/∂n = f·(r·I + n·(∂r/∂n)ᵀ)
			double a00 = f*(r + nx*drdn*nx), a01 = f*nx*drdn*ny;
			double a10 = f*ny*drdn*nx, a11 = f*(r + ny*drdn*ny);
			// ∂n/∂(X,Y,Z) = [[−1/Z, 0, X/Z²], [0, −1/Z, Y/Z²]]
			double iz = 1.0/Z;
			double b00 = -iz, b02 = X*iz*iz;
			double b11 = -iz, b12 = Y*iz*iz;

			// ∂pixel/∂(X,Y,Z) = ∂pixel/∂n · ∂n/∂(X,Y,Z)
			workJac.reshape(2, 3);
			workJac.d(0, 0, a00*b00);
			workJac.d(0, 1, a01*b11);
			workJac.d(0, 2, a00*b02 + a01*b12);
			workJac.d(1, 0, a10*b00);
			workJac.d(1, 1, a11*b11);
			workJac.d(1, 2, a10*b02 + a11*b12);
			math.mult(dout, workJac, jacobianInputs.get(IN_POINT));
		}

		if (!jacobianInputs.get(IN_CAMERA).isInvalid()) {
			// ∂pixel/∂(f, k1, k2) = [r·n, f·|n|²·n, f·|n|⁴·n]
			workJac.reshape(2, 3);
			workJac.d(0, 0, r*nx);
			workJac.d(0, 1, f*n2*nx);
			workJac.d(0, 2, f*n2*n2*nx);
			workJac.d(1, 0, r*ny);
			workJac.d(1, 1, f*n2*ny);
			workJac.d(1, 2, f*n2*n2*ny);
			math.mult(dout, workJac, jacobianInputs.get(IN_CAMERA));
		}
	}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case IN_POINT, IN_CAMERA -> 3;
			default -> throw new IllegalArgumentException("Unknown input " + which);
		};
	}

	@Override public int getInputCount() {return 2;}

	@Override public int getOutputSize() {return 2;}

	@Override public Class<double[]> getArrayType() {return double[].class;}
}
