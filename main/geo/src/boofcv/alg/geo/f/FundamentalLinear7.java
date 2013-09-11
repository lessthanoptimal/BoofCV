/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.solver.Polynomial;
import org.ddogleg.solver.PolynomialRoots;
import org.ddogleg.solver.PolynomialSolver;
import org.ddogleg.solver.RootFinderType;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.SpecializedOps;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Computes the essential or fundamental matrix using exactly 7 points with linear algebra.  The number of required points
 * is reduced from 8 to 7 by enforcing the singularity constraint, det(F) = 0.  The number of solutions found is
 * either one or three depending on the number of real roots found in the quadratic.
 * </p>
 *
 * <p>
 * The computed fundamental matrix follow the following convention (with no noise) for the associated pair:
 * x2<sup>T</sup>*F*x1 = 0<br>
 * x1 = keyLoc and x2 = currLoc.
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalLinear7 extends FundamentalLinear {
	// extracted from the null space of A
	protected DenseMatrix64F F1 = new DenseMatrix64F(3,3);
	protected DenseMatrix64F F2 = new DenseMatrix64F(3,3);

	// temporary storage for cubic coefficients
	private Polynomial poly = new Polynomial(4);
	private PolynomialRoots rootFinder = PolynomialSolver.createRootFinder(RootFinderType.EVD,4);

	// Matrix from SVD
	private DenseMatrix64F V = new DenseMatrix64F(9,9);

	/**
	 * When computing the essential matrix normalization is optional because pixel coordinates
	 *
	 * @param computeFundamental true it computes a fundamental matrix and false for essential
	 */
	public FundamentalLinear7(boolean computeFundamental) {
		super(computeFundamental);
	}

	/**
	 * <p>
	 * Computes a fundamental or essential matrix from a set of associated point correspondences.
	 * </p>
	 *
	 * @param points Input: List of corresponding image coordinates. In pixel for fundamental matrix or
	 *               normalized coordinates for essential matrix.
	 * @param solutions Output: Storage for the found solutions.
	 * @return true If successful or false if it failed
	 */
	public boolean process( List<AssociatedPair> points , FastQueue<DenseMatrix64F> solutions ) {
		if( points.size() != 7 )
			throw new IllegalArgumentException("Must be exactly 7 points. Not "+points.size()+" you gelatinous piece of pond scum.");

		// reset data structures
		solutions.reset();

		// must normalize for when points are in either pixel or calibrated units
		// TODO re-evaluate decision to normalize for calibrated case
		LowLevelMultiViewOps.computeNormalization(points, N1, N2);

		// extract F1 and F2 from two null spaces
		createA(points,A);

		if (!process(A))
			return false;

		undoNormalizationF(F1,N1,N2);
		undoNormalizationF(F2,N1,N2);

		// compute polynomial coefficients
		computeCoefficients(F1, F2, poly.c);

		// Find polynomial roots and solve for Fundamental matrices
		computeSolutions( solutions );

		return true;
	}

	/**
	 * Computes the SVD of A and extracts the essential/fundamental matrix from its null space
	 */
	private boolean process(DenseMatrix64F A) {
		if( !svdNull.decompose(A) )
			return false;

		// extract the two singular vectors
		// no need to sort by singular values because the two automatic null spaces are being sampled
		svdNull.getV(V,false);
		SpecializedOps.subvector(V, 0, 7, 9, false, 0, F1);
		SpecializedOps.subvector(V, 0, 8, 9, false, 0, F2);

		return true;
	}

	/**
	 * <p>
	 * Find the polynomial roots and for each root compute the Fundamental matrix.
	 * Given the two matrices it will compute an alpha such that the determinant is zero.<br>
	 *
	 * det(&alpha*F1 + (1-&alpha;)*F2 ) = 0
	 * </p>

	 */
	public void computeSolutions( FastQueue<DenseMatrix64F> solutions )
	{
		if( !rootFinder.process(poly))
			return;

		List<Complex64F> zeros = rootFinder.getRoots();

		for( Complex64F c : zeros ) {
			if( !c.isReal() && Math.abs(c.imaginary) > 1e-10 )
				continue;

			DenseMatrix64F F = solutions.grow();

			double a = c.real;
			double b = 1-c.real;

			for( int i = 0; i < 9; i++ ) {
				F.data[i] = a*F1.data[i] + b*F2.data[i];
			}

			// det(F) = 0 is already enforced, but for essential matrices it needs to enforce
			// that the first two singular values are zero and the last one is zero
			if( !computeFundamental && !projectOntoEssential(F) ) {
					solutions.removeTail();
			}
		}
	}

	/**
	 * <p>
	 * Computes the coefficients such that the following is true:<br>
	 *
	 * det(&alpha*F1 + (1-&alpha;)*F2 ) = c<sub>0</sub> + c<sub>1</sub>*&alpha; + c<sub>2</sub>*&alpha;<sup>2</sup>  + c<sub>2</sub>*&alpha;<sup>3</sup><br>
	 * </p>
	 *
	 * @param F1 a fundamental matrix
	 * @param F2 a fundamental matrix
	 * @param coefs Where results are returned.
	 */
	public static void computeCoefficients( DenseMatrix64F F1 ,
											DenseMatrix64F F2 ,
											double coefs[] )
	{
		Arrays.fill(coefs, 0);

		computeCoefficients(F1,F2,0,4,8,coefs,false);
		computeCoefficients(F1,F2,1,5,6,coefs,false);
		computeCoefficients(F1,F2,2,3,7,coefs,false);
		computeCoefficients(F1,F2,2,4,6,coefs,true);
		computeCoefficients(F1,F2,1,3,8,coefs,true);
		computeCoefficients(F1,F2,0,5,7,coefs,true);
	}

	public static void computeCoefficients( DenseMatrix64F F1 ,
											DenseMatrix64F F2 ,
											int i , int j , int k ,
											double coefs[] , boolean minus )
	{
		if( minus )
			computeCoefficientsMinus(F1.data[i], F1.data[j], F1.data[k], F2.data[i], F2.data[j], F2.data[k], coefs);
		else
			computeCoefficients(F1.data[i],F1.data[j],F1.data[k],F2.data[i],F2.data[j],F2.data[k],coefs);
	}

	public static void computeCoefficients( double x1 , double y1 , double z1 ,
											double x2 , double y2 , double z2 ,
											double coefs[] )
	{
		coefs[3] += x1*(y1*(z1 - z2) + y2*(z2 - z1)) + x2*( y1*(z2 - z1) + y2*(z1 - z2));
		coefs[2] += x1*(y1*z2 + y2*(z1 - 2*z2)) + x2*(y1*(z1 - 2*z2) + y2*( 3*z2 - 2*z1) );
		coefs[1] += x1*y2*z2 + x2*(y1*z2 + y2*(z1 - 3*z2));
		coefs[0] += x2*y2*z2;
	}
	public static void computeCoefficientsMinus( double x1 , double y1 , double z1 ,
												 double x2 , double y2 , double z2 ,
												 double coefs[] )
	{
		coefs[3] -= x1*(y1*(z1 - z2) + y2*(z2 - z1)) + x2*( y1*(z2 - z1) + y2*(z1 - z2));
		coefs[2] -= x1*(y1*z2 + y2*(z1 - 2*z2)) + x2*(y1*(z1 - 2*z2) + y2*( 3*z2 - 2*z1) );
		coefs[1] -= x1*y2*z2 + x2*(y1*z2 + y2*(z1 - 3*z2));
		coefs[0] -= x2*y2*z2;
	}
}
