/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.wrap;

import boofcv.numerics.optimization.functions.FunctionNtoS;
import org.junit.Test;

import static boofcv.numerics.optimization.wrap.TestLsToNonLinearDeriv.FuncLS;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestLsToNonLinear {

	@Test
	public void compareToNumeric() {
		FuncLS funcLS = new FuncLS();
		FunctionNtoS func = new LsToNonLinear(funcLS);

		double point[] = new double[]{1,2};
		double output[] = new double[funcLS.getM()];
		funcLS.process(point,output);

		double found = func.process(point);
		double expected = 0;
		for( int i = 0; i < output.length; i++ )
			expected += output[i]*output[i];

		assertEquals(expected,found,1e-8);
	}
}
