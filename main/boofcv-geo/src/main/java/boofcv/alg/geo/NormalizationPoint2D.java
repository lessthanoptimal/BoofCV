/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import georegression.geometry.UtilCurves_F64;
import georegression.struct.curve.ConicGeneral_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * Describes how to normalize a set of points such that they have zero mean and variance. This is equivalent
 * to applying the matrix below. Normalization is often needed as a preprocessing step for solving linear equations.
 * Greatly reduces bias and numerical errors.
 *
 * <pre>
 * N = [ 1/&sigma;_x     0      -&mu;_x/&sigma;_x ]
 *     [    0   1/&sigma;_y 0   -&mu;_y/&sigma;_y ]
 *     [    0      0          1    ]
 * </pre>
 *
 * <p>
 * Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class NormalizationPoint2D {
	// default value is do nothing
	public double meanX = 0, stdX = 1;
	public double meanY = 0, stdY = 1;

	// internal workspace
	private DMatrix3x3 work = new DMatrix3x3();

	public NormalizationPoint2D() {}

	public NormalizationPoint2D( double meanX, double stdX, double meanY, double stdY ) {
		this.meanX = meanX;
		this.stdX = stdX;
		this.meanY = meanY;
		this.stdY = stdY;
	}

	public void set( double meanX, double stdX, double meanY, double stdY ) {
		this.meanX = meanX;
		this.stdX = stdX;
		this.meanY = meanY;
		this.stdY = stdY;
	}

	/**
	 * Applies normalization to a H=3xN matrix
	 *
	 * out = Norm*H
	 *
	 * @param H 3xN matrix. Can be same as input matrix
	 */
	public void apply( DMatrixRMaj H, DMatrixRMaj output ) {
		output.reshape(3, H.numCols);
		int stride = H.numCols;
		for (int col = 0; col < H.numCols; col++) {
			// This column in H
			double h1 = H.data[col], h2 = H.data[col + stride], h3 = H.data[col + 2*stride];

			output.data[col] = h1/stdX - meanX*h3/stdX;
			output.data[col + stride] = h2/stdY - meanY*h3/stdY;
			output.data[col + 2*stride] = h3;
		}
	}

	/**
	 * Applies normalization to a H=3xN matrix
	 *
	 * out = Norm*H
	 *
	 * @param H 3xN matrix. Can be same as input matrix
	 */
	public void remove( DMatrixRMaj H, DMatrixRMaj output ) {
		output.reshape(3, H.numCols);
		int stride = H.numCols;
		for (int col = 0; col < H.numCols; col++) {
			// This column in H
			double h1 = H.data[col], h2 = H.data[col + stride], h3 = H.data[col + 2*stride];

			output.data[col] = h1*stdX + h3*meanX;
			output.data[col + stride] = h2*stdY + h3*meanY;
			output.data[col + 2*stride] = h3;
		}
	}

	public void apply( Point2D_F64 p, Point2D_F64 output ) {
		output.x = (p.x - meanX)/stdX;
		output.y = (p.y - meanY)/stdY;
	}

	public void apply( Point3D_F64 p, Point3D_F64 output ) {
		output.x = (p.x - p.z*meanX)/stdX;
		output.y = (p.y - p.z*meanY)/stdY;
	}

	/**
	 * C* = H'*C*H
	 */
	public void apply( ConicGeneral_F64 p, ConicGeneral_F64 output ) {
		DMatrixRMaj C = UtilCurves_F64.convert(p, (DMatrixRMaj)null);
		DMatrixRMaj Hinv = matrixInv(null);
		DMatrixRMaj CP = new DMatrixRMaj(3, 3);
		PerspectiveOps.multTranA(Hinv, C, Hinv, CP);
		UtilCurves_F64.convert(CP, output);
	}

	/**
	 * Apply transform to conic in 3x3 matrix format.
	 */
	public void apply( DMatrix3x3 C, DMatrix3x3 output ) {
		DMatrix3x3 Hinv = matrixInv3(work);
		PerspectiveOps.multTranA(Hinv, C, Hinv, output);
	}

	public void remove( Point2D_F64 p, Point2D_F64 output ) {
		output.x = p.x*stdX + meanX;
		output.y = p.y*stdY + meanY;
	}

	public void remove( Point3D_F64 p, Point3D_F64 output ) {
		output.x = p.x*stdX + p.z*meanX;
		output.y = p.y*stdY + p.z*meanY;
	}

	public void remove( ConicGeneral_F64 p, ConicGeneral_F64 output ) {
		DMatrixRMaj C = UtilCurves_F64.convert(p, (DMatrixRMaj)null);
		DMatrixRMaj H = matrix(null);
		DMatrixRMaj CP = new DMatrixRMaj(3, 3);
		PerspectiveOps.multTranA(H, C, H, CP);
		UtilCurves_F64.convert(CP, output);
	}

	public void remove( DMatrix3x3 C, DMatrix3x3 output ) {
		DMatrix3x3 H = matrix3(work);
		PerspectiveOps.multTranA(H, C, H, output);
	}

	public DMatrixRMaj matrix( @Nullable DMatrixRMaj M ) {
		if (M == null)
			M = new DMatrixRMaj(3, 3);
		else
			M.reshape(3, 3);
		M.set(0, 0, 1.0/stdX);
		M.set(1, 1, 1.0/stdY);
		M.set(0, 2, -meanX/stdX);
		M.set(1, 2, -meanY/stdY);
		M.set(2, 2, 1);
		return M;
	}

	public DMatrixRMaj matrixInv( @Nullable DMatrixRMaj M ) {
		if (M == null)
			M = new DMatrixRMaj(3, 3);
		else
			M.reshape(3, 3);
		M.set(0, 0, stdX);
		M.set(1, 1, stdY);
		M.set(0, 2, meanX);
		M.set(1, 2, meanY);
		M.set(2, 2, 1);
		return M;
	}

	public DMatrix3x3 matrix3( @Nullable DMatrix3x3 M ) {
		if (M == null)
			M = new DMatrix3x3();
		else {
			M.a21 = 0;
			M.a31 = 0;
			M.a31 = 0;
		}
		M.a11 = 1.0/stdX;
		M.a12 = 1.0/stdY;
		M.a13 = -meanX/stdX;
		M.a23 = -meanY/stdY;
		M.a22 = 1;
		return M;
	}

	public DMatrix3x3 matrixInv3( DMatrix3x3 M ) {
		if (M == null)
			M = new DMatrix3x3();
		else {
			M.a21 = 0;
			M.a31 = 0;
			M.a31 = 0;
		}
		M.a11 = stdX;
		M.a12 = stdY;
		M.a13 = meanX;
		M.a23 = meanY;
		M.a22 = 1;
		return M;
	}

	public boolean isEquals( NormalizationPoint2D a, double tol ) {
		if (Math.abs(a.meanX - meanX) > tol)
			return false;
		if (Math.abs(a.meanY - meanY) > tol)
			return false;
		if (Math.abs(a.stdX - stdX) > tol)
			return false;
		if (Math.abs(a.stdY - stdY) > tol)
			return false;
		return true;
	}
}
