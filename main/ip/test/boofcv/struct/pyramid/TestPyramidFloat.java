/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.pyramid;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPyramidFloat {

	int width = 80;
	int height = 160;

	@Test
	public void setScaling() {
		// see if all the layers are set correctly
		PyramidFloat<GrayU8> pyramid = new DummyFloat<>(GrayU8.class);

		pyramid.setScaleFactors(1,2,5.5);
		pyramid.initialize(width,height);
		assertEquals(width , pyramid.getLayer(0).width);
		assertEquals(height , pyramid.getLayer(0).height);

		assertEquals(width / 2, pyramid.getLayer(1).width);
		assertEquals(height / 2, pyramid.getLayer(1).height);

		assertEquals((int)Math.ceil(width / 5.5), pyramid.getLayer(2).width);
		assertEquals((int)Math.ceil(height / 5.5), pyramid.getLayer(2).height);

		// try it with a scaling not equal to 1
		pyramid.setScaleFactors(2,4);
		pyramid.initialize(width,height);

		assertEquals(width / 2, pyramid.getLayer(0).width);
		assertEquals(height / 2, pyramid.getLayer(0).height);
		assertEquals(width / 4, pyramid.getLayer(1).width);
		assertEquals(height / 4, pyramid.getLayer(1).height);
	}

	private static class DummyFloat<T extends ImageGray> extends PyramidFloat<T> {

		public DummyFloat(Class<T> imageType) {
			super(imageType);
		}

		@Override
		public void process(T input) {}

		@Override
		public double getSampleOffset(int layer) {return 0;}

		@Override
		public double getSigma(int layer) {return 0;}
	}
}
