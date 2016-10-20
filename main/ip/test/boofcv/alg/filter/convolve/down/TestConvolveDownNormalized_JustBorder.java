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

package boofcv.alg.filter.convolve.down;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.filter.convolve.normalized.ConvolveNormalizedNaive;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveDownNormalized_JustBorder {

	Random rand = new Random(0xFF);

	static int width;
	static int height;
	static int kernelRadius = 2;
	static int skip;

	@Test
	public void compareToFullNaive() {
		CompareToFull compare = new CompareToFull(ConvolveDownNormalized_JustBorder.class);

		for( int i = 0; i < 2; i++ ) {
		    width = 20 + i;
			height = 25 + i;
			skip = 1;
			compare.performTests(9);
			skip = 2;
			compare.performTests(9);
			skip = 3;
			compare.performTests(9);
		}
	}

	@SuppressWarnings({"UnusedAssignment"})
	public class CompareToFull extends CompareEquivalentFunctions {

		int DIV = 10;

		protected CompareToFull(Class<?> testClass ) {
			super(testClass, ConvolveNormalizedNaive.class );
		}

		@Override
		protected Object[][] createInputParam(Method candidate, Method validation) {
			Class<?> paramTypes[] = candidate.getParameterTypes();

			KernelBase kernel = FactoryKernel.random(paramTypes[0],kernelRadius,0,5,rand);

			int divW,divH;

			if( candidate.getName().compareTo("horizontal") == 0) {
				divW = skip; divH = 1;
			} else if( candidate.getName().compareTo("vertical") == 0) {
				divW = 1; divH = skip;
			} else {
				divW = divH = skip;
			}

			ImageGray src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
			GImageMiscOps.fillUniform(src, rand, 0, 130);
			ImageGray dst = ConvolutionTestHelper.createImage(paramTypes[2], width/divW, height/divH);

			Object[][] ret = new Object[1][paramTypes.length];

			ret[0][0] = kernel;
			ret[0][1] = src;
			ret[0][2] = dst;
			ret[0][3] = skip;
			if( paramTypes.length == 5 )
				ret[0][4] = DIV;

			return ret;
		}

		@Override
		protected boolean isTestMethod(Method m) {
			Class<?> e[] = m.getParameterTypes();

			for( Class<?> c : e ) {
				if( ImageGray.class.isAssignableFrom(c))
					return true;
			}
			return false;
		}

		@Override
		protected boolean isEquivalent(Method candidate, Method evaluation) {
			if( evaluation.getName().compareTo(candidate.getName()) != 0 )
				return false;

			Class<?> e[] = evaluation.getParameterTypes();
			Class<?> c[] = candidate.getParameterTypes();

			if( c.length < 3 )
				return false;

			for( int i = 0; i < 3; i++ ) {
				if( e[i] != c[i])
					return false;
			}

			// handle kernels with divisors
			if( e.length == 5 ) {
				if( c.length < 4 )
					return false;
				if( c[3] != int.class )
					return false;
			}

			return true;
		}

		@Override
		protected Object[] reformatForValidation(Method m, Object[] targetParam) {

			int kernelDimen = ((KernelBase)targetParam[0]).getDimension();
			int validationParams = targetParam.length-1;

			ImageGray input = (ImageGray)targetParam[1];
			ImageGray output = (ImageGray)targetParam[2];

			Object[] ret = new Object[ validationParams ];

			ret[0] = targetParam[0];
			ret[1] = input.clone();
			ret[2] = output.createNew(input.width,input.height);

			if( ret.length == 4 ) {
				if( m.getParameterTypes()[3] == int.class )
					ret[3] = DIV;
				else
					ret[3] = true;
			} else if( ret.length == 5 ) {
				ret[3] = DIV;
				ret[4] = true;
			}

			return ret;
		}

		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

			GImageGray t = FactoryGImageGray.wrap((ImageGray) targetParam[2]);
			GImageGray v = FactoryGImageGray.wrap((ImageGray) validationParam[2]);

			int ratioWidth = v.getWidth() != t.getWidth() ? skip : 1;
			int ratioHeight = v.getHeight() != t.getHeight() ? skip : 1;

			int begin = UtilDownConvolve.computeOffset(skip,kernelRadius);
			int endX = UtilDownConvolve.computeMaxSide(width,skip,kernelRadius);
			int endY = UtilDownConvolve.computeMaxSide(height,skip,kernelRadius);

			begin /= skip;
			endX /= skip;
			endY /= skip;

			if( methodTest.getName().contentEquals("convolve")) {
				for( int y = 0; y < height/skip; y++ ) {
					for( int x = 0; x < width/skip; x++ ) {
						if( y < begin || y > endY || x < begin || x > endX)
						{
							Number numT = t.get(x,y);
							Number numV = v.get(x*ratioWidth,y*ratioHeight);

							assertEquals( "("+x+" , "+y+")",numV.doubleValue() , numT.doubleValue() , 1e-4 );
						}
					}
				}
			} else if( methodTest.getName().contentEquals("horizontal") ) {
				for( int y = 0; y < height; y++ ) {
					for( int x = 0; x < width/skip; x++ ) {
						if( x < begin || x > endX )
						{
							Number numT = t.get(x,y);
							Number numV = v.get(x*ratioWidth,y*ratioHeight);

							assertEquals( x+" "+y,numV.doubleValue() , numT.doubleValue() , 1e-4 );
						}
					}
				}
			} else if( methodTest.getName().contentEquals("vertical")) {
				for( int y = 0; y < height/skip; y++ ) {
					for( int x = 0; x < width; x++ ) {
						if( y < begin || y > endY )
						{
							Number numT = t.get(x,y);
							Number numV = v.get(x*ratioWidth,y*ratioHeight);

							assertEquals( x+" "+y,numV.doubleValue() , numT.doubleValue() , 1e-4 );
						}
					}
				}
			}
		}
	}
}
