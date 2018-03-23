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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.decomposition.svd.SafeSvd_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.List;

/**
 * <p>
 * Estimates the {@link TrifocalTensor} using a linear algorithm from 7 or more image points correspondences
 * from three views, see page 394 of [1] for details.  After an initial linear solution has been computed
 * it is improved upon by applying geometric constraints.  Note that the solution will not be optimal in a geometric
 * or algebraic sense, but can be used as an initial estimate for refinement algorithms.
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 * </p>
 *
 * @see EnforceTrifocalGeometry
 *
 * @author Peter Abeles
 */
public class TrifocalLinearPoint7 {

	// trifocal tensor computed from normalized coordinates
	protected TrifocalTensor solutionN = new TrifocalTensor();

	// normalization matrices for points
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();
	protected NormalizationPoint2D N2 = new NormalizationPoint2D();
	protected NormalizationPoint2D N3 = new NormalizationPoint2D();

	// the linear system which is solved for
	protected DMatrixRMaj A = new DMatrixRMaj(7,27);

	// svd used to extract the null space
	protected SingularValueDecomposition_F64<DMatrixRMaj> svdNull;

	// Solution in vector format
	protected DMatrixRMaj vectorizedSolution = new DMatrixRMaj(27,1);

	// triple of points after normalization
	protected Point2D_F64 p1_norm = new Point2D_F64();
	protected Point2D_F64 p2_norm = new Point2D_F64();
	protected Point2D_F64 p3_norm = new Point2D_F64();

	// enforces the geometry constraints
	protected EnforceTrifocalGeometry enforce = new EnforceTrifocalGeometry();
	protected TrifocalExtractEpipoles extractEpipoles = new TrifocalExtractEpipoles();
	// Epipoles needed to enforce the above constraints
	protected Point3D_F64 e2 = new Point3D_F64();
	protected Point3D_F64 e3 = new Point3D_F64();

	public TrifocalLinearPoint7() {
		svdNull = DecompositionFactory_DDRM.svd(24, 27, false, true, false);
		svdNull = new SafeSvd_DDRM(svdNull);
	}

	/**
	 * Estimates the trifocal tensor given the set of observations
	 *
	 * @param observations Set of observations
	 * @param solution Output: Where the solution is written to
	 * @return true if successful and false if it fails
	 */
	public boolean process( List<AssociatedTriple> observations , TrifocalTensor solution ) {

		if( observations.size() < 7 )
			throw new IllegalArgumentException("At least 7 correspondences must be provided");

		// compute normalization to reduce numerical errors
		LowLevelMultiViewOps.computeNormalization(observations, N1, N2, N3);

		// compute solution in normalized pixel coordinates
		createLinearSystem(observations);

		// solve for the trifocal tensor
		solveLinearSystem();

		// enforce geometric constraints to improve solution
		extractEpipoles.process(solutionN,e2,e3);
		enforce.process(e2,e3,A);
		enforce.extractSolution(solutionN);

		// undo normalization
		removeNormalization(solution);

		return true;
	}

