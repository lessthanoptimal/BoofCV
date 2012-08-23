/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.solver;

import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.EigenDecomposition;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the roots of a polynomial using a companion matrix and eigenvalue decomposition.
 *
 * @author Peter Abeles
 */
public class RootFinderCompanion implements PolynomialRootFinder {

	// Companion matrix
	DenseMatrix64F c = new DenseMatrix64F(1,1);

	// use generalized eigenvalue decomposition to find the roots
	EigenDecomposition<DenseMatrix64F> evd =  DecompositionFactory.eig(20, false, false);

	// storage for found roots
	List<Complex64F> roots = new ArrayList<Complex64F>();

	@Override
	public boolean process(double[] coefficients) {
		int N = coefficients.length-1;

		// Companion matrix
		if( c.numCols != N ) {
			c.reshape(N,N);
			c.zero();
		}

		double a = coefficients[N];
		for( int i = 0; i < N; i++ ) {
			c.set(i,N-1,-coefficients[i]/a);
		}
		for( int i = 1; i < N; i++ ) {
			c.set(i,i-1,1);
		}

		if( !evd.decompose(c) )
			return false;

		roots.clear();
		for( int i = 0; i < N; i++ ) {
			roots.add(evd.getEigenvalue(i));
		}

		return true;
	}

	@Override
	public List<Complex64F> getRoots() {
		return roots;
	}
}
