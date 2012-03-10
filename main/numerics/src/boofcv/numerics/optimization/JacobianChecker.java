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

package boofcv.numerics.optimization;

import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import boofcv.numerics.optimization.impl.NumericalJacobianForward;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;

/**
 * Used to validate an algebraic Jacobian numerically.
 *
 * @author Peter Abeles
 */
public class JacobianChecker {

	public static void jacobianPrint( FunctionNtoM func , FunctionNtoMxN jacobian ,
									  double param[] , double tol )
	{
		NumericalJacobianForward numerical = new NumericalJacobianForward(func);

		DenseMatrix64F found = new DenseMatrix64F(func.getM(),func.getN());
		DenseMatrix64F expected = new DenseMatrix64F(func.getM(),func.getN());

		jacobian.process(param,found.data);
		numerical.process(param,expected.data);

		System.out.println("FOUND:");
		found.print();
		System.out.println("-----------------------------");
		System.out.println("Numerical");
		expected.print();
		
		System.out.println("-----------------------------");
		System.out.println("Large Differences");
		for( int y = 0; y < found.numRows; y++ ) {
			for( int x = 0; x < found.numCols; x++ ) {
				double diff = Math.abs(found.get(y,x)-expected.get(y,x));
				if( diff > tol )
					System.out.print("1");
				else
					System.out.print("0");
			}
			System.out.println();
		}
	}
	
	public static boolean jacobian( FunctionNtoM func , FunctionNtoMxN jacobian ,
									double param[] , double tol )
	{
		NumericalJacobianForward numerical = new NumericalJacobianForward(func);

		if( numerical.getM() != jacobian.getM() )
			throw new RuntimeException("M is not equal "+numerical.getM()+"  "+jacobian.getM());

		if( numerical.getN() != jacobian.getN() )
			throw new RuntimeException("N is not equal: "+numerical.getN()+"  "+jacobian.getN());

		DenseMatrix64F found = new DenseMatrix64F(func.getM(),func.getN());
		DenseMatrix64F expected = new DenseMatrix64F(func.getM(),func.getN());

		jacobian.process(param,found.data);
		numerical.process(param,expected.data);

		return MatrixFeatures.isIdentical(expected,found,tol);
	}
}
