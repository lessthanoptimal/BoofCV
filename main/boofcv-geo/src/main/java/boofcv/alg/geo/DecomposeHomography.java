/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.decomposition.svd.SafeSvd_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Decomposes a homography matrix to extract its internal geometric structure.  There are four possible solutions,
 * with two that are physically possible.  The physically possible solution can be found by imposing a positive
 * depth constraint.  See {@link PositiveDepthConstraintCheck} for details on how to do that.
 * </p>
 *
 * <p>
 * A homography matrix is defined as H = (R + (1/d)*T*N<sup>T</sup>), where R is a 3x3 rotation matrix,
 * d is the distance of the plane, N is the plane's normal, T is the translation vector.  The decomposition
 * works by computing the SVD of H<sup>T</sup>H and the following the procedure outlines in [1].
 * </p>
 *
 * <p>
 * The input homography is assumed to be from view 'a' to view 'b'.  Then the resulting transform (R,T) is the
 * transform from view 'a' to view 'b'. The values of d and N are all from the view 'a' perspective. Since there's a
 * scale ambiguity the value of d is assumed to be 1 and T is scaled appropriately.
 * </p>
 *
 * <p>
 * "An Invitation to 3-D Vision" 1st edition page 137.
 * </p>
 *
 * @author Peter Abeles
 */
public class DecomposeHomography {
	private SingularValueDecomposition<DMatrixRMaj> svd;

	// storage for the four possible solutions
	// Camera motion part of the solution
	List<Se3_F64> solutionsSE = new ArrayList<>();
	// normal of the plane
	List<Vector3D_F64> solutionsN = new ArrayList<>();

	// used for internal house keeping during decomposition
	Vector3D_F64 u1 = new Vector3D_F64();
	Vector3D_F64 u2 = new Vector3D_F64();
	Vector3D_F64 v2 = new Vector3D_F64();

	Vector3D_F64 tempV = new Vector3D_F64();

	DMatrixRMaj W1 = new DMatrixRMaj(3,3);
	DMatrixRMaj W2 = new DMatrixRMaj(3,3);
	DMatrixRMaj U1 = new DMatrixRMaj(3,3);
	DMatrixRMaj U2 = new DMatrixRMaj(3,3);

	DMatrixRMaj Hv2 = new DMatrixRMaj(3,3);
	DMatrixRMaj tempM = new DMatrixRMaj(3,3);

	// workspace for H
	DMatrixRMaj H = new DMatrixRMaj(3,3);

	public DecomposeHomography() {
		for( int i = 0; i < 4; i++ ) {
			solutionsN.add( new Vector3D_F64() );
			solutionsSE.add( new Se3_F64() );
		}

		// insure that the inputs are not modified
		svd = new SafeSvd_DDRM(DecompositionFactory_DDRM.svd(3, 3, false, true, false));
	}

	/**
	 * Decomposed the provided homography matrix into its R,T/d,N components. Four
	 * solutions will be produced and can be accessed with {@link #getSolutionsN()} and
	 * {@link #getSolutionsSE()}.
	 *
	 * @param homography Homography matrix.  Not modified.
	 */
	public void decompose( DMatrixRMaj homography ) {
		if( !svd.decompose(homography) )
			throw new RuntimeException("SVD failed somehow");

		DMatrixRMaj V = svd.getV(null,false);
		DMatrixRMaj S = svd.getW(null);

		SingularOps_DDRM.descendingOrder(null,false,S, V,false);

		double s0 = S.get(0,0);
		double s1 = S.get(1,1); // This is the scale. If normalize it will be one
		double s2 = S.get(2,2);

		// force s1 to be 1
		s0 /= s1;
		s2 /= s1;
		CommonOps_DDRM.scale(1.0/s1,homography,this.H);

		// square s0 and s1 for other parts of the calculations
		s0 *= s0;
		s2 *= s2;

		v2.set(V.get(0,1),V.get(1,1),V.get(2,1));

		double a = Math.sqrt(1-s2);
		double b = Math.sqrt(s0-1);
		double div = Math.sqrt(s0-s2);

		for( int i = 0; i < 3; i++ ) {
			double e1 = (a*V.get(i,0) + b*V.get(i,2))/div;
			double e2 = (a*V.get(i,0) - b*V.get(i,2))/div;

			u1.setIndex(i,e1);
			u2.setIndex(i,e2);
		}

		setU(U1, v2, u1);
		setU(U2, v2, u2);
		setW(W1, H , v2, u1);
		setW(W2, H , v2, u2);

		// create the four solutions
		createSolution(W1, U1, u1,H,solutionsSE.get(0), solutionsN.get(0));
		createSolution(W2, U2, u2,H,solutionsSE.get(1), solutionsN.get(1));
		createMirrorSolution(0,2);
		createMirrorSolution(1,3);
	}

