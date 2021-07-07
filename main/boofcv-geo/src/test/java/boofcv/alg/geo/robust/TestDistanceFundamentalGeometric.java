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

package boofcv.alg.geo.robust;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.f.EpipolarTestSimulation;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceFundamentalGeometric extends EpipolarTestSimulation {
	@Test void simple() {
		init(20,true);

		// create an outlier
		pairs.get(5).p1.setTo(rand.nextInt(intrinsic.width),rand.nextInt(intrinsic.height));
		pairs.get(5).p2.setTo(rand.nextInt(intrinsic.width),rand.nextInt(intrinsic.height));

		DMatrixRMaj E = MultiViewOps.createEssential(a_to_b.R,a_to_b.T,null);
		DMatrixRMaj F21 = MultiViewOps.createFundamental(E,K);

		DistanceFundamentalGeometric alg = new DistanceFundamentalGeometric();
		alg.setModel(F21);

		for(int i =0; i <pairs.size(); i++ ) {
			AssociatedPair p = pairs.get(i);

			if( i == 5 )
				assertTrue(Math.abs(MultiViewOps.constraint(F21,p.p1,p.p2)) >= UtilEjml.TEST_F64_SQ);
			else
				assertEquals(0, MultiViewOps.constraint(F21,p.p1,p.p2), UtilEjml.TEST_F64_SQ);
		}
	}
}
