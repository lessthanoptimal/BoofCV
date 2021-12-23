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

package boofcv.alg.geo.structure;

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.misc.BoofLambdas;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Given a set of homographies mapping pixels in view i to view 0 this will estimate
 * the projective camera matrix of each view. x' = P*X, where P is the camera matrix that
 * is to be estimated, x' is pixel, and X is 3D feature location in view 0's reference frame.
 * Output is 3D structure and camera matrices.
 * </p>
 *
 * <p>
 * A camera matrix is a 3x4 projective matrix with the following structure [M,t]. If a homography defines the
 * transform between pixels in one view to another view then M is equal to that homography. The camera matrix
 * is now defined up to the unknown t. To solve for t the points in this projective frame also need to be
 * derived. This is done by solving a linear system:
 * </p>
 * <pre>
 *     [ p.x*m'[2] - m'[0] , -1 , 0  , p.x ]
 *     [ p.y*m'[2] - m'[1] ,  0 , -1 , p.y ] * [X ; t ]
 * </pre>
 * where m'[i] is row 'i' in M transposed. (p.x,p.y) is pixel observation, X and t are the unknowns. Each
 * observation x = P*X generates 2 equations. See [1] for derivation. Vector product of both sides is involved.
 *
 * <p>
 * Normalization is automatically applied to inputs and undo for output. Normalization matrix is computed using
 * {@link LowLevelMultiViewOps#computeNormalizationLL(List, BoofLambdas.ConvertOut, NormalizationPoint2D)}
 * </p>
 *
 * <p>
 * [1] 18.5.1 Page 448 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ProjectiveStructureFromHomographies {

	// used for null space computation
	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(false, true, true);

	// list of points after points that are on plane at infinity have been removed
	List<List<PointIndex2D_F64>> filtered = new ArrayList<>();

	// reference to input homographies
	List<DMatrixRMaj> homographies;

	// storage for linear system being solved
	DMatrixRMaj A = new DMatrixRMaj(1, 1);
	DMatrixRMaj B = new DMatrixRMaj(1, 1);

	// work space
	Point3D_F64 tmp = new Point3D_F64();

	// normalize pixel coordinates
	NormalizationPoint2D N = new NormalizationPoint2D();

	// Threshold used to see if a point lines on the plane at infinity
	double infinityThreshold = UtilEjml.EPS;

	int numViews; // number of views
	int numEquations; // number of equations
	int numUnknown; // number of unknowns being solved for
	int totalFeatures;

	// Define a lambda here since it's not clear if new instances are created or not
	BoofLambdas.ConvertOut<PointIndex2D_F64, Point2D_F64> convert = ( pi ) -> pi.p;

	/**
	 * <p>Solves for camera matrices and scene structure.</p>
	 *
	 * Homographies from view i to 0:<br>
	 * x[0] = H*x[i]
	 *
	 * @param homographies_view0_to_viewI (Input) Homographies matching pixels from view i to view 0.
	 * @param observations (Input) Observed features in each view, except view 0. Indexes of points must be from 0 to totalFeatures-1
	 * @param totalFeatures (Input) total number of features being solved for. Uses to sanity check input
	 * @return true if successful or false if it failed
	 */
	public boolean proccess( List<DMatrixRMaj> homographies_view0_to_viewI,
							 List<List<PointIndex2D_F64>> observations,
							 int totalFeatures ) {
		if (homographies_view0_to_viewI.size() != observations.size()) {
			throw new IllegalArgumentException("Number of homographies and observations do not match");
		}

		LowLevelMultiViewOps.computeNormalizationLL(observations, convert, N);

		// Apply normalization to homographies
		this.homographies = homographies_view0_to_viewI;
		filterPointsOnPlaneAtInfinity(homographies, observations, totalFeatures);

		// compute some internal working variables and determine if there are enough observations to compute a
		// solution
		computeConstants(homographies_view0_to_viewI, filtered, totalFeatures);

		// Solve the problem
		constructLinearSystem(homographies, filtered);

		if (!svd.decompose(A))
			return false;

		// get null vector. camera matrices and scene structure are extracted from B as requested
		SingularOps_DDRM.nullVector(svd, true, B);

		return true;
	}

	void computeConstants( List<DMatrixRMaj> homographies_view0_to_viewI,
						   List<List<PointIndex2D_F64>> observations,
						   int totalFeatures ) {
		int totalObservations = 0;
		for (int i = 0; i < observations.size(); i++) {
			totalObservations += observations.get(i).size();
		}

		this.totalFeatures = totalFeatures;
		numViews = homographies_view0_to_viewI.size();
		numEquations = 2*totalObservations;
		numUnknown = 3*totalFeatures + 3*numViews;
	}

	/**
	 * Constructs the linear systems. Unknowns are sorted in index order. structure (3D points) are first followed
	 * by unknown t from projective.
	 */
	void constructLinearSystem( List<DMatrixRMaj> homographies,
								List<List<PointIndex2D_F64>> observations ) {
		// parameters are encoded points first then the
		int startView = totalFeatures*3;

		A.reshape(numEquations, numUnknown);

		DMatrixRMaj H = new DMatrixRMaj(3, 3);
		Point2D_F64 p = new Point2D_F64();

		int row = 0;
		for (int viewIdx = 0; viewIdx < homographies.size(); viewIdx++) {
			N.apply(homographies.get(viewIdx), H);

			int colView = startView + viewIdx*3;

			List<PointIndex2D_F64> obs = observations.get(viewIdx);
			for (int i = 0; i < obs.size(); i++) {
				PointIndex2D_F64 p_pixel = obs.get(i);

				N.apply(p_pixel.p, p);

				// column this feature is at
				int col = p_pixel.index*3;

				// x component of pixel
				// A(row,colView) =  ...
				A.data[row*A.numCols + col] = p.x*H.get(2, 0) - H.get(0, 0);
				A.data[row*A.numCols + col + 1] = p.x*H.get(2, 1) - H.get(0, 1);
				A.data[row*A.numCols + col + 2] = p.x*H.get(2, 2) - H.get(0, 2);

				A.data[row*A.numCols + colView] = -1;
				A.data[row*A.numCols + colView + 1] = 0;
				A.data[row*A.numCols + colView + 2] = p.x;

				// y component of pixel
				row += 1;
				A.data[row*A.numCols + col] = p.y*H.get(2, 0) - H.get(1, 0);
				A.data[row*A.numCols + col + 1] = p.y*H.get(2, 1) - H.get(1, 1);
				A.data[row*A.numCols + col + 2] = p.y*H.get(2, 2) - H.get(1, 2);

				A.data[row*A.numCols + colView] = 0;
				A.data[row*A.numCols + colView + 1] = -1;
				A.data[row*A.numCols + colView + 2] = p.y;

				row += 1;
			}
		}
	}

	/**
	 * Identifies points which lie on the plane at infinity. That is done by computing x' = H*x and seeing if the w
	 * term is nearly zero, e.g. x' = (x,y,w)
	 */
	void filterPointsOnPlaneAtInfinity( List<DMatrixRMaj> homographies_view1_to_view0,
										List<List<PointIndex2D_F64>> observations,
										int totalFeatures ) {
		filtered.clear();
		for (int viewIdx = 0; viewIdx < homographies_view1_to_view0.size(); viewIdx++) {
			List<PointIndex2D_F64> filter = new ArrayList<>();
			filtered.add(filter);
			DMatrixRMaj H = homographies_view1_to_view0.get(viewIdx);

			List<PointIndex2D_F64> obs = observations.get(viewIdx);
			for (int i = 0; i < obs.size(); i++) {
				PointIndex2D_F64 p = obs.get(i);

				if (p.index < 0 || p.index >= totalFeatures)
					throw new IllegalArgumentException("Feature index outside of bounds. Must be from 0 to " + (totalFeatures - 1));

				GeometryMath_F64.mult(H, p.p, tmp);
				// Homogenous coordinates are scale invariant. A scale
				// needs to be picked for consistency. I picked the largest x or y value
				double m = Math.max(Math.abs(tmp.x), Math.abs(tmp.y));
				if (m == 0) m = 1;
				tmp.z /= m;

				// See if it's zero or almost zero, meaning it's on the plane at infinity
				if (Math.abs(tmp.z) > infinityThreshold) {
					filter.add(p);
				}
			}
		}
	}

	public void getCameraMatrix( int viewIdx, DMatrixRMaj P ) {
		DMatrixRMaj H = homographies.get(viewIdx);

		int row = totalFeatures*3 + viewIdx*3;

		tmp.x = B.unsafe_get(row, 0);
		tmp.y = B.unsafe_get(row + 1, 0);
		tmp.z = B.unsafe_get(row + 2, 0);

		N.remove(tmp, tmp);

		CommonOps_DDRM.insert(H, P, 0, 0);
		P.set(0, 3, tmp.x);
		P.set(1, 3, tmp.y);
		P.set(2, 3, tmp.z);
	}

	public void getFeature3D( int featureIdx, Point3D_F64 X ) {
		int row = featureIdx*3;

		X.x = B.unsafe_get(row, 0);
		X.y = B.unsafe_get(row + 1, 0);
		X.z = B.unsafe_get(row + 2, 0);
	}

	public double getInfinityThreshold() {
		return infinityThreshold;
	}

	public void setInfinityThreshold( double infinityThreshold ) {
		this.infinityThreshold = infinityThreshold;
	}
}
