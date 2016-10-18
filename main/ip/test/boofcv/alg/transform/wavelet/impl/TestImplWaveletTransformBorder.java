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

import boofcv.core.image.border.BorderIndex1D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlBorderCoef;
import boofcv.struct.wavelet.WlCoef;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author Peter Abeles
 */
public class TestImplWaveletTransformBorder extends CompareToNaiveWavelet {

	public TestImplWaveletTransformBorder() {
		super(2,ImplWaveletTransformBorder.class);
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
		PermuteWaveletCompare test = new BorderCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(false);
	}

	public void checkVertical( Method m ) {
		PermuteWaveletCompare test = new BorderCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(false);
	}

	public void checkHorizontalInverse( Method m ) {
		PermuteWaveletCompare test = new BorderCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
//				System.out.println();
//				BoofTesting.printDiff(expected,found);
				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(true);
	}

	public void checkVerticalInverse( Method m ) {
		PermuteWaveletCompare test = new BorderCompare() {
			@Override
			public void compareResults(WaveletDescription<?> desc, ImageGray input,
									   ImageGray expected, ImageGray found ) {
				BoofTesting.assertEquals(expected, found, 1e-4f);
			}
		};

		test.runTests(true);
	}

	private abstract class BorderCompare extends BaseCompare {
		@Override
		public void applyTransform(WaveletDescription<?> desc, ImageGray input, ImageGray output) {
			TestImplWaveletTransformInner.applyInnerMethod(functionName,desc,input,output);
			
			Method m;
			Object args[];

			BorderIndex1D border = desc.getBorder();

			if( functionName.contains("Inverse")) {
				WlBorderCoef<?> inv = desc.getInverse();
				m = BoofTesting.findMethod(ImplWaveletTransformBorder.class,functionName,border.getClass(),inv.getClass(),input.getClass(),output.getClass());
				args = new Object[]{border,inv,input,output};
			} else {
				WlCoef forward = desc.getForward();
				m = BoofTesting.findMethod(ImplWaveletTransformBorder.class,functionName,border.getClass(),forward.getClass(),input.getClass(),output.getClass());
				args = new Object[]{border,forward,input,output};
			}

			try {
				m.invoke(null,args);
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
