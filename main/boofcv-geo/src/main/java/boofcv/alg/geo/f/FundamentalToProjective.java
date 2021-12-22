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

package boofcv.alg.geo.f;

import boofcv.alg.geo.impl.ProjectiveToIdentity;
import boofcv.misc.ConfigConverge;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.point.Vector4D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.derivative.NumericalJacobianForward_DDRM;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;

/**
 * Computes projective camera matrices from a fundamental matrix. All work space is predeclared and won't thrash
 * the garbage collector.
 *
 * See section 9.5 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
 *
 * @author Peter Abeles
 */
public class FundamentalToProjective {

	private DMatrixRMaj outer = new DMatrixRMaj(3, 3);
	private DMatrixRMaj KR = new DMatrixRMaj(3, 3);

	FundamentalExtractEpipoles alg = new FundamentalExtractEpipoles();
	Point3D_F64 e1 = new Point3D_F64();
	Point3D_F64 e2 = new Point3D_F64();

	Vector3D_F64 zero = new Vector3D_F64();

	// used to convert P*H = [I|0]
	ProjectiveToIdentity p2i = new ProjectiveToIdentity();

	ConfigConverge convergence = new ConfigConverge(1e-8, 1e-8, 25);
	UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
	FundamentalResidual residual = new FundamentalResidual();
	NumericalJacobianForward_DDRM jacobian = new NumericalJacobianForward_DDRM(residual);
	private double[] initialV = new double[3];

	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P0 and P1 can be extracted. Same
	 * {@link #twoView(DMatrixRMaj, DMatrixRMaj)} but with the suggested values for all variables filled in for you.
	 * </p>
	 *
	 * @param F (Input) Fundamental Matrix
	 * @param cameraMatrix (Output) resulting projective camera matrix P'. (3 by 4) Known up to a projective transform.
	 */
	public void twoView( DMatrixRMaj F, DMatrixRMaj cameraMatrix ) {
		alg.process(F, e1, e2);
		twoView(F, e2, zero, 1, cameraMatrix);
	}

	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P and P1' are extracted. The camera matrices
	 * are 3 by 4 and used to project a 3D homogenous point onto the image plane. These camera matrices will only
	 * be known up to a projective transform, thus there are multiple solutions, The canonical camera
	 * matrix is defined as: <br>
	 * <pre>
	 * P=[I|0] and P'= [M|-M*t] = [[e']*F + e'*v^t | lambda*e']
	 * </pre>
	 * where e' is the epipole F<sup>T</sup>e' = 0, [e'] is the cross product matrix for the enclosed vector,
	 * v is an arbitrary 3-vector and lambda is a non-zero scalar.
	 * </p>
	 *
	 * <p>
	 * NOTE: Additional information is needed to upgrade this projective transform into a metric transform.
	 * </p>
	 * <p>
	 * Page 256 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
	 * </p>
	 *
	 * @param F (Input) A fundamental matrix
	 * @param e2 (Input) Left epipole of fundamental matrix, F<sup>T</sup>*e2 = 0.
	 * @param v (Input) Arbitrary 3-vector. Just pick some value, say (0,0,0).
	 * @param lambda (Input) A non zero scalar. Try one.
	 * @param cameraMatrix (Output) resulting projective camera matrix P'. (3 by 4) Known up to a projective transform.
	 */
	public void twoView( DMatrixRMaj F, Point3D_F64 e2, Vector3D_F64 v, double lambda,
						 DMatrixRMaj cameraMatrix ) {
		GeometryMath_F64.outerProd(e2, v, outer);

		GeometryMath_F64.multCrossA(e2, F, KR);
		CommonOps_DDRM.add(KR, outer, KR);

		CommonOps_DDRM.insert(KR, cameraMatrix, 0, 0);

		cameraMatrix.set(0, 3, lambda*e2.x);
		cameraMatrix.set(1, 3, lambda*e2.y);
		cameraMatrix.set(2, 3, lambda*e2.z);
	}

	/**
	 * <p>
	 * Given three fundamental matrices describing the relationship between three views, compute a consistent
	 * set of projective camera matrices. Consistent means that the camera matrices will give back the same
	 * fundamental matrices. Non-linear refinement might be needed to get an optimal solution.
	 * </p>
	 *
	 * The first camera matrix, without loss of generality, P1 = [I|0].
	 *
	 * NOTE: HZ does not recommend using this function because the optimal solution is not geometric and prone
	 * to errors. Instead it recommends use of trifocal tensor to find a set of camera matrices. When testing
	 * the function it was noted that the geometry had to be carefully selected to ensure stability. Hinting that
	 * it might not be the most practical algorithm in real data.
	 *
	 * <ol>
	 * <li>Page 301 in Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision"
	 * Springer-Verlad, 2004</li>
	 * <li>Page 386 and 255 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003</li>
	 * </ol>
	 *
	 * @param F21 (Input) Fundamental matrix between view 1 and 2
	 * @param F31 (Input) Fundamental matrix between view 1 and 3
	 * @param F32 (Input) Fundamental matrix between view 2 and 3
	 * @param P2 (Output) Camera matrix for view 2
	 * @param P3 (Output) Camera matrix for view 3
	 */
	public boolean threeView( DMatrixRMaj F21, DMatrixRMaj F31, DMatrixRMaj F32,
							  DMatrixRMaj P2, DMatrixRMaj P3 ) {
		// left epipoles. renamed to make code easier to read
		Point3D_F64 e21 = e1;
		Point3D_F64 e31 = e2;

		alg.process(F21, null, e21);
		twoView(F21, e21, zero, 1, P2);
		alg.process(F31, null, e31);

		// find transform which will make P2 = [I|0]
		if (!p2i.process(P2))
			return false;

		// Perform non-linear optimization to find the parameters for u and minimize the difference
		residual.setF31(F31, e31);
		residual.setF32(F32);
		residual.setH(p2i.getPseudoInvP(), p2i.getU());

//		optimizer.setVerbose(System.out,0);
		optimizer.setFunction(residual, jacobian);
		optimizer.initialize(initialV, convergence.ftol, convergence.gtol);
		for (int i = 0; i < convergence.maxIterations; i++) {
			if (optimizer.iterate())
				break;
		}

		P3.setTo(residual.computeP3(optimizer.getParameters()));

//		// used to sanity check the found solution
//		double err[] = new double[9];
//		residual.process(optimizer.getParameters(),err);
//		residual.F32.print();
//		residual.F32_est.print();

		return true;
	}

	public double getThreeViewError() {
		return optimizer.getFunctionValue();
	}

	public ConfigConverge getConvergence() {
		return convergence;
	}

	/**
	 * Residual function used to minimize the difference between two versions of the F32 fundamental matrix.
	 * One version is computed from F21 and F31. The other version was provided by the user.
	 */
	@SuppressWarnings({"NullAway.Init"})
	class FundamentalResidual implements FunctionNtoM {

		// Input F32
		DMatrixRMaj F32 = new DMatrixRMaj(3, 3);
		// Computed F32
		DMatrixRMaj F32_est = new DMatrixRMaj(3, 3);

		// computed P3
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

		// H = [P2inv | u ]; P2*H = [I|0]
		DMatrixRMaj P2inv; //pseudo inverse
		Vector4D_F64 u = new Vector4D_F64();
		// input fundamental matrix F31
		DMatrixRMaj F31;
		// epipole from F31
		Point3D_F64 T31;

		// Storage for decomposition of P3
		DMatrixRMaj R = new DMatrixRMaj(3, 3);
		Vector3D_F64 a = new Vector3D_F64();

		// work space for input parameters
		Vector3D_F64 v = new Vector3D_F64();

		@Override
		public void process( double[] input, double[] output ) {
			// given the current value of v, compute P3
			// P3 = [cross(T31)'*F31 + T31*v' | T31 ]
			computeP3(input);

			// [R,T] = [P3*P2 | P3*u]
			CommonOps_DDRM.mult(P3, P2inv, R);
			GeometryMath_F64.mult(P3, u, a);

			// Compute the new fundamental matrix
			GeometryMath_F64.multCrossA(a, R, F32_est);

			// TODO sign ambuguity?
			// Put into a common scale so that it can be compared against F32
			double n = NormOps_DDRM.normF(F32_est);
			CommonOps_DDRM.scale(1.0/n, F32_est);

			for (int i = 0; i < 9; i++) {
				output[i] = F32_est.data[i] - F32.data[i];
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 3;
		}

		@Override
		public int getNumOfOutputsM() {
			return 9;
		}

		public DMatrixRMaj computeP3( double[] input ) {
			v.x = input[0];
			v.y = input[1];
			v.z = input[2];
			FundamentalToProjective.this.twoView(F31, T31, v, 1, P3);
			return P3;
		}

		public void setF31( DMatrixRMaj F31, Point3D_F64 T31 ) {
			this.F31 = F31;
			this.T31 = T31;
		}

		public void setF32( DMatrixRMaj F32 ) {
			this.F32.setTo(F32);
			double n = NormOps_DDRM.normF(this.F32);
			CommonOps_DDRM.scale(1.0/n, this.F32);
		}

		/**
		 * H = [P2inv | u ]
		 */
		public void setH( DMatrixRMaj P2inv, DMatrixRMaj u ) {
			this.P2inv = P2inv;
			this.u.x = u.data[0];
			this.u.y = u.data[1];
			this.u.z = u.data[2];
			this.u.w = u.data[3];
		}
	}
}
