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
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestPyramidDiscrete {

	/**
	 * provide positive examples of working scales
	 */
	@Test
	public void setScaling_positive() {
		PyramidDiscrete<GrayU8> pyramid = new DummyDiscrete<>(GrayU8.class,false);

		pyramid.setScaleFactors(1,2,4);
		pyramid.initialize(100,200);
		assertEquals(100,pyramid.getWidth(0));

		pyramid.setScaleFactors(2,4,8);
		pyramid.initialize(100,200);
		assertEquals(50,pyramid.getWidth(0));

		pyramid.setScaleFactors(1,3,6);
		pyramid.initialize(100,200);
		assertEquals(100,pyramid.getWidth(0));
		assertEquals(34,pyramid.getWidth(1));
	}

	/**
	 * Try some illegal scale values
	 */
	@Test
	public void setScaling_negative() {
		PyramidDiscrete<GrayU8> pyramid = new DummyDiscrete<>(GrayU8.class,true);

		try {
			pyramid.setScaleFactors(1,2,5);
			fail("didn't throw an exception");
		} catch( RuntimeException e ){}
		try {
			pyramid.setScaleFactors(2,5,8);
			fail("didn't throw an exception");
		} catch( RuntimeException e ){}
		try {
			pyramid.setScaleFactors(-1,3,6);
			fail("didn't throw an exception");
		} catch( RuntimeException e ){}
	}

	private static class DummyDiscrete<T extends ImageGray> extends PyramidDiscrete<T> {

		public DummyDiscrete(Class<T> imageType, boolean saveOriginalReference) {
			super(imageType, saveOriginalReference);
		}

		@Override
		public void process(T input) {}

		@Override
		public double getSampleOffset(int layer) {return 0;}

		@Override
		public double getSigma(int layer) {return 0;}
	}
}
