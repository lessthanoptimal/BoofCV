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

import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPixelMath {
	int width = 10;
	int height = 15;
	Random rand = new Random(234);

	@Test
	public void checkAll() {
		int numExpected = 2*11+11*8;
		Method methods[] = PixelMath.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
				System.out.println(m.getName());
				if( m.getName().compareTo("divide") == 0 ) {
					if( m.getParameterTypes().length == 3 ) {
						if( ImageBase.class.isAssignableFrom(m.getParameterTypes()[1]))  {
							testDividePixel(m);
						} else {
							testDivide(m);
						}
					} else
						testDivideBounded(m);
				} else if( m.getName().compareTo("multiply") == 0 ) {
					if( m.getParameterTypes().length == 3 ) {
						if( ImageBase.class.isAssignableFrom(m.getParameterTypes()[1]))  {
							testMultiplyPixel(m);
						} else {
							testMultiply(m);
						}
					} else
						testMultiplyBounded(m);
				} else if( m.getName().compareTo("plus") == 0 ) {
					if( m.getParameterTypes().length == 3 )
						testPlus(m);
					else
						testPlusBounded(m);
				} else if( m.getName().compareTo("add") == 0 ) {
					testAdd(m);
				} else if( m.getName().compareTo("log") == 0 ) {
					testLog(m);
				} else if( m.getName().compareTo("pow2") == 0 ) {
					testPow2(m);
				} else if( m.getName().compareTo("sqrt") == 0 ) {
					testSqrt(m);
				} else if( m.getName().compareTo("invert") == 0 ) {
					testInvert(m);
				} else if( m.getName().compareTo("subtract") == 0 ) {
					testSubtract(m);
				} else if( m.getName().compareTo("boundImage") == 0 ) {
				    testBound(m);
				} else if( m.getName().compareTo("abs") == 0 ) {
					testAbs(m);
				} else if( m.getName().compareTo("diffAbs") == 0 ) {
				    testDiffAbs(m);
				} else if( m.getName().compareTo("averageBand") == 0 ) {
					TestAverageBand(m);
				} else {
					throw new RuntimeException("Unknown function: "+m.getName());
				}
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	private boolean isTestMethod(Method m ) {

		Class param[] = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		return ImageBase.class.isAssignableFrom(param[0]);
	}

	private void testDivide( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);
		GImageMiscOps.fillUniform(input, rand, 0,20);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,10,output);
		else
			m.invoke(null,input,10.0f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()/10,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()/10f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}

	private void testDividePixel( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20,20);
		GImageMiscOps.fillUniform(inputB, rand, -20,20);

		m.invoke(null,inputA,inputB,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(inputA);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(inputB);
		GImageSingleBand o = FactoryGImageSingleBand.wrap(output);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(a.get(j,i).doubleValue()/b.get(j,i).doubleValue(),o.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testDivideBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[4], width, height);
		GImageMiscOps.fillUniform(input, rand, 0,20);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,10,-1,1,output);
		else
			m.invoke(null,input,10.0f,-1f,1f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					int expected = a.get(j,i).intValue()/10;
					int found = b.get(j,i).intValue();
					if( expected < -1 ) expected = -1;
					if( expected > 1 ) expected = 1;

					assertEquals(expected,found);
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					float expected = a.get(j,i).floatValue()/10.0f;
					float found = b.get(j,i).floatValue();
					if( expected < -1 ) expected = -1;
					if( expected > 1 ) expected = 1;

					assertEquals(expected, found, 1e-4);
				}
			}
		}
	}

	private void testMultiply( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);
		GImageMiscOps.fillUniform(input, rand, 0,20);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,2,output);
		else
			m.invoke(null,input,2.0f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()*2,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()*2f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}

	private void testMultiplyPixel( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20,20);
		GImageMiscOps.fillUniform(inputB, rand, -20,20);

		m.invoke(null,inputA,inputB,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(inputA);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(inputB);
		GImageSingleBand o = FactoryGImageSingleBand.wrap(output);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(a.get(j,i).doubleValue()*b.get(j,i).doubleValue(),o.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testMultiplyBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[4], width, height);
		GImageMiscOps.fillUniform(input, rand, 0,20);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,2,-30,30,output);
		else
			m.invoke(null,input,2.0f,-30f,30f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					int expected = a.get(j,i).intValue()*2;
					int found = b.get(j,i).intValue();
					if( expected < -30 ) expected = -30;
					if( expected > 30 ) expected = 30;

					assertEquals(expected,found);
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					float expected = a.get(j,i).floatValue()*2f;
					float found = b.get(j,i).floatValue();
					if( expected < -30 ) expected = -30;
					if( expected > 30 ) expected = 30;

					assertEquals(expected,found,1e-4);
				}
			}
		}
	}

	private void testPlus( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,2,output);
		else
			m.invoke(null,input,2.0f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()+2,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()+2f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}

	private void testPlusBounded( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		if( input.getDataType().isInteger() )
			m.invoke(null,input,2,-10,12,output);
		else
			m.invoke(null,input,2.0f,-10f,12f,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(output);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					int expected = a.get(j,i).intValue() + 2;
					int found = b.get(j,i).intValue();
					if( expected < -10 ) expected = -10;
					if( expected > 12 ) expected = 12;

					assertEquals(expected,found);
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					float expected = a.get(j,i).floatValue() + 2f;
					float found = b.get(j,i).floatValue();
					if( expected < -10 ) expected = -10;
					if( expected > 12 ) expected = 12;

					assertEquals(expected,found,1e-4);
				}
			}
		}
	}

	private void testBound( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		double max = 100;
		double min = -100;

		GImageMiscOps.fillUniform(input, rand, (int)min, (int)max);

		if( input.getDataType().isInteger() ) {
			m.invoke(null,input,2,10);
		} else
			m.invoke(null,input,2.0f,10.0f);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
		if( input.getDataType().isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					int v = a.get(j,i).intValue();
					assertTrue(v >= 2 && v <= 10);
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					float v = a.get(j,i).floatValue();
					assertTrue(v >= 2f && v <= 10f);
				}
			}
		}
	}

	private void testAbs(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		m.invoke(null,inputA,inputB);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);

				assertEquals(Math.abs(a),b,1e-4);
			}
		}
	}

	private void testInvert(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			throw new RuntimeException("Shouldn't be used on unsigned images");
		}

		m.invoke(null,inputA,inputB);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);

				assertEquals(a,-b,1e-4);
			}
		}
	}

	private void testDiffAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputC = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		}

		m.invoke(null,inputA,inputB,inputC);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(inputA);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(inputB);
		GImageSingleBand c = FactoryGImageSingleBand.wrap(inputC);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				float v = a.get(j,i).floatValue()-b.get(j,i).floatValue();
				assertEquals(Math.abs(v),c.get(j,i).floatValue(),1e-4);
			}
		}
	}

	private void testAdd(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageSingleBand inputC = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		}

		m.invoke(null,inputA,inputB,inputC);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);
				double c = GeneralizedImageOps.get(inputC,j,i);

				assertEquals(a+b,c,1e-4);
			}
		}
	}

	private void testSubtract(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
		ImageSingleBand inputC = GeneralizedImageOps.createSingleBand(paramTypes[2], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,40);
			GImageMiscOps.fillUniform(inputB, rand, 0,40);
		}

		m.invoke(null,inputA,inputB,inputC);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);
				double c = GeneralizedImageOps.get(inputC,j,i);

				assertEquals(a-b,c,1e-4);
			}
		}
	}

	private void testLog(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20,20);
		GImageMiscOps.fillUniform(inputB, rand, -20,20);

		m.invoke(null,inputA,inputB);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);

				assertEquals(Math.log(1+a),b,1e-4);
			}
		}
	}

	private void testPow2(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20,20);
		GImageMiscOps.fillUniform(inputB, rand, -20,20);

		m.invoke(null,inputA,inputB);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);

				assertEquals(a*a,b,1e-4);
			}
		}
	}

	private void testSqrt(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		GImageMiscOps.fillUniform(inputA, rand, -20,20);
		GImageMiscOps.fillUniform(inputB, rand, -20,20);

		m.invoke(null,inputA,inputB);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);

				assertEquals(Math.sqrt(a),b,1e-4);
			}
		}
	}

	private void TestAverageBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		MultiSpectral input = new MultiSpectral(paramTypes[1], width, height,3);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}

		m.invoke(null,input,output);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input.getBand(0));
		GImageSingleBand b = FactoryGImageSingleBand.wrap(input.getBand(1));
		GImageSingleBand c = FactoryGImageSingleBand.wrap(input.getBand(2));
		GImageSingleBand d = FactoryGImageSingleBand.wrap(output);

		boolean isInteger = output.getDataType().isInteger();

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double expected = 0;
				expected += a.get(j,i).doubleValue();
				expected += b.get(j,i).doubleValue();
				expected += c.get(j,i).doubleValue();
				expected /= 3;

				double found = d.get(j,i).doubleValue();

				if( isInteger )
					expected = (int)expected;
			
				assertEquals(expected,found,1e-4);
			}
		}
	}
}
