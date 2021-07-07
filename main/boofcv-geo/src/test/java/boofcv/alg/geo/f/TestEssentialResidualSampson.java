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
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEssentialResidualSampson extends EpipolarTestSimulation {

	/**
	 * First check to see if the error is very low for perfect parameters. Then
	 * give it incorrect parameters and make sure it is not zero.
	 */
	@Test void checkChangeInCost() {
		init(30,false);

		// compute true essential matrix
		DMatrixRMaj E = MultiViewOps.createEssential(a_to_b.getR(), a_to_b.getT(), null);

		EssentialResidualSampson alg = new EssentialResidualSampson();

		alg.setCalibration1(intrinsic);
		alg.setCalibration2(intrinsic);

		// see if it returns no error for the perfect model
		alg.setModel(E);
		for(AssociatedPair p : pairs ) {
			assertEquals(0, alg.computeResidual(p), 1e-8);
		}

		// Make sure the point ordering is consistent, e.g. x2'*E*x1 and NOT x1'*E*x2
		assertEquals(0,MultiViewOps.constraint(E,pairs.get(0).p1,pairs.get(0).p2), UtilEjml.TEST_F64);

		// see if the error is in pixels or not
		AssociatedPair tmpPair = pairs.get(0).copy();
		PerspectiveOps.convertNormToPixel(intrinsic,tmpPair.p1,tmpPair.p1);
		PerspectiveOps.convertNormToPixel(intrinsic,tmpPair.p2,tmpPair.p2);
		tmpPair.p1.x+=2;
		tmpPair.p1.y-=2;
		PerspectiveOps.convertPixelToNorm(intrinsic,tmpPair.p1,tmpPair.p1);
		PerspectiveOps.convertPixelToNorm(intrinsic,tmpPair.p2,tmpPair.p2);

		assertTrue(Math.abs(alg.computeResidual(tmpPair)) > 1);
		
		// now make it a bit off
		E.data[1] += 0.1;
		alg.setModel(E);
		for(AssociatedPair p : pairs ) {
			assertTrue(Math.abs(alg.computeResidual(p)) > 1e-8);
		}
	}
}
