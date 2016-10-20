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
import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
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

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestConvolveDownNoBorderStandard {
	Random rand = new Random(0xFF);

	int width;
	int height;
	int kernelRadius = 2;
	int skip;

	/**
	 * Automatically compares all the box filters against a generalize convolution
	 */
	@Test
	public void compareToGeneral() {
		// try different image sizes
		for( int plus = 0; plus <= kernelRadius+1; plus++ ) {
			width = 10 + plus;
			height = 10 + plus;
			// try different edges in the image as test points
			for( skip = 1; skip <= 4; skip++ ) {
				System.out.println(width+" "+height+" skip "+skip);
				CompareToFull tests = new CompareToFull(ConvolveDownNoBorderStandard.class);

				tests.performTests(15);
			}
		}
	}

	@SuppressWarnings({"UnusedAssignment"})
	public class CompareToFull extends CompareEquivalentFunctions {

		int DIV = 10;

		protected CompareToFull(Class<?> testClass ) {
			super(testClass, ConvolveImageNoBorder.class );
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

			if( evaluation.getName().compareTo("convolve") == 0) {
				if( e.length != c.length+1 )
					return false;
			} else  {
				if( e.length != c.length+1 )
					return false;
			}

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

			int validationParams = m.getParameterTypes().length;

			ImageGray input = (ImageGray)targetParam[1];
			ImageGray output = (ImageGray)targetParam[2];

			Object[] ret = new Object[ validationParams ];

			ret[0] = targetParam[0];
			ret[1] = input.clone();
			ret[2] = output.createNew(input.width,input.height);

			if( ret.length == 4 ) {
				ret[3] = DIV;
			}

			return ret;
		}

		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

			ImageGray input = (ImageGray)targetParam[1];

			GImageGray t = FactoryGImageGray.wrap((ImageGray) targetParam[2]);
			GImageGray v = FactoryGImageGray.wrap((ImageGray) validationParam[2]);


			int minY=0,minX=0,maxX=input.width,maxY=input.height;

			int ratioWidth = v.getWidth() != t.getWidth() ? skip : 1;
			int ratioHeight = v.getHeight() != t.getHeight() ? skip : 1;

			if( methodTest.getName().contentEquals("convolve")) {
				minX = minY = UtilDownConvolve.computeOffset(skip,kernelRadius);
				maxX = UtilDownConvolve.computeMaxSide(input.width,skip,kernelRadius);
				maxY = UtilDownConvolve.computeMaxSide(input.width,skip,kernelRadius);
			} else if( methodTest.getName().contentEquals("horizontal")) {
				minX = UtilDownConvolve.computeOffset(skip,kernelRadius);
				maxX = UtilDownConvolve.computeMaxSide(input.width,skip,kernelRadius);
			} else if( methodTest.getName().contentEquals("vertical")) {
				minY = UtilDownConvolve.computeOffset(skip,kernelRadius);
				maxY = UtilDownConvolve.computeMaxSide(input.height,skip,kernelRadius);
			}

			for( int y = 0; y < t.getHeight(); y++ ) {
				for( int x = 0; x < t.getWidth(); x++ ) {
					int xx = x*ratioWidth;
					int yy = y*ratioHeight;

					double valT = t.get(x,y).doubleValue();
					double valV = v.get(x*ratioWidth,y*ratioHeight).doubleValue();
					if( xx < minX || xx > maxX || yy < minY || y > maxY )
						valV = 0;

					double sum = Math.abs(valT)+Math.abs(valV);
					if( sum > 0 ) {
						// normalizing the magnitude is needed for floating point numbers
						double diff = Math.abs(valV-valT)/sum;
						assertTrue(valV+" "+valT+"  diff "+diff+"  at ( "+x+" "+y+" )",diff<=1e-4);
					}
				}
			}
		}
	}
}
