/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestTrifocalAlgebraicPoint7 extends CommonTrifocalChecks {

	/**
	 * Give it perfect inputs and make sure it doesn't screw things up
	 */
	@Test
	void perfect() {
		perfect(true);
		perfect(false);
	}

	private void perfect( boolean planar ) {
		createSceneObservations(planar);

		UnconstrainedLeastSquares optimizer = FactoryOptimization.levenbergMarquardt(null,false);
		TrifocalAlgebraicPoint7 alg = new TrifocalAlgebraicPoint7(optimizer,300,1e-20,1e-20);

		assertTrue(alg.process(observations, found));

		checkTrifocalWithConstraint(found, 5e-7);
		assertEquals(0, computeTransferError(found,observations), UtilEjml.TEST_F64);
	}

	/**
	 * Give it noisy inputs and see if it produces a better solution than the non-iterative algorithm.
	 */
	@Test
	void noisy() {
//		noisy(true); // can't test the planar scenario here because the linear solution is used and that blows up
		noisy(false);
	}
	private void noisy( boolean planar ) {
		createSceneObservations(planar);

		List<AssociatedTriple> noisyObs = new ArrayList<>();

		// create a noisy set of observations
		double noiseLevel = 0.5;
		for( AssociatedTriple p : observationsPixels ) {
			AssociatedTriple n = p.copy();

			n.p1.x += rand.nextGaussian()*noiseLevel;
			n.p1.y += rand.nextGaussian()*noiseLevel;
			n.p2.x += rand.nextGaussian()*noiseLevel;
			n.p2.y += rand.nextGaussian()*noiseLevel;
			n.p3.x += rand.nextGaussian()*noiseLevel;
			n.p3.y += rand.nextGaussian()*noiseLevel;

			noisyObs.add(n);
		}

		TrifocalLinearPoint7 linear = new TrifocalLinearPoint7();
		linear.process(noisyObs,found);
		double errorLinear = computeTransferError(found,noisyObs);

		UnconstrainedLeastSquares optimizer = FactoryOptimization.levenbergMarquardt(null,true);
		TrifocalAlgebraicPoint7 alg = new TrifocalAlgebraicPoint7(optimizer,300,1e-20,1e-20);

		assertTrue(alg.process(noisyObs,found));

		// only the induced error is tested since the constraint error has unknown units and is quite large
		double errorsAlg = computeTransferError(found,noisyObs);

		// it appears that the linear estimate is very accurate. This is a really a test to see if it made
		// the solution significantly worse
		assertTrue(errorsAlg<=errorLinear*1.2);
	}

	/**
	 * Computes the error using induced homographies.
	 */
	double computeTransferError(TrifocalTensor tensor , List<AssociatedTriple> observations )
	{
		TrifocalTransfer transfer = new TrifocalTransfer();
		transfer.setTrifocal(tensor);

		double errors = 0;
		for( AssociatedTriple o : observations ) {

			Point3D_F64 p = new Point3D_F64();
			transfer.transfer_1_to_3(o.p1.x,o.p1.y,o.p2.x,o.p2.y,p);
			errors += o.p3.distance(p.x/p.z,p.y/p.z);
			transfer.transfer_1_to_2(o.p1.x,o.p1.y,o.p3.x,o.p3.y,p);
			errors += o.p2.distance(p.x/p.z,p.y/p.z);
		}

		return errors/(2*observations.size());
	}
}
