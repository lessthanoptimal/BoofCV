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

package boofcv.alg.misc;

import boofcv.alg.misc.PixelMathLambdas.*;
import boofcv.core.image.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static boofcv.BoofTesting.primitive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"ALL","SelfAssignment"})
public class TestPixelMath extends BoofStandardJUnit {
	int width = 10;
	int height = 15;
	int numBands = 2;

	float[] pixelA = new float[numBands];
	float[] pixelB = new float[numBands];

	@Test void checkAll() {
		int numExpected = 328;
		Method[] methods = PixelMath.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if (!isTestMethod(m))
				continue;
			try {
//				System.out.println(m.getName()+"  "+m.getParameterTypes().length+"  "+m.getParameterTypes()[0].getSimpleName());
				if (m.getName().compareTo("operator1") == 0) {
					testOperator1(m);
				} else if (m.getName().compareTo("operator2") == 0) {
					testOperator2(m);
				} else if (m.getName().compareTo("divide") == 0) {
					if (m.getParameterTypes().length == 3) {
						if (ImageBase.class.isAssignableFrom(m.getParameterTypes()[1])) {
							testDividePixel(m);
						} else {
							testDivide(m);
						}
					} else
						testDivideBounded(m);
				} else if (m.getName().compareTo("multiply") == 0) {
					if (m.getParameterTypes().length == 3) {
						if (ImageBase.class.isAssignableFrom(m.getParameterTypes()[1])) {
							testMultiplyPixel(m);
						} else {
							testMultiply(m);
						}
					} else
						testMultiplyBounded(m);
				} else if (m.getName().compareTo("plus") == 0) {
					if (m.getParameterTypes().length == 3)
						testPlus(m);
					else
						testPlusBounded(m);
				} else if (m.getName().compareTo("minus") == 0) {
					if (m.getParameterTypes().length == 3)
						testMinus(m);
					else
						testMinusBounded(m);
				} else if (m.getName().compareTo("add") == 0) {
					testAdd(m);
				} else if (m.getName().compareTo("log") == 0) {
					testLog(m);
				} else if (m.getName().compareTo("logSign") == 0) {
					testLogSign(m);
				} else if (m.getName().compareTo("pow2") == 0) {
					testPow2(m);
				} else if (m.getName().compareTo("sqrt") == 0) {
					testSqrt(m);
				} else if (m.getName().compareTo("negative") == 0) {
					testNegative(m);
				} else if (m.getName().compareTo("subtract") == 0) {
					testSubtract(m);
				} else if (m.getName().compareTo("boundImage") == 0) {
					testBound(m);
				} else if (m.getName().compareTo("abs") == 0) {
					testAbs(m);
				} else if (m.getName().compareTo("diffAbs") == 0) {
					testDiffAbs(m);
				} else if (m.getName().compareTo("averageBand") == 0) {
					TestAverageBand(m);
				} else if (m.getName().compareTo("stdev") == 0) {
					testStdev(m);
				} else {
					throw new RuntimeException("Unknown function: " + m.getName());
				}
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if (numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found " + numFound + "  expected " + numExpected);
	}

	private boolean isTestMethod( Method m ) {

		Class[] param = m.getParameterTypes();

		if (param.length < 1)
			return false;

		for (int i = 0; i < param.length; i++) {
			if (ImageBase.class.isAssignableFrom(param[i]))
				return true;
		}
		return false;
	}

	public static PixelMathLambdas.Function1 createOperator1_Plus5( ImageDataType type ) {
		return switch( type ) {
			case I8,S8,U8 -> new Function1_I8() { @Override public byte process( byte a ) { return (byte)(a + 5); } };
			case I16,S16,U16 -> new Function1_I16() { @Override public short process( short a ) { return (short)(a + 5); } };
			case S32 -> new Function1_S32() { @Override public int process( int a ) { return (int)(a + 5); } };
			case S64 -> new Function1_S64() { @Override public long process( long a ) { return (long)(a + 5); } };
			case F32 -> new Function1_F32() { @Override public float process( float a ) { return (float)(a + 5); } };
			case F64 -> new Function1_F64() { @Override public double process( double a ) { return (double)(a + 5); } };
			default -> throw new RuntimeException("Unknown type");
		};
	}

	public static PixelMathLambdas.Function2 createOperator2_AddPlus5( ImageDataType type ) {
		return switch( type ) {
			case I8,S8,U8 -> new Function2_I8() { @Override public byte process( byte a, byte b ) { return (byte)(a + b + 5); } };
			case I16,S16,U16 -> new Function2_I16() { @Override public short process( short a, short b ) { return (short)(a + b + 5); } };
			case S32 -> new Function2_S32() { @Override public int process( int a, int b ) { return (int)(a + b + 5); } };
			case S64 -> new Function2_S64() { @Override public long process( long a, long b ) { return (long)(a + b + 5); } };
			case F32 -> new Function2_F32() { @Override public float process( float a, float b ) { return (float)(a + b + 5); } };
			case F64 -> new Function2_F64() { @Override public double process( double a, double b ) { return (double)(a + b + 5); } };
			default -> throw new RuntimeException("Unknown type");
		};
	}

	private void testOperator1( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		m.invoke(null, input, createOperator1_Plus5(input.imageType.getDataType()), output);

		double tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double valA = GeneralizedImageOps.get(input, j, i, k);
					double valB = GeneralizedImageOps.get(output, j, i, k);

					assertEquals(valA+5.0, valB, tol);
				}
			}
		}
	}

	private void testOperator2( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase imgA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase imgB = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[3], width, height, numBands);
		int numBands = imgA.getImageType().getNumBands();

		GImageMiscOps.fillUniform(imgA, rand, 0, 20);
		GImageMiscOps.fillUniform(imgB, rand, 0, 20);

		if (imgA.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(imgA, rand, -20, 20);
			GImageMiscOps.fillUniform(imgB, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(imgA, rand, 0, 20);
			GImageMiscOps.fillUniform(imgB, rand, 0, 20);
		}

		m.invoke(null, imgA, createOperator2_AddPlus5(imgA.imageType.getDataType()), imgB, output);

		double tol = imgA.getImageType().getDataType().isInteger() ? 1 : 1e-4;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double valA = GeneralizedImageOps.get(imgA, j, i, k);
					double valB = GeneralizedImageOps.get(imgB, j, i, k);
					double valC = GeneralizedImageOps.get(output, j, i, k);

					assertEquals(valA+valB+5.0, valC, tol);
				}
			}
		}
	}

	private void testDivide( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (input.getImageType().getDataType().isInteger())
			m.invoke(null, input, 10, output);
		else
			m.invoke(null, input, 10.0f, output);

		double tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double valA = GeneralizedImageOps.get(input, j, i, k);
					double valB = GeneralizedImageOps.get(output, j, i, k);

					assertEquals(valA/10.0, valB, tol);
				}
			}
		}
	}

	private void testDividePixel( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageGray inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20, 20);
		GImageMiscOps.fillUniform(inputB, rand, -20, 20);

		m.invoke(null, inputA, inputB, output);

		GImageGray a = FactoryGImageGray.wrap(inputA);
		GImageGray b = FactoryGImageGray.wrap(inputB);
		GImageGray o = FactoryGImageGray.wrap(output);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				assertEquals(a.get(j, i).doubleValue()/b.get(j, i).doubleValue(), o.get(j, i).doubleValue(), 1e-4);
			}
		}
	}

	private void testDivideBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[4], width, height, numBands);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (input.getImageType().getDataType().isInteger())
			m.invoke(null, input, 10, -1, 1, output);
		else
			m.invoke(null, input, 10.0f, -1f, 1f, output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double expected = GeneralizedImageOps.get(input, j, i, k)/10.0f;
					double found = GeneralizedImageOps.get(output, j, i, k);

					if (expected < -1) expected = -1;
					if (expected > 1) expected = 1;

					assertEquals(expected, found, tol);
				}
			}
		}
	}

	private void testMultiply( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();

		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (input.getImageType().getDataType().isInteger())
			m.invoke(null, input, 2, output);
		else
			m.invoke(null, input, 2.0f, output);

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		double tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					assertEquals(pixelA[k]*2, pixelB[k], tol);
				}
			}
		}
	}

	private void testMultiplyPixel( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageGray inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20, 20);
		GImageMiscOps.fillUniform(inputB, rand, -20, 20);

		m.invoke(null, inputA, inputB, output);

		GImageGray a = FactoryGImageGray.wrap(inputA);
		GImageGray b = FactoryGImageGray.wrap(inputB);
		GImageGray o = FactoryGImageGray.wrap(output);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				assertEquals(a.get(j, i).doubleValue()*b.get(j, i).doubleValue(), o.get(j, i).doubleValue(), 1e-4);
			}
		}
	}

	private void testMultiplyBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[4], width, height, numBands);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (input.getImageType().getDataType().isInteger())
			m.invoke(null, input, 2, -30, 30, output);
		else
			m.invoke(null, input, 2.0f, -30f, 30f, output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {

				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					float expected = pixelA[k]*2;
					float found = pixelB[k];
					if (expected < -30) expected = -30;
					if (expected > 30) expected = 30;

					assertEquals(expected, found, tol);
				}
			}
		}
	}

	private void testPlus( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);

		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (output.getImageType().getDataType().isInteger())
			m.invoke(null, input, 2, output);
		else
			m.invoke(null, input, 2.0f, output);

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					assertEquals(pixelA[k] + 2, pixelB[k], tol);
				}
			}
		}
	}

	private void testPlusBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		if (input.getImageType().getDataType().isInteger())
			m.invoke(null, input, 2, -10, 12, output);
		else
			m.invoke(null, input, 2.0f, -10f, 12f, output);

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					float expected = pixelA[k] + 2;
					float found = pixelB[k];
					if (expected < -10) expected = -10;
					if (expected > 12) expected = 12;

					assertEquals(expected, found, tol);
				}
			}
		}
	}

	private void testMinus( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();

		boolean imageFirst = ImageBase.class.isAssignableFrom(paramTypes[0]);
		int indexImg = imageFirst ? 0 : 1;

		ImageBase input = GeneralizedImageOps.createImage(paramTypes[indexImg], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);

		int numBands = input.getImageType().getNumBands();

		float val = imageFirst ? 2 : 20;
		Object scalar;
		if (output.getImageType().getDataType().isInteger()) {
			if (imageFirst) {
				GImageMiscOps.fillUniform(input, rand, 10, 25);
			} else {
				GImageMiscOps.fillUniform(input, rand, 3, 10);
			}
			scalar = (int)val;
		} else {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
			scalar = val;
		}

		Object[] args = imageFirst ?
				new Object[]{input, scalar, output} :
				new Object[]{scalar, input, output};
		m.invoke(null, args);

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					if (imageFirst)
						assertEquals(pixelA[k] - val, pixelB[k], tol);
					else
						assertEquals(val - pixelA[k], pixelB[k], tol);
				}
			}
		}
	}

	private void testMinusBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();

		boolean imageFirst = ImageBase.class.isAssignableFrom(paramTypes[0]);
		int indexImg = imageFirst ? 0 : 1;

		ImageBase input = GeneralizedImageOps.createImage(paramTypes[indexImg], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[indexImg], width, height, numBands);

		int numBands = input.getImageType().getNumBands();

		float val = imageFirst ? 2 : 32;
		float l = 9, u = 15;
		Object scalar, lower, upper;
		if (input.getImageType().getDataType().isInteger()) {
			if (imageFirst) {
				GImageMiscOps.fillUniform(input, rand, 10, 20);
			} else {
				GImageMiscOps.fillUniform(input, rand, 7, 15);
			}
			scalar = (int)val;
			lower = (int)l;
			upper = (int)u;
		} else {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
			scalar = val;
			lower = l;
			upper = u;
		}
		Object[] args = imageFirst ?
				new Object[]{input, scalar, lower, upper, output} :
				new Object[]{scalar, input, lower, upper, output};
		m.invoke(null, args);

		GImageMultiBand a = FactoryGImageMultiBand.wrap(input);
		GImageMultiBand b = FactoryGImageMultiBand.wrap(output);

		float tol = input.getImageType().getDataType().isInteger() ? 1 : 1e-4f;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				a.get(j, i, pixelA);
				b.get(j, i, pixelB);

				for (int k = 0; k < numBands; k++) {
					float expected = imageFirst ? pixelA[k] - val : val - pixelA[k];
					float found = pixelB[k];
					if (expected < l) expected = l;
					if (expected > u) expected = u;

					assertEquals(expected, found, tol);
				}
			}
		}
	}

	private void testBound( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		double max = 100;
		double min = -100;

		GImageMiscOps.fillUniform(input, rand, (int)min, (int)max);

		if (input.getDataType().isInteger()) {
			m.invoke(null, input, 2, 10);
		} else
			m.invoke(null, input, 2.0f, 10.0f);

		GImageGray a = FactoryGImageGray.wrap(input);
		if (input.getDataType().isInteger()) {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int v = a.get(j, i).intValue();
					assertTrue(v >= 2 && v <= 10);
				}
			}
		} else {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					float v = a.get(j, i).floatValue();
					assertTrue(v >= 2f && v <= 10f);
				}
			}
		}
	}

	private void testAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputB = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if (inputA.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(inputA, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0, 20);
		}

		m.invoke(null, inputA, inputB);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double a = GeneralizedImageOps.get(inputA, j, i, k);
					double b = GeneralizedImageOps.get(inputB, j, i, k);

					assertEquals(Math.abs(a), b, 1e-4);
				}
			}
		}
	}

	private void testNegative( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputB = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if (inputA.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(inputA, rand, -20, 20);
		} else {
			throw new RuntimeException("Shouldn't be used on unsigned images");
		}

		m.invoke(null, inputA, inputB);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double a = GeneralizedImageOps.get(inputA, j, i, k);
					double b = GeneralizedImageOps.get(inputB, j, i, k);

					assertEquals(a, -b, 1e-4);
				}
			}
		}
	}

	private void testDiffAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputB = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputC = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if (inputA.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(inputA, rand, -20, 20);
			GImageMiscOps.fillUniform(inputB, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0, 20);
			GImageMiscOps.fillUniform(inputB, rand, -20, 20);
		}

		m.invoke(null, inputA, inputB, inputC);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < numBands; k++) {
					double v = GeneralizedImageOps.get(inputA, j, i, k) - GeneralizedImageOps.get(inputB, j, i, k);
					double valC = GeneralizedImageOps.get(inputC, j, i, k);
					assertEquals(Math.abs(v), valC, 1e-4);
				}
			}
		}
	}

	private void testAdd( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageGray inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageGray inputC = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		if (inputA.getDataType().isSigned()) {
			GImageMiscOps.fillUniform(inputA, rand, -20, 20);
			GImageMiscOps.fillUniform(inputB, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0, 20);
			GImageMiscOps.fillUniform(inputB, rand, -20, 20);
		}

		m.invoke(null, inputA, inputB, inputC);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double a = GeneralizedImageOps.get(inputA, j, i);
				double b = GeneralizedImageOps.get(inputB, j, i);
				double c = GeneralizedImageOps.get(inputC, j, i);

				assertEquals(a + b, c, 1e-4);
			}
		}
	}

	private void testSubtract( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageGray inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageGray inputC = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		if (inputA.getDataType().isSigned()) {
			GImageMiscOps.fillUniform(inputA, rand, -20, 20);
			GImageMiscOps.fillUniform(inputB, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0, 40);
			GImageMiscOps.fillUniform(inputB, rand, 0, 40);
		}

		m.invoke(null, inputA, inputB, inputC);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double a = GeneralizedImageOps.get(inputA, j, i);
				double b = GeneralizedImageOps.get(inputB, j, i);
				double c = GeneralizedImageOps.get(inputC, j, i);

				assertEquals(a - b, c, 1e-4);
			}
		}
	}

	private void testLog( Method m ) throws InvocationTargetException, IllegalAccessException {
		double value = 0.5;
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		Object number = primitive(value, paramTypes[1]);
		;
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		GImageMiscOps.fillUniform(input, rand, -20, 20);
		GImageMiscOps.fillUniform(output, rand, -20, 20);

		m.invoke(null, input, number, output);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				for (int band = 0; band < numBands; band++) {
					double a = GeneralizedImageOps.get(input, col, row, band);
					double b = GeneralizedImageOps.get(output, col, row, band);

					assertEquals(Math.log(value + a), b, 1e-4);
				}
			}
		}
	}

	private void testLogSign( Method m ) throws InvocationTargetException, IllegalAccessException {
		double value = 0.5;

		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		Object number = primitive(value, paramTypes[1]);
		;
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[2], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		GImageMiscOps.fillUniform(input, rand, -20, 20);
		GImageMiscOps.fillUniform(output, rand, -20, 20);

		m.invoke(null, input, number, output);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				for (int band = 0; band < numBands; band++) {
					double a = GeneralizedImageOps.get(input, col, row, band);
					double b = GeneralizedImageOps.get(output, col, row, band);

					double expected = a < 0 ? -Math.log(value - a) : Math.log(value + a);

					assertEquals(expected, b, 1e-4);
				}
			}
		}
	}

	private void testPow2( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		if (input.getImageType().getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
			GImageMiscOps.fillUniform(output, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 40);
			GImageMiscOps.fillUniform(output, rand, 0, 40);
		}
		m.invoke(null, input, output);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				for (int band = 0; band < numBands; band++) {
					double a = GeneralizedImageOps.get(input, col, row, band);
					double b = GeneralizedImageOps.get(output, col, row, band);

					assertEquals(a*a, b, 1e-4);
				}
			}
		}
	}

	private void testStdev( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageGray inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		GImageMiscOps.fillUniform(inputA, rand, 0, 40);
		GImageMiscOps.fillUniform(inputB, rand, 0, 100);
		GImageMiscOps.fillUniform(output, rand, 0, 40);
		m.invoke(null, inputA, inputB, output);

		double tol = inputA.getDataType().isInteger() ? 1.0 : 1e-4;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double a = GeneralizedImageOps.get(inputA, j, i);
				double b = GeneralizedImageOps.get(inputB, j, i);
				double found = GeneralizedImageOps.get(output, j, i);

				double expected = Math.sqrt(Math.max(0, b - a*a));

				assertEquals(expected, found, tol);
			}
		}
	}

	private void testSqrt( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase output = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);
		int numBands = input.getImageType().getNumBands();

		GImageMiscOps.fillUniform(input, rand, -20, 20);
		GImageMiscOps.fillUniform(output, rand, -20, 20);

		m.invoke(null, input, output);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				for (int band = 0; band < numBands; band++) {
					double a = GeneralizedImageOps.get(input, col, row, band);
					double b = GeneralizedImageOps.get(output, col, row, band);

					assertEquals(Math.sqrt(a), b, 1e-4);
				}
			}
		}
	}

	private void TestAverageBand( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height, 3);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if (output.getDataType().isSigned()) {
			GImageMiscOps.fillUniform(input, rand, -20, 20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		m.invoke(null, input, output);

		GImageGray a = FactoryGImageGray.wrap(input.getBand(0));
		GImageGray b = FactoryGImageGray.wrap(input.getBand(1));
		GImageGray c = FactoryGImageGray.wrap(input.getBand(2));
		GImageGray d = FactoryGImageGray.wrap(output);

		boolean isInteger = output.getDataType().isInteger();

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double expected = 0;
				expected += a.get(j, i).doubleValue();
				expected += b.get(j, i).doubleValue();
				expected += c.get(j, i).doubleValue();
				expected /= 3;

				double found = d.get(j, i).doubleValue();

				if (isInteger)
					expected = (int)expected;

				assertEquals(expected, found, 1e-4);
			}
		}
	}
}
