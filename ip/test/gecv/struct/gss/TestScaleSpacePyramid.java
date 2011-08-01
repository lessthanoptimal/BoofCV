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

package gecv.struct.gss;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageUInt8;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScaleSpacePyramid {

	int width = 80;
	int height = 160;

	@Test
	public void setScaling() {
		// see if all the layers are set correctly
		ImagePyramid<ImageUInt8> pyramid = new ScaleSpacePyramid<ImageUInt8>(new DoNothingUpdater<ImageUInt8>(),1,2,5.5);

		ImageUInt8 input = new ImageUInt8(width,height);
		pyramid.update(input);

		assertEquals(width , pyramid.getLayer(0).width);
		assertEquals(height , pyramid.getLayer(0).height);

		assertEquals(width / 2, pyramid.getLayer(1).width);
		assertEquals(height / 2, pyramid.getLayer(1).height);

		assertEquals((int)Math.ceil(width / 5.5), pyramid.getLayer(2).width);
		assertEquals((int)Math.ceil(height / 5.5), pyramid.getLayer(2).height);

		// try it with a scaling not equal to 1
		pyramid = new ScaleSpacePyramid<ImageUInt8>(new DoNothingUpdater<ImageUInt8>(),2,4);
		pyramid.update(input);

		assertEquals(width / 2, pyramid.getLayer(0).width);
		assertEquals(height / 2, pyramid.getLayer(0).height);
		assertEquals(width / 4, pyramid.getLayer(1).width);
		assertEquals(height / 4, pyramid.getLayer(1).height);
	}

	public static class DoNothingUpdater<T extends ImageBase> implements PyramidUpdater<T>
	{
		@Override
		public void update(T input, ImagePyramid<T> imagePyramid) {
		}
	}
}
