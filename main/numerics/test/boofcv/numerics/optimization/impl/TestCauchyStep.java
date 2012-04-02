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

package boofcv.numerics.optimization.impl;

import org.ejml.UtilEjml;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCauchyStep {

	DenseMatrix64F J,x,residuals,gradient;

	public TestCauchyStep() {
		J = new DenseMatrix64F(3,2,true,1,0,0,Math.sqrt(2),0,0);

		x = new DenseMatrix64F(2,1,true,0.5,1.5);
		residuals = new DenseMatrix64F(3,1,true,-1,-2,-3);

		gradient = new DenseMatrix64F(2,1);
		CommonOps.multTransA(J, residuals, gradient);
	}
	/**
	 * The optimal solution falls inside the trust region
	 */
	@Test
	public void computeStep_inside() {
		CauchyStep alg = new CauchyStep();
		alg.init(2,3);
		alg.setInputs(x,residuals,J,gradient,-1);
		
		DenseMatrix64F step = new DenseMatrix64F(2,1);
		
		alg.computeStep(10,step);
		
		// empirical test to see if it is a local minimum
		double a =  cost(residuals,J,step,0);
		double b =  cost(residuals, J, step, 0.01);
		double c =  cost(residuals,J,step,-0.01);

		assertTrue( a < b );
		assertTrue(a < c);
	}
	
	public static double cost( DenseMatrix64F residuals , DenseMatrix64F J , DenseMatrix64F h , double delta )
	{
		// adjust the value of h along the gradient's direction
		DenseMatrix64F direction = h.copy();
		CommonOps.scale(1.0/ NormOps.normF(h),direction);
		
		h = h.copy();
		for( int i = 0; i < h.numRows; i++ )
			h.data[i] += delta*direction.data[i];
		
		DenseMatrix64F B = new DenseMatrix64F(J.numCols,J.numCols);
		CommonOps.multTransA(J,J,B);

		double left = VectorVectorMult.innerProd(residuals, residuals);
		double middle = VectorVectorMult.innerProdA(residuals, J, h);
		double right = VectorVectorMult.innerProdA(h, B, h);

//		double cost =  0.5*left + middle + 0.5*right;
//
//		SimpleMatrix _r = SimpleMatrix.wrap(residuals);
//		SimpleMatrix _J = SimpleMatrix.wrap(J);
//		SimpleMatrix _h = SimpleMatrix.wrap(h);
//
//		double v = _r.plus(_J.mult(_h)).normF();
//
//		double alt = 0.5*v*v;
//
//		if( Math.abs(alt-cost) > 1e-8 )
//			throw new RuntimeException("Oh crap");
		
		return 0.5*left + middle + 0.5*right;
	}

	/**
	 * The optimal solution falls outside the trust region
	 */
	@Test
	public void computeStep_outside() {

		CauchyStep alg = new CauchyStep();
		alg.init(2,3);
		alg.setInputs(x,residuals,J,gradient,-1);

		DenseMatrix64F step = new DenseMatrix64F(2,1);

		alg.computeStep(1,step);

		// make sure it on he trust region border
		double l = NormOps.normF(step);
		assertTrue(Math.abs(l-1)<= UtilEjml.EPS);
		
		// empirical test to see if it is a local minimum
		double a =  cost(residuals,J,step,0);
		double c =  cost(residuals,J,step,-0.01);

		assertTrue(a < c);
	}

	/**
	 * Check predicted reduction against direct computation
	 */
	@Test
	public void predictedReduction_inside() {
		CauchyStep alg = new CauchyStep();
		alg.init(2,3);
		alg.setInputs(x,residuals,J,gradient,-1);

		DenseMatrix64F step = new DenseMatrix64F(2,1);

		alg.computeStep(10,step);

		// empirical calculation of the reduction
		double a =  VectorVectorMult.innerProd(residuals,residuals)*0.5;
		double c =  cost(residuals,J,step,0);

		assertEquals(a-c,alg.predictedReduction(),1e-8);
	}

	/**
	 * Check predicted reduction against direct computation
	 */
	@Test
	public void predictedReduction_outside() {
		CauchyStep alg = new CauchyStep();
		alg.init(2,3);
		alg.setInputs(x,residuals,J,gradient,-1);

		DenseMatrix64F step = new DenseMatrix64F(2,1);

		alg.computeStep(1,step);

		// empirical calculation of the reduction
		double a =  VectorVectorMult.innerProd(residuals,residuals)*0.5;
		double c =  cost(residuals,J,step,0);

		assertEquals(a-c,alg.predictedReduction(),1e-8);
	}
}
