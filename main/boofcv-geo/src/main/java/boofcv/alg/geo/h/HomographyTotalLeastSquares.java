/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF2;
import org.ejml.dense.row.linsol.qr.SolveNullSpaceQRP_DDRM;
import org.ejml.interfaces.SolveNullSpace;

import java.util.List;

/**
 * <p>Direct method for computing homography that is more computationally efficient and stable than DLT. Takes
 * advantage of the sparse structure of the matrix found in {@link HomographyDirectLinearTransform DLT} to reduce
 * the number of computations and EYM matrix approximation. See the paper [1] for details. Requires at least
 * four points.</p>
 *
 *
 * <p>[1] Harker, Matthew, and Paul O'Leary. "Computation of Homographies." BMVC. 2005.</p>
 *
 * @author Peter Abeles
 */
public class HomographyTotalLeastSquares {
	// Solver for null space
	SolveNullSpace<DMatrixRMaj> solverNull = new SolveNullSpaceQRP_DDRM();
	DMatrixRMaj nullspace = new DMatrixRMaj(3,1);

	// Used to normalize input points
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();
	protected NormalizationPoint2D N2 = new NormalizationPoint2D();

	DMatrixRMaj X1 = new DMatrixRMaj(1,1);
	DMatrixRMaj X2 = new DMatrixRMaj(1,1);

	DMatrixRMaj A = new DMatrixRMaj();

	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images.  A set of {@link AssociatedPair}
	 * is passed in.  The computed homography 'H' is found such that the attributes 'p1' and 'p2' in {@link AssociatedPair}
	 * refers to x1 and x2, respectively, in the equation  below:<br>
	 * x<sub>2</sub> = H*x<sub>1</sub>
	 * </p>
	 *
	 * @param points A set of observed image points that are generated from a planar object.  Minimum of 4 pairs required.
	 * @param foundH Output: Storage for the found solution. 3x3 matrix.
	 * @return true if the calculation was a success.
	 */
	public boolean process(List<AssociatedPair> points , DMatrixRMaj foundH ) {
		if (points.size() < 4)
			throw new IllegalArgumentException("Must be at least 4 points.");

		final int N = points.size();

		// Have to normalize the points. Being zero mean is essential in its derivation
		LowLevelMultiViewOps.computeNormalization(points, N1, N2);
		LowLevelMultiViewOps.applyNormalization(points,N1,N2,X1,X2);
		// X1 = (x[i],y[i]; ... ) and X2 = (x[i]',y[i]'; ... )

		constructA79(N);

		// Solve for elements H_7 to H_9
		if( !solverNull.process(A,1,nullspace))
			return false;

		// Determine H_1 to H_6
		backSubstitution();

		return false;
	}

	private void constructA79(int n) {
		// Pseudo-inverse of hat(p)
		DMatrixRMaj P_plus = new DMatrixRMaj(1,1);
		computePseudo(X1,P_plus);

		DMatrixRMaj PPpXP = new DMatrixRMaj(1,1);
		DMatrixRMaj PPpYP = new DMatrixRMaj(1,1);
		computePPXP(X1,P_plus,X2,0,PPpXP);
		computePPXP(X1,P_plus,X2,1,PPpYP);

		DMatrixRMaj PPpX = new DMatrixRMaj(1,1);
		DMatrixRMaj PPpY = new DMatrixRMaj(1,1);
		computePPpX(X1,P_plus,X2,0,PPpX);
		computePPpX(X1,P_plus,X2,1,PPpY);

		//============ Equations 20
		double XP_bar[] = new double[2];
		double YP_bar[] = new double[2];
		computeEq20(X1,X2,0, n,XP_bar);
		computeEq20(X1,X2,1, n,YP_bar);

		//============ Equation 21
		DMatrixRMaj A = new DMatrixRMaj(n *2,3);
		double XP_bar_x = XP_bar[0];
		double XP_bar_y = XP_bar[1];
		double YP_bar_x = YP_bar[0];
		double YP_bar_y = YP_bar[1];

		// Compute the top half of A
		for (int i = 0, index = 0, indexA = 0; i < n; i++, index+=2) {
			double x = -X1.data[i*2];
			double x_hat = X2.data[index];
			double y_hat = X2.data[index+1];

			// x'*hat{p} - bar{X*P} - PPpXP
			A.data[indexA++] = x*x_hat - XP_bar_x - PPpXP.data[index];
			A.data[indexA++] = x*y_hat - XP_bar_y - PPpXP.data[index+1];

			// X'*1 - PPx1
			A.data[indexA++] = x - PPpX.data[i];
		}
		// Compute the bottom half of A
		for (int i = 0, index = 0, indexA = 0; i < n; i++, index+=2) {
			double y = -X1.data[i*2+1];
			double x_hat = X2.data[index];
			double y_hat = X2.data[index+1];

			// x'*hat{p} - bar{X*P} - PPpXP
			A.data[indexA++] = y*x_hat - YP_bar_x - PPpYP.data[index];
			A.data[indexA++] = y*y_hat - YP_bar_y - PPpYP.data[index+1];

			// X'*1 - PPx1
			A.data[indexA++] = y - PPpY.data[i];
		}
	}

