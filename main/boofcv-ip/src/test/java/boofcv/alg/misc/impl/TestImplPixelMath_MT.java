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

package boofcv.alg.misc.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static boofcv.testing.BoofTesting.primitive;
import static boofcv.testing.BoofTesting.randomArray;

class TestImplPixelMath_MT extends CompareIdenticalFunctions  {

	private Random rand = new Random(234);
	private int width = 105;
	private int height = 100;

	TestImplPixelMath_MT() {
		super(ImplPixelMath_MT.class, ImplPixelMath.class);
	}

	@Test
	void performTests() {
		performTests(108);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class param[] = m.getParameterTypes();

		if( param.length < 3 )
			return false;
		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();

		System.out.println(candidate.getName());

		switch( candidate.getName() ) {
			case "abs": return inputs_abs(types);
			case "add": return inputs_add(types);
			case "bound": return inputs_bound(types);
			case "diffAbs": return inputs_diffAbs(types);
			case "divide": return inputs_divide(types);
			case "divide_A": return inputs_divide_A(types);
			case "log": return inputs_log(types);
			case "minus_A": return inputs_minus_A(types);
			case "minus_B": return inputs_minus_B(types);
			case "minusU_A": return minusU_A(types);
			case "multiply": return minusU_A(types);
			case "multiply_A": return minusU_A(types);
		}

		throw new RuntimeException("Unknown function: "+candidate.getName());
	}

	private Object[][] inputs_abs( Class[] inputTypes ) {
		Object[] inputs = new Object[8];
		inputs[0] = randomArray(inputTypes[0],200,rand);
		inputs[1] = 1;
		inputs[2] = 10;
		inputs[3] = randomArray(inputTypes[3],200,rand);
		inputs[4] = 0;
		inputs[5] = 11;
		inputs[6] = 12;
		inputs[7] = 9;

		return new Object[][]{inputs};
	}

	private Object[][] inputs_add( Class[] inputTypes ) {
		return null;
	}

	private Object[][] inputs_bound( Class[] inputTypes ) {
		return null;
	}


	private Object[][] inputs_diffAbs( Class[] inputTypes ) {
		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0],width,height,2);
		ImageBase b = GeneralizedImageOps.createImage(inputTypes[1],width,height,2);
		ImageBase c = GeneralizedImageOps.createImage(inputTypes[2],width,height,2);

		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);
		GImageMiscOps.fillUniform(c,rand,0,200);

		return new Object[][]{{a,b,c}};
	}

	private Object[][] inputs_divide( Class[] inputTypes ) {
		return null;
	}

	private Object[][] inputs_divide_A( Class[] inputTypes ) {
		if( inputTypes.length == 9 ) {
			Object[] inputs = new Object[9];
			inputs[0] = randomArray(inputTypes[0], 200, rand);
			inputs[1] = 1;
			inputs[2] = 10;
			inputs[3] = primitive(1.5 , inputTypes[3]);
			inputs[4] = randomArray(inputTypes[4], 200, rand);
			inputs[5] = 0;
			inputs[6] = 11;
			inputs[7] = 12;
			inputs[8] = 9;
			return new Object[][]{inputs};
		} else {
			Object[] inputs = new Object[11];
			inputs[0] = randomArray(inputTypes[0], 200, rand);
			inputs[1] = 1;
			inputs[2] = 10;
			inputs[3] = primitive(1.5 , inputTypes[3]);
			inputs[4] = primitive(1 ,   inputTypes[4]);
			inputs[5] = primitive(30,   inputTypes[5]);
			inputs[6] = randomArray(inputTypes[6], 200, rand);
			inputs[7] = 0;
			inputs[8] = 11;
			inputs[9] = 12;
			inputs[10] = 9;
			return new Object[][]{inputs};
		}
	}

	private Object[][] inputs_log( Class[] inputTypes ) {
		return null;
	}

	private Object[][] inputs_minus_A(Class[] inputTypes ) {
		return null;
	}

	private Object[][] inputs_minus_B(Class[] inputTypes ) {
		return null;
	}

	private Object[][] minusU_A(Class[] inputTypes ) {
		return null;
	}


	private Object[][] multiply(Class[] inputTypes ) {
		return null;
	}


	private Object[][] multiply_A(Class[] inputTypes ) {
		return null;
	}


}

