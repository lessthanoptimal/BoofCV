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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.SpecializedOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.List;

/**
 * Performs projective reconstruction via factorization. For every view all points are observed. The algorithm
 * works by iteratively estimating the depth of each point in every view and this results in better and better
 * estimates of the projective camera matrices and the points being found. An initial estimate of feature
 * depth can be provided. Unfortunately, there is no grantee of this method converging to a valid solution.
 *
 * <pre>
 * [ &lambda;[1,1]*x[1,1] , &lambda;[1,2]*x[1,2] , ... , &lambda;[1,M]*x[1,M] ]  = [ P[1] ] * [X[1], X[2], ... , X[N]
 * [ &lambda;[2,1]*x[1,1] , &lambda;[2,2]*x[1,2] , ... , &lambda;[2,M]*x[2,M] ]  = [ P[2] ]
 * [                                 ...               ]  = [ ... ]
 * [ &lambda;[N,1]*x[1,1] , &lambda;[N,2]*x[1,2] , ... , &lambda;[N,M]*x[N,M] ]  = [ P[M] ]
 * </pre>
 * where &lambda; is the depth, x is homogenous pixel coordinate, P is 3x4 projective, X is 3D feature location in
 * world coordinate system.
 *
 * Procedure:
 * <ol>
 *     <li>Call {@link #initialize} and specify the system's size</li>
 *     <li>Set initial pixel depths</li>
 *     <li>Set pixel observations for each view</li>
 *     <li>Call {@link #process()}</li>
 *     <li>Get results with {@link #getFeature3D} and {@link #getCameraMatrix}</li>
 * </ol>
 *
 * <p>
 *     Internally depth and pixel values are scaled so that they are close to unity then undo for output. This ensures
 *     better approximation of errors and has other desirable numerical properties.
 * </p>
 *
 * <p>
 * [1] Page 444 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class ProjectiveStructureByFactorization {

	// Convergence tolerances
	int maxIterations = 10;
	double minimumChangeTol = 1e-6;

	// Depth for each feature in each view. rows = view, cols = features
	DMatrixRMaj depths = new DMatrixRMaj(1, 1);
	DMatrixRMaj pixels = new DMatrixRMaj(1, 1);
	// used to improve numerics. Pixel coordinate should be of an oder of magnitude of 1
	double pixelScale; // See discussion in 18.4.4. By scaling pixels the algebraic error is closer to geometric

	// Left side of equation = depth*[x,y,1]'
	DMatrixRMaj A = new DMatrixRMaj(1, 1);
	DMatrixRMaj B = new DMatrixRMaj(1, 1);
	// matrix which stores projections
	DMatrixRMaj P = new DMatrixRMaj(1, 4);
	// matrix which stores the points
	DMatrixRMaj X = new DMatrixRMaj(3, 1);

	// SVD work spacce
	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(10, 10, true, true, true);
	DMatrixRMaj U = new DMatrixRMaj(1, 1);
	DMatrixRMaj Vt = new DMatrixRMaj(1, 1);

	/**
	 * Initializes internal data structures. Must be called first
	 *
	 * @param numFeatures Number of features
	 * @param numViews Number of views
	 */
	public void initialize( int numFeatures, int numViews ) {
		depths.reshape(numViews, numFeatures);
		pixels.reshape(numViews*2, numFeatures);
		pixelScale = 0;
	}

	/**
	 * Sets pixel observations for a paricular view
	 *
	 * @param view the view
	 * @param pixelsInView list of 2D pixel observations
	 */
	public void setPixels( int view, List<Point2D_F64> pixelsInView ) {
		if (pixelsInView.size() != pixels.numCols)
			throw new IllegalArgumentException("Pixel count must be constant and match " + pixels.numCols);

		int row = view*2;
		for (int i = 0; i < pixelsInView.size(); i++) {
			Point2D_F64 p = pixelsInView.get(i);
			pixels.set(row, i, p.x);
			pixels.set(row + 1, i, p.y);
			pixelScale = Math.max(Math.abs(p.x), Math.abs(p.y));
		}
	}

	/**
	 * Sets all depths to an initial value
	 */
	public void setAllDepths( double value ) {
		CommonOps_DDRM.fill(depths, value);
	}

	/**
	 * Sets depths for a particular value to the values in the passed in array
	 */
	public void setDepths( int view, double featureDepths[] ) {
		if (featureDepths.length < depths.numCols)
			throw new IllegalArgumentException("Pixel count must be constant and match " + pixels.numCols);

		int N = depths.numCols;
		for (int i = 0; i < N; i++) {
			depths.set(view, i, featureDepths[i]);
		}
	}

	/**
	 * Assigns depth to the z value of all the features in the list. Features must be in the coordinate system
	 * of the view for this to be correct
	 *
	 * @param view which view is features are in
	 * @param locations Location of features in the view's reference frame
	 */
	public void setDepthsFrom3D( int view, List<Point3D_F64> locations ) {
		if (locations.size() != pixels.numCols)
			throw new IllegalArgumentException("Pixel count must be constant and match " + pixels.numCols);

		int N = depths.numCols;
		for (int i = 0; i < N; i++) {
			depths.set(view, i, locations.get(i).z);
		}
	}

	/**
	 * Performs iteration to find camera matrices and feature locations in world frame
	 *
	 * @return true if no exception was thrown. Does not mean it converged to a valid solution
	 */
	public boolean process() {
		int numViews = depths.numRows;
		int numFeatures = depths.numCols;
		P.reshape(3*numViews, 4);
		X.reshape(4, numFeatures);

		A.reshape(numViews*3, numFeatures);
		B.reshape(numViews*3, numFeatures);

		// Scale depths so that they are close to unity
		normalizeDepths(depths);

		// Compute the initial A matirx
		assignValuesToA(A);

		for (int iter = 0; iter < maxIterations; iter++) {
			if (!svd.decompose(A))
				return false;

			svd.getU(U, false);
			svd.getV(Vt, true);
			double sv[] = svd.getSingularValues();

			SingularOps_DDRM.descendingOrder(U, false, sv, A.numCols, Vt, true);

			// This is equivalent to forcing the rank to be 4
			CommonOps_DDRM.extract(U, 0, 0, P);
			CommonOps_DDRM.multCols(P, sv);
			CommonOps_DDRM.extract(Vt, 0, 0, X);

			// Compute the new value of A
			CommonOps_DDRM.mult(P, X, B);

			// See how much change there is
			double delta = SpecializedOps_DDRM.diffNormF(A, B)/(A.numCols*A.numRows);

			// swap arrays for the next iteration
			DMatrixRMaj tmp = A;
			A = B;
			B = tmp;

			// exit if converged
			if (delta <= minimumChangeTol)
				break;
		}

		return true;
	}

	/**
	 * Used to get found camera matrix for a view
	 *
	 * @param view Which view
	 * @param cameraMatrix storage for 3x4 projective camera matrix
	 */
	public void getCameraMatrix( int view, DMatrixRMaj cameraMatrix ) {
		cameraMatrix.reshape(3, 4);
		CommonOps_DDRM.extract(P, view*3, 0, cameraMatrix);

		for (int col = 0; col < 4; col++) {
			cameraMatrix.data[cameraMatrix.getIndex(0, col)] *= pixelScale;
			cameraMatrix.data[cameraMatrix.getIndex(1, col)] *= pixelScale;
		}
	}

	/**
	 * Returns location of 3D feature for a view
	 *
	 * @param feature Index of feature to retrieve
	 * @param out (Output) Storage for 3D feature. homogenous coordinates
	 */
	public void getFeature3D( int feature, Point4D_F64 out ) {
		out.x = X.get(0, feature);
		out.y = X.get(1, feature);
		out.z = X.get(2, feature);
		out.w = X.get(3, feature);
	}

	/**
	 * A[:,0] = depth*[x,y,1]'
	 */
	public void assignValuesToA( DMatrixRMaj A ) {
		for (int viewIdx = 0; viewIdx < depths.numRows; viewIdx++) {
			int rowA = viewIdx*3;
			int rowPixels = viewIdx*2;

			for (int pointIdx = 0; pointIdx < depths.numCols; pointIdx++) {
				double depth = depths.get(viewIdx, pointIdx);

				// pixels are in homogenous coordinates A(:,i) = depth*(x,y,1)
				A.set(rowA, pointIdx, depth*pixels.get(rowPixels, pointIdx)/pixelScale);
				A.set(rowA + 1, pointIdx, depth*pixels.get(rowPixels + 1, pointIdx)/pixelScale);
				A.set(rowA + 2, pointIdx, depth);
			}
		}
	}

	/**
	 * <p>Rescale A so that its rows and columns have a value of approximately 1. This is done to try to get
	 * everything set to unity for desirable numerical properties</p>
	 *
	 * (&alpha; &beta; &lambda;) x = (&alpha;P)(&beta;X)
	 */
	public void normalizeDepths( DMatrixRMaj depths ) {
		// normalize rows first
		for (int row = 0; row < depths.numRows; row++) {
			int idx = row*depths.numCols;
			double sum = 0;
			for (int col = 0; col < depths.numCols; col++) {
				double v = depths.data[idx++];
				sum += v*v;
			}
			double norm = Math.sqrt(sum)/depths.numCols;
			idx = row*depths.numCols;
			for (int j = 0; j < depths.numCols; j++) {
				depths.data[idx++] /= norm;
			}
		}

		// normalize columns
		for (int col = 0; col < depths.numCols; col++) {
			double norm = 0;
			for (int row = 0; row < depths.numRows; row++) {
				double v = depths.get(row, col);
				norm += v*v;
			}
			norm = Math.sqrt(norm);
			for (int row = 0; row < depths.numRows; row++) {
				depths.data[depths.getIndex(row, col)] /= norm;
			}
		}
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations( int maxIterations ) {
		this.maxIterations = maxIterations;
	}

	public double getMinimumChangeTol() {
		return minimumChangeTol;
	}

	public void setMinimumChangeTol( double minimumChangeTol ) {
		this.minimumChangeTol = minimumChangeTol;
	}
}
