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
import org.ejml.ops.SpecializedOps;

/**
 * @author Peter Abeles
 */
public class DoglegStepF implements TrustRegionStep {

	// linear solver for least squares problem, needs to handle singular matrices
	LinearSolver<DenseMatrix64F> pinv;

	// gradient J'*f
	private DenseMatrix64F gradient;

	// negative of the residuals
	private DenseMatrix64F residualsNeg = new DenseMatrix64F(1,1);

	// predicted reduction.  Is computed efficiently depending on the case
	private double predicted;

	// if the step is at the region's border
	private boolean maxStep;

	// step and distance of Cauchy point
	protected DenseMatrix64F stepCauchy = new DenseMatrix64F(1,1);
	private double distanceCauchy;
	private double alpha;

	// step computed using Gauss-Newton
	protected DenseMatrix64F stepGN = new DenseMatrix64F(1,1);
	// distance of the Gauss-Newton step
	private double distanceGN;

	double gnorm;

	// Jacobian times the gradient
	DenseMatrix64F Jg = new DenseMatrix64F(1,1);

	/**
	 * Configure internal algorithms
	 *
	 * @param pinv Linear solver for least-squares problem. Needs to handle
	 */
	public DoglegStepF(LinearSolver<DenseMatrix64F> pinv) {
		this.pinv = pinv;
	}

	/**
	 * Default solver
	 */
	public DoglegStepF() {
		this(LinearSolverFactory.leastSquaresQrPivot(true, false));
	}

	@Override
	public void init(int numParam, int numFunctions) {
		stepCauchy.reshape(numParam,1);
		stepGN.reshape(numParam,1);
		residualsNeg.reshape(numFunctions,1);
		Jg.reshape(numFunctions, 1);
	}

	@Override
	public void setInputs(DenseMatrix64F x, DenseMatrix64F residuals, 
						  DenseMatrix64F J, DenseMatrix64F gradient, double fx) {

		this.gradient = gradient;

		if( !pinv.setA(J) )
			throw new RuntimeException("Solver failed");

		// compute Gauss Newton step
		CommonOps.scale(-1,residuals,residualsNeg);
		pinv.solve(residualsNeg,stepGN);
		distanceGN = NormOps.normF(stepGN);

		// Compute Cauchy step
		CommonOps.mult(J,gradient, Jg);
		alpha = SpecializedOps.elementSumSq(gradient)/SpecializedOps.elementSumSq(Jg);
		gnorm = NormOps.normF(gradient);
		distanceCauchy = alpha*gnorm;
	}

	@Override
	public void computeStep(double regionRadius, DenseMatrix64F step) {

		// of the Gauss-Newton solution is inside the trust region use that
		if( distanceGN <= regionRadius ) {
			step.set(stepGN);
			maxStep = distanceGN == regionRadius;
			predicted = -0.5* VectorVectorMult.innerProd(stepGN, gradient);
		} else if( distanceCauchy >= regionRadius ) {
			// if the trust region comes before the Cauchy point then perform the cauchy step
			cauchyStep(regionRadius, step);
		} else {
			combinedStep(regionRadius, step);
			maxStep = true;
		}
	}

	/**
	 * Computes the Cauchy step and the predicted reduction
	 */
	protected void cauchyStep(double regionRadius, DenseMatrix64F step) {

		double dist;

		if( distanceCauchy >= regionRadius ) {
			maxStep = true;
			dist = regionRadius;
		} else {
			maxStep = false;
			dist = distanceCauchy;
		}
		CommonOps.scale(-dist/gnorm, gradient, step);

		predicted = regionRadius*(2.0*alpha*gnorm - regionRadius)/(2.0*alpha);
	}

	/**
	 * Computes a linear interpolation between the Cauchy and Gauss-Newton steps
	 */
	protected void combinedStep(double regionRadius, DenseMatrix64F step) {
		// find the Cauchy point
		CommonOps.scale(-distanceCauchy/gnorm, gradient, stepCauchy);

		// compute the combined step
		double beta = DoglegStepFtF.combinedStep(stepCauchy,stepGN,regionRadius,step);

		// predicted reduction
		double predictedGN = -0.5* VectorVectorMult.innerProd(stepGN, gradient);

		predicted = 0.5*alpha*(1-beta)*(1-beta)*gnorm*gnorm + beta*(2-beta)*predictedGN;
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
