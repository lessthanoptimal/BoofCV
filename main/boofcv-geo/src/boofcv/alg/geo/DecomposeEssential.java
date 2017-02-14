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

import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Decomposed the essential matrix into a rigid body motion; rotation and translation.  This is the rigid body
 * transformation from the first camera frame into the second camera frame.  A total f four possible motions
 * will be found and the ambiguity can be removed by calling {@link PositiveDepthConstraintCheck} on each hypothesis.
 * </p>
 *
 * <p>
 * An essential matrix is defined as E=cross(T)*R, where cross(T) is a cross product matrix,
 * T is translation vector, and R is a 3x3 rotation matrix.  The decomposition works by computing
 * the SVD of E.  For more details see "An Invitation to 3-D Vision" 1st edition page 116.
 * </p>
 *
 * @author Peter Abeles
 */
public class DecomposeEssential {

	private SingularValueDecomposition<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(3, 3, true, true, false);

	// storage for SVD
	DMatrixRMaj U,S,V;

	// storage for the four possible solutions
	List<Se3_F64> solutions = new ArrayList<>();

	// working copy of E
	DMatrixRMaj E_copy = new DMatrixRMaj(3,3);

	// local storage used when computing a hypothesis
	DMatrixRMaj temp = new DMatrixRMaj(3,3);
	DMatrixRMaj temp2 = new DMatrixRMaj(3,3);
	DMatrixRMaj Rz = new DMatrixRMaj(3,3);

	public DecomposeEssential() {
		solutions.add( new Se3_F64());
		solutions.add( new Se3_F64());
		solutions.add( new Se3_F64());
		solutions.add( new Se3_F64());

		Rz.set(0,1,1);
		Rz.set(1,0,-1);
		Rz.set(2,2,1);
	}

	/**
	 * Computes the decomposition from an essential matrix.
	 *
	 * @param E essential matrix
	 */
	public void decompose( DMatrixRMaj E ) {
		if( svd.inputModified() ) {
			E_copy.set(E);
			E = E_copy;
		}

		if( !svd.decompose(E))
			throw new RuntimeException("Svd some how failed");

		U = svd.getU(U,false);
		V = svd.getV(V,false);
		S = svd.getW(S);

		SingularOps_DDRM.descendingOrder(U,false,S,V,false);

		decompose(U, S, V);
	}

	/**
	 * Compute the decomposition given the SVD of E=U*S*V<sup>T</sup>.
	 *
	 * @param U Orthogonal matrix from SVD.
	 * @param S Diagonal matrix containing singular values from SVD.
	 * @param V Orthogonal matrix from SVD.
	 */
	public void decompose( DMatrixRMaj U , DMatrixRMaj S , DMatrixRMaj V ) {
		// this ensures the resulting rotation matrix will have a determinant of +1 and thus be a real rotation matrix
		if( CommonOps_DDRM.det(U) < 0 ) {
			CommonOps_DDRM.scale(-1,U);
			CommonOps_DDRM.scale(-1,S);
		}

		if( CommonOps_DDRM.det(V) < 0 ) {
			CommonOps_DDRM.scale(-1,V);
			CommonOps_DDRM.scale(-1,S);
		}

		// for possible solutions due to ambiguity in the sign of T and rotation
		extractTransform(U, V, S, solutions.get(0), true, true);
		extractTransform(U, V, S, solutions.get(1), true, false);
		extractTransform(U, V, S, solutions.get(2) , false,false);
		extractTransform(U, V, S, solutions.get(3), false, true);
	}

	/**
	 * <p>
	 * Returns the four possible solutions found in the decomposition.  The returned motions go from the
	 * first into the second camera frame.
	 * </p>
	 *
	 * <p>
	 * WARNING: This list is modified on each call to decompose.  Create a copy of any
	 * solution that needs to be saved.
	 * </p>
	 *
	 * @return Four possible solutions to the decomposition
	 */
	public List<Se3_F64> getSolutions() {
		return solutions;
	}

	/**
	 * There are four possible reconstructions from an essential matrix.  This function will compute different
	 * permutations depending on optionA and optionB being true or false.
	 */
	private void extractTransform( DMatrixRMaj U , DMatrixRMaj V , DMatrixRMaj S ,
								   Se3_F64 se , boolean optionA , boolean optionB )
	{
		DMatrixRMaj R = se.getR();
		Vector3D_F64 T = se.getT();

		// extract rotation
		if( optionA )
			CommonOps_DDRM.mult(U,Rz,temp);
		else
			CommonOps_DDRM.multTransB(U,Rz,temp);
		CommonOps_DDRM.multTransB(temp,V,R);

		// extract screw symmetric translation matrix
		if( optionB )
			CommonOps_DDRM.multTransB(U,Rz,temp);
		else
			CommonOps_DDRM.mult(U,Rz,temp);
		CommonOps_DDRM.mult(temp,S,temp2);
		CommonOps_DDRM.multTransB(temp2,U,temp);

		T.x = temp.get(2,1);
		T.y = temp.get(0,2);
		T.z = temp.get(1,0);
	}

}
