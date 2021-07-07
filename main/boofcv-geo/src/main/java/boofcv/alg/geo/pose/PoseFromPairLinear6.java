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

package boofcv.alg.geo.pose;

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;

import java.util.List;

/**
 * <p>
 * Estimates the camera motion using linear algebra given a set of N associated point observations and the
 * depth (z-coordinate) of each object, where N &ge; 6. Note this is similar to, but not exactly the PnP problem.
 * </p>
 *
 * <p>
 * Output from this class is a rotation and translation that converts a point from the first to second
 * camera's reference frame:</p>
 * {@code X' = R*X+T}<br>
 * where R is a rotation matrix, T is a translation matrix, X is a coordinate in 1st reference frame, and X'
 * in the second.
 *
 * <p>
 * This approach is a modified version of the approach discussed in [1]. It is derived by using
 * bilinear and trilinear constraints, as is discussed in Section 8.3. It has been modified to remove
 * redundant rows and so that the computed rotation matrix is row major. The solution is derived from
 * the equations below and by computing the null space from the resulting matrix:<br>
 * cross(x<sub>2</sub>)*(A*x<sub>1</sub>) + cross(x<sub>2</sub>)*T/&lambda;<sub>i</sub>=0<br>
 * where cross(x) is the cross product matrix of X,  x<sub>i</sub> is the pixel coordinate (normalized or not) in the
 * i<sup>th</sup> image, A is rotation and T translation.
 * </p>
 *
 * <p>
 * [1] Page 279 in "An Invitation to 3-D Vision, From Images to Geometric Models" 1st Ed. 2004. Springer.
 * </p>
 *
 * @author Peter Abeles
 */
public class PoseFromPairLinear6 {

	// The rank 11 linear system
	private DMatrixRMaj A = new DMatrixRMaj(1, 12);

	private SolveNullSpace<DMatrixRMaj> solveNullspace = new SolveNullSpaceSvd_DDRM();

	// Found projective transform
	private DMatrixRMaj P = new DMatrixRMaj(3, 4);

	/**
	 * Computes the transformation between two camera frames using a linear equation. Both the
	 * observed feature locations in each camera image and the depth (z-coordinate) of each feature
	 * must be known. Feature locations are in calibrated image coordinates.
	 *
	 * @param observations List of observations on the image plane in calibrated coordinates.
	 * @param locations List of object locations. One for each observation pair.
	 */
	public boolean process( List<AssociatedPair> observations, List<Point3D_F64> locations ) {
		if (observations.size() != locations.size())
			throw new IllegalArgumentException("Number of observations and locations must match.");

		if (observations.size() < 6)
			throw new IllegalArgumentException("At least (if not more than) six points are required.");

		setupA(observations, locations);

		if (!solveNullspace.process(A, 1, P))
			return false;

		P.numRows = 3;
		P.numCols = 4;

		return true;
	}

	/**
	 * Computes the transformation between two camera frames using a linear equation. Both the
	 * observed feature locations in each camera image and the depth (z-coordinate) of each feature
	 * must be known. Feature locations are in calibrated image coordinates.
	 *
	 * @param observations List of pixel or normalized image coordinate observations
	 * @param locations List of object locations in homogenous coordinates. One for each observation pair.
	 */
	public boolean processHomogenous( List<AssociatedPair> observations, List<Point4D_F64> locations ) {
		if (observations.size() != locations.size())
			throw new IllegalArgumentException("Number of observations and locations must match.");

		if (observations.size() < 6)
			throw new IllegalArgumentException("At least (if not more than) six points are required.");

		setupHomogenousA(observations, locations);

		if (!solveNullspace.process(A, 1, P))
			return false;

		P.numRows = 3;
		P.numCols = 4;

		return true;
	}

	/**
	 * P=[A|T]
	 *
	 * @return projective A
	 */
	public DMatrixRMaj getProjective() {
		return P;
	}

	/**
	 * Matrix used internally.
	 */
	protected DMatrixRMaj getA() {
		return A;
	}

	private void setupA( List<AssociatedPair> observations, List<Point3D_F64> locations ) {
		A.reshape(2*observations.size(), 12, false);

		for (int i = 0; i < observations.size(); i++) {
			AssociatedPair p = observations.get(i);
			Point3D_F64 loc = locations.get(i);

			Point2D_F64 pt1 = p.p1;
			Point2D_F64 pt2 = p.p2;

			// normalize the points
			int w = i*2;

			double alpha = 1.0/loc.z;

			A.set(w, 4, -pt1.x);
			A.set(w, 5, -pt1.y);
			A.set(w, 6, -1);
			A.set(w, 8, pt2.y*pt1.x);
			A.set(w, 9, pt2.y*pt1.y);
			A.set(w, 10, pt2.y);
			A.set(w, 3, 0);
			A.set(w, 7, -alpha);
			A.set(w, 11, alpha*pt2.y);

			w++;

			A.set(w, 0, pt1.x);
			A.set(w, 1, pt1.y);
			A.set(w, 2, 1);
			A.set(w, 8, -pt2.x*pt1.x);
			A.set(w, 9, -pt2.x*pt1.y);
			A.set(w, 10, -pt2.x);
			A.set(w, 3, alpha);
			A.set(w, 7, 0);
			A.set(w, 11, -alpha*pt2.x);
		}
	}

	private void setupHomogenousA( List<AssociatedPair> observations, List<Point4D_F64> locations ) {
		A.reshape(2*observations.size(), 12, false);

		for (int i = 0; i < observations.size(); i++) {
			AssociatedPair p = observations.get(i);
			Point4D_F64 loc = locations.get(i);

			Point2D_F64 pt1 = p.p1;
			Point2D_F64 pt2 = p.p2;

			// normalize the points
			int w = i*2;

			double alpha = loc.w/loc.z;

			A.set(w, 4, -pt1.x);
			A.set(w, 5, -pt1.y);
			A.set(w, 6, -1);
			A.set(w, 8, pt2.y*pt1.x);
			A.set(w, 9, pt2.y*pt1.y);
			A.set(w, 10, pt2.y);
			A.set(w, 3, 0);
			A.set(w, 7, -alpha);
			A.set(w, 11, alpha*pt2.y);

			w++;

			A.set(w, 0, pt1.x);
			A.set(w, 1, pt1.y);
			A.set(w, 2, 1);
			A.set(w, 8, -pt2.x*pt1.x);
			A.set(w, 9, -pt2.x*pt1.y);
			A.set(w, 10, -pt2.x);
			A.set(w, 3, alpha);
			A.set(w, 7, 0);
			A.set(w, 11, -alpha*pt2.x);
		}
	}
}
