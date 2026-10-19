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

/// [QuaternionNumericsHam] where the parameters specify the rotation by their direction alone. Every
/// quaternion read in is normalized first, so scaling the parameters leaves the rotation unchanged and
/// a non-unit input is valid rather than a contract violation.
///
/// This exists for iterative solvers. With the plain parameterization the rotation matrix is scaled by
/// the norm squared, which hands each rotation a spurious 4th degree of freedom, and the derivative
/// reports a real sensitivity along the radial direction even at unit length. Normalizing removes both:
/// the radial direction becomes a null direction of the cost, so re-normalizing the parameters after a
/// step is free instead of fighting the model.
///
/// Values are computed by the parent from the normalized quaternion. Jacobians additionally carry the
/// derivative of the normalization itself, `(I - qq')/n` applied to each block of input parameters.
public class QuaternionNumericsHamNorm extends QuaternionNumericsHam {
	/// Parameters exactly as they were passed into [#set]
	final double[] raw = new double[4];
	/// [#raw] scaled to unit length, which is what the parent operates on
	final double[] unitA = new double[4];
	/// Length of [#raw]
	double normA;

	/// The second rotation's parameters, normalized, and their original length
	final double[] unitB = new double[4];
	double normB;

	@Override public void set( double[] array, int offset ) {
		System.arraycopy(array, offset, raw, 0, 4);
		normA = normalize(raw, 0, unitA);
		super.set(unitA, 0);
	}

	/// Returns the parameters as they were passed into [#set], not their normalized form
	@Override public void get( double[] array, int offset ) {
		System.arraycopy(raw, 0, array, offset, 4);
	}

	/// Any non-zero vector specifies a rotation
	@Override public boolean isValid( double[] array, int offset ) {
		double norm = 0;
		for (int i = 0; i < 4; i++) {
			double v = array[offset + i];
			norm += v*v;
		}
		return norm > 0 && !Double.isInfinite(norm) && !Double.isNaN(norm);
	}

	/// Scales the parameters to unit length. Unlike the parent this leaves the state of 'this' alone,
	/// which would otherwise go stale relative to the cached parameters
	@Override public void normalizeParams( double[] params, int offset ) {
		normalize(params, offset, params, offset);
	}

	@Override public void concatAB( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.concatAB(unitB, 0, result, offsetResult);
	}

	@Override public void concatABJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.concatABJacobian(unitB, 0, outputA, outputB);
		chainRule(outputA, unitA, normA);
		chainRule(outputB, unitB, normB);
	}

	@Override public void concatBA( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.concatBA(unitB, 0, result, offsetResult);
	}

	@Override public void concatBAJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.concatBAJacobian(unitB, 0, outputA, outputB);
		chainRule(outputA, unitA, normA);
		chainRule(outputB, unitB, normB);
	}

	@Override public double angleError( double[] paramsB ) {
		normB = normalize(paramsB, 0, unitB);
		return super.angleError(unitB);
	}

	@Override public void angleErrorJacobian( double[] paramsB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		normB = normalize(paramsB, 0, unitB);
		super.angleErrorJacobian(unitB, outputA, outputB);
		chainRule(outputA, unitA, normA);
		chainRule(outputB, unitB, normB);
	}

	@Override public void residual( double[] paramsB, int offsetB, double[] residuals, int offset ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.residual(unitB, 0, residuals, offset);
	}

	@Override public void residualJacobian( double[] paramsB, int offsetB, DMatrixRMaj jacA, DMatrixRMaj jacB ) {
		normB = normalize(paramsB, offsetB, unitB);
		super.residualJacobian(unitB, 0, jacA, jacB);
		chainRule(jacA, unitA, normA);
		chainRule(jacB, unitB, normB);
	}

	/// The parent's partials are with respect to the normalized quaternion. Same chain rule as
	/// [#chainRule], applied one element of the rotation matrix at a time since the 4 partials are
	/// stored as separate matrices
	@Override public void matrixJacobians( DogArray<DMatrixRMaj> output ) {
		super.matrixJacobians(output);

		for (int element = 0; element < 9; element++) {
			double dot = 0;
			for (int i = 0; i < 4; i++)
				dot += unitA[i]*output.get(i).data[element];

			for (int i = 0; i < 4; i++) {
				double value = output.get(i).data[element];
				output.get(i).data[element] = (value - unitA[i]*dot)/normA;
			}
		}
	}

	/// Converts a Jacobian with respect to the normalized quaternion into one with respect to the
	/// parameters it came from: `J <- J*(I - qq')/n`, where `q` is normalized. Rows are outputs and
	/// the 4 columns are the parameters
	static void chainRule( DMatrixRMaj jacobian, double[] unit, double norm ) {
		if (jacobian.numCols != 4)
			throw new IllegalArgumentException("Expected 4 columns, one per parameter");

		for (int row = 0; row < jacobian.numRows; row++) {
			int index = row*4;

			double dot = 0;
			for (int col = 0; col < 4; col++)
				dot += jacobian.data[index + col]*unit[col];

			for (int col = 0; col < 4; col++)
				jacobian.data[index + col] = (jacobian.data[index + col] - unit[col]*dot)/norm;
		}
	}

	/// Scales the quaternion to unit length
	///
	/// @return length before scaling
	static double normalize( double[] src, int srcOffset, double[] dst ) {
		return normalize(src, srcOffset, dst, 0);
	}

	static double normalize( double[] src, int srcOffset, double[] dst, int dstOffset ) {
		double norm = 0;
		for (int i = 0; i < 4; i++) {
			double v = src[srcOffset + i];
			norm += v*v;
		}
		norm = Math.sqrt(norm);
		if (norm == 0.0)
			throw new IllegalArgumentException("Uninitialized quaternion, all zeros");

		for (int i = 0; i < 4; i++)
			dst[dstOffset + i] = src[srcOffset + i]/norm;

		return norm;
	}
}
