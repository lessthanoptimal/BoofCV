/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEpipolarMinimizeGeometricError extends EpipolarTestSimulation {

	/**
	 * The perfect solution is passed in. it should no change
	 */
	@Test void perfect() {
		init(50,true);

		DMatrixRMaj E = MultiViewOps.createEssential(a_to_b.R, a_to_b.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E,intrinsic);

		EpipolarMinimizeGeometricError alg = new EpipolarMinimizeGeometricError();

		Point2D_F64 fa = new Point2D_F64();
		Point2D_F64 fb = new Point2D_F64();

		for (int i = 0; i < pairs.size(); i++) {
			AssociatedPair p = pairs.get(i);

			assertTrue(alg.process(F,p.p1.x,p.p1.y,p.p2.x,p.p2.y,fa,fb));

			assertEquals(0,p.p1.distance(fa), UtilEjml.TEST_F64);
			assertEquals(0,p.p2.distance(fb), UtilEjml.TEST_F64);
		}
	}

	/**
	 * Checks the solution given noisy pixel inputs. It should produce a solution which lies on the epipolar
	 * line perfectly and is close to the input
	 */
	@Test void noisy() {
		init(50,true);

		DMatrixRMaj E = MultiViewOps.createEssential(a_to_b.R, a_to_b.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E,intrinsic);

		EpipolarMinimizeGeometricError alg = new EpipolarMinimizeGeometricError();

		Point2D_F64 fa = new Point2D_F64();
		Point2D_F64 fb = new Point2D_F64();

		for (int i = 0; i < pairs.size(); i++) {
			AssociatedPair p = pairs.get(i);

			p.p1.x += rand.nextGaussian()*0.5;
			p.p1.x += rand.nextGaussian()*0.5;
			p.p2.y += rand.nextGaussian()*0.5;
			p.p2.y += rand.nextGaussian()*0.5;

			assertTrue(alg.process(F,p.p1.x,p.p1.y,p.p2.x,p.p2.y,fa,fb));

			assertEquals(0, MultiViewOps.constraint(F,fa,fb), UtilEjml.TEST_F64);
			assertEquals(0,p.p1.distance(fa), 2);
			assertEquals(0,p.p2.distance(fb), 2);
		}
	}
}
