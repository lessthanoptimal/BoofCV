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

import gecv.abst.filter.derivative.ImageGradient;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestGradientPyramid {

	int width = 50;
	int height = 40;

	/**
	 * Sees if the gradient computation is called the expected number of times
	 */
	@Test
	public void checkNumCalls() {

		ImageGradient<ImageFloat32,ImageFloat32> gradient = new DummyGradient();

		GradientPyramid<ImageFloat32,ImageFloat32> updater = new
				GradientPyramid<ImageFloat32,ImageFloat32>(gradient);

		ImagePyramid<ImageFloat32> in = ImagePyramidFactory.create_F32(width,height,false);
		ImagePyramid<ImageFloat32> outX = ImagePyramidFactory.create_F32(width,height,false);
		ImagePyramid<ImageFloat32> outY = ImagePyramidFactory.create_F32(width,height,false);

		in.setScaling(1,2,2);
		outX.setScaling(1,2,2);
		outY.setScaling(1,2,2);

		updater.update(in,outX,outY);

		assertEquals(3,((DummyGradient)gradient).numCalls);
	}

	public static class DummyGradient implements ImageGradient<ImageFloat32,ImageFloat32>
	{
		public int numCalls = 0;

		@Override
		public void process(ImageFloat32 inputImage, ImageFloat32 derivX, ImageFloat32 derivY) {
			numCalls++;
		}

		@Override
		public int getBorder() {
			return 0;
		}
	}
}
