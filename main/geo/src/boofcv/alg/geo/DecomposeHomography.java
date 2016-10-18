/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import org.ejml.alg.dense.decomposition.svd.SafeSvd;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

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
 * transform from view 'a' to view 'b'
 * </p>
 *
 * <p>
 * "An Invitation to 3-D Vision" 1st edition page 137.
 * </p>
 *
 * @author Peter Abeles
 */
public class DecomposeHomography {
	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(3, 3, false, true, false);

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

	DenseMatrix64F W1 = new DenseMatrix64F(3,3);
	DenseMatrix64F W2 = new DenseMatrix64F(3,3);
	DenseMatrix64F U1 = new DenseMatrix64F(3,3);
	DenseMatrix64F U2 = new DenseMatrix64F(3,3);

	DenseMatrix64F Hv2 = new DenseMatrix64F(3,3);
	DenseMatrix64F tempM = new DenseMatrix64F(3,3);

	public DecomposeHomography() {
		for( int i = 0; i < 4; i++ ) {
			solutionsN.add( new Vector3D_F64() );
			solutionsSE.add( new Se3_F64() );
		}

		// insure that the inputs are not modified
		svd = new SafeSvd(DecompositionFactory.svd(3, 3, false, true, false));
	}

	/**
	 * Decomposed the provided homography matrix into its R,T/d,N components. Four
	 * solutions will be produced and can be accessed with {@link #getSolutionsN()} and
	 * {@link #getSolutionsSE()}.
	 *
	 * @param H Homography matrix.  Not modified.
	 */
	public void decompose( DenseMatrix64F H ) {
		if( !svd.decompose(H) )
			throw new RuntimeException("SVD failed somehow");

		DenseMatrix64F V = svd.getV(null,false);
		DenseMatrix64F S = svd.getW(null);

		SingularOps.descendingOrder(null,false,S, V,false);

		// COMMENT: The smallest singular value should be zero so I'm not sure why
		// that is not assumed here.  Seen the same strategy done in a few papers
		// Maybe that really isn't always the case?
		double s0 = S.get(0,0)*S.get(0,0);
		// the middle singular value is known to be one
		double s2 = S.get(2,2)*S.get(2,2);

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
	private void setU( DenseMatrix64F U, Vector3D_F64 a , Vector3D_F64 b ) {
		setColumn(U, 0, a);
		setColumn(U, 1, b);

		GeometryMath_F64.cross(a, b, tempV);
		setColumn(U, 2, tempV);
	}

	/**
	 * W=[H*a,H*b,hat(H*a)*H*b]
	 */
	private void setW( DenseMatrix64F W, DenseMatrix64F H , Vector3D_F64 a , Vector3D_F64 b ) {
		GeometryMath_F64.mult(H,b, tempV);
		setColumn(W,1, tempV);

		GeometryMath_F64.mult(H,a, tempV);
		setColumn(W,0, tempV);

		GeometryMath_F64.crossMatrix(tempV,Hv2);
		CommonOps.mult(Hv2,H,tempM);
		GeometryMath_F64.mult(tempM,b, tempV);
		setColumn(W,2, tempV);
	}

	private void setColumn( DenseMatrix64F U , int column , Vector3D_F64 a ) {
		U.set(0, column, a.x);
		U.set(1, column, a.y);
		U.set(2, column, a.z);
	}

	/**
	 * R = W*U^T
	 * N = v2 cross u
	 * (1/d)*T = (H-R)*N
	 */
	private void createSolution( DenseMatrix64F W , DenseMatrix64F U , Vector3D_F64 u ,
								 DenseMatrix64F H ,
								 Se3_F64 se , Vector3D_F64 N )
	{
		CommonOps.multTransB(W,U,se.getR());
		GeometryMath_F64.cross(v2,u,N);

		CommonOps.subtract(H, se.getR(), tempM);
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
