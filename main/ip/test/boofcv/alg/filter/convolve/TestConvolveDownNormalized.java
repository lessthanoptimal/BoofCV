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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.down.CompareToStandardConvolveDownNormalized;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestConvolveDownNormalized {

	@Test
	public void compareToStandard() {
		CompareToStandardConvolveDownNormalized test = new CompareToStandardConvolveDownNormalized(ConvolveDownNormalized.class);

		test.setSkip(2);
		for( int i = 0; i < 2; i++ ) {
			test.setImageDimension(15+i,20+i);
			// convolve with different kernel sizes relative to the skip amount
			test.setKernelRadius(1);
			test.performTests(9);
			test.setKernelRadius(2);
			test.performTests(9);
			test.setKernelRadius(3);
			test.performTests(9);

			// now try a pathological case where the kernel is larger than the image
			test.setKernelRadius(10);
			test.performTests(9);
		}
	}
}
