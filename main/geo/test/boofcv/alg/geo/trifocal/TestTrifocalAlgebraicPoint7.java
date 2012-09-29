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
import org.ejml.ops.NormOps;
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
	 */
	@Test
	public void noisy() {
		List<AssociatedTriple> noisyObs = new ArrayList<AssociatedTriple>();

		// create a noisy set of observations
		double noiseLevel = 0.001;
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

		TrifocalLinearPoint7 validator = new TrifocalLinearPoint7();
		assertTrue(validator.process(noisyObs));

		TrifocalTensor compare = validator.getSolution();
		TrifocalTensor found = alg.getSolution();

		double a = computeError(compare,noisyObs);
		double b = computeError(found,noisyObs);

		assertTrue( b < a*0.1 );
	}

	public double computeError( TrifocalTensor tensor , List<AssociatedTriple> observations ) {

		double sum = 0;

		for( int i = 0; i < observations.size(); i++ ) {
			AssociatedTriple o = observations.get(i);

			DenseMatrix64F c = MultiViewOps.constraint(tensor, o.p1, o.p2, o.p3, null);

			sum += NormOps.normF(c)/(c.numCols*c.numRows);
		}

		return sum;
	}
}
