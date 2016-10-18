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

package boofcv.alg.transform.wavelet.impl;

import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;
import boofcv.testing.BoofTesting;
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
		super(2,ImplWaveletTransformInner.class);
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
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				equalsTranHorizontal(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkVertical( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				equalsTranVertical(desc,input,expected,found );
			}
		};

		test.runTests(false);
	}

	public void checkHorizontalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
//				BoofTesting.printDiff(expected,found);
				int lowerBorder = UtilWavelet.borderInverseLower(desc.getInverse(),desc.getBorder());
				int upperBorder = UtilWavelet.borderInverseUpper(desc.getInverse(),desc.getBorder(),found.getWidth());

				int w = expected.getWidth();
				int h = expected.getHeight();
				
//				System.out.print("lb "+lowerBorder+" ub "+upperBorder);

				// only compare the relevant portion of the images
				expected = expected.subimage(lowerBorder,0,w+w%2-upperBorder,h, null);
				found = found.subimage(lowerBorder,0,w+w%2-upperBorder,h, null);
				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(true);
	}

	public void checkVerticalInverse( Method m ) {
		PermuteWaveletCompare test = new InnerCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				int lowerBorder = UtilWavelet.borderInverseLower(desc.getInverse(),desc.getBorder());
				int upperBorder = UtilWavelet.borderInverseUpper(desc.getInverse(),desc.getBorder(),found.getHeight());

				int w = expected.getWidth();
				int h = expected.getWidth();

				// only compare the relevant portion of the images
				expected = expected.subimage(0,lowerBorder,w,h+h%2-upperBorder, null);
				found = found.subimage(0,lowerBorder,w,h+h%2-upperBorder, null);

				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(true);
	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal(WaveletDescription<?> desc,
									  ImageGray input ,
									  ImageGray expected , ImageGray found ) {
		int w = expected.width;
		int h = expected.height;

		int lowerBorder = UtilWavelet.borderForwardLower(desc.getInverse().getInnerCoefficients());
		int upperBorder = input.width-UtilWavelet.borderForwardUpper(desc.getInverse().getInnerCoefficients(),input.width);

		equalsTranHorizontal(expected.subimage(0,0,w/2,h, null),found.subimage(0,0,w/2,h, null),lowerBorder/2,upperBorder/2,"left");
		equalsTranHorizontal(expected.subimage(w/2,0,w,h, null),found.subimage(w/2,0,w,h, null),lowerBorder/2,upperBorder/2,"right");
	}

	/**
	 * Compares two wavelet transformations while ignoring the input image borders.  Input borders
	 * affect the borders of internal segments inside the transformation.
	 */
	private void equalsTranHorizontal(ImageGray expected , ImageGray found ,
									  int begin , int end , String quad ) {

		GImageGray e = FactoryGImageGray.wrap(expected);
		GImageGray f = FactoryGImageGray.wrap(found);

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

	private void equalsTranVertical(WaveletDescription<?> desc,
									ImageGray input ,
									ImageGray expected , ImageGray found ) {
		int w = expected.width;
		int h = expected.height;

		int lowerBorder = UtilWavelet.borderForwardLower(desc.getInverse().getInnerCoefficients());
		int upperBorder = input.height-UtilWavelet.borderForwardUpper(desc.getInverse().getInnerCoefficients(),input.height);

		equalsTranVertical(expected.subimage(0,0,w,h/2, null),found.subimage(0,0,w,h/2, null),lowerBorder/2,upperBorder/2,"top");
		equalsTranVertical(expected.subimage(0,h/2,w,h, null),found.subimage(0,h/2,w,h, null),lowerBorder/2,upperBorder/2,"bottom");
	}

	private void equalsTranVertical(ImageGray expected , ImageGray found ,
									int begin , int end , String quad ) {

		GImageGray e = FactoryGImageGray.wrap(expected);
		GImageGray f = FactoryGImageGray.wrap(found);

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
		public void applyTransform(WaveletDescription<?> desc, ImageGray input, ImageGray output) {
			applyInnerMethod(functionName,desc, input, output);
		}
	}

	public static void applyInnerMethod(String functionName , WaveletDescription<?> desc, ImageGray input, ImageGray output ) {

		Method m;
		Object args[];
		if( functionName.contains("Inverse")) {
			WlCoef coef = desc.getInverse().getInnerCoefficients();
			m = BoofTesting.findMethod(ImplWaveletTransformInner.class,functionName,coef.getClass(),input.getClass(),output.getClass());
			args = new Object[]{coef,input,output};
		} else {
			WlCoef coef = desc.getForward();
			m = BoofTesting.findMethod(ImplWaveletTransformInner.class,functionName,coef.getClass(),input.getClass(),output.getClass());
			args = new Object[]{coef,input,output};
		}

		try {
			m.invoke(null,args);
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
