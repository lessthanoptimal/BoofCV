/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.wavelet.impl;

import gecv.alg.wavelet.UtilWavelet;
import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageBase;
import gecv.struct.wavelet.WaveletCoefficient;
import gecv.struct.wavelet.WaveletCoefficient_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformInner {
	int numExpected = 3;

	boolean isFloat;
	Class<?> typeInput;
	Class<?> typeOutput;
	String functionName;

	@Test
	public void checkAllHorizontal() {
		checkAll("horizontal","checkHorizontal");
	}

	@Test
	public void checkAllVertical() {
		checkAll("vertical","checkVertical");
	}

	@Test
	public void checkAllHorizontalInverse() {
		checkAll("horizontalInverse","checkHorizontalInverse");
	}

	@Test
	public void checkAllVerticalInverse() {
		checkAll("verticalInverse","checkVerticalInverse");
	}

	private void checkAll( String functionName , String testMethodName ) {
		this.functionName = functionName;
		Method methods[] = ImplWaveletTransformInner.class.getMethods();
		Method testMethod;

		try {
			testMethod = getClass().getMethod(testMethodName,Method.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( m.getName().compareTo(functionName) != 0)
				continue;

			isFloat = m.getParameterTypes()[0] == WaveletCoefficient_F32.class;
			typeInput = m.getParameterTypes()[1];
			typeOutput = m.getParameterTypes()[2];

//			System.out.println(typeInput.getSimpleName()+" "+typeOutput.getSimpleName());

			try {
				testMethod.invoke(this,m);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}


	public void checkHorizontal( Method m ) {
		PermuteWaveletCompare test = new BaseCompare() {
			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				equalsTranHorizontal(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkVertical( Method m ) {
		PermuteWaveletCompare test = new BaseCompare() {
			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				equalsTranVertical(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkHorizontalInverse( Method m ) {
		PermuteWaveletCompare test = new BaseCompare() {
			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				int border = Math.max(UtilWavelet.computeBorderStart(desc),
						UtilWavelet.computeBorderEnd(desc,expected.width,input.width))-desc.offsetScaling*2;

				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f,border);
			}
		};

		test.runTests(true);
	}

	public void checkVerticalInverse( Method m ) {
		PermuteWaveletCompare test = new BaseCompare() {
			@Override
			public void compareResults(WaveletCoefficient desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				int border = Math.max(UtilWavelet.computeBorderStart(desc),
						UtilWavelet.computeBorderEnd(desc,expected.height,input.height))-desc.offsetScaling*2;

				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f,border);
			}
		};

		test.runTests(true);
	}

	/**
	 * Invokes test validation methods using reflections
	 */
	private abstract class BaseCompare extends PermuteWaveletCompare {
		protected BaseCompare() {
			super(typeInput, typeOutput);
		}

		@Override
		public void applyValidation(WaveletCoefficient desc, ImageBase input, ImageBase output) {
			Method m = GecvTesting.findMethod(ImplWaveletTransformNaive.class,functionName,desc.getClass(),input.getClass(),output.getClass());
			try {
				m.invoke(null,desc,input,output);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void applyTransform(WaveletCoefficient desc, ImageBase input, ImageBase output) {
			try {
				Method m = ImplWaveletTransformInner.class.getMethod(functionName,desc.getClass(),input.getClass(),output.getClass());
				m.invoke(null,desc,input,output);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal( WaveletCoefficient desc,
									   ImageBase input ,
									   ImageBase expected , ImageBase found ) {

		int begin = UtilWavelet.computeBorderStart(desc);
		int end = expected.getWidth()-UtilWavelet.computeBorderEnd(desc,input.width,expected.width);

		int w = expected.width;
		int h = expected.height;

		equalsTranHorizontal(expected.subimage(0,0,w/2,h),found.subimage(0,0,w/2,h),begin/2,end/2,"left");
		equalsTranHorizontal(expected.subimage(w/2,0,w,h),found.subimage(w/2,0,w,h),begin/2,end/2,"right");
	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal( ImageBase expected , ImageBase found ,
									   int begin , int end , String quad ) {

		SingleBandImage e = FactorySingleBandImage.wrap(expected);
		SingleBandImage f = FactorySingleBandImage.wrap(found);

		for( int y = 0; y < expected.height; y++ ) {
			for( int x = 0; x < expected.width; x++ ) {
				// see if the inner image is identical to the naive implementation
				// the border should be unmodified, zeros
				if( x >= begin && x < end )
					assertEquals(quad+" ( "+x+" , "+y+" )",e.get(x,y).floatValue() , f.get(x,y).floatValue() , 1e-4f);
				else
					assertTrue(quad+" ( "+x+" , "+y+" ) 0 != "+f.get(x,y),0 == f.get(x,y).floatValue());
			}
		}
	}

	private void equalsTranVertical( WaveletCoefficient desc,
									 ImageBase input ,
									 ImageBase expected , ImageBase found ) {
		int begin = UtilWavelet.computeBorderStart(desc);
		int end = expected.getHeight()-UtilWavelet.computeBorderEnd(desc,input.height,expected.height);

		int w = expected.width;
		int h = expected.height;

		equalsTranVertical(expected.subimage(0,0,w,h/2),found.subimage(0,0,w,h/2),begin/2,end/2,"top");
		equalsTranVertical(expected.subimage(0,h/2,w,h),found.subimage(0,h/2,w,h),begin/2,end/2,"bottom");
	}

	private void equalsTranVertical( ImageBase expected , ImageBase found ,
									 int begin , int end , String quad ) {

		SingleBandImage e = FactorySingleBandImage.wrap(expected);
		SingleBandImage f = FactorySingleBandImage.wrap(found);

		for( int y = 0; y < expected.height; y++ ) {
			// see if the inner image is identical to the naive implementation
			// the border should be unmodified, zeros
			if( y >= begin && y < end ) {
				for( int x = 0; x < expected.width; x++ ) {
					assertEquals(quad+" ( "+x+" , "+y+" )",e.get(x,y).floatValue() , f.get(x,y).floatValue() , 1e-4f);
				}
			} else {
				for( int x = 0; x < expected.width; x++ ) {
					assertTrue(quad+" ( "+x+" , "+y+" ) 0 != "+f.get(x,y),0 == f.get(x,y).floatValue());
				}
			}
		}
	}

}
