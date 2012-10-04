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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.MultiViewOps;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.SpecializedOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalAlgebraicPoint7 extends CommonTrifocalChecks {

	/**
	 * Give it perfect inputs and make sure it doesn't screw things up
	 */
	@Test
	public void perfect() {
		UnconstrainedLeastSquares optimizer = FactoryOptimization.leastSquareLevenberg(1e-3);
		TrifocalAlgebraicPoint7 alg = new TrifocalAlgebraicPoint7(optimizer,300,1e-20,1e-20);

		assertTrue(alg.process(observations));

		TrifocalTensor found = alg.getSolution();

		checkTrifocalWithConstraint(found,1e-8);
	}

	/**
	 * Give it noisy inputs and see if it produces a better solution than the non-iterative algorithm.
	 *
	 * NO TEST IS ACTUALLY PERFORMED HERE.  SEE COMMENTS BELOW.
	 */
	@Test
	public void noisy() {
		List<AssociatedTriple> noisyObs = new ArrayList<AssociatedTriple>();

		// create a noisy set of observations
		double noiseLevel = 0.5;
		for( AssociatedTriple p : observations ) {
			AssociatedTriple n = p.copy();

			n.p1.x += rand.nextGaussian()*noiseLevel;
			n.p1.y += rand.nextGaussian()*noiseLevel;
			n.p2.x += rand.nextGaussian()*noiseLevel;
			n.p2.y += rand.nextGaussian()*noiseLevel;
			n.p3.x += rand.nextGaussian()*noiseLevel;
			n.p3.y += rand.nextGaussian()*noiseLevel;

			noisyObs.add(n);
		}

		UnconstrainedLeastSquares optimizer = FactoryOptimization.leastSquareLevenberg(1e-3);
		TrifocalAlgebraicPoint7 alg = new TrifocalAlgebraicPoint7(optimizer,300,1e-20,1e-20);

		assertTrue(alg.process(noisyObs));


		TrifocalTensor found = alg.getSolution();
		found.normalizeScale();

		// OK I have no idea how to evaluate these noisy results.
		//
		// 1) Strictly enforcing geometric constraints can cause an error metrics to get worse because
		//    the linear model has more degrees of freedom and can fit the data better.  Can't compare it to the
		//    non-optimized version.
		// 2) When camera matrices are extracted from the tensor there is an unknown (but common) projective transform
		//    being applied to them.  This is true even when normalized image coordinate are used.
		// 2a) Because the projective transform is unknown any error metrics calculated in the distorted image
		//    will have an unknown magnitude.
		//
		// When computing the error using the 3-pt constraint the error values seem very high.  The same test was
		// performed using matlab code found online and it exhibited the same behavior.  See ValidationBoof project.
		// So I think this algorithm is correctly implemented, but I have no way of sanity check its sensitivity to
		// noise.

		double a = computeError(found, noisyObs);
	}

	public double computeError( TrifocalTensor tensor , List<AssociatedTriple> observations ) {

		double sum = 0;

		for( int i = 0; i < observations.size(); i++ ) {
			AssociatedTriple o = observations.get(i);

			DenseMatrix64F c = MultiViewOps.constraint(tensor, o.p1, o.p2, o.p3, null);

			sum += SpecializedOps.elementSumSq(c);
		}

		return sum/2.0;
	}

}
