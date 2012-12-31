/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;

import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class HomographyResidualTests extends CommonHomographyChecks {

	public abstract ModelObservationResidualN<DenseMatrix64F,AssociatedPair> createAlg();

	@Test
	public void basicTest() {
		createScene(20,false);

		// use the linear algorithm to compute the homography
		HomographyLinear4 estimator = new HomographyLinear4(true);
		estimator.process(pairs,solution);

		ModelObservationResidualN<DenseMatrix64F,AssociatedPair> alg = createAlg();

		double residuals[] = new double[alg.getN()];
		
		// no noise
		alg.setModel(solution);
		for( AssociatedPair p : pairs ) {
			int index = alg.computeResiduals(p,residuals,0);
			for( int i = 0; i < alg.getN(); i++ ) {
				assertEquals(0,residuals[i],1e-8);
			}
			assertEquals(index,alg.getN());
		}


		// with model error
		solution.data[0] += 0.6;
		solution.data[4] += 0.6;
		alg.setModel(solution);
		for( AssociatedPair p : pairs ) {
			int index = alg.computeResiduals(p,residuals,0);
			for( int i = 0; i < alg.getN(); i++ ) {
				assertTrue(Math.abs(residuals[i])>1e-8);
			}
			assertEquals(index,alg.getN());
		}
	}
}
