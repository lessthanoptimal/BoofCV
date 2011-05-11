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

package gecv.alg.filter.convolve.normalized;

import gecv.struct.image.ImageBase;
import gecv.struct.image.generalized.FactorySingleBandImage;
import gecv.struct.image.generalized.SingleBandImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalized_JustBorder {
	@Test
	public void compareToNaive() {
		CompareToNaive test = new CompareToNaive();
		test.setImageDimension(10,12);
		test.setKernelRadius(1);
		test.performTests(6);

		test.setKernelRadius(2);
		test.performTests(6);
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
			SingleBandImage t = FactorySingleBandImage.wrap((ImageBase)targetParam[2]);
			SingleBandImage e = FactorySingleBandImage.wrap((ImageBase)targetParam[2]);

			final int width = t.getWidth();
			final int height = t.getHeight();

			for( int y = 0; y < height; y++ ) {
				if( !(y < kernelRadius || y >= height - kernelRadius) ) {
					continue;
				}

				for( int x = 0; x < kernelRadius; x++ ) {
					Number numT = t.get(x,y);
					Number numE = e.get(x,y);

					assertEquals( numT.doubleValue() , numE.doubleValue() , 1e-4 );
				}

				for( int x = width-kernelRadius; x < width; x++ ) {
					Number numT = t.get(x,y);
					Number numE = e.get(x,y);

					assertEquals( numT.doubleValue() , numE.doubleValue() , 1e-4 );
				}
			}
		}
	}
}