	/**
	 * Constructs the linear matrix that describes from the 3-point constraint with linear
	 * dependent rows removed
	 */
	protected void createLinearSystem( List<AssociatedTriple> observations ) {
		int N = observations.size();

		A.reshape(4*N,27);
		A.zero();

		int index1 = 0;
		for( int i = 0; i < N; i++ ) {
			AssociatedTriple t = observations.get(i);

			N1.apply(t.p1,p1_norm);
			N2.apply(t.p2,p2_norm);
			N3.apply(t.p3,p3_norm);

			int index2 = index1+9;
			int index3 = index1+18;

			// i = 1, j = 1
			A.data[index1+8] =  p1_norm.x*p2_norm.x*p3_norm.x;
			A.data[index1+2] = -p1_norm.x*p3_norm.x;
			A.data[index1+6] = -p1_norm.x*p2_norm.x;
			A.data[index1]   =  p1_norm.x;
			A.data[index2+8] =  p1_norm.y*p2_norm.x*p3_norm.x;
			A.data[index2+2] = -p1_norm.y*p3_norm.x;
			A.data[index2+6] = -p1_norm.y*p2_norm.x;
			A.data[index2]   =  p1_norm.y;
			A.data[index3+8] =  p2_norm.x*p3_norm.x;
			A.data[index3+2] = -p3_norm.x;
			A.data[index3+6] = -p2_norm.x;
			A.data[index3]   =  1;

			// i = 1, j = 2;
			index1 += 27;
			index2 = index1+9;
			index3 = index1+18;

			A.data[index1+8] =  p1_norm.x*p2_norm.x*p3_norm.y;
			A.data[index1+2] = -p1_norm.x*p3_norm.y;
			A.data[index1+7] = -p1_norm.x*p2_norm.x;
			A.data[index1+1] =  p1_norm.x;
			A.data[index2+8] =  p1_norm.y*p2_norm.x*p3_norm.y;
			A.data[index2+2] = -p1_norm.y*p3_norm.y;
			A.data[index2+7] = -p1_norm.y*p2_norm.x;
			A.data[index2+1] =  p1_norm.y;
			A.data[index3+8] =  p2_norm.x*p3_norm.y;
			A.data[index3+2] = -p3_norm.y;
			A.data[index3+7] = -p2_norm.x;
			A.data[index3+1] =  1;

			// i = 2, j = 2;
			index1 += 27;
			index2 = index1+9;
			index3 = index1+18;

			A.data[index1+8] =  p1_norm.x*p2_norm.y*p3_norm.y;
			A.data[index1+5] = -p1_norm.x*p3_norm.y;
			A.data[index1+7] = -p1_norm.x*p2_norm.y;
			A.data[index1+4] =  p1_norm.x;
			A.data[index2+8] =  p1_norm.y*p2_norm.y*p3_norm.y;
			A.data[index2+5] = -p1_norm.y*p3_norm.y;
			A.data[index2+7] = -p1_norm.y*p2_norm.y;
			A.data[index2+4] =  p1_norm.y;
			A.data[index3+8] =  p2_norm.y*p3_norm.y;
			A.data[index3+5] = -p3_norm.y;
			A.data[index3+7] = -p2_norm.y;
			A.data[index3+4] =  1;

			// i = 2, j = 1;
			index1 += 27;
			index2 = index1+9;
			index3 = index1+18;

			A.data[index1+8] =  p1_norm.x*p2_norm.y*p3_norm.x;
			A.data[index1+5] = -p1_norm.x*p3_norm.x;
			A.data[index1+6] = -p1_norm.x*p2_norm.y;
			A.data[index1+3] =  p1_norm.x;
			A.data[index2+8] =  p1_norm.y*p2_norm.y*p3_norm.x;
			A.data[index2+5] = -p1_norm.y*p3_norm.x;
			A.data[index2+6] = -p1_norm.y*p2_norm.y;
			A.data[index2+3] =  p1_norm.y;
			A.data[index3+8] =  p2_norm.y*p3_norm.x;
			A.data[index3+5] = -p3_norm.x;
			A.data[index3+6] = -p2_norm.y;
			A.data[index3+3] =  1;

			index1 += 27;
		}
	}

	/**
	 * Computes the null space of the linear system to find the trifocal tensor
	 */
	protected boolean solveLinearSystem() {
		if( !svdNull.decompose(A) )
			return false;

		SingularOps_DDRM.nullVector(svdNull,true,vectorizedSolution);

		solutionN.convertFrom(vectorizedSolution);

		return true;
	}

	/**
	 * Translates the trifocal tensor back into regular coordinate system
	 */
	protected void removeNormalization( TrifocalTensor solution ) {
		DMatrixRMaj N2_inv = N2.matrixInv();
		DMatrixRMaj N3_inv = N3.matrixInv();

		DMatrixRMaj N1 = this.N1.matrix();

		for( int i = 0; i < 3; i++ ) {
			DMatrixRMaj T = solution.getT(i);
			for( int j = 0; j < 3; j++ ) {
				for( int k = 0; k < 3; k++ ) {

					double sum = 0;

					for( int r = 0; r < 3; r++ ) {
						double n1 = N1.get(r,i);
						DMatrixRMaj TN = solutionN.getT(r);
	                    for( int s = 0; s < 3; s++ ) {
							double n2 = N2_inv.get(j,s);
							for( int t = 0; t < 3; t++ ) {
								sum += n1*n2*N3_inv.get(k,t)*TN.get(s,t);
							}
						}
					}

					T.set(j,k,sum);
				}
			}
		}
	}
}
