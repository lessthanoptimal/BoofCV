/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.NormalizationPoint2D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Triangulates the location of a 3D point given two or more views of the point using the
 * Discrete Linear Transform (DLT). Modified to work only with a calibrated camera. The second singular value
 * is checked to see if a solution was possible.
 * </p>
 *
 * <p>
 * [1] Page 312 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class TriangulateMetricLinearDLT {

	private final SolveNullSpaceSvd_DDRM solverNull = new SolveNullSpaceSvd_DDRM();
	private final DMatrixRMaj nullspace = new DMatrixRMaj(4, 1);
	private final DMatrixRMaj A = new DMatrixRMaj(4, 4);

	/** used in geometry test */
	public @Getter @Setter double singularThreshold = 1;

	// used for normalizing pixel coordinates and improving linear solution
	final NormalizationPoint2D stats = new NormalizationPoint2D();

	/**
	 * <p>
	 * Given N observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to be less generic and use calibrated cameras.
	 * </p>
	 *
	 * @param observations Observation in each view in normalized coordinates. Not modified.
	 * @param worldToView Transformations from world to the view. Not modified.
	 * @param found (Output) 3D point in homogenous coordinates. Modified.
	 */
	public GeometricResult triangulate( List<Point2D_F64> observations,
										List<Se3_F64> worldToView,
										Point4D_F64 found ) {
		if (observations.size() != worldToView.size())
			throw new IllegalArgumentException("Number of observations must match the number of motions");

		final int N = worldToView.size();

		A.reshape(2*N, 4, false);

		int index = 0;

		for (int i = 0; i < N; i++) {
			index = addView(worldToView.get(i), observations.get(i), index);
		}

		return finishSolving(found);
	}

	/**
	 * <p>
	 * Given two observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to be less generic and use calibrated cameras.
	 * </p>
	 *
	 * @param a Observation 'a' in normalized coordinates. Not modified.
	 * @param b Observation 'b' in normalized coordinates. Not modified.
	 * @param fromAtoB Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA Output, the found 3D position of the point. Modified.
	 */
	public GeometricResult triangulate( Point2D_F64 a, Point2D_F64 b,
										Se3_F64 fromAtoB,
										Point4D_F64 foundInA ) {
		A.reshape(4, 4);

		int index = addView(fromAtoB, b, 0);

		// third row
		A.data[index++] = -1;
		A.data[index++] = 0;
		A.data[index++] = a.x;
		A.data[index++] = 0;

		// fourth row
		A.data[index++] = 0;
		A.data[index++] = -1;
		A.data[index++] = a.y;
		A.data[index] = 0;

		return finishSolving(foundInA);
	}

	/**
	 * <p>
	 * Given N observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to be less generic and use calibrated cameras.
	 * </p>
	 *
	 * @param observations Observation in each view 3d pointing vectors. Not modified.
	 * @param worldToView Transformations from world to the view. Not modified.
	 * @param found (Output) 3D point in homogenous coordinates. Modified.
	 */
	public GeometricResult triangulateP( List<Point3D_F64> observations,
										 List<Se3_F64> worldToView,
										 Point4D_F64 found ) {
		if (observations.size() != worldToView.size())
			throw new IllegalArgumentException("Number of observations must match the number of motions");

		final int N = worldToView.size();

		A.reshape(2*N, 4, false);

		int index = 0;

		for (int i = 0; i < N; i++) {
			index = addView(worldToView.get(i), observations.get(i), index);
		}

		return finishSolving(found);
	}

	/**
	 * <p>
	 * Given two observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to use 3D vectors instead of normalized image coordinates.
	 * </p>
	 *
	 * @param a Observation 'a' as a 3d pointing vector. Not modified.
	 * @param b Observation 'b' as a 3d pointing vector. Not modified.
	 * @param fromAtoB Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA Output, the found 3D position of the point. Modified.
	 */
	public GeometricResult triangulateP( Point3D_F64 a, Point3D_F64 b,
										 Se3_F64 fromAtoB,
										 Point4D_F64 foundInA ) {
		A.reshape(4, 4);

		int index = addView(fromAtoB, b, 0);

		// third row
		A.data[index++] = -a.z;
		A.data[index++] = 0;
		A.data[index++] = a.x;
		A.data[index++] = 0;

		// fourth row
		A.data[index++] = 0;
		A.data[index++] = -a.z;
		A.data[index++] = a.y;
		A.data[index] = 0;

		return finishSolving(foundInA);
	}

	private GeometricResult finishSolving( Point4D_F64 foundInA ) {
		if (!solverNull.process(A, 1, nullspace))
			return GeometricResult.SOLVE_FAILED;

		// if the second smallest singular value is the same size as the smallest there's problem
		double[] sv = solverNull.getSingularValues();
		Arrays.sort(sv);
		if (sv[1]*singularThreshold <= sv[0]) {
			return GeometricResult.GEOMETRY_POOR;
		}

		foundInA.x = nullspace.get(0);
		foundInA.y = nullspace.get(1);
		foundInA.z = nullspace.get(2);
		foundInA.w = nullspace.get(3);

		return GeometricResult.SUCCESS;
	}

	private int addView( Se3_F64 motion, Point2D_F64 a, int index ) {

		final double sx = stats.stdX, sy = stats.stdY;
//		final double cx = stats.meanX, cy = stats.meanY;

		DMatrixRMaj R = motion.getR();
		Vector3D_F64 T = motion.getT();

		double r11 = R.data[0], r12 = R.data[1], r13 = R.data[2];
		double r21 = R.data[3], r22 = R.data[4], r23 = R.data[5];
		double r31 = R.data[6], r32 = R.data[7], r33 = R.data[8];

		// These rows are derived by applying the scaling matrix to pixels and camera matrix
		// more comments are in the projective code

		// first row
		A.data[index++] = (a.x*r31 - r11)/sx;
		A.data[index++] = (a.x*r32 - r12)/sx;
		A.data[index++] = (a.x*r33 - r13)/sx;
		A.data[index++] = (a.x*T.z - T.x)/sx;

		// second row
		A.data[index++] = (a.y*r31 - r21)/sy;
		A.data[index++] = (a.y*r32 - r22)/sy;
		A.data[index++] = (a.y*r33 - r23)/sy;
		A.data[index++] = (a.y*T.z - T.y)/sy;

		return index;
	}

	/** Add observations of 3d pointing vectors. No need to normalize since the norm is 1 */
	private int addView( Se3_F64 motion, Point3D_F64 a, int index ) {
		DMatrixRMaj R = motion.getR();
		Vector3D_F64 T = motion.getT();

		double r11 = R.data[0], r12 = R.data[1], r13 = R.data[2];
		double r21 = R.data[3], r22 = R.data[4], r23 = R.data[5];
		double r31 = R.data[6], r32 = R.data[7], r33 = R.data[8];

		// first row
		A.data[index++] = (a.x*r31 - a.z*r11);
		A.data[index++] = (a.x*r32 - a.z*r12);
		A.data[index++] = (a.x*r33 - a.z*r13);
		A.data[index++] = (a.x*T.z - a.z*T.x);

		// second row
		A.data[index++] = (a.y*r31 - a.z*r21);
		A.data[index++] = (a.y*r32 - a.z*r22);
		A.data[index++] = (a.y*r33 - a.z*r23);
		A.data[index++] = (a.y*T.z - a.z*T.y);

		return index;
	}
}