	/**
	 * Now that the first 3 elements are known use back substitution to get the other unknown.
	 */
	void backSubstitution() {

	}

	static void computeEq20( DMatrixRMaj A , DMatrixRMaj B , int offset, final int N , double output[]) {
		double a00=0,a01=0;
		for (int i = 0, index = 0; i < N; i++) {
			double xi = A.data[index+offset];
			a00 += xi*B.data[index++];
			a01 += xi*B.data[index++];
		}
		output[0] = -a00/N;
		output[1] = -a01/N;
	}

	/**
	 * Computes inv(A<sup>T</sup>*A)*A<sup>T</sup>
	 */
	static void computePseudo(DMatrixRMaj A , DMatrixRMaj output ) {
		final int N = A.numRows;

		DMatrix2x2 m = new DMatrix2x2();
		DMatrix2x2 m_inv = new DMatrix2x2();

		for (int i = 0, index = 0; i < N; i++) {
			double a_i0 = A.data[index];
			double a_i1 = A.data[index+1];

			m.a11 += a_i0*a_i0;
			m.a12 += a_i0*a_i1;
			m.a22 += a_i1*a_i1;
		}
		m.a21 = m.a12;
		CommonOps_DDF2.invert(m,m_inv);

		output.reshape(2,N);
		for (int i = 0,index=0; i < N; i++,index+=2) {
			output.data[i] = A.data[index]*m_inv.a12 + A.data[index+1]*m_inv.a11;
		}
		int end = 2*N;
		for (int i = N,A_index=0; i < end; i++,A_index+=2) {
			output.data[i] = A.data[A_index]*m_inv.a22 + A.data[A_index+1]*m_inv.a21;
		}
	}

	/**
	 * Computes P*P_plus*X*P. Takes in account the size of each matrix and does the multiplcation in an order
	 * to minimize memory requirements. A naive implementation requires a temporary array of NxN
	 * @param P
	 * @param P_plus
	 * @param X A diagonal matrix
	 * @param output
	 */
	static void computePPXP( DMatrixRMaj P , DMatrixRMaj P_plus, DMatrixRMaj X , int offsetX,  DMatrixRMaj output ) {
		final int N = P.numRows;
		output.reshape(N,2);

		// diag(X) * P <-- N x 2
		for (int i = 0, index=0; i < N; i++, index += 2) {
			double x = -X.data[index+offsetX];
			output.data[index] = x*P.data[index];
			output.data[index+1] = x*P.data[index+1];
		}

		// A = P_plus*( diag(x)*P ) <-- 2 x 2
		double a00=0,a01=0,a10=0,a11=0;
		for (int i = 0, index=0; i < N; i++, index += 2) {
			a00 += P_plus.data[index]*output.data[index];
			a01 += P_plus.data[index]*output.data[index+1];
			a10 += P_plus.data[index+1]*output.data[index];
			a11 += P_plus.data[index+1]*output.data[index+1];
		}

		// P*A <-- N x 2
		for (int i = 0, index=0; i < N; i++, index += 2) {
			output.data[index]   = P.data[index]*a00 + P.data[index+1]*a10;
			output.data[index+1] = P.data[index]*a01+ P.data[index+1]*a11;
		}
	}

	static void computePPpX( DMatrixRMaj P , DMatrixRMaj P_plus, DMatrixRMaj X , int offsetX,  DMatrixRMaj output ) {
		final int N = P.numRows;
		output.reshape(N,1);

		// A=P_plus * X <-- 2x1
		double a00=0,a10=0;
		for (int i = 0,indexX=offsetX; i < N; i++, indexX += 2) {
			double x = -X.data[indexX];
			a00 += x*P_plus.data[i];
			a10 += x*P_plus.data[i+N];
		}

		// P*A
		for (int i = 0,indexP=0; i < N; i++) {
			output.data[i] = a00*P.data[indexP++] + a10*P.data[indexP++];
		}

	}
}
