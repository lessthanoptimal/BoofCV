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

import boofcv.alg.geo.RodriguesRotationJacobian_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/// Implementation of [RotationNumerics] for a 3-parameter implementation of [Rodrigues_F64].
/// 3-DOF Rodrigues parameters. Axis of rotation where radians is encoded in vector f-norm
public class Rodrigues3Numerics implements RotationNumerics {
	// 3-DOF Rodrigues parameters. Axis of rotation where radians is encoded in vector f-norm
	double rodX, rodY, rodZ;

	private RodriguesRotationJacobian_F64 jac = new RodriguesRotationJacobian_F64();

	@Override public void set( double[] array, int offset ) {
		this.rodX = array[offset];
		this.rodY = array[offset + 1];
		this.rodZ = array[offset + 2];
	}

	@Override public void get( double[] array, int offset ) {
		array[offset] = rodX;
		array[offset + 1] = rodY;
		array[offset + 2] = rodZ;
	}

	@Override public void identity( double[] array, int offset ) {
		array[offset] = 0;
		array[offset + 1] = 0;
		array[offset + 2] = 0;
	}

	@Override public boolean isValid( double[] array, int offset ) {return true;}

	@Override public void rodriguesToParam( double rodX, double rodY, double rodZ, double[] params ) {
		params[0] = rodX;
		params[1] = rodY;
		params[2] = rodZ;
	}

	@Override public void rodriguesToJacobian( double rodX, double rodY, double rodZ, DMatrixRMaj jacobian ) {
		jacobian.reshape(3, 3);
		CommonOps_DDRM.setIdentity(jacobian);
	}

	@Override public void concatAB( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void concatABJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void concatBA( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void concatBAJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void toMatrix( DMatrixRMaj r ) {
		ConvertRotation3D_F64.rodriguesToMatrix(rodX, rodY, rodZ, r);
	}

	@Override public void matrixJacobians( DogArray<DMatrixRMaj> output ) {
		jac.process(rodX, rodY, rodZ);
		output.reset();
		output.grow().setTo(jac.Rx);
		output.grow().setTo(jac.Ry);
		output.grow().setTo(jac.Rz);
	}

	@Override public void matrixToParam( DMatrixRMaj r, double[] array, int offset ) {
		var rodrigues = new Rodrigues_F64();
		ConvertRotation3D_F64.matrixToRodrigues(r, rodrigues);
		array[offset] = rodrigues.unitAxisRotation.x*rodrigues.theta;
		array[offset + 1] = rodrigues.unitAxisRotation.y*rodrigues.theta;
		array[offset + 2] = rodrigues.unitAxisRotation.z*rodrigues.theta;
	}

	/// A rodrigues vector has no constraint to restore, so the parameters are left as they are
	@Override public void normalizeParams( double[] params, int offset ) {}

	@Override public double angleError( double[] paramsB ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void angleErrorJacobian( double[] paramsB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void residual( double[] paramsB, int offsetB, double[] residuals, int offset ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public int residualLength() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void residualJacobian( double[] paramsB, int offsetB, DMatrixRMaj jacA, DMatrixRMaj jacB ) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override public void invert( double[] input, int offsetInput, double[] output, int offsetOutput ) {
		output[offsetOutput++] = -input[offsetInput++];
		output[offsetOutput++] = -input[offsetInput++];
		output[offsetOutput] = -input[offsetInput];
	}

	@Override public void invertJacobian( double[] input, int offset, DMatrixRMaj jac ) {
		jac.reshape(3, 3);
		jac.zero();
		for (int i = 0; i < 3; i++) {
			jac.unsafe_set(i, i, -1);
		}
	}

	@Override public int length() {
		return 3;
	}
}
