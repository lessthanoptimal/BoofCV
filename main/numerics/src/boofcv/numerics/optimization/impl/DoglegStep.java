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

import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

/**
 * <p>
 * Approximates the optimal step within the trust region using the so called dogleg.  This implementation
 * is based off the description found in [1,2], but some of the equations have been modified for simplicity and
 * correctness.
 *
 * </p>
 *
 *
 * <p>
 * <ul>
 * <li>[1] K. Madsen, H.B. Nielson,and O. Tingleff, "Methods for Non-Linear Least Squares Problems" 2nd Ed, April 2004</li>
 * <li>[2] Jorge Nocedal,and Stephen J. Wright "Numerical Optimization" 2nd Ed. Springer 2006
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class DoglegStep implements TrustRegionStep {

	// use QR decomposition to solve the system, should work even if it is degenerate
	private LinearSolver<DenseMatrix64F> pinv = LinearSolverFactory.pseudoInverse(false);

	// B=J'*J estimated Hessian
	private DenseMatrix64F B = new DenseMatrix64F(1,1);
	// gradient J'*f
	private DenseMatrix64F gradient;

	// negative of the gradient
	private DenseMatrix64F gradientNeg = new DenseMatrix64F(1,1);

	// predicted reduction.  Is computed efficiently depending on the case
	private double predicted;

	// if the step is at the region's border
	private boolean maxStep;

	// step and distance of Cauchy point
	protected DenseMatrix64F stepCauchy = new DenseMatrix64F(1,1);
	private double distanceCauchy;

	// step computed using Gauss-Newton
	protected DenseMatrix64F stepGN = new DenseMatrix64F(1,1);
	// distance of the Gauss-Newton step
	private double distanceGN;

	// intermediate values when computing cauchy step
	private double gBg;
	private double gnorm;

	@Override
	public void init(int numParam, int numFunctions) {
		B.reshape(numParam,numParam);
		stepCauchy.reshape(numParam,1);
		stepGN.reshape(numParam,1);
		gradientNeg.reshape(numParam,1);
	}

	@Override
	public void setInputs(DenseMatrix64F x, DenseMatrix64F residuals, DenseMatrix64F J,
						  DenseMatrix64F gradient , double fx ) {
		this.gradient = gradient;
		CommonOps.scale(-1, gradient, gradientNeg);

		CommonOps.multInner(J, B);

		gBg = VectorVectorMult.innerProdA(gradient, B, gradient);
		gnorm = NormOps.normF(gradient);

		// compute and distance location of the Cauchy step
		if( gBg == 0 )
			distanceCauchy = 0;
		else
			distanceCauchy = gnorm*gnorm/gBg;

		// compute location of Gauss-Newton step
		if( !pinv.setA(B) )
			throw new RuntimeException("pinv failed?!?");

		pinv.solve(gradientNeg, stepGN);
		distanceGN = NormOps.normF(stepGN);
	}

	/**
	 * Uses the Cauchy point, Gauss-Newton point, or a linear combination of both
	 * depending on the regionRadius.  The predicted reduction is computed differently
	 * depending on which of the 3 cases is active.
	 */
	@Override
	public void computeStep(double regionRadius, DenseMatrix64F step) {
		// of the Gauss-Newton solution is inside the trust region use that
		if( distanceGN <= regionRadius ) {
			step.set(stepGN);
			maxStep = distanceGN == regionRadius;
			predicted = -0.5*VectorVectorMult.innerProd(stepGN,gradient);
		} else if( distanceCauchy*gnorm >= regionRadius ) {
			// if the trust region comes before the Cauchy point then perform the cauchy step
			cauchyStep(regionRadius, step);
		} else {
			combinedStep(regionRadius, step);
			maxStep = true;
		}
	}

	/**
	 * Computes the Cauchy step, truncates it if the regionRadius is less than the optimal step
	 * @param regionRadius
	 * @param step
	 */
	protected void cauchyStep(double regionRadius, DenseMatrix64F step) {
		double normRadius = regionRadius/gnorm;

		double dist = distanceCauchy;
		if( dist >= normRadius ) {
			maxStep = true;
			dist = normRadius;
		} else {
			maxStep = false;
		}
		CommonOps.scale(-dist, gradient, step);
		predicted = predictCauchy(dist);
	}

	/**
	 * Compute the step which is a linear combination of the cauchy point and the Gauss-Newton
	 * point.  The distance is computed so that it is at the edge of the allowed region.
	 */
	protected void combinedStep(double regionRadius, DenseMatrix64F step) {
		// find a point that is a linear interpolation between the Cauchy and GN points
		CommonOps.scale(-distanceCauchy, gradient, stepCauchy);

		// c = a'*(b-a)
		double c = 0;
		for( int i = 0; i < stepCauchy.numRows; i++ )
			c += stepCauchy.data[i]*(stepGN.data[i]-stepCauchy.data[i]);

		// solve phi(beta) = ||a + beta*(b-1)||^2 - radius^2

		// bma2 = ||b-a||^2
		// a2 = ||a||^2
		double bma2 = 0;
		double a2 = 0;
		for( int i = 0; i < stepCauchy.numRows; i++ ) {
			double a = stepCauchy.data[i];
			double d = stepGN.data[i]-a;
			bma2 += d*d;
			a2 += a*a;
		}

		double r2 = regionRadius*regionRadius;

		double beta;

		if( c <= 0 )
			beta = (-c+Math.sqrt(c*c + bma2*(r2-a2)))/bma2;
		else
			beta = (r2-a2)/(c+Math.sqrt(c*c + bma2*(r2-a2)));

		// step = a + beta*(b-a)
		step.zero();
		for( int i = 0; i < stepCauchy.numRows; i++ )
			step.data[i] = stepCauchy.data[i] + beta*(stepGN.data[i]-stepCauchy.data[i]);

		// compute the predicted reduction

		// This was found by plugging in h=beta*stepGN + stepC*(1-beta) to
		// L(0) - L(h) = -F(x)'*J(x)*h - 0.5*h'*B*h

		double dotGandGN = VectorVectorMult.innerProd(stepGN,gradient);
		double oneMb = (1-beta);
		double left = -0.5*distanceCauchy*distanceCauchy*oneMb*oneMb*gBg;
		double middle = -distanceCauchy*oneMb*(beta-1)*gnorm*gnorm;
		double right = (beta*beta/2.0 - beta)*dotGandGN;

		predicted = left+middle+right;
	}

	/**
	 * Computes reduction for cauchy point.
	 *
	 * @param dist distance traveled normalized for the gradient
	 * @return predicted reduction
	 */
	private double predictCauchy( double dist ) {
		return dist*gnorm*gnorm - 0.5*dist*dist*gBg;
	}
	
	@Override
	public double predictedReduction() {
		return predicted;
	}

	@Override
	public boolean isMaxStep() {
		return maxStep;
	}
}
