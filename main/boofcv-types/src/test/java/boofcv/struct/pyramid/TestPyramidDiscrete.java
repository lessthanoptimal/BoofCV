/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPyramidDiscrete extends BoofStandardJUnit {

	/**
	 * provide positive examples of working scales
	 */
	@Test
	void pyramidLevels() {

		ConfigDiscreteLevels config = ConfigDiscreteLevels.levels(3);

		PyramidDiscrete<GrayU8> pyramid = new DummyDiscrete<>(GrayU8.class,false,config);

		int width = 100;
		int height = 200;

		pyramid.initialize(width,height);
		assertEquals(3,pyramid.getNumLayers());
		for (int i = 0,scale=1; i < 3; i++, scale *= 2) {
			assertEquals(scale,pyramid.getScale(i));
			assertEquals(width/scale, pyramid.getLayer(i).width);
			assertEquals(height/scale, pyramid.getLayer(i).height);
		}
	}

	private static class DummyDiscrete<T extends ImageGray<T>> extends PyramidDiscrete<T> {

		public DummyDiscrete(Class<T> imageType, boolean saveOriginalReference, ConfigDiscreteLevels config ) {
			super(ImageType.single(imageType), saveOriginalReference, config);
		}

		@Override
		public void process(T input) {}

		@Override
		public double getSampleOffset(int layer) {return 0;}

		@Override
		public double getSigma(int layer) {return 0;}

		@Override
		public ImagePyramid<T> copyStructure() {
			return null;
		}
	}
}
