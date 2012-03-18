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
import boofcv.alg.geo.UtilEpipolar;
import boofcv.numerics.solver.PolynomialSolver;
import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SpecializedOps;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Computes the essential or fundamental matrix using 7 points with linear algebra.  The number of required points
 * is reduced from 8 to 7 by enforcing the singularity constraint, det(F) = 0.
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class FundamentalLinear7 extends FundamentalLinear8 {

	// extracted from the null space of A
	protected DenseMatrix64F F1 = new DenseMatrix64F(3,3);
	protected DenseMatrix64F F2 = new DenseMatrix64F(3,3);

	// temporary storage for cubic coefficients
	private double[] coefs = new double[4];

	/**
	 * When computing the essential matrix normalization is optional because pixel coordinates
	 *
	 * @param computeFundamental true it computes a fundamental matrix and false for essential
	 */
	public FundamentalLinear7(boolean computeFundamental) {
		super(computeFundamental);
	}

	@Override
	public boolean process( List<AssociatedPair> points ) {
		if( points.size() != 7 )
			throw new IllegalArgumentException("Must be exactly 7 points. Not "+points.size()+" you gelatinous piece of pond scum.");

		// must normalize for when points are in either pixel or calibrated units
		UtilEpipolar.computeNormalization(N1, N2, points);

		// extract F1 and F2 from two null spaces
		createA(points,A);

		if (process(A))
			return false;

		undoNormalizationF(F1,N1,N2);
		undoNormalizationF(F2,N1,N2);

		// enforce the zero determinant constraint and computing weighting
		// factor between F1 and F2
		double alpha = enforceZeroDeterminant(F1,F2,coefs);
		CommonOps.scale(alpha,F1);
		CommonOps.scale(1-alpha,F2);
		CommonOps.add(F1,F2,F);

		// enforce final constraints and normalize to canonical scale
		if( computeFundamental)
			return projectOntoFundamentalSpace(F);
		else
			return projectOntoEssential(F);
	}

	/**
	 * Computes the SVD of A and extracts the essential/fundamental matrix from its null space
	 */
	@Override
	protected boolean process(DenseMatrix64F A) {
		if( !svd.decompose(A) )
			return true;

		// extract the two singular vectors
		DenseMatrix64F V = svd.getV(false);
		SpecializedOps.subvector(V, 0, 7, V.numCols, false, 0, F1);
		SpecializedOps.subvector(V, 0, 8, V.numCols, false, 0, F2);

		return false;
	}

	/**
	 * <p>
	 * Given the two matrices it will compute an alpha such that the determinant is zero.<br>
	 *
	 * det(&alpha*F1 + (1-&alpha;)*F2 ) = 0
	 * </p>
	 *
	 * @param F1 a matrix
	 * @param F2 a matrix
	 * @param coefs double array of length 4.  used for temporary storage
	 * @return alpha
	 */
	public static double enforceZeroDeterminant( DenseMatrix64F F1 ,
												 DenseMatrix64F F2 ,
												 double coefs[] )
	{
		computeCoefficients(F1,F2,coefs);

		// using a specialized algorithm is faster, but this implementation is less numerically stable
//		double alpha =  PolynomialSolver.cubicRootReal(coefs[0],coefs[1],coefs[2],coefs[3]);

		Complex64F[] zeros = PolynomialSolver.polynomialRootsEVD(coefs);

//		// not because its the biggest
		double alpha = zeros[0].real;
		double minImaginary = Math.abs(zeros[0].imaginary);
		for( int i = 1; i < zeros.length; i++ ) {
			double img = Math.abs(zeros[i].imaginary);
			if( img < minImaginary ) {
				minImaginary = img;
				alpha = zeros[i].getReal();
			}
		}
		return alpha;
	}

	/**
	 * <p>
	 * Computes the coefficients such that the following is true:
	 *
	 * det(&alpha*F1 + (1-&alpha;)*F2 ) = c<sub>0</sub> + c<sub>1</sub>*&alpha; + c<sub>2</sub>*&alpha;<sup>2</sup>  + c<sub>2</sub>*&alpha;<sup>3</sup><br>
	 * </p>
	 *
	 * @param F1 a matrix
	 * @param F2 a matrix
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
		coefs[3] += x1*y1*z1 - x1*y1*z2 - x1*y2*z1 + x1*y2*z2 - x2*y1*z1 + x2*y1*z2 + x2*y2*z1 - x2*y2*z2;
		coefs[2] += x1*y1*z2 + x1*y2*z1 - 2*x1*y2*z2 + x2*y1*z1 - 2*x2*y1*z2 - 2*x2*y2*z1 + 3*x2*y2*z2;
		coefs[1] += x1*y2*z2 + x2*y1*z2 + x2*y2*z1 - 3*x2*y2*z2;
		coefs[0] += x2*y2*z2;
	}
	public static void computeCoefficientsMinus( double x1 , double y1 , double z1 ,
												 double x2 , double y2 , double z2 ,
												 double coefs[] )
	{
		coefs[3] -= x1*y1*z1 - x1*y1*z2 - x1*y2*z1 + x1*y2*z2 - x2*y1*z1 + x2*y1*z2 + x2*y2*z1 - x2*y2*z2;
		coefs[2] -= x1*y1*z2 + x1*y2*z1 - 2*x1*y2*z2 + x2*y1*z1 - 2*x2*y1*z2 - 2*x2*y2*z1 + 3*x2*y2*z2;
		coefs[1] -= x1*y2*z2 + x2*y1*z2 + x2*y2*z1 - 3*x2*y2*z2;
		coefs[0] -= x2*y2*z2;
	}
}
