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

import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

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

	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(3, 3, true, true, false);

	// storage for SVD
	DenseMatrix64F U,S,V;

	// storage for the four possible solutions
	List<Se3_F64> solutions = new ArrayList<>();

	// working copy of E
	DenseMatrix64F E_copy = new DenseMatrix64F(3,3);

	// local storage used when computing a hypothesis
	DenseMatrix64F temp = new DenseMatrix64F(3,3);
	DenseMatrix64F temp2 = new DenseMatrix64F(3,3);
	DenseMatrix64F Rz = new DenseMatrix64F(3,3);

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
	public void decompose( DenseMatrix64F E ) {
		if( svd.inputModified() ) {
			E_copy.set(E);
			E = E_copy;
		}

		if( !svd.decompose(E))
			throw new RuntimeException("Svd some how failed");

		U = svd.getU(U,false);
		V = svd.getV(V,false);
		S = svd.getW(S);

		SingularOps.descendingOrder(U,false,S,V,false);

		decompose(U, S, V);
	}

	/**
	 * Compute the decomposition given the SVD of E=U*S*V<sup>T</sup>.
	 *
	 * @param U Orthogonal matrix from SVD.
	 * @param S Diagonal matrix containing singular values from SVD.
	 * @param V Orthogonal matrix from SVD.
	 */
	public void decompose( DenseMatrix64F U , DenseMatrix64F S , DenseMatrix64F V ) {
		// this ensures the resulting rotation matrix will have a determinant of +1 and thus be a real rotation matrix
		if( CommonOps.det(U) < 0 ) {
			CommonOps.scale(-1,U);
			CommonOps.scale(-1,S);
		}

		if( CommonOps.det(V) < 0 ) {
			CommonOps.scale(-1,V);
			CommonOps.scale(-1,S);
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
	private void extractTransform( DenseMatrix64F U , DenseMatrix64F V , DenseMatrix64F S ,
								   Se3_F64 se , boolean optionA , boolean optionB )
	{
		DenseMatrix64F R = se.getR();
		Vector3D_F64 T = se.getT();

		// extract rotation
		if( optionA )
			CommonOps.mult(U,Rz,temp);
		else
			CommonOps.multTransB(U,Rz,temp);
		CommonOps.multTransB(temp,V,R);

		// extract screw symmetric translation matrix
		if( optionB )
			CommonOps.multTransB(U,Rz,temp);
		else
			CommonOps.mult(U,Rz,temp);
		CommonOps.mult(temp,S,temp2);
		CommonOps.multTransB(temp2,U,temp);

		T.x = temp.get(2,1);
		T.y = temp.get(0,2);
		T.z = temp.get(1,0);
	}

}
