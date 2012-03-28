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
import org.ejml.ops.NormOps;

/**
 * <p>
 * Selects the optimal point along the gradient line within the trust region's constraint.
 * </p>
 *
 * <p>
 * The negative definite case is not considered because it is impossible when the Hessian is
 * approximated by squaring the Jacobian.  For a matrix to be negative definite there must be a
 * vector 'x' which will produce a negative result:<br>
 * x'*H*x < 0  --> x'*J'*J*x --> (J*x)'*(J*x)<br>
 * which is clearly always >= 0
 * </p>
 *
 * @author Peter Abeles
 */
public class CauchyStep implements TrustRegionStep {

	DenseMatrix64F B = new DenseMatrix64F(1,1);
	DenseMatrix64F gradient;
	DenseMatrix64F residuals;
	DenseMatrix64F J;

	double gBg;
	double gnorm;

	boolean maxStep;

	double predicted;

	public void init( int numParam , int numFunctions ) {
		B.reshape(numParam,numParam);
	}

	@Override
	public void setInputs(  DenseMatrix64F x , DenseMatrix64F residuals , DenseMatrix64F J ,
							DenseMatrix64F gradient , double fx )
	{
		this.gradient = gradient;
		CommonOps.multInner(J, B);

		gBg = VectorVectorMult.innerProdA(gradient, B, gradient);
		gnorm = NormOps.normF(gradient);
		
		this.residuals = residuals;
		this.J = J;

	}

	/**
	 *
	 * Computes the Cauchy step.  See comment in class description for why negative definite case
	 * is not considered.
	 *
	 * @param regionRadius
	 * @param step
	 */
	@Override
	public void computeStep( double regionRadius , DenseMatrix64F step) {

		double dist;

		double normRadius = regionRadius/gnorm;

		if( gBg == 0 ) {
			dist = normRadius;
			maxStep = true;
		} else {
			// find the distance of the minimum point
			dist = gnorm*gnorm/gBg;
			// use the border or dist, which ever is closer
			if( dist >= normRadius ) {
				maxStep = true;
				dist = normRadius;
			} else {
				maxStep = false;
			}
		}

		CommonOps.scale(-dist,gradient,step);

		// compute predicted reduction
		predicted = dist*gnorm*gnorm - 0.5*dist*dist*gBg;
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
