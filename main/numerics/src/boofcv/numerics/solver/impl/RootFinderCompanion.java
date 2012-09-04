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

package boofcv.numerics.solver.impl;

import boofcv.numerics.solver.Polynomial;
import boofcv.numerics.solver.PolynomialOps;
import boofcv.numerics.solver.PolynomialRoots;
import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.EigenDecomposition;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the roots of a polynomial using a companion matrix and eigenvalue decomposition.  Note that the companion
 * matrix is not as stable as people think and will some times fail.  To help improve the situations the found roots
 * are refined.
 *
 * @author Peter Abeles
 */
public class RootFinderCompanion implements PolynomialRoots {

	// Companion matrix
	DenseMatrix64F c = new DenseMatrix64F(1,1);

	// use generalized eigenvalue decomposition to find the roots
	EigenDecomposition<DenseMatrix64F> evd =  DecompositionFactory.eig(11, false, false);

	// storage for found roots
	List<Complex64F> roots = new ArrayList<Complex64F>();


	public RootFinderCompanion() {
	}

	@Override
	public boolean process( Polynomial poly ) {
		int N = poly.size-1;

		while( poly.c[N] == 0.0 && N > 0 )
			N--;

		if( N <= 0)
			return false;

		// Companion matrix
		if( c.numCols != N ) {
			c.reshape(N,N);
			c.zero();
		} else if( evd.inputModified() ) {
			c.zero();
		}

		double a = poly.c[N];
		for( int i = 0; i < N; i++ ) {
			c.set(i,N-1,-poly.c[i]/a);
		}
		for( int i = 1; i < N; i++ ) {
			c.set(i,i-1,1);
		}

		if( !evd.decompose(c) ) {
			return false;
		}

		roots.clear();
		for( int i = 0; i < N; i++ ) {
			Complex64F r = evd.getEigenvalue(i);

			// increase the accuracy of real roots
			if( r.isReal() ) {
				r.real = PolynomialOps.refineRoot(poly, r.real, 30);
			}

			roots.add(r);
		}

		return true;
	}

	@Override
	public List<Complex64F> getRoots() {
		return roots;
	}
}
