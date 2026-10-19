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

import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

/// Abstractions for working with parameterized rotation matrices designed for use in state estimation.
///
/// * All input parameters are always normalized, that is [#isValid] would be true.
/// * Parameters can be normalized using [#normalizeParams].
/// * You must remember to re-normalize after an iterative solver has updated the parameter.
/// * By default normalization is not checked and silently degrades results.
/// * Some implementations have a debug mode to turn it on
public interface RotationNumerics {
	/// Sets its parameters to the values read from `array`
	void set( double[] array, int offset );

	/// Retrieves the parameters and stores them in the `array`
	void get( double[] array, int offset );

	/// Sets `array` to a value that represents identity or no rotation
	void identity( double[] array, int offset );

	/// Returns true if `array` encodes valid parameters, e.g. a unit quaternion
	boolean isValid( double[] array, int offset );

	/// Converts the euler angle in xyz format into parameter in this rotation encoding
	@Deprecated
	default void eulerXyzToParam( double rotX, double rotY, double rotZ, double[] params ) {throw new RuntimeException("Not supported");}

	/// Jacobian on the xyz to param function
	@Deprecated
	default void eulerXyzToJacobian( double rotX, double rotY, double rotZ, DMatrixRMaj jacobian ) {throw new RuntimeException("Not supported");}

	/// Converts [georegression.struct.so.Rodrigues_F64] axis of rotation with angle encoded as the magnitude into
	/// the rotation parameters understood by this class.
	void rodriguesToParam( double rodX, double rodY, double rodZ, double[] params );

	/// Jacobian of [#rodriguesToJacobian]
	void rodriguesToJacobian( double rodX, double rodY, double rodZ, DMatrixRMaj jacobian );

	/// Let the passed in parameters specify rotation 'B' and "this" being rotation 'A', then this is the same as
	/// applying rotation 'B' followed by 'A'. If 'A' and 'B' are rotation matrices then this is the formula: y=A\*B\*x
	void concatAB( double[] paramsB, int offsetB, double[] result, int offsetResults );

	/// Partials of [#concatAB]
	void concatABJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB );

	/// Let the passed in parameters specify rotation 'B' and "this" being rotation 'A', then this is the same as
	/// applying rotation 'A' followed by 'B'. If 'A' and 'B' are rotation matrices then this is the formula: y=B\*A\*x
	void concatBA( double[] paramsB, int offsetB, double[] result, int offsetResults );

	/// Partials of [#concatBA]
	void concatBAJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB );

	/// Computes rotation matrix that it represents
	void toMatrix( DMatrixRMaj r );

	/// Converts the rotation matrix into a parameterized array. Does not change the state of 'this'.
	void matrixToParam( DMatrixRMaj r, double[] array, int offset );

	/// Computes Jocobian matrices when you compute the partial of the rotation matrix relative to the parameters
	void matrixJacobians( DogArray<DMatrixRMaj> output );

	/// If applicable, it will normalize the parameters. For example, if quaternion this function will ensure the
	/// norm is 1. The encoded transform's meaning will not change. This function might be called after an
	/// iterative step when the precise structure can't be enforced.
	///
	/// It is not specified if this function changes the class' internal state. You must call [#set] after
	/// invoking to ensure the state is known. However, you must not assume it will change the
	/// internal state to match params. Justification: Reduce memory by allowing internal data structures to
	/// be used as workspace.
	///
	/// @param params Input rotation parameters. Output normalized parameters.
	void normalizeParams( double[] params, int offset );

	/// Computes how many radians the two rotations are
	double angleError( double[] paramsB );

	/// Partials of angleError to inner parameters (A) and passed in parameters (B)
	void angleErrorJacobian( double[] paramsB, DMatrixRMaj outputA, DMatrixRMaj outputB );

	/// Computes the residual error between the two rotations. This should represent, paramB - paramA.
	/// When done as rotation matrices the residual represents: residual = A<sup>T</sup>B.
	///
	/// NOTE: Do we need a residual to rotation matrix? For quaternions, we represent this as a 3-DOF axis-angle.
	/// Would that be true for others?
	///
	/// @param paramsB Second parameterized rotation
	/// @param offsetB offset in array
	/// @param residuals (Output) Storage for residual error
	/// @param offset (Output) where it should start writing the residual error too in 'residuals'
	void residual( double[] paramsB, int offsetB, double[] residuals, int offset );

	/// Number of elements in the residual error
	int residualLength();

	/// Jacobians for the residual error
	void residualJacobian( double[] paramsB, int offsetB, DMatrixRMaj jacA, DMatrixRMaj jacB );

	/// Converts the input parameters into a set that represent a rotation in the opposite direction
	void invert( double[] input, int offsetInput, double[] output, int offsetOutput );

	/// Jacobian for [#invert]
	void invertJacobian( double[] input, int offset, DMatrixRMaj jac );

	/// Number of parameters / DOF
	int length();
}
