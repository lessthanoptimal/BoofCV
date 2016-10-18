/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
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

		assertTrue(alg.process(observations, found));

		checkTrifocalWithConstraint(found, 1e-8);
		checkInducedErrors(found,observations,1e-8);
	}

	/**
	 * Give it noisy inputs and see if it produces a better solution than the non-iterative algorithm.
	 */
	@Test
	public void noisy() {
		List<AssociatedTriple> noisyObs = new ArrayList<>();

		// create a noisy set of observations
		double noiseLevel = 0.25;
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

		assertTrue(alg.process(noisyObs,found));

		found.normalizeScale();

		// only the induced error is tested since the constraint error has unknown units and is quite large
		checkInducedErrors(found,noisyObs,2);
	}

	/**
	 * Computes the error using induced homographies.
	 */
	public void checkInducedErrors( TrifocalTensor tensor , List<AssociatedTriple> observations , double tol )
	{
		for( AssociatedTriple o : observations ) {

			// homogeneous vector for observations in views 2 and 3
			Vector3D_F64 obs2 = new Vector3D_F64(o.p2.x,o.p2.y,1);
			Vector3D_F64 obs3 = new Vector3D_F64(o.p3.x,o.p3.y,1);

			// compute lines which pass through the observations
			Vector3D_F64 axisY = new Vector3D_F64(0,1,0);

			Vector3D_F64 line2 = new Vector3D_F64();
			Vector3D_F64 line3 = new Vector3D_F64();

			GeometryMath_F64.cross(obs2,axisY,line2);
			GeometryMath_F64.cross(obs3,axisY,line3);

			// compute induced homographies
			DenseMatrix64F H12 = MultiViewOps.inducedHomography12(tensor,line3,null);
			DenseMatrix64F H13 = MultiViewOps.inducedHomography13(tensor, line2, null);

			Point2D_F64 induced2 = new Point2D_F64();
			Point2D_F64 induced3 = new Point2D_F64();

			GeometryMath_F64.mult(H12,o.p1,induced2);
			GeometryMath_F64.mult(H13,o.p1,induced3);

			assertEquals(0,induced2.distance(o.p2),tol);
			assertEquals(0,induced3.distance(o.p3),tol);
		}
	}
}
