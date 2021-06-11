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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.structure.DecomposeAbsoluteDualQuadratic;
import boofcv.struct.calib.CameraPinhole;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF4;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.Arrays;

/**
 * <p>
 * Computes intrinsic calibration matrix for each view using projective camera matrices to compute the
 * the dual absolute quadratic (DAQ) and by assuming different elements in the 3x3 calibration matrix
 * have linear constraints. A minimum of three views are required to solve for the 10 unknowns in
 * the DAQ if all constraints are applied. A solution found in the null space of the linear system below:
 * </p>
 *
 * <p>w<sup>*</sup><sub>i</sub> = P<sub>i</sub>*Q<sup>*</sup><sub>&infin;</sub>*P<sup>T</sup><sub>i</sub></p>
 * <p>Where Q<sup>*</sup> is DAQ and w<sup>*</sup> is the Dual Image Absolute Cubic (DIAC)</p>
 *
 * <p>On output, a list of intrinsic parameters is returned (fx,fy,skew) for every view which is provided.
 * For a complete discussion of the theory see the auto calibration section of [1] with missing equations
 * derived in [2].</p>
 *
 * <p>Two types of constraints can be specified; zero skew and fixed aspect ratio. Zero principle point is a mandatory
 * constraint. Zero skew is required if fixed aspect ratio is a constraint.</p>
 *
 * <p>A check for sufficient geometric diversity is done by looking at singular values. The nullity should be one,
 * but if its more than one then there is too much ambiguity. The tolerance can be adjusted by changing the
 * {@link #setSingularThreshold(double) singular threshold}.</p>
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * <li> P. Abeles, "BoofCV Technical Report: Automatic Camera Calibration" 2018-1 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class SelfCalibrationLinearDualQuadratic extends SelfCalibrationBase {

	@Getter SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(10, 10,
			false, true, true);

	public @Getter DecomposeAbsoluteDualQuadratic decomposeADQ = new DecomposeAbsoluteDualQuadratic();

	// constraints
	public @Getter boolean knownAspect;
	public @Getter boolean zeroSkew;

	// number of equations
	int eqs;

	// known aspect ratio
	double aspectRatio;

	// Found calibration parameters
	DogArray<Intrinsic> solutions = new DogArray<>(Intrinsic::new);

	// The dual absolute quadratic
	DMatrix4x4 Q = new DMatrix4x4();

	// Modifies the singular values so that the smallest one will be zero
//	protected final MassageSingularValues forceSmallestSingularToZero;  <-- kept for future work/ideas

	// A singular value is considered zero if it is smaller than this number
	@Getter @Setter double singularThreshold = 1e-3;

	//---------------- Internal workspace
	private final DMatrixRMaj L = new DMatrixRMaj(1, 1);
	private final DMatrixRMaj w_i = new DMatrixRMaj(3, 3);

	// Storage for null space vector
	private final DMatrixRMaj nv = new DMatrixRMaj(10, 1);

	/**
	 * Constructor for zero-principle point and (optional) zero-skew
	 *
	 * @param zeroSkew if true zero is assumed to be zero
	 */
	public SelfCalibrationLinearDualQuadratic( boolean zeroSkew ) {
		this(zeroSkew ? 3 : 2);
		knownAspect = false;
		this.zeroSkew = zeroSkew;
	}

	/**
	 * Constructor for zero-principle point, zero-skew, and known fixed aspect ratio (fy/fx)
	 *
	 * @param aspectRatio Specifies the known aspect ratio. fy/fx
	 */
	public SelfCalibrationLinearDualQuadratic( double aspectRatio ) {
		this(4);
		knownAspect = true;
		this.zeroSkew = true;
		this.aspectRatio = aspectRatio;
	}

	private SelfCalibrationLinearDualQuadratic( int equations ) {
		eqs = equations;
		minimumProjectives = (int)Math.ceil(10.0/eqs);
//		forceSmallestSingularToZero = new MassageSingularValues(( W ) -> W.unsafe_set(3, 3, 0.0));
	}

	/**
	 * Resets it into its initial state
	 */
	public void reset() {
		cameras.reset();
	}

	/**
	 * Solve for camera calibration. A sanity check is performed to ensure that a valid calibration is found.
	 * All values must be countable numbers and the focal lengths must be positive numbers.
	 *
	 * @return Indicates if it was successful or not. If it fails it says why
	 */
	public GeometricResult solve() {
		solutions.reset();

		if (cameras.size < minimumProjectives) {
			throw new IllegalArgumentException("You need at least " + minimumProjectives + " motions");
		}

		int N = cameras.size;
		L.reshape(N*eqs, 10);

		// Convert constraints into a (N*eqs) by 10 matrix. Null space is Q
		constructMatrix(L);

		// Compute the SVD for its null space
		if (!svd.decompose(L)) {
			return GeometricResult.SOLVE_FAILED;
		}

		// Extract the null space
		SingularOps_DDRM.nullVector(svd, true, nv);

		// determine if the solution is good by looking at two smallest singular values
		// If there isn't a steep drop it either isn't singular or more there is more than 1 singular value
		double[] sv = svd.getSingularValues();
		Arrays.sort(sv);
		if (singularThreshold*sv[1] <= sv[0]) {
//			System.out.println("ratio = "+(sv[0]/sv[1]));
			return GeometricResult.GEOMETRY_POOR;
		}

		// Examine the null space to find the values of Q
		if (!extractSolutionForQ(Q))
			return GeometricResult.SOLVE_FAILED;

		// Enforce constraints and solve for each view
		if (!MultiViewOps.enforceAbsoluteQuadraticConstraints(Q, true, zeroSkew, decomposeADQ)) {
			return GeometricResult.SOLVE_FAILED;
		}
		computeSolutions(Q);

		if (solutions.size() != N) {
			return GeometricResult.SOLUTION_NAN;
		} else {
			return GeometricResult.SUCCESS;
		}
	}

	/**
	 * Extracts the null space and converts it into the Q matrix
	 */
	private boolean extractSolutionForQ( DMatrix4x4 Q ) {
		// Convert the solution into a fixed sized matrix because it's easier to read
		encodeQ(Q, nv.data);

		// The code below is commented out since it seems to make things worse. Unit tests which used to
		// pass but now fail appear to have a much small difference in value between the smallest and second smallest
		// singular values. That could be a hint as to what's going on.

		// Enforce Q being a rank-3 matrix
//		L.reshape(4, 4); // recycle this variable
//		DConvertMatrixStruct.convert(Q, L);
//
//		if (!forceSmallestSingularToZero.process(L))
//			return false;
//		DConvertMatrixStruct.convert(L, Q);

		// diagonal elements must be positive because Q = [K*K' .. ; ... ]
		// If they are a mix of positive and negative that's bad and can't be fixed
		// All 3 are checked because technically they could be zero. Not a physically useful solution but...
		if (Q.a11 < 0 || Q.a22 < 0 || Q.a33 < 0) {
			CommonOps_DDF4.scale(-1, Q);
		}
		return true;
	}

	/**
	 * Computes the calibration for each view..
	 */
	private void computeSolutions( DMatrix4x4 Q ) {
		for (int i = 0; i < cameras.size; i++) {
			computeW(cameras.get(i), Q, w_i);
			Intrinsic calib = solutions.grow();
			solveForCalibration(w_i, calib);
			if (!sanityCheck(calib)) {
				solutions.removeTail();
			}
		}
	}

	/**
	 * Given the solution for w and the constraints solve for the remaining parameters
	 */
	private void solveForCalibration( DMatrixRMaj w, Intrinsic calib ) {
		if (zeroSkew) {
			calib.skew = 0;
			calib.fy = Math.sqrt(w.get(1, 1));

			if (knownAspect) {
				calib.fx = calib.fy/aspectRatio;
			} else {
				calib.fx = Math.sqrt(w.get(0, 0));
			}
		} else if (knownAspect) {
			calib.fy = Math.sqrt(w.get(1, 1));
			calib.fx = calib.fy/aspectRatio;
			calib.skew = w.get(0, 1)/calib.fy;
		} else {
			calib.fy = Math.sqrt(w.get(1, 1));
			calib.skew = w.get(0, 1)/calib.fy;
			calib.fx = Math.sqrt(w.get(0, 0) - calib.skew*calib.skew);
		}
	}

	/**
	 * Makes sure that the found solution is valid and physically possible
	 *
	 * @return true if valid
	 */
	boolean sanityCheck( Intrinsic calib ) {
		if (UtilEjml.isUncountable(calib.fx))
			return false;
		if (UtilEjml.isUncountable(calib.fy))
			return false;
		if (UtilEjml.isUncountable(calib.skew))
			return false;

		if (calib.fx < 0)
			return false;
		if (calib.fy < 0)
			return false;

		return true;
	}

	/**
	 * Constructs the linear system by applying specified constraints
	 */
	void constructMatrix( DMatrixRMaj L ) {
		L.reshape(cameras.size*eqs, 10);

		// Known aspect ratio constraint makes more sense as a square
		double RR = this.aspectRatio*this.aspectRatio;

		// F = P*Q*P'
		// F is an arbitrary variable name and not fundamental matrix
		int index = 0;
		for (int i = 0; i < cameras.size; i++) {
			Projective P = cameras.get(i);
			DMatrix3x3 A = P.A;
			DMatrix3 B = P.a;

			// hard to tell if this helped or hurt. Keeping commented out for future investigation on proper scaling
//			double scale = NormOps_DDF3.normF(P.A);
//			CommonOps_DDF3.divide(P.A,scale);
//			CommonOps_DDF3.divide(P.a,scale);
			// Scaling camera matrices P on input by [1/f 0 0; 0 1/f 0; 0 0 1] seems to make it slightly worse on
			// real world data!

			// constraint for a zero principle point

			// row for F[0,2] == 0
			L.data[index++] = A.a11*A.a31;
			L.data[index++] = (A.a12*A.a31 + A.a11*A.a32);
			L.data[index++] = (A.a13*A.a31 + A.a11*A.a33);
			L.data[index++] = (B.a1*A.a31 + A.a11*B.a3);
			L.data[index++] = A.a12*A.a32;
			L.data[index++] = (A.a13*A.a32 + A.a12*A.a33);
			L.data[index++] = (B.a1*A.a32 + A.a12*B.a3);
			L.data[index++] = A.a13*A.a33;
			L.data[index++] = (B.a1*A.a33 + A.a13*B.a3);
			L.data[index++] = B.a1*B.a3;

			// row for F[1,2] == 0
			L.data[index++] = A.a21*A.a31;
			L.data[index++] = (A.a22*A.a31 + A.a21*A.a32);
			L.data[index++] = (A.a23*A.a31 + A.a21*A.a33);
			L.data[index++] = (B.a2*A.a31 + A.a21*B.a3);
			L.data[index++] = A.a22*A.a32;
			L.data[index++] = (A.a23*A.a32 + A.a22*A.a33);
			L.data[index++] = (B.a2*A.a32 + A.a22*B.a3);
			L.data[index++] = A.a23*A.a33;
			L.data[index++] = (B.a2*A.a33 + A.a23*B.a3);
			L.data[index++] = B.a2*B.a3;

			if (zeroSkew) {
				// row for F[0,1] == 0
				L.data[index++] = A.a11*A.a21;
				L.data[index++] = (A.a12*A.a21 + A.a11*A.a22);
				L.data[index++] = (A.a13*A.a21 + A.a11*A.a23);
				L.data[index++] = (B.a1*A.a21 + A.a11*B.a2);
				L.data[index++] = A.a12*A.a22;
				L.data[index++] = (A.a13*A.a22 + A.a12*A.a23);
				L.data[index++] = (B.a1*A.a22 + A.a12*B.a2);
				L.data[index++] = A.a13*A.a23;
				L.data[index++] = (B.a1*A.a23 + A.a13*B.a2);
				L.data[index++] = B.a1*B.a2;
			}

			if (knownAspect) {
				// aspect^2*F[0,0]-F[1,1]
				L.data[index++] = A.a11*A.a11*RR - A.a21*A.a21;
				L.data[index++] = 2*(A.a11*A.a12*RR - A.a21*A.a22);
				L.data[index++] = 2*(A.a11*A.a13*RR - A.a21*A.a23);
				L.data[index++] = 2*(A.a11*B.a1*RR - A.a21*B.a2);
				L.data[index++] = A.a12*A.a12*RR - A.a22*A.a22;
				L.data[index++] = 2*(A.a12*A.a13*RR - A.a22*A.a23);
				L.data[index++] = 2*(A.a12*B.a1*RR - A.a22*B.a2);
				L.data[index++] = A.a13*A.a13*RR - A.a23*A.a23;
				L.data[index++] = 2*(A.a13*B.a1*RR - A.a23*B.a2);
				L.data[index++] = B.a1*B.a1*RR - B.a2*B.a2;
			}
		}
	}

	public FastAccess<Intrinsic> getIntrinsics() {
		return solutions;
	}

	public DMatrix3 getPlaneAtInfinity() {
		return decomposeADQ.getP();
	}

	/**
	 * Returns the absolute quadratic
	 *
	 * @return 4x4 quadratic
	 */
	public DMatrix4x4 getQ() {
		return Q;
	}

	public static class Intrinsic {
		public double fx, fy, skew;

		/** Copies the values into this class in to the more generalized {@link boofcv.struct.calib.CameraPinhole} */
		public void copyTo( CameraPinhole pinhole ) {
			pinhole.fx = fx;
			pinhole.fy = fy;
			pinhole.skew = skew;
			pinhole.cx = 0;
			pinhole.cy = 0;
		}
	}
}
