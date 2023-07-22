/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.ImageMultiBand;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConvolveNormalized_JustBorder_IL extends BoofStandardJUnit {
	@Test void compareToNaive() {
		CompareToNaive test = new CompareToNaive();
		int numFunctions = 20;

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
			super(ConvolveNormalized_JustBorder_IL.class);
		}

		/**
		 * Just compares the image border against each other.
		 */
		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

			GImageMultiBand t,v;

			int borderX0=0,borderX1=0;
			int borderY0=0,borderY1=0;

			if( methodTest.getName().contentEquals("convolve")) {
				t = FactoryGImageMultiBand.wrap((ImageMultiBand) targetParam[2]);
				v = FactoryGImageMultiBand.wrap((ImageMultiBand) validationParam[2]);
				borderX0=borderY0 = offset;
				borderX1=borderY1 = kernelRadius*2-offset;
			} else if( methodTest.getName().contentEquals("horizontal") ) {
				t = FactoryGImageMultiBand.wrap((ImageMultiBand) targetParam[2]);
				v = FactoryGImageMultiBand.wrap((ImageMultiBand) validationParam[2]);
				borderX0 = offset;
				borderX1 = kernelRadius*2-offset;
			} else if( methodTest.getName().contentEquals("vertical")) {
				if( methodTest.getParameterTypes().length == 3 ) {
					t = FactoryGImageMultiBand.wrap((ImageMultiBand) targetParam[2]);
					v = FactoryGImageMultiBand.wrap((ImageMultiBand) validationParam[2]);
					borderY0 = offset;
					borderY1 = kernelRadius * 2 - offset;
				} else {
					t = FactoryGImageMultiBand.wrap((ImageMultiBand) targetParam[3]);
					v = FactoryGImageMultiBand.wrap((ImageMultiBand) validationParam[3]);
					borderX0=borderY0 = offset;
					borderX1=borderY1 = kernelRadius*2-offset;
				}
			} else {
				throw new RuntimeException("Unknown");
			}

			final int width = t.getWidth();
			final int height = t.getHeight();
			final float pixelT[] = new float[ t.getNumberOfBands() ];
			final float pixelV[] = new float[ t.getNumberOfBands() ];

//			System.out.println("   t");
//			System.out.println(t.getImage());
//			System.out.println("   v");
//			System.out.println(v.getImage());

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					if( x < borderX0 || y < borderY0 || x >= width - borderX1 || y >= height - borderY1 )
					{

						t.get(x,y,pixelT);
						v.get(x,y,pixelV);

						for (int band = 0; band < t.getNumberOfBands(); band++) {
							assertEquals(pixelV[band] , pixelT[band] , 1e-4 , x+" "+y);
						}
					} else {
						t.get(x,y,pixelT);
						for (int band = 0; band < t.getNumberOfBands(); band++) {
							assertEquals( 0 , pixelT[band] , 1e-4 , x+" "+y);
						}
					}
				}
			}
		}
	}
}
