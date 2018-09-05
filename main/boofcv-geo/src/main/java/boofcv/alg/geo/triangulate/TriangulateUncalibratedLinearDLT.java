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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.GeometricResult;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Triangulates the location of a 3D point given two or more views of the point using the
 * Discrete Linear Transform (DLT). Works with an uncalibrated camera. Pixel observations and camera projection
 * matrices are input. Works on projective geometry. Normalization is automatically applied each row in the projective
 * matrix.
 * </p>
 *
 * <p>A geometric test is done using singular values. There should be a fairly obvious null space. If this
 * is not the case then the geometry will be considered bad</p>
 *
 * <p>
 * [1] Page 312 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class TriangulateUncalibratedLinearDLT {
	private SolveNullSpaceSvd_DDRM solverNull = new SolveNullSpaceSvd_DDRM();
	private DMatrixRMaj nullspace = new DMatrixRMaj(4,1);
	private DMatrixRMaj A = new DMatrixRMaj(4,4);

	// used in geometry test
	public double singularThreshold = 1;

	/**
	 * <p>
	 * Given N observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 *
	 * @param observations Observation in each view in pixel coordinates. Not modified.
	 * @param cameraMatrices Camera projection matrices, e.g. x = P*X.  3 by 4 projectives. Not modified.
	 * @param found Output, found 3D point in homogenous coordinates.  Modified.
	 * @return true if triangulation was successful or false if it failed
	 */
	public GeometricResult triangulate( List<Point2D_F64> observations ,
							 List<DMatrixRMaj> cameraMatrices,
							 Point4D_F64 found ) {
		if( observations.size() != cameraMatrices.size() )
			throw new IllegalArgumentException("Number of observations must match the number of motions");

		final int N = cameraMatrices.size();

		A.reshape(2*N,4);

		int index = 0;

		for( int i = 0; i < N; i++ ) {
			index = addView(cameraMatrices.get(i),observations.get(i),index);
		}

		// improve numerics
		normalizeRows(A);

		if( !solverNull.process(A,1, nullspace) )
			return GeometricResult.SOLVE_FAILED;

		// if the second smallest singular value is the same size as the smallest there's problem
		double sv[] = solverNull.getSingularValues();
		Arrays.sort(sv);
		if( sv[1]*singularThreshold <= sv[0] ) {
			return GeometricResult.GEOMETRY_POOR;
		}

		found.x = nullspace.get(0);
		found.y = nullspace.get(1);
		found.z = nullspace.get(2);
		found.w = nullspace.get(3);

		return GeometricResult.SUCCESS;
	}

	/**
	 * Adds a view to the A matrix
	 */
	private int addView( DMatrixRMaj P , Point2D_F64 a , int index ) {

		// Easier to read the code when P is broken up this way
		double r11 = P.data[0], r12 = P.data[1], r13 = P.data[2],  r14=P.data[3];
		double r21 = P.data[4], r22 = P.data[5], r23 = P.data[6],  r24=P.data[7];
		double r31 = P.data[8], r32 = P.data[9], r33 = P.data[10], r34=P.data[11];

		// first row
		A.data[index++] = a.x*r31-r11;
		A.data[index++] = a.x*r32-r12;
		A.data[index++] = a.x*r33-r13;
		A.data[index++] = a.x*r34-r14;

		// second row
		A.data[index++] = a.y*r31-r21;
		A.data[index++] = a.y*r32-r22;
		A.data[index++] = a.y*r33-r23;
		A.data[index++] = a.y*r34-r24;

		return index;
	}

	/**
	 * Normalized rows in A for better numerical stability. Solution is scale invariant so this will not change
	 * the result, but will ensure all the inputs are of the same order of magnitude.
	 */
	public static void normalizeRows( DMatrixRMaj A ) {

		int idx = 0;
		for (int row = 0; row < A.numRows; row++) {
			double r0 = A.data[idx];
			double r1 = A.data[idx+1];
			double r2 = A.data[idx+2];
			double r3 = A.data[idx+3];

			double f_norm = Math.sqrt(r0*r0 + r1*r1 + r2*r2 + r3*r3);
			A.data[idx++] = r0/f_norm;
			A.data[idx++] = r1/f_norm;
			A.data[idx++] = r2/f_norm;
			A.data[idx++] = r3/f_norm;
		}
	}

	public double getSingularThreshold() {
		return singularThreshold;
	}

	public void setSingularThreshold(double singularThreshold) {
		this.singularThreshold = singularThreshold;
	}
}
