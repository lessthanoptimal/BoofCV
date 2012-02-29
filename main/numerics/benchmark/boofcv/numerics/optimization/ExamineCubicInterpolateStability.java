/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization;

import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;

/**
 * Tests to see which stabilization routine is more numerically stable
 *
 * @author Peter Abeles
 */
public class ExamineCubicInterpolateStability {

	public static double cubic( double a , double b , double alpha , double f0 , double g0 ) {
		double alpha2 = alpha*alpha;
		double alpha3 = alpha2*alpha;

		return a*alpha3 + b*alpha2 + alpha*g0 + f0;
	}
	
	/**
	 * Solve for [a,b] using a direct algebraic equation
	 */
	public static double[] cubicDirect( double f0 , double g0 ,
										double f1 , double alpha1 ,
										double f2 , double alpha2 ) {


		
		double denominator = alpha1*alpha1*alpha2*alpha2*(alpha2-alpha1);
		double a11 = alpha1*alpha1/denominator;
		double a12 = -alpha2*alpha2/denominator;
		double a21 = -alpha1*a11;
		double a22 = -alpha2*a12;

		double y1 = f2 - f0 - g0*alpha2;
		double y2 = f1 - f0 - g0*alpha1;

		double[] ret = new double[2];
		
		ret[0] = (a11*y1 + a12*y2);
		ret[1] = (a21*y1 + a22*y2);

		return ret;
	}

	public static double[] cubicDirect2( double f0 , double g0 ,
										 double f1 , double alpha1 ,
										 double f2 , double alpha2 ) {

		
		double a11 = 1.0/(alpha2*alpha2*(alpha2-alpha1));
		double a12 = -1.0/(alpha1*alpha1*(alpha2-alpha1));
		double a21 = -alpha1/(alpha2*alpha2*(alpha2-alpha1));
		double a22 = alpha2/(alpha1*alpha1*(alpha2-alpha1));

		double y1 = f2 - f0 - g0*alpha2;
		double y2 = f1 - f0 - g0*alpha1;

		double[] ret = new double[2];

		ret[0] = a11*y1 + a12*y2;
		ret[1] = a21*y1 + a22*y2;

		return ret;
	}

	/**
	 * Solve for [a,b] using a linear solver
	 */
	public static double[] cubicLinear( double f0 , double g0 ,
										double f1 , double alpha1 ,
										double f2 , double alpha2 ) {

		DenseMatrix64F A = new DenseMatrix64F(2,2);
		A.set(0,0,alpha1*alpha1*alpha1);
		A.set(0,1,alpha1*alpha1);
		A.set(1,0,alpha2*alpha2*alpha2);
		A.set(1,1,alpha2*alpha2);

		DenseMatrix64F Y = new DenseMatrix64F(2,1);
		Y.set(0,f1 - f0 - g0*alpha1);
		Y.set(1,f2 - f0 - g0*alpha2);

		DenseMatrix64F X = new DenseMatrix64F(2,1);

		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.linear(2);
//		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(2,2);

		if( !solver.setA(A))
			return X.data;

		solver.solve(Y,X);

		return X.data;
	}

	private static void evaluate(double a, double b, double f0, double g0, double alpha1, double alpha2) {
		double f1 = cubic(a,b,alpha1,f0,g0);
		double f2 = cubic(a,b,alpha2,f0,g0);

		double[] direct = cubicDirect(f0,g0,f1,alpha1,f2,alpha2);
		double[] direct2 = cubicDirect2(f0, g0, f1, alpha1, f2, alpha2);
		double[] linear = cubicLinear(f0, g0, f1, alpha1, f2, alpha2);

		double errorDirect = Math.abs(a-direct[0])/Math.abs(a) + Math.abs(b-direct[1])/Math.abs(b);
		double errorDirect2 = Math.abs(a-direct2[0])/Math.abs(a) + Math.abs(b-direct2[1])/Math.abs(b);
		double errorLinear = Math.abs(a-linear[0])/Math.abs(a) + Math.abs(b-linear[1])/Math.abs(b);

		System.out.println("Direct :  "+errorDirect);
		System.out.println("Direct2:  "+errorDirect2);
		System.out.println("Linear :  " + errorLinear);
	}

	/**
	 * Sees how it creates to small differences in alpha
	 */
	public static void distanceAlpha( int N ) {
		double a = 0.1;
		double b = 2.5;
		double f0 = 2;
		double g0 =  -5;
		double alpha1 = 1;

		for( int i = 0; i < N; i++ ) {
			double delta = 1.5*Math.exp(-i);
			double alpha2 = alpha1+delta;
			System.out.println("delta "+delta);
			evaluate(a, b, f0, g0, alpha1, alpha2);
		}
	}

	public static void distanceScale( int N ) {


		for( int i = 0; i < N; i++ ) {
			double scale = Math.pow(10, -i);

			double a = 0.1;
			double b = 2.5;
			double f0 = 2;
			double g0 =  -5;
			double alpha1 = 1*scale;
			double alpha2 = 2.5*scale;

			System.out.println("scale "+scale);
			evaluate(a, b, f0, g0, alpha1, alpha2);
		}
	}
	
	public static void main( String args[] ) {
//		distanceAlpha(35);
		distanceScale(20);
	}
}
