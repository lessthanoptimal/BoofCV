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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Computes intrinsic calibration matrix using the absolute dual quadratic, projective transforms, and by assuming
 * different elements in the calibration matrix have a linear constraint. All camera parameters which are not
 * constrained are allowed to vary from frame to frame. The solution is found by computing the null space of
 * the constrained version of the system below:</p>
 * <p>w<sup>*</sup><sub>i</sub> = P<sub>i</sub>*Q<sup>*</sup><sub>&infin;</sub>*P<sup>T</sup><sub>i</sub></p>
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

	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(10,10,
			false,true,true);

	// constraints
	boolean knownAspect;
	boolean zeroSkew;

	// number of equations
	int eqs;

	// known aspect ratio
	double aspectRatio;

	// Found calibration parameters
	List<Intrinsic> solutions = new ArrayList<>();

	// The dual absolute quadratic
	DMatrix4x4 Q = new DMatrix4x4();

	// A singular value is considered zero if it is smaller than this number
	double singularThreshold=1e-3;

	/**
	 * Constructor for zero-principle point and (optional) zero-skew
	 *
	 * @param zeroSkew if true zero is assumed to be zero
	 */
	public SelfCalibrationLinearDualQuadratic(boolean zeroSkew ) {
		this(zeroSkew?3:2);
		knownAspect = false;
		this.zeroSkew = zeroSkew;
	}

	/**
	 * Constructor for zero-principle point, zero-skew, and known fixed aspect ratio (fy/fx)
	 *
	 * @param aspectRatio Specifies the known aspect ratio. fy/fx
	 */
	public SelfCalibrationLinearDualQuadratic(double aspectRatio ) {
		this(4);
		knownAspect = true;
		this.zeroSkew = true;
		this.aspectRatio = aspectRatio;
	}

	private SelfCalibrationLinearDualQuadratic(int equations ) {
		eqs = equations;
		minimumProjectives = (int)Math.ceil(10.0/eqs);
	}

	/**
	 * Solve for camera calibration. A sanity check is performed to ensure that a valid calibration is found.
	 * All values must be countable numbers and the focal lengths must be positive numbers.
	 *
	 * @return Indicates if it was successful or not. If it fails it says why
	 */
	public GeometricResult solve() {
		if( cameras.size < minimumProjectives )
			throw new IllegalArgumentException("You need at least "+minimumProjectives+" motions");

		int N = cameras.size;
		DMatrixRMaj L = new DMatrixRMaj(N*eqs,10);

		// Convert constraints into a (N*eqs) by 10 matrix. Null space is Q
		constructMatrix(L);

		// Compute the SVD for its null space
		if( !svd.decompose(L)) {
			return GeometricResult.SOLVE_FAILED;
		}

		// Examine the null space to find the values of Q
		// Then compute the solution for each view
		extractSolutionForQ(Q);
		if( !MultiViewOps.enforceAbsoluteQuadraticConstraints(Q,true,zeroSkew) ) {
			return GeometricResult.SOLVE_FAILED;
		}
		computeSolutions(Q);

		// determine if the solution is good by looking at two smallest singular values
		// If there isn't a steep drop it either isn't singular or more there is more than 1 singular value
		double sv[] = svd.getSingularValues();
		Arrays.sort(sv);
		if( singularThreshold*sv[1] <= sv[0] )  {
//			System.out.println("ratio = "+(sv[0]/sv[1]));
			return GeometricResult.GEOMETRY_POOR;
		}

		if( solutions.size() != N ) {
			return GeometricResult.SOLUTION_NAN;
		} else {
			return GeometricResult.SUCCESS;
		}
	}

	/**
	 * Extracts the null space and converts it into the Q matrix
	 */
	private void extractSolutionForQ( DMatrix4x4 Q ) {
		DMatrixRMaj nv = new DMatrixRMaj(10,1);
		SingularOps_DDRM.nullVector(svd,true,nv);

		// Convert the solution into a fixed sized matrix because it's easier to read
		encodeQ(Q,nv.data);
		// TODO enforce structure
	}

	/**
	 * Computes average W across all cameras. The idea being that the average will
	 * be a better estimate than selecting a random one.
	 */
	private void computeSolutions(DMatrix4x4 Q) {
		DMatrixRMaj w_i = new DMatrixRMaj(3,3);

		for (int i = 0; i < cameras.size; i++) {
			computeW(cameras.get(i),Q,w_i);
			Intrinsic calib = solveForCalibration(w_i);
			if( sanityCheck(calib)) {
				solutions.add(calib);
			}
		}
	}

	/**
	 * Given the solution for w and the constraints solve for the remaining parameters
	 */
	private Intrinsic solveForCalibration(DMatrixRMaj w) {
		Intrinsic calib = new Intrinsic();

//		CholeskyDecomposition_F64<DMatrixRMaj> chol = DecompositionFactory_DDRM.chol(false);
//
//		chol.decompose(w.copy());
//		DMatrixRMaj R = chol.getT(w);
//		R.print();

		if( zeroSkew ) {
			calib.skew = 0;
			calib.fy = Math.sqrt(w.get(1,1));

			if( knownAspect ) {
				calib.fx = calib.fy/aspectRatio;
			} else {
				calib.fx = Math.sqrt(w.get(0,0));
			}
		} else if( knownAspect ) {
			calib.fy = Math.sqrt(w.get(1,1));
			calib.fx = calib.fy/aspectRatio;
			calib.skew = w.get(0,1)/calib.fy;
		} else {
			calib.fy = Math.sqrt(w.get(1,1));
			calib.skew = w.get(0,1)/calib.fy;
			calib.fx = Math.sqrt(w.get(0,0) - calib.skew*calib.skew);
		}
		return calib;
	}

	/**
	 * Makes sure that the found solution is valid and physically possible
	 * @return true if valid
	 */
	boolean sanityCheck(Intrinsic calib ) {
		if(UtilEjml.isUncountable(calib.fx))
			return false;
		if(UtilEjml.isUncountable(calib.fy))
			return false;
		if(UtilEjml.isUncountable(calib.skew))
			return false;

		if( calib.fx < 0 )
			return false;
		if( calib.fy < 0 )
			return false;

		return true;
	}

	/**
	 * Constructs the linear system by applying specified constraints
	 */
	void constructMatrix(DMatrixRMaj L) {
		L.reshape(cameras.size*eqs,10);

		// Known aspect ratio constraint makes more sense as a square
		double RR = this.aspectRatio *this.aspectRatio;

		// F = P*Q*P'
		// F is an arbitrary variable name and not fundamental matrix
		int index = 0;
		for (int i = 0; i < cameras.size; i++) {
			Projective P = cameras.get(i);
			DMatrix3x3 A = P.A;
			DMatrix3 B = P.a;

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

			if( zeroSkew ) {
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

			if( knownAspect ) {
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

	public double getSingularThreshold() {
		return singularThreshold;
	}

	public void setSingularThreshold(double singularThreshold) {
		this.singularThreshold = singularThreshold;
	}

	public List<Intrinsic> getSolutions() {
		return solutions;
	}

	/**
	 * Returns the absolute quadratic
	 * @return 4x4 quadratic
	 */
	public DMatrix4x4 getQ() {
		return Q;
	}

	public static class Intrinsic {
		public double fx,fy,skew;
	}
}
