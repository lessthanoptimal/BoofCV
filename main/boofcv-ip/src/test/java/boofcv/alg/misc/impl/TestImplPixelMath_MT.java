/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.TestPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static boofcv.BoofTesting.primitive;
import static boofcv.BoofTesting.randomArray;

@SuppressWarnings("rawtypes")
class TestImplPixelMath_MT extends CompareIdenticalFunctions  {

	private final int width = 105;
	private final int height = 100;

	TestImplPixelMath_MT() {
		super(ImplPixelMath_MT.class, ImplPixelMath.class);
	}

	@Test
	void performTests() {
		performTests(184);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class[] param = m.getParameterTypes();

		if( param.length < 3 )
			return false;
		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();

//		System.out.println(candidate.getName());

		Object[][] inputs = defaultInputs(types,candidate.getName());

		if( inputs == null ) {
			return switch (candidate.getName()) {
				case "boundImage" -> boundImage(types);
				case "negative", "abs" -> abs(types);
				case "log", "logSign" -> log(types);
				case "operator1" -> operator1(types);
				case "operator2" -> operator2(types);
				default -> throw new RuntimeException("Unknown function " + candidate.getName());
			};
		} else {
			return inputs;
		}
	}

	private Object[][] operator1( Class[] inputTypes ) {
		Object[] inputs = new Object[9];
		inputs[0] = randomArray(inputTypes[0], 200, rand);
		inputs[1] = 1;
		inputs[2] = 10;
		inputs[3] = randomArray(inputTypes[3], 200, rand);
		inputs[4] = 2;
		inputs[5] = 10;
		inputs[6] = 12;
		inputs[7] = 9;
		inputs[8] = TestPixelMath.createOperator1_Plus5(BoofTesting.pritiveToImageDataType(inputTypes[0]));

		return new Object[][]{inputs};
	}

	private Object[][] operator2( Class[] inputTypes ) {
		Object[] inputs = new Object[12];
		inputs[0] = randomArray(inputTypes[0], 200, rand);
		inputs[1] = 1;
		inputs[2] = 10;
		inputs[3] = randomArray(inputTypes[3], 200, rand);
		inputs[4] = 2;
		inputs[5] = 10;
		inputs[6] = randomArray(inputTypes[3], 200, rand);;
		inputs[7] = 3;
		inputs[8] = 10;
		inputs[9] = 12;
		inputs[10] = 9;
		inputs[11] = TestPixelMath.createOperator2_AddPlus5(BoofTesting.pritiveToImageDataType(inputTypes[0]));

		return new Object[][]{inputs};
	}

	private Object[][] boundImage( Class[] inputTypes ) {
		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0],width,height,2);
		GImageMiscOps.fillUniform(a,rand,0,200);

		Object[] inputs = new Object[3];
		inputs[0] = a;
		inputs[1] = primitive(10 , inputTypes[1]);
		inputs[2] = primitive(80 , inputTypes[2]);

		return new Object[][]{inputs};
	}

	private Object[][] abs( Class[] inputTypes ) {
		Object[] inputs = new Object[8];
		inputs[0] = randomArray(inputTypes[0], 200, rand);
		inputs[1] = 1;
		inputs[2] = 10;
		inputs[3] = randomArray(inputTypes[3], 200, rand);
		inputs[4] = 0;
		inputs[5] = 11;
		inputs[6] = 12;
		inputs[7] = 9;
		return new Object[][]{inputs};
	}

	private Object[][] log( Class[] inputTypes ) {
		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0],width,height,2);
		ImageBase b = GeneralizedImageOps.createImage(inputTypes[2],width,height,2);

		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);

		Object[] inputs = new Object[3];
		inputs[0] = a;
		inputs[1] = primitive(0.5 , inputTypes[1]);;
		inputs[2] = b;
		return new Object[][]{inputs};
	}

	/**
	 * Several element-wise operation use the same argument pattern
	 */
	private Object[][] elementWiseInputs(Class[] inputTypes , double value ) {
		if( inputTypes.length == 9 ) {
			Object[] inputs = new Object[9];
			inputs[0] = randomArray(inputTypes[0], 200, rand);
			inputs[1] = 1;
			inputs[2] = 10;
			inputs[3] = primitive(value , inputTypes[3]);
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
			inputs[3] = primitive(value , inputTypes[3]);
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

	private Object[][] elementWiseInputs(Class[] inputTypes  ) {
		if( inputTypes.length == 8 ) {
			Object[] inputs = new Object[8];
			inputs[0] = randomArray(inputTypes[0], 200, rand);
			inputs[1] = 1;
			inputs[2] = 10;
			inputs[3] = randomArray(inputTypes[3], 200, rand);
			inputs[4] = 0;
			inputs[5] = 11;
			inputs[6] = 12;
			inputs[7] = 9;
			return new Object[][]{inputs};
		} else {
			throw new IllegalArgumentException("Unexpected");
		}
	}

	private Object[][] defaultInputs( Class[] inputTypes , String name ) {
		if( name.contains("operator"))
			return null;
		if( inputTypes.length == 3 ) {
			boolean allImages = true;
			for (int i = 0; i < 3; i++) {
				if( !ImageBase.class.isAssignableFrom(inputTypes[i])) {
					allImages = false;
					break;
				}
			}
			if( allImages ) {
				return threeImages(inputTypes);
			}
		} else if( inputTypes.length == 9 || inputTypes.length == 11 ) {
			double value = name.startsWith("divide") ? 1.5 : 20;
			return elementWiseInputs(inputTypes,value);
		} else if( inputTypes.length == 8 ) {
			return elementWiseInputs(inputTypes);
		}
		return null;
	}

	private Object[][] threeImages( Class[] inputTypes ) {
		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0],width,height,2);
		ImageBase b = GeneralizedImageOps.createImage(inputTypes[1],width,height,2);
		ImageBase c = GeneralizedImageOps.createImage(inputTypes[2],width,height,2);

		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);
		GImageMiscOps.fillUniform(c,rand,0,200);

		return new Object[][]{{a,b,c}};
	}
}

