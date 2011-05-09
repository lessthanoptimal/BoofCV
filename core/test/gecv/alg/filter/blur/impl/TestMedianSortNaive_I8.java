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

import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMedianSortNaive_I8 {

	@Test
	public void trivialTest() {
		ImageInt8 image = new ImageInt8(4,4);
		for( int i = 0; i < image.width; i++ ) {
			for( int j = 0; j < image.height; j++ ) {
				image.set(j,i,i*image.width+j);
			}
		}

		ImageInt8 found = new ImageInt8(4,4);

		GecvTesting.checkSubImage(this, "trivialTest", true, image, found);
	}

	public void trivialTest(ImageInt8 image, ImageInt8 found) {
		MedianSortNaive_I8 alg = new MedianSortNaive_I8(1);

		alg.process(image,found);

		assertEquals(5,found.get(1,1));
		assertEquals(6,found.get(2,1));
		assertEquals(9,found.get(1,2));
		assertEquals(10,found.get(2,2));
	}
}