	/**
	 * <p>
	 * Returns the camera motion part of the solution. Note that se.T=(1/d)T
	 * </p>
	 * <p>
	 * WARNING: Data is reused each time decompose is called.
	 * </p>
	 * @return Set of rigid body camera motions
	 */
	public List<Se3_F64> getSolutionsSE() {
		return solutionsSE;
	}

	/**
	 * <P>
	 * Returns the normal of the plane part of the solution
	 * </p>
	 * <p>
	 * WARNING: Data is reused each time decompose is called.
	 * </p>
	 * @return Set of plane normals
	 */
	public List<Vector3D_F64> getSolutionsN() {
		return solutionsN;
	}

	/**
	 * U=[a,b,hat(a)*b]
	 */
	private void setU( DMatrixRMaj U, Vector3D_F64 a , Vector3D_F64 b ) {
		setColumn(U, 0, a);
		setColumn(U, 1, b);

		GeometryMath_F64.cross(a, b, tempV);
		setColumn(U, 2, tempV);
	}

	/**
	 * W=[H*a,H*b,hat(H*a)*H*b]
	 */
	private void setW( DMatrixRMaj W, DMatrixRMaj H , Vector3D_F64 a , Vector3D_F64 b ) {
		GeometryMath_F64.mult(H,b, tempV);
		setColumn(W,1, tempV);

		GeometryMath_F64.mult(H,a, tempV);
		setColumn(W,0, tempV);

		GeometryMath_F64.crossMatrix(tempV,Hv2);
		CommonOps_DDRM.mult(Hv2,H,tempM);
		GeometryMath_F64.mult(tempM,b, tempV);
		setColumn(W,2, tempV);
	}

	private void setColumn( DMatrixRMaj U , int column , Vector3D_F64 a ) {
		U.set(0, column, a.x);
		U.set(1, column, a.y);
		U.set(2, column, a.z);
	}

	/**
	 * R = W*U^T
	 * N = v2 cross u
	 * (1/d)*T = (H-R)*N
	 */
	private void createSolution( DMatrixRMaj W , DMatrixRMaj U , Vector3D_F64 u ,
								 DMatrixRMaj H ,
								 Se3_F64 se , Vector3D_F64 N )
	{
		CommonOps_DDRM.multTransB(W,U,se.getR());
		GeometryMath_F64.cross(v2,u,N);

		CommonOps_DDRM.subtract(H, se.getR(), tempM);
		GeometryMath_F64.mult(tempM,N,se.getT());
	}

	private void createMirrorSolution( int origIndex , int index  )
	{
		Se3_F64 origSE = solutionsSE.get(origIndex);
		Vector3D_F64 origN = solutionsN.get(origIndex);

		Se3_F64 se = solutionsSE.get(index);
		Vector3D_F64 N = solutionsN.get(index);

		se.getR().set(origSE.getR());
		N.x = -origN.x;
		N.y = -origN.y;
		N.z = -origN.z;

		Vector3D_F64 origT = origSE.getT();
		Vector3D_F64 T = se.getT();

		T.x = -origT.x;
		T.y = -origT.y;
		T.z = -origT.z;
	}
}
