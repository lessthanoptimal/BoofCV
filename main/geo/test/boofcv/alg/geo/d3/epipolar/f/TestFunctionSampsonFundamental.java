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

package boofcv.alg.geo.d3.epipolar.f;

import boofcv.alg.geo.d3.epipolar.CommonFundamentalChecks;
import boofcv.alg.geo.d3.epipolar.UtilEpipolar;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFunctionSampsonFundamental extends CommonFundamentalChecks {

	/**
	 * First check to see if the error is very low for perfect parameters.  Then
	 * give it incorrect parameters and make sure it is not zero.
	 */
	@Test
	public void checkChangeInCost() {
		init(30,false);

		// compute true essential matrix
		DenseMatrix64F E = UtilEpipolar.computeEssential(motion.getR(),motion.getT());
		
		ParamFundamentalEpipolar param = new ParamFundamentalEpipolar();
		
		FunctionSampsonFundamental alg = new FunctionSampsonFundamental(param,pairs);
		
		// see if it returns no error for the perfect model
		double[] d = new double[7];
		param.modelToParam(E,d);
		
		assertEquals(0,alg.process(d),1e-8);
		
		// now make it a bit off
		d[1] += 0.1;
		assertTrue(alg.process(d) > 1e-8);
	}
}
