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

package gecv.alg.transform.pyramid;

import gecv.struct.image.ImageUInt8;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestPyramidUpdater {

	int width = 80;
	int height = 160;

	/**
	 * Makes sure update checks the input image
	 */
	@Test
	public void update() {
		// positive case
		ImageUInt8 input = new ImageUInt8(width, height);
		ImagePyramid<ImageUInt8> pyramid = ImagePyramidFactory.create_U8(width, height, true);

		Dummy updater = new Dummy();
		updater.setPyramid(pyramid);
		pyramid.setScaling(1, 2);

		updater.update(input);
		assertEquals(1, updater.numCalled);

		// negative case
		try {
			input = new ImageUInt8(width + 1, height + 1);
			updater.update(input);
			fail("Should have failed");
		} catch (IllegalArgumentException e) {
		}
	}

	protected static class Dummy extends PyramidUpdater<ImageUInt8> {
		public int numCalled;

		@Override
		public void _update(ImageUInt8 original) {
			numCalled++;
		}
	}
}
