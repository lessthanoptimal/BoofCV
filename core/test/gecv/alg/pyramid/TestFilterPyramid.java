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

package gecv.alg.pyramid;

import gecv.abst.filter.FilterImageInterface;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFilterPyramid {
	int width = 50;
	int height = 40;

	/**
	 * Sees if the filter computation is called the expected number of times
	 */
	@Test
	public void checkNumCalls() {

		FilterImageInterface<ImageFloat32,ImageFloat32> filter = new DummyFilter();

		FilterPyramid<ImageFloat32,ImageFloat32> updater = new
				FilterPyramid<ImageFloat32,ImageFloat32>(filter);

		ImagePyramid<ImageFloat32> in = ImagePyramidFactory.create_F32(width,height,false);
		ImagePyramid<ImageFloat32> out = ImagePyramidFactory.create_F32(width,height,false);

		in.setScaling(1,2,2);
		out.setScaling(1,2,2);

		updater.update(in,out);

		assertEquals(3,((DummyFilter)filter).numCalls);
	}

	public static class DummyFilter implements FilterImageInterface<ImageFloat32,ImageFloat32>
	{
		public int numCalls = 0;

		@Override
		public void process(ImageFloat32 inputImage, ImageFloat32 out ) {
			numCalls++;
		}

		@Override
		public int getHorizontalBorder() {
			return 0;
		}

		@Override
		public int getVerticalBorder() {
			return 0;
		}
	}
}
