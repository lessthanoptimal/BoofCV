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

package boofcv.alg.filter.convolve.normalized;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalized_JustBorder {
	@Test
	public void compareToNaive() {
		CompareToNaive test = new CompareToNaive();
		int numFunctions = 17;

		for( int i = 0; i < 2; i++ ) {
			test.setImageDimension(15+i,20+i);          
			// convolve with different kernel sizes relative to the skip amount
			test.setKernelRadius(1,1);
			test.performTests(numFunctions);
			test.setKernelRadius(2,2);
			test.performTests(numFunctions);
			test.setKernelRadius(3,3);
			test.performTests(numFunctions);

			// non symmetric
			test.setKernelRadius(3,1);
			test.performTests(numFunctions);
			test.setKernelRadius(3,4);
			test.performTests(numFunctions);

			// NOTE it intentionally can't handle this special case
			// now try a pathological case where the kernel is larger than the image
//		test.setKernelRadius(10);
//		test.performTests(9);
		}
	}

	public static class CompareToNaive extends CompareToStandardConvolutionNormalized {

		public CompareToNaive() {
			super(ConvolveNormalized_JustBorder.class);
		}

		/**
		 * Just compares the image border against each other.
		 */
		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

			GImageGray t,v;

			int borderX0=0,borderX1=0;
			int borderY0=0,borderY1=0;

			if( methodTest.getName().contentEquals("convolve")) {
				t = FactoryGImageGray.wrap((ImageGray) targetParam[2]);
				v = FactoryGImageGray.wrap((ImageGray) validationParam[2]);
				borderX0=borderY0 = offset;
				borderX1=borderY1 = kernelRadius*2-offset;
			} else if( methodTest.getName().contentEquals("horizontal") ) {
				t = FactoryGImageGray.wrap((ImageGray) targetParam[2]);
				v = FactoryGImageGray.wrap((ImageGray) validationParam[2]);
				borderX0 = offset;
				borderX1 = kernelRadius*2-offset;
			} else if( methodTest.getName().contentEquals("vertical")) {
				if( methodTest.getParameterTypes().length == 3 ) {
					t = FactoryGImageGray.wrap((ImageGray) targetParam[2]);
					v = FactoryGImageGray.wrap((ImageGray) validationParam[2]);
					borderY0 = offset;
					borderY1 = kernelRadius * 2 - offset;
				} else {
					t = FactoryGImageGray.wrap((ImageGray) targetParam[3]);
					v = FactoryGImageGray.wrap((ImageGray) validationParam[3]);
					borderX0=borderY0 = offset;
					borderX1=borderY1 = kernelRadius*2-offset;
				}
			} else {
				throw new RuntimeException("Unknown");
			}

			final int width = t.getWidth();
			final int height = t.getHeight();

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					if( x < borderX0 || y < borderY0 || x >= width - borderX1 || y >= height - borderY1 )
					{
						Number numT = t.get(x,y);
						Number numV = v.get(x,y);

						assertEquals( x+" "+y,numV.doubleValue() , numT.doubleValue() , 1e-4 );
					}
				}
			}
		}
	}
}
