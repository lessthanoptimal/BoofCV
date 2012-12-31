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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.DisparityScoreSadRect;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWrapDisparitySadRect {
	/**
	 * The whole image should be set as invalid since it is not being written over
	 */
	@Test
	public void borderSetToInvalid() {
		int range = 4;
		Foo foo = new Foo(1,1+range,2,2);
		WrapDisparitySadRect<ImageFloat32,ImageFloat32> alg = new WrapDisparitySadRect<ImageFloat32, ImageFloat32>(foo);

		ImageFloat32 l = new ImageFloat32(10,20);
		ImageFloat32 r = new ImageFloat32(10,20);

		alg.process(l,r);

		ImageFloat32 found = alg.getDisparity();

		for( int y = 0; y < found.height; y++ )
			for( int x = 0; x < found.width; x++ )
				assertTrue(x+" "+y,found.get(x,y) > range );
	}

	private static class Foo extends DisparityScoreSadRect<ImageFloat32,ImageFloat32>
	{
		public Foo(int minDisparity, int maxDisparity, int regionRadiusX, int regionRadiusY) {
			super(minDisparity, maxDisparity, regionRadiusX, regionRadiusY);
		}

		@Override
		public void _process(ImageFloat32 left, ImageFloat32 right, ImageFloat32 imageFloat32) {
		}

		@Override
		public Class<ImageFloat32> getInputType() {
			return ImageFloat32.class;
		}

		@Override
		public Class<ImageFloat32> getDisparityType() {
			return ImageFloat32.class;
		}
	}


}
