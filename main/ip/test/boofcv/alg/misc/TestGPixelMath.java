/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
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
		performTests(22);
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
		} else if( name.equals("minus") && param.length == 3) {
			output = createImage(param[param.length - 1],null);
			boolean first = ImageBase.class.isAssignableFrom(param[0]);
			if( inputA == null ) inputA = createImage(param[1],null);
			ret[0][0] = first ? inputA : 3;
			ret[0][1] = first ? 3 : inputA;
			ret[0][2] = output;
		} else if( name.equals("minus") && param.length == 5) {
			output = createImage(param[param.length-1],null);
			boolean first = ImageBase.class.isAssignableFrom(param[0]);
			if( inputA == null ) inputA = createImage(param[1],null);
			ret[0][0] = first ? inputA : 3;
			ret[0][1] = first ? 3 : inputA;
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

		ImageBase t = (ImageBase)targetParam[which];
		ImageBase v = (ImageBase)validationParam[which];

		// if it is full of zeros something went wrong
		boolean foundNotZero = false;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if( GeneralizedImageOps.get(t,j,i,0) != 0 ) {
					foundNotZero = true;
					break;
				}
			}
		}
		assertTrue(foundNotZero);

		BoofTesting.assertEquals(t, v, 0);
	}

	/**
	 * Tests all functions with inputs from planar images
	 */
	@Test
	public void all_planar_images() {

		int total = 0;
		Method[] methods = GPixelMath.class.getMethods();

		for( Method m : methods ) {
			if(!Modifier.isStatic(m.getModifiers()))
				continue;
			Class[] param = m.getParameterTypes();
			if( param.length < 1 )
				continue;

			// create input arguments
			Object[] inputs = new Object[ param.length ];

			for (int i = 0; i < inputs.length; i++) {
				if( param[i] == ImageBase.class) {
					inputs[i] = new Planar(GrayF32.class,width,height,2);
					GImageMiscOps.fillUniform((ImageBase)inputs[i],rand,-100,100);
				}
			}

			// specialized inputs for individual functions
			String name = m.getName();
			if( name.equals("divide") && param.length == 3) {
				if( !ImageBase.class.isAssignableFrom(param[1]) )  {
					inputs[1] = 3;
				}
			} else if( name.equals("divide") && param.length == 5) {
				inputs[1] = 3;
				inputs[2] = -1;
				inputs[3] = 5;
			} else if( name.equals("multiply") && param.length == 3) {
				if( !ImageBase.class.isAssignableFrom(param[1]) )  {
					inputs[1] = 3;
				}
			} else if( name.equals("multiply") && param.length == 5) {
				inputs[1] = 3;
				inputs[2] = -20;
				inputs[3] = 12;
			} else if( name.equals("plus") && param.length == 3) {
				inputs[1] = 3;
			} else if( name.equals("plus") && param.length == 5) {
				inputs[1] = 3;
				inputs[2] = -10;
				inputs[3] = 12;
			} else if( name.equals("minus") && param.length == 3) {
				boolean first = ImageBase.class.isAssignableFrom(param[0]);
				inputs[first?1:0] = 3;
			} else if( name.equals("minus") && param.length == 5) {
				boolean first = ImageBase.class.isAssignableFrom(param[0]);
				inputs[first?1:0] = 3;
				inputs[2] = -10;
				inputs[3] = 12;
			} else if( name.equals("boundImage") ) {
				inputs[1] = 2;
				inputs[2] = 8;
			} else if( name.equals("averageBand")) {
				continue;
			}

			try {
				// create the expected results
				Object[] inputsByBand = copy(inputs);
				invokeByBand(m,inputsByBand);

				// invoke this function
				m.invoke(null,inputs );

				// compare against each other
				for (int i = 0; i < inputs.length; i++) {
					if (Planar.class == inputs[i].getClass()) {
						BoofTesting.assertEquals((ImageBase)inputs[i],(ImageBase)inputsByBand[i], 1e-4);
					}
				}
				total++;
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		assertEquals(21,total);
	}

	private Object[] copy( Object inputs[] ) {
		Object copy[] = new Object[inputs.length];

		for (int i = 0; i < inputs.length; i++) {
			if( Planar.class == inputs[i].getClass() ) {
				copy[i] = ((Planar)inputs[i]).createSameShape();
				((Planar)copy[i]).setTo((Planar)inputs[i]);
			} else {
				copy[i] = inputs[i];
			}
		}
		return copy;
	}

	private void invokeByBand( Method m , Object inputs[] ) {
		Object modified[] = new Object[inputs.length];

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < inputs.length; j++) {
				if( Planar.class == inputs[j].getClass() ) {
					modified[j] = ((Planar)inputs[j]).getBand(i);
				} else {
					modified[j] = inputs[j];
				}
			}
			try {
				m.invoke(null, modified);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}


	@Test
	public void divide_planar_by_gray_3() {
		Planar<GrayF32> numerator = new Planar<>(GrayF32.class,width,height,2);
		GrayF32 denominator = new GrayF32(width,height);
		GImageMiscOps.fillUniform(numerator,rand,-10,10);
		GImageMiscOps.fillUniform(denominator,rand,1,2);

		Planar<GrayF32> output = new Planar<>(GrayF32.class,width,height,2);

		GPixelMath.divide(numerator,denominator,output);

		GrayF32 expected = denominator.createSameShape();

		for (int i = 0; i < numerator.getNumBands(); i++) {
			GPixelMath.divide(numerator.getBand(i),denominator, expected);
			BoofTesting.assertEquals(output.getBand(i),expected, 1e-4);
		}
	}

	@Test
	public void multiply_planar_by_gray_3() {
		Planar<GrayF32> numerator = new Planar<>(GrayF32.class,width,height,2);
		GrayF32 denominator = new GrayF32(width,height);
		GImageMiscOps.fillUniform(numerator,rand,-10,10);
		GImageMiscOps.fillUniform(denominator,rand,1,2);

		Planar<GrayF32> output = new Planar<>(GrayF32.class,width,height,2);

		GPixelMath.multiply(numerator,denominator,output);
		GrayF32 expected = denominator.createSameShape();

		for (int i = 0; i < numerator.getNumBands(); i++) {
			GPixelMath.multiply(numerator.getBand(i),denominator, expected);
			BoofTesting.assertEquals(output.getBand(i),expected, 1e-4);
		}
	}
}
