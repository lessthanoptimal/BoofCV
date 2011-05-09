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

package gecv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Adds tests specific to integer images
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"PointlessBooleanExpression"})
public abstract class StandardImageIntegerTests extends StandardImageTests {
	/**
	 * Makes sure the image being signed/unsigned is correctly set by the function _createNew()
	 */
	@Test
	public void createNew_Signedness() {
		ImageInteger<?> image = (ImageInteger<?>)createImage(10,20);

		// first check with no data being declared
		image.signed = true;
		ImageInteger<?> a = image._createNew(-1,-1);
		assertTrue(true == a.signed);
		image.signed = false;
		a = image._createNew(-1,-1);
		assertTrue(false == a.signed);

		// now check with data being declared
		image.signed = true;
		a = image._createNew(10,20);
		assertTrue(true == a.signed);
		image.signed = false;
		a = image._createNew(10,20);
		assertTrue(false == a.signed);
	}

	@Test
	public void checkDataIsUnsigned() {
		ImageInteger<?> image = (ImageInteger<?>)createImage(10,20);

		image.signed = true;
		image.set(0,0,-1);
		assertEquals(-1,image.get(0,0));
		image.signed = false;
		assertTrue(-1 != image.get(0,0));
	}
}
