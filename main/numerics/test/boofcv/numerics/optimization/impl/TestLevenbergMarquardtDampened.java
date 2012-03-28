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

import boofcv.numerics.optimization.EvaluateLevenbergMarquardtDampened;
import boofcv.numerics.optimization.NonlinearResults;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import boofcv.numerics.optimization.wrap.WrapCoupledJacobian;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLevenbergMarquardtDampened {

	EvaluateLevenbergMarquardtDampened evaluator = new EvaluateLevenbergMarquardtDampened(false);

	@Test
	public void basicTest() {
		double a=2,b=0.1;

		LevenbergMarquardtDampened alg = createAlg(a,b);
		
		alg.initialize(new double[]{1,0.5});
		
		int i;
		for( i = 0; i < 200 && !alg.iterate(); i++ ) { }

		// should converge way before this
		assertTrue(i!=200);
		assertTrue(alg.isConverged());

		double found[] = alg.getParameters();

		assertEquals(a,found[0],1e-4);
		assertEquals(b,found[1],1e-4);
	}

	private LevenbergMarquardtDampened createAlg( double a, double b ) {

		FunctionNtoM residual = new TrivialLeastSquaresResidual(a,b);
		FunctionNtoMxN jacobian = new NumericalJacobianForward(residual);

		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.pseudoInverse(true);

		LevenbergMarquardtDampened alg = new LevenbergMarquardtDampened(solver,1e-3);

		alg.setConvergence(1e-6);
		alg.setFunction(new WrapCoupledJacobian(residual,jacobian));

		return alg;
	}

	@Test
	public void helicalvalley() {
		NonlinearResults results = evaluator.helicalValley();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient<100);
		assertEquals(1,results.x[0],1e-4);
		assertEquals(0,results.x[1],1e-4);
		assertEquals(0,results.x[2],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	@Test
	public void rosenbrock() {
		NonlinearResults results = evaluator.rosenbrock();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient<100);
		assertEquals(1,results.x[0],1e-4);
		assertEquals(1,results.x[1],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	// Omitting this test because LM is known to have scaling issues and the problem
	// should be reformulated for LM
//	@Test
//	public void badlyScaledBrown() {
//		NonlinearResults results = evaluator.badlyScaledBrown();
//
//		// no algorithm to compare it against, just do some sanity checks for changes
//		assertTrue(results.numFunction<100);
//		assertTrue(results.numGradient<100);
//		assertEquals(1e6,results.x[0],1e-4);
//		assertEquals(2e-6,results.x[1],1e-4);
//		assertEquals(0,results.f,1e-4);
//	}


//	@Test
//	public void trigonometric() {
//		NonlinearResults results = evaluator.trigonometric();
//
//		// no algorithm to compare it against, just do some sanity checks for changes
//		assertTrue(results.numFunction<100);
//		assertTrue(results.numGradient < 100);
//		assertEquals(0,results.f,1e-4);
//	}
}
