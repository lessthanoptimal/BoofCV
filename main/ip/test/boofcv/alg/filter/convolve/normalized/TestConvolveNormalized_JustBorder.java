/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalized_JustBorder {
	@Test
	public void compareToNaive() {
		CompareToNaive test = new CompareToNaive();
		int numFunctions = 12;

		for( int i = 0; i < 2; i++ ) {
			test.setImageDimension(15+i,20+i);          
			// convolve with different kernel sizes relative to the skip amount
			test.setKernelRadius(1);
			test.performTests(numFunctions);
			test.setKernelRadius(2);
			test.performTests(numFunctions);
			test.setKernelRadius(3);
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
			GImageSingleBand t = FactoryGImageSingleBand.wrap((ImageSingleBand) targetParam[2]);
			GImageSingleBand v = FactoryGImageSingleBand.wrap((ImageSingleBand) validationParam[2]);

			final int width = t.getWidth();
			final int height = t.getHeight();

			if( methodTest.getName().contentEquals("convolve")) {
				for( int y = 0; y < height; y++ ) {
					for( int x = 0; x < width; x++ ) {
						if( y < kernelRadius || y >= height - kernelRadius || x < kernelRadius || x >= width-kernelRadius)
						{
							Number numT = t.get(x,y);
							Number numV = v.get(x,y);

							assertEquals( x+" "+y,numV.doubleValue() , numT.doubleValue() , 1e-4 );
						}
					}
				}
			} else if( methodTest.getName().contentEquals("horizontal") ) {
				for( int y = 0; y < height; y++ ) {
					for( int x = 0; x < width; x++ ) {
						if( x < kernelRadius || x >= width-kernelRadius)
						{
							Number numT = t.get(x,y);
							Number numV = v.get(x,y);

							assertEquals( x+" "+y,numV.doubleValue() , numT.doubleValue() , 1e-4 );
						}
					}
				}
			} else if( methodTest.getName().contentEquals("vertical")) {
				for( int y = 0; y < height; y++ ) {
					for( int x = 0; x < width; x++ ) {
						if( y < kernelRadius || y >= height - kernelRadius )
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
}
