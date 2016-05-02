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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.normalized.CompareToStandardConvolutionNormalized;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalized {
	@Test
	public void compareToNaive() {
		int numFunctions = 14;
		CompareToStandardConvolutionNormalized test = new CompareToStandardConvolutionNormalized(ConvolveNormalized.class);

		for( int i = 0; i < 2; i++ ) {
			test.setImageDimension(15+i,20+i);
			// convolve with different kernel sizes relative to the skip amount
			test.setKernelRadius(1,1);
			test.performTests(numFunctions);
			test.setKernelRadius(2,2);
			test.performTests(numFunctions);
			test.setKernelRadius(3,3);
			test.performTests(numFunctions);

			// non-symmetric
			test.setKernelRadius(3,1);
			test.performTests(numFunctions);

//			// now try a pathological case where the kernel is larger than the image
			// --- too big for width
			test.setKernelRadius(8,8);
			test.performTests(numFunctions);

			// -- too big for height
			test.setImageDimension(20+i,15+i);
			test.setKernelRadius(8,8);
			test.performTests(numFunctions);
		}
	}
}
