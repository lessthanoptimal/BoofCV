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

package gecv.alg.filter.blur.impl;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestMedianHistogramNaive_I8 {

	@Test
	public void compareToSort() {
		ImageInt8 image = new ImageInt8(20,30);
		BasicDrawing_I8.randomize(image,new Random(234));

		ImageInt8 found = new ImageInt8( image.width , image.height );
		ImageInt8 expected = new ImageInt8( image.width , image.height );

		GecvTesting.checkSubImage(this, "compareToSort", true, image, found, expected);
	}

	public void compareToSort(ImageInt8 image, ImageInt8 found, ImageInt8 expected) {
		for( int i = 1; i <= 3; i++ ) {
			MedianHistogramNaive_I8 alg = new MedianHistogramNaive_I8(i);
			MedianSortNaive_I8 testAlg = new MedianSortNaive_I8(i);

			alg.process(image,found);
			testAlg.process(image,expected);

			GecvTesting.assertEquals(expected,found,i);
		}
	}
}
