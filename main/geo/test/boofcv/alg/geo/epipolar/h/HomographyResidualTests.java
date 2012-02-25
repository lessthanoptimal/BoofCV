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

package boofcv.alg.geo.epipolar.h;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.List;

import static boofcv.alg.geo.epipolar.f.TestResidualFundamentalSampson.computeCost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class HomographyResidualTests extends CommonHomographyChecks {

	public abstract FunctionNtoM createAlg( List<AssociatedPair> pairs );

	@Test
	public void basicTest() {
		createScene(20,false);

		// use the linear algorithm to compute the homography
		HomographyLinear4 estimator = new HomographyLinear4(true);
		estimator.process(pairs);
		DenseMatrix64F H = estimator.getHomography();

		FunctionNtoM alg = createAlg(pairs);

		// no noise
		double []residuals = new double[alg.getM()];
		alg.process(H.data,residuals);

		assertEquals(0, computeCost(residuals),1e-8);

		// with model error
		H.data[0] += 0.6;
		H.data[4] += 0.6;
		alg.process(H.data,residuals);

		assertTrue(computeCost(residuals) > 1e-8);
	}
}
