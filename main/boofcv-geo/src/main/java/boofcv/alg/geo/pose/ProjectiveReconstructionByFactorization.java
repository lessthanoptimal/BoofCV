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

package boofcv.alg.geo.pose;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
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
 * [                                 ...                 ]  = [ ...  ]
 * [ &lambda;[N,1]*x[1,1] , &lambda;[N,2]*x[1,2] , ... , &lambda;[N,M]*x[N,M] ]  = [ P[M] ]
 * </pre>
 *
 * Procedure:
 * <ol>
 *     <li>Call {@link #initialize} and specify the system's size</li>
 *     <li>Set initial pixel depths</li>
 *     <li>Set pixel observations for each view</li>
 *     <li>Call {@link #process()}</li>
 * </ol>
 *
 * <p>
 * [1] Page 444 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class ProjectiveReconstructionByFactorization {

	// Convergence tolerances
	int maxIterations = 10;
	double minimumChangeTol=1e-6;

	// Depth for each feature in each view. rows = view, cols = features
	DMatrixRMaj depths = new DMatrixRMaj(1,1);
	DMatrixRMaj pixels = new DMatrixRMaj(1,1);

	// Left side of equation = depth*[x,y,1]'
	DMatrixRMaj A = new DMatrixRMaj(1,1);
	DMatrixRMaj B = new DMatrixRMaj(1,1);

	// SVD work spacce
	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(10,10,true,true,false);
	DMatrixRMaj U = new DMatrixRMaj(1,1);
	DMatrixRMaj Vt = new DMatrixRMaj(1,1);

	/**
	 * Initializes internal data structures. Must be called first
	 * @param numFeatures Number of features
	 * @param numViews Number of views
	 */
	public void initialize( int numFeatures , int numViews ) {
		depths.reshape(numViews,numFeatures);
		pixels.reshape(numViews*2,numFeatures);
	}

	/**
	 * Sets pixel observations for a paricular view
	 * @param view the view
	 * @param pixelsInView list of 2D pixel observations
	 */
	public void setPixels(int view , List<Point2D_F64> pixelsInView ) {
		if( pixelsInView.size() != pixels.numCols )
			throw new IllegalArgumentException("Pixel count must be constant and match "+pixels.numCols);

		int row = view*2;
		for (int i = 0; i < pixelsInView.size(); i++) {
			Point2D_F64 p = pixelsInView.get(i);
			pixels.set(row,i,p.x);
			pixels.set(row+1,i,p.y);
		}
	}

	/**
	 * Sets all depths to an initial value
	 * @param value
	 */
	public void setAllDepths( double value ) {
		CommonOps_DDRM.fill(depths,value);
	}

	/**
	 * Sets depths for a particular value to the values in the passed in array
	 * @param view
	 * @param featureDepths
	 */
	public void setDepths( int view , double featureDepths[] ) {
		if( featureDepths.length < depths.numCols )
			throw new IllegalArgumentException("Pixel count must be constant and match "+pixels.numCols);

		int N = depths.numCols;
		for (int i = 0; i < N; i++) {
			depths.set(view,i, featureDepths[i]);
		}
	}

	/**
	 * Assigns depth to the z value of all the features in the list. Features must be in the coordinate system
	 * of the view for this to be correct
	 * @param view which view is features are in
	 * @param locations Location of features in the view's reference frame
	 */
	public void setDepthsFrom3D(int view , List<Point3D_F64> locations ) {
		if( locations.size() != pixels.numCols )
			throw new IllegalArgumentException("Pixel count must be constant and match "+pixels.numCols);

		int N = depths.numCols;
		for (int i = 0; i < N; i++) {
			depths.set(view,i, locations.get(i).z );
		}
	}

	/**
	 * Performs iteration to find camera matrices and feature locations in world frame
	 * @return true if no exception was thrown. Does not mean it converged to a valid solution
	 */
	public boolean process() {
		A.reshape(depths.numRows*3,depths.numCols);
		B.reshape(depths.numRows*3,depths.numCols);

		for (int iter = 0; iter < maxIterations; iter++) {
			assignValuesToA(A);
			if( !svd.decompose(A) )
				return false;

			svd.getU(U,false);
			svd.getV(Vt,true);
			double sv[] = svd.getSingularValues();

			CommonOps_DDRM.multCols(U,sv);

			CommonOps_DDRM.mult(U,Vt,B);

			double delta = SpecializedOps_DDRM.diffNormF(A,B)/(A.numCols*A.numRows);
			// swap arrays for the next iteration
			DMatrixRMaj tmp = A;
			A = B;
			B = tmp;

			// exit if converged
			if( delta <= minimumChangeTol )
				break;
		}

		return true;
	}

	/**
	 * Used to get found projective for a view
	 * @param view Which view
	 * @param P storage for 3x4 projective matrix
	 */
	public void getProjective( int view , DMatrixRMaj P ) {
		P.reshape(3,4);
		CommonOps_DDRM.extract(A,view*3,0,P);
	}

	/**
	 * Returns location of 3D feature for a view
	 * @param feature which feature to retrieve
	 * @param X Storage for 3D feature
	 */
	public void getFeature3D( int feature , Point3D_F64 X ) {
		X.x = Vt.get(0,feature);
		X.y = Vt.get(1,feature);
		X.z = Vt.get(2,feature);
	}

	public void assignValuesToA( DMatrixRMaj A ) {
		for (int viewIdx = 0; viewIdx < depths.numRows; viewIdx++) {
			int rowA = viewIdx*3;
			int rowPixels = viewIdx*2;

			for (int pointIdx = 0; pointIdx < depths.numCols; pointIdx++) {
				double depth = depths.get(viewIdx,pointIdx);

				// pixels are in homogenous coordinates A(:,i) = depth*(x,y,1)
				A.set(rowA,pointIdx, depth*pixels.get(rowPixels,pointIdx));
				A.set(rowA+1,pointIdx, depth*pixels.get(rowPixels+1,pointIdx));
				A.set(rowA+2,pointIdx, depth);
			}
		}
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double getMinimumChangeTol() {
		return minimumChangeTol;
	}

	public void setMinimumChangeTol(double minimumChangeTol) {
		this.minimumChangeTol = minimumChangeTol;
	}
}
