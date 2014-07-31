/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGPixelMath extends BaseGClassChecksInMisc {

	public TestGPixelMath() {
		super(GPixelMath.class, PixelMath.class);
	}

	@Test
	public void compareToPixelMath() {
		performTests(18);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> param[] = validation.getParameterTypes();
		String name = candidate.getName();

		ImageBase inputA = createImage(param[0],null);
		ImageBase inputB=null,output=null;

		Object[][] ret = new Object[1][param.length];

		if( name.equals("abs")) {
			output = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = output;
		} else if( name.equals("invert")) {
			output = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = output;
		} else if( name.equals("divide") && param.length == 3) {
			output = createImage(param[param.length-1],null);
			if( ImageBase.class.isAssignableFrom(param[1]) )  {
				ret[0][0] = inputA;
				ret[0][1] = inputB = createImage(param[1],null);
				ret[0][2] = output;
			} else {
				ret[0][0] = inputA;
				ret[0][1] = 3;
				ret[0][2] = output;
			}
		} else if( name.equals("divide") && param.length == 5) {
			output = createImage(param[param.length - 1],null);
			ret[0][0] = inputA;
			ret[0][1] = 3;
			ret[0][2] = -1;
			ret[0][3] = 5;
			ret[0][4] = output;
		} else if( name.equals("multiply") && param.length == 3) {
			output = createImage(param[param.length-1],null);
			if( ImageBase.class.isAssignableFrom(param[1]) )  {
				ret[0][0] = inputA;
				ret[0][1] = inputB = createImage(param[1],null);
				ret[0][2] = output;
			} else {
				ret[0][0] = inputA;
				ret[0][1] = 3;
				ret[0][2] = output;
			}
		} else if( name.equals("multiply") && param.length == 5) {
			output = createImage(param[param.length - 1],null);
			ret[0][0] = inputA;
			ret[0][1] = 3;
			ret[0][2] = -20;
			ret[0][3] = 12;
			ret[0][4] = output;
		} else if( name.equals("plus") && param.length == 3) {
			output = createImage(param[param.length - 1],null);
			ret[0][0] = inputA;
			ret[0][1] = 3;
			ret[0][2] = output;
		} else if( name.equals("plus") && param.length == 5) {
			output = createImage(param[param.length-1],null);
			ret[0][0] = inputA;
			ret[0][1] = 3;
			ret[0][2] = -10;
			ret[0][3] = 12;
			ret[0][4] = output;
		} else if( name.equals("log") ) {
			inputB = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
		} else if( name.equals("pow2") ) {
			inputB = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
		} else if( name.equals("sqrt") ) {
			inputB = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
		} else if( name.equals("add") ) {
			inputB = createImage(param[1],null);
			output = createImage(param[2],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
			ret[0][2] = output;
		} else if( name.equals("subtract") ) {
			inputB = createImage(param[1],null);
			output = createImage(param[2],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
			ret[0][2] = output;
		} else if( name.equals("boundImage") ) {
			ret[0][0] = inputA;
			ret[0][1] = 2;
			ret[0][2] = 8;
		} else if( name.equals("diffAbs") ) {
			inputB = createImage(param[1],null);
			output = createImage(param[2],null);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
			ret[0][2] = output;
		} else if( name.equals("averageBand") ) {
			inputA = createImage(param[0],param[1]);
			output = createImage(param[1],null);
			ret[0][0] = inputA;
			ret[0][1] = output;
		}

		fillRandom(inputA);
		fillRandom(inputB);
		fillRandom(output);

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

		int which;

		if( targetParam[targetParam.length-1] instanceof ImageBase ) {
			which = targetParam.length-1;
		} else {
			which = 0;
		}

		ImageSingleBand t = (ImageSingleBand)targetParam[which];
		ImageSingleBand v = (ImageSingleBand)validationParam[which];

		// if it is full of zeros something went wrong
		assertTrue(GImageStatistics.maxAbs(t) != 0);

		BoofTesting.assertEquals(t, v, 0);
	}
}
