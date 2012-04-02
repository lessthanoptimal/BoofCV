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

import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.NormOps;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDoglegStepFtF {

	double cauchyRadius = 0.5;
	double gaussRadius = 10;
	double combinedRadius = 0.9;

	DenseMatrix64F J,x,residuals,gradient;

	public TestDoglegStepFtF() {
		J = new DenseMatrix64F(3,2,true,1,0.5,2,Math.sqrt(2),-2,4);

		x = new DenseMatrix64F(2,1,true,0.5,1.5);
		residuals = new DenseMatrix64F(3,1,true,-1,-2,-3);

		gradient = new DenseMatrix64F(2,1);
		CommonOps.multTransA(J, residuals, gradient);
	}

	/**
	 * Pick a step less than the Cauchy step and see if the CauchyStep produces
	 * the same output as DoglegStep
	 */
	@Test
	public void computeStep_cauchy() {
		CauchyStep cauchy = new CauchyStep();
		WrappedDog alg = new WrappedDog();
		
		cauchy.init(2, 3);
		alg.init(2,3);
		cauchy.setInputs(x, residuals, J, gradient, -1);
		alg.setInputs(x, residuals, J, gradient, -1);

		DenseMatrix64F expected = new DenseMatrix64F(2,1);
		DenseMatrix64F found = new DenseMatrix64F(2,1);

		// step less than the cauchy step
		cauchy.computeStep(cauchyRadius, expected);
		alg.computeStep(cauchyRadius,found);
		assertTrue(cauchy.isMaxStep());
		assertTrue(alg.isMaxStep());
		assertTrue(alg.calledCauchy);

		assertTrue(MatrixFeatures.isIdentical(expected,found,1e-8));
	}

	/**
	 * Set the region so large that it will select the Gauss-Newton point.  This should should be
	 * a minimum for cost
	 */
	@Test
	public void computeStep_GaussNewton() {
		DoglegStepFtF alg = new DoglegStepFtF();

		alg.init(2, 3);
		alg.setInputs(x, residuals, J, gradient, -1);

		DenseMatrix64F step = new DenseMatrix64F(2,1);

		// give it a very large region
		alg.computeStep(gaussRadius,step);

		assertFalse(alg.isMaxStep());
		
		// should be a local max
		double a = cost(residuals, J, step, 0 , 0 );
		double b = cost(residuals, J, step, 0.01 , 0);
		double c = cost(residuals, J, step,-0.01 , 0);
		double d = cost(residuals, J, step, 0 , 0.01);
		double e = cost(residuals, J, step, 0 , -0.01);

		// should be less than all the others too
		assertTrue(a<b);
		assertTrue(a<c);
		assertTrue(a<d);
		assertTrue(a<e);
	}

	@Test
	public void computeStep_Hybrid() {
		WrappedDog alg = new WrappedDog();

		alg.init(2, 3);
		alg.setInputs(x, residuals, J, gradient, -1);

		DenseMatrix64F step = new DenseMatrix64F(2,1);

		// give it a specially selected step
		alg.computeStep(combinedRadius,step);

		// make sure the combined step was actually called
		assertTrue(alg.calledCombined);
		
		// check to see if it is along the region's radius
		assertTrue(alg.isMaxStep());
		double r = NormOps.normF(step);
		assertEquals(combinedRadius,r,1e-8);
	}

	@Test
	public void predict_cauchy() {
		checkPredictedCost(cauchyRadius,true,false);
	}

	@Test
	public void predict_GaussNewton() {
		checkPredictedCost(gaussRadius, false, false);
	}

	@Test
	public void predict_Hybrid() {
		checkPredictedCost(combinedRadius, false, true);
	}

	private void checkPredictedCost( double radius , boolean calledCauchy , boolean calledCombined  )
	{
		double fx = VectorVectorMult.innerProd(residuals,residuals)*0.5;
		WrappedDog alg = new WrappedDog();

		alg.init(2, 3);
		alg.setInputs(x, residuals, J, gradient, fx);


		DenseMatrix64F step = new DenseMatrix64F(2,1);

		alg.computeStep(radius,step);
		assertTrue(alg.calledCauchy==calledCauchy);
		assertTrue(alg.calledCombined==calledCombined);

		// compare found to predicted cost

		// F(0) - F(h)
		double expected = fx-cost(residuals,J,step,0,0);
		assertEquals(expected,alg.predictedReduction(),1e-8);
	}
	
	public static double cost( DenseMatrix64F residuals , DenseMatrix64F J , DenseMatrix64F h , double... delta )
	{
		h = h.copy();
		for( int i = 0; i < h.numRows; i++ )
			h.data[i] += delta[i];

		DenseMatrix64F B = new DenseMatrix64F(J.numCols,J.numCols);
		CommonOps.multTransA(J,J,B);

		double left = VectorVectorMult.innerProd(residuals, residuals);
		double middle = VectorVectorMult.innerProdA(residuals, J, h);
		double right = VectorVectorMult.innerProdA(h, B, h);

		return 0.5*left + middle + 0.5*right;
	}
	
	protected  static class WrappedDog extends DoglegStepFtF {
		boolean calledCombined = false;
		boolean calledCauchy = false;

		@Override
		protected void cauchyStep(double regionRadius, DenseMatrix64F step) {
			super.cauchyStep(regionRadius, step);
			calledCauchy = true;
		}

		@Override
		protected void combinedStep(double regionRadius, DenseMatrix64F step) {
			super.combinedStep(regionRadius, step);
			calledCombined = true;
		}
	}
}
