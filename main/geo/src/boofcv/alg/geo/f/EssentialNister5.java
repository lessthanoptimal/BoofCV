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

package boofcv.alg.geo.f;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.SingularValueDecomposition;
import org.ejml.ops.SingularOps;

import java.util.List;

/**
 * <p>
 * Finds the essential matrix given 5 or more corresponding points.  The approach is described
 * in details in [1] and works by linearlizing the problem then solving for the roots in a polynomial.  It
 * is considered one of the fastest and most stable solutions for this problem.
 * </p>
 *
 * <p>
 * THIS IMPLEMENTATION DOES NOT CONTAIN ALL THE OPTIMIZATIONS OUTLIED IN [1].  A full implementation is
 * quite involved.
 * </p>
 *
 * <p>
 * [1] David Nister "An Efficient Solution to the Five-Point Relative Pose Problem"
 * Pattern Analysis and Machine Intelligence, 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class EssentialNister5 {

	// Linear system describing p'*E*q = 0
	DenseMatrix64F A = new DenseMatrix64F(5,9);
	// contains the span of A
	DenseMatrix64F V = new DenseMatrix64F(9,9);
	// TODO use QR-Factorization as in the paper
	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(5,9,false,true,false);

	// the span containing E
	double []X = new double[9];
	double []Y = new double[9];
	double []Z = new double[9];
	double []W = new double[9];


	// found essential matrix
	DenseMatrix64F E = new DenseMatrix64F(3,3);

	/**
	 * Computes the essential matrix from point correspondences.
	 *
	 * @param points List of points correspondences in normalized image coordinates.
	 * @return true for success or false if a fault has been detected
	 */
	public boolean process( List<AssociatedPair> points ) {
		if( points.size() < 5 )
			throw new IllegalArgumentException("A minimum of five points are required");

		// Computes the 4-vector span which contains E.  See equation 7-9
		computeSpan(points);


		return true;
	}

	/**
	 * From the epipolar constraint p2^T*E*p1 = 0 construct a linear system
	 * and find its null space.
	 */
	private void computeSpan( List<AssociatedPair> points ) {

		A.reshape(points.size(),9);
		int index = 0;

		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);

			Point2D_F64 a = p.currLoc;
			Point2D_F64 b = p.keyLoc;

			// The points are assumed to be in homogeneous coordinates.  This means z = 1
			A.data[index++] =  a.x*b.x;
			A.data[index++] =  a.y*b.x;
			A.data[index++] =      b.x;
			A.data[index++] =  a.x*b.y;
			A.data[index++] =  a.y*b.y;
			A.data[index++] =      b.y;
			A.data[index++] =  a.x;
			A.data[index++] =  a.y;
			A.data[index++] =  1;
		}

		if( !svd.decompose(A) )
			throw new RuntimeException("SVD should never fail, probably bad input");

		svd.getV(V,true);

		// Order singular values if needed
		if( points.size() > 5 ) {
			double s[] = svd.getSingularValues();
			svd.getV(V,true);
			SingularOps.descendingOrder(null,false,s,svd.numberOfSingularValues(),V,true);
		}

		// extract the spam of solutions for E
		for( int i = 0; i < 9; i++ ) {
			X[i] = V.unsafe_get(5,i);
			Y[i] = V.unsafe_get(6,i);
			Z[i] = V.unsafe_get(7,i);
			W[i] = V.unsafe_get(8,i);
		}
	}
}
