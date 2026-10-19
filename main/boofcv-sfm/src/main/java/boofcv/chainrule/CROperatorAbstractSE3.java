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
import georegression.struct.so.Rodrigues_F64;
import lombok.Getter;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorForward;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

/// Abstract class for blocks which takes in a SE3 as input. Encodes rotation using [Rodrigues_F64].
public abstract class CROperatorAbstractSE3 implements CROperatorForward<double[]> {
	/// Decoded rigid body motion from src to dst reference frames
	@Getter protected final Se3_F64 srcToDst = new Se3_F64();

	/// If true then input and outputs are in homogeneous coordinates
	@Getter protected boolean homogeneous;

	// rotation parameterization of numerics
	protected RotationNumerics rotate;

	/// Storage for SE3 state
	protected SE3State state;

	protected final CRMatrix<double[]> workJac = CRMatrix.createD(5, 5);

	protected final MathCRMatrixD math = new MathCRMatrixD();
	protected final DogArray<DMatrixRMaj> rotationJacs = new DogArray<>(() -> new DMatrixRMaj(3, 3));

	public CROperatorAbstractSE3( boolean homogeneous, RotationNumerics rotate ) {
		this.homogeneous = homogeneous;
		this.rotate = rotate;
		this.state = new SE3State(rotate.length());
	}

	/// Encodes the SE3 into the parameter vector
	public void encode( Se3_F64 otherToView, double[] out, int offset ) {
		this.srcToDst.setTo(otherToView);

		out[offset++] = otherToView.T.x;
		out[offset++] = otherToView.T.y;
		out[offset++] = otherToView.T.z;

		rotate.matrixToParam(otherToView.R, out, offset);
	}

	protected void saveInputSE3( CRArray<double[]> input ) {
		state.decode(input.a, input.offset);
	}

	protected void copyStateToSE3() {
		srcToDst.T.setTo(state.loc);
		rotate.set(state.ori, 0);
		rotate.toMatrix(srcToDst.R);
	}

	protected void addToJac( int col, DMatrixRMaj dR, double X, double Y, double Z ) {
		// these are the outputs, so they will be put into their respective row
		double x = dR.data[0]*X + dR.data[1]*Y + dR.data[2]*Z;
		double y = dR.data[3]*X + dR.data[4]*Y + dR.data[5]*Z;
		double z = dR.data[6]*X + dR.data[7]*Y + dR.data[8]*Z;

		// row major matrix indexing Jacobian[row, 3 + col]
		final int cols = workJac.cols;
		int index = workJac.offset + 3 + col;

		// Save the results int
		workJac.data[index] = x;
		workJac.data[index + cols] = y;
		workJac.data[index + cols*2] = z;
	}

	public int getSe3Size() {return state.length();}

	@Override public Class<double[]> getArrayType() {
		return double[].class;
	}
}
