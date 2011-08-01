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

package gecv.struct.pyramid;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImagePyramid {

	@Test
	public void checkScales() {
		// rest a positive case
		DiscreteImagePyramid pyramid = new DiscreteImagePyramid(false,null,1,2,4,8);
		pyramid.checkScales();

		// duplicate scale
		try {
			pyramid.setScaleFactors(1,2,2,4);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}

		// out of order scale
		try {
			pyramid.setScaleFactors(1,2,4,2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}

		// negative first out of order scale
		try {
			pyramid.setScaleFactors(-1,2,4,2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}
	}
}
