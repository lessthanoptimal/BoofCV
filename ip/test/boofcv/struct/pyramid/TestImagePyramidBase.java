/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImagePyramidBase {

	@Test
	public void initialize() {
		Dummy pyramid = new Dummy(ImageUInt8.class,false);
		pyramid.setScaleFactors(1,2,4);
		pyramid.initialize(100,120);

		for( int i = 0; i < 3; i++ ) {
			assertTrue(pyramid.layers[i] != null);
		}

		// see if it obeys the saveOriginalReference flag
		pyramid = new Dummy(ImageUInt8.class,true);
		pyramid.setScaleFactors(1,2,4);
		pyramid.initialize(100,120);
		assertTrue(pyramid.layers[0] == null);
		
		// if the first layer is not 1 then an image should be declared
		pyramid.setScaleFactors(2,4);
		pyramid.initialize(100,120);
		assertTrue(pyramid.layers[0] != null);
	}

	@Test
	public void getWidth_Height() {
		Dummy pyramid = new Dummy(ImageUInt8.class,false);
		pyramid.setScaleFactors(1,2,4);
		pyramid.initialize(100,120);

		assertEquals(100,pyramid.getWidth(0));
		assertEquals(120,pyramid.getHeight(0));
		assertEquals(50,pyramid.getWidth(1));
		assertEquals(60,pyramid.getHeight(1));
		assertEquals(25,pyramid.getWidth(2));
		assertEquals(30,pyramid.getHeight(2));
	}

	@Test
	public void isInitialized() {
		Dummy pyramid = new Dummy(ImageUInt8.class,false);
		assertFalse(pyramid.isInitialized());
		pyramid.setScaleFactors(1,2,4);
		pyramid.initialize(100,120);
		assertTrue(pyramid.isInitialized());
	}

	@Test
	public void checkScales() {
		// rest a positive case
		Dummy pyramid = new Dummy(ImageUInt8.class,false);
		pyramid.setScaleFactors(1,2,4,8);
		pyramid.checkScales();

		// duplicate scale
		try {
			pyramid.setScaleFactors(1,2,2,4);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}

		// out of order scale
		try {
			pyramid.setScaleFactors(1,2,4,2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}

		// negative first out of order scale
		try {
			pyramid.setScaleFactors(-1,2,4,2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch( IllegalArgumentException e ) {}
	}

	private static class Dummy extends ImagePyramidBase
	{
		int scales[];

		public Dummy(Class imageType, boolean saveOriginalReference) {
			super(imageType, saveOriginalReference);
		}

		public void setScaleFactors(int... scales) {
			this.scales = scales;
		}

		@Override
		public double getScale(int layer) {
			return scales[layer];
		}

		@Override
		public int getNumLayers() {
			return scales.length;
		}
	}
}
