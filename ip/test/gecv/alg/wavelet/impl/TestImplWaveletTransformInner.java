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
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformInner extends CompareToNaiveWavelet {

	public TestImplWaveletTransformInner() {
		super(3,ImplWaveletTransformInner.class);
	}

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

	public void checkHorizontal( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				equalsTranHorizontal(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkVertical( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				equalsTranVertical(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkHorizontalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				GecvTesting.printDiff(expected,found);
				int lowerBorder = UtilWavelet.borderInverseLower(desc.getInverse(),desc.getBorder());
				int upperBorder = UtilWavelet.borderInverseUpper(desc.getInverse(),desc.getBorder(),found.getWidth());

				int w = expected.getWidth();
				int h = expected.getHeight();

				// only compare the relevant portion of the images
				expected = expected.subimage(lowerBorder,0,w+w%2-upperBorder,h);
				found = found.subimage(lowerBorder,0,w+w%2-upperBorder,h);
				System.out.print("lb "+lowerBorder+" ub "+upperBorder);
				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}

	public void checkVerticalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				int lowerBorder = UtilWavelet.borderInverseLower(desc.getInverse(),desc.getBorder());
				int upperBorder = UtilWavelet.borderInverseUpper(desc.getInverse(),desc.getBorder(),found.getHeight());

				int w = expected.getWidth();
				int h = expected.getWidth();

				// only compare the relevant portion of the images
				expected = expected.subimage(0,lowerBorder,w,h+h%2-upperBorder);
				found = found.subimage(0,lowerBorder,w,h+h%2-upperBorder);

				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f);
			}
		};

		test.runTests(true);
	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal( WaveletDescription<?> desc,
									   ImageBase input ,
									   ImageBase expected , ImageBase found ) {
		int w = expected.width;
		int h = expected.height;

		int lowerBorder = UtilWavelet.borderForwardLower(desc.getInverse().getInnerCoefficients());
		int upperBorder = input.width-UtilWavelet.borderForwardUpper(desc.getInverse().getInnerCoefficients(),input.width);

		equalsTranHorizontal(expected.subimage(0,0,w/2,h),found.subimage(0,0,w/2,h),lowerBorder/2,upperBorder/2,"left");
		equalsTranHorizontal(expected.subimage(w/2,0,w,h),found.subimage(w/2,0,w,h),lowerBorder/2,upperBorder/2,"right");
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

	private void equalsTranVertical( WaveletDescription<?> desc,
									 ImageBase input ,
									 ImageBase expected , ImageBase found ) {
		int w = expected.width;
		int h = expected.height;

		int lowerBorder = UtilWavelet.borderForwardLower(desc.getInverse().getInnerCoefficients());
		int upperBorder = input.height-UtilWavelet.borderForwardUpper(desc.getInverse().getInnerCoefficients(),input.height);

		equalsTranVertical(expected.subimage(0,0,w,h/2),found.subimage(0,0,w,h/2),lowerBorder/2,upperBorder/2,"top");
		equalsTranVertical(expected.subimage(0,h/2,w,h),found.subimage(0,h/2,w,h),lowerBorder/2,upperBorder/2,"bottom");
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

	private abstract class InnerCompare extends BaseCompare {
		@Override
		public void applyTransform(WaveletDescription<?> desc, ImageBase input, ImageBase output) {
			applyInnerMethod(functionName,desc, input, output);
		}
	}

	public static void applyInnerMethod( String functionName , WaveletDescription<?> desc, ImageBase input, ImageBase output ) {

		Method m;
		Object args[];
		if( functionName.contains("Inverse")) {
			WlCoef coef = desc.getInverse().getInnerCoefficients();
			m = GecvTesting.findMethod(ImplWaveletTransformInner.class,functionName,coef.getClass(),input.getClass(),output.getClass());
			args = new Object[]{coef,input,output};
		} else {
			WlCoef coef = desc.getForward();
			m = GecvTesting.findMethod(ImplWaveletTransformInner.class,functionName,coef.getClass(),input.getClass(),output.getClass());
			args = new Object[]{coef,input,output};
		}

		try {
			m.invoke(null,args);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
