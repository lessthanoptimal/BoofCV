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
import gecv.struct.wavelet.WlBorderCoef;
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
				WlCoef coef = desc.getForward();
				equalsTranHorizontal(coef,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkVertical( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				WlCoef coef = desc.getForward();
				equalsTranVertical(coef,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkHorizontalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				WlCoef coef = desc.getForward();
				int border = Math.max(UtilWavelet.computeBorderStart(coef),
						UtilWavelet.computeBorderEnd(coef,expected.width,input.width))-coef.offsetScaling*2;

				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f,border);
			}
		};

		test.runTests(true);
	}

	public void checkVerticalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageBase input,
									   ImageBase expected, ImageBase found ) {
				WlCoef coef = desc.getForward();
				int border = Math.max(UtilWavelet.computeBorderStart(coef),
						UtilWavelet.computeBorderEnd(coef,expected.height,input.height))-coef.offsetScaling*2;

				GecvTesting.assertEqualsGeneric(expected,found,0,1e-4f,border);
			}
		};

		test.runTests(true);
	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal( WlCoef desc,
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

	private void equalsTranVertical( WlCoef desc,
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

	private abstract class InnerCompare extends BaseCompare {
		@Override
		public void applyTransform(WaveletDescription<?> desc, ImageBase input, ImageBase output) {
			applyInnerMethod(functionName,desc, input, output);
		}
	}

	public static void applyInnerMethod( String functionName , WaveletDescription<?> desc, ImageBase input, ImageBase output) {
		Method m;
		Object args[];
		if( functionName.contains("Inverse")) {
			WlBorderCoef<?> inv = desc.getInverse();
			m = GecvTesting.findMethod(ImplWaveletTransformInner.class,functionName,inv.getClass(),input.getClass(),output.getClass());
			args = new Object[]{inv,input,output};
		} else {
			Class<?> coefClass = desc.getForward().getClass();
			m = GecvTesting.findMethod(ImplWaveletTransformInner.class,functionName,coefClass,input.getClass(),output.getClass());
			args = new Object[]{desc.getForward(),input,output};
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
