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

import gecv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImagePyramid {

	int width = 80;
	int height = 160;

	@Test
	public void getScalingAtLayer() {
		fail("implement");
	}

	@Test
	public void setScaling() {
		// see if all the layers are set correctly
		Dummy pyramid = new Dummy(width, height, true);
		pyramid.setScaling(1, 2, 2);

		assertTrue(null == pyramid.getLayer(0));

		assertEquals(width / 2, pyramid.getLayer(1).width);
		assertEquals(height / 2, pyramid.getLayer(1).height);

		assertEquals(width / 4, pyramid.getLayer(2).width);
		assertEquals(height / 4, pyramid.getLayer(2).height);


		// tell it to creates a new image in the first layer
		pyramid = new Dummy(width, height, false);
		pyramid.setScaling(1, 2, 2);

		assertTrue(null != pyramid.getLayer(0));

		assertEquals(width, pyramid.getLayer(0).width);
		assertEquals(height, pyramid.getLayer(0).height);

		// try it with a scaling not equal to 1
		pyramid.setScaling(2, 2);

		assertEquals(width / 2, pyramid.getLayer(0).width);
		assertEquals(height / 2, pyramid.getLayer(0).height);
		assertEquals(width / 4, pyramid.getLayer(1).width);
		assertEquals(height / 4, pyramid.getLayer(1).height);
	}


	protected static class Dummy extends ImagePyramid<ImageUInt8> {
		public Dummy(int topWidth, int topHeight, boolean saveOriginalReference) {
			super(topWidth, topHeight, saveOriginalReference);
		}

		@Override
		protected ImageUInt8 createImage(int width, int height) {
			return new ImageUInt8(width, height);
		}

		@Override
		public Class<ImageUInt8> getImageType() {
			return ImageUInt8.class;
		}
	}
}
