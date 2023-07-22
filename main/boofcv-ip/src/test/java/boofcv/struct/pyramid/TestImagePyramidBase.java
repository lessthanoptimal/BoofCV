/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL") public class TestImagePyramidBase extends BoofStandardJUnit {

	@Test
	void setTo() {
		Dummy a = new Dummy(GrayU8.class, false);
		a.setScaleFactors(1, 2, 4);
		a.initialize(100, 120);
		Dummy b = new Dummy(GrayU8.class, false);
		b.setScaleFactors(1, 2, 4);
		b.initialize(100, 120);

		for (int i = 0; i < b.getNumLayers(); i++) {
			GImageMiscOps.fillUniform(b.getLayer(i), rand, 0, 100);
		}

		a.setTo(b);

		for (int i = 0; i < b.getNumLayers(); i++) {
			BoofTesting.assertEquals(a.getLayer(i), b.getLayer(i), 1);
		}
	}

	/**
	 * If told to use the original image then no image should be declared for layer 0
	 */
	@Test
	void saveOriginalReference() {
		Dummy pyramid = new Dummy(GrayU8.class, false);
		pyramid.setScaleFactors(1, 2, 4);
		pyramid.initialize(100, 120);

		assertNotNull(pyramid.getLayer(0));

		pyramid = new Dummy(GrayU8.class, true);
		pyramid.setScaleFactors(1, 2, 4);
		pyramid.initialize(100, 120);

		assertNull(pyramid.getLayer(0));

		// first layer is not 1 so the flag should be ignored
		pyramid = new Dummy(GrayU8.class, true);
		pyramid.setScaleFactors(2, 4);
		pyramid.initialize(100, 120);

		assertNotNull(pyramid.getLayer(0));
	}

	@Test
	void initialize() {
		Dummy pyramid = new Dummy(GrayU8.class, false);
		pyramid.setScaleFactors(1, 2, 4);
		pyramid.initialize(100, 120);

		for (int i = 0; i < 3; i++) {
			assertNotNull(pyramid.layers[i]);
		}

		// see if it obeys the saveOriginalReference flag
		pyramid = new Dummy(GrayU8.class, true);
		pyramid.setScaleFactors(1, 2, 4);
		pyramid.initialize(100, 120);
		assertNull(pyramid.layers[0]);

		// if the first layer is not 1 then an image should be declared
		pyramid = new Dummy(GrayU8.class, true);
		pyramid.setScaleFactors(2, 4);
		pyramid.initialize(100, 120);
		assertNotNull(pyramid.layers[0]);
	}

	@Test
	void isInitialized() {
		Dummy pyramid = new Dummy(GrayU8.class, false);
		pyramid.setScaleFactors(1, 2, 4);
		assertFalse(pyramid.isInitialized());

		pyramid.initialize(100, 120);
		assertTrue(pyramid.isInitialized());
	}

	@Test
	void getWidth_Height() {
		Dummy pyramid = new Dummy(GrayU8.class, false);
		pyramid.setScaleFactors(1, 2, 4);
		pyramid.initialize(100, 120);

		assertEquals(100, pyramid.getWidth(0));
		assertEquals(120, pyramid.getHeight(0));
		assertEquals(50, pyramid.getWidth(1));
		assertEquals(60, pyramid.getHeight(1));
		assertEquals(25, pyramid.getWidth(2));
		assertEquals(30, pyramid.getHeight(2));
	}

	@Test
	void checkScales() {
		// Test positive cases
		Dummy pyramid = new Dummy(GrayU8.class, false);
		pyramid.setScaleFactors(1, 2, 4, 8);
		pyramid.checkScales();

		// multiple scales at the same resolution are allowed
		pyramid.setScaleFactors(1, 2, 2, 4);
		pyramid.checkScales();

		// out of order scale
		try {
			pyramid.setScaleFactors(1, 2, 4, 2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException ignore) {}

		// negative first out of order scale
		try {
			pyramid.setScaleFactors(-1, 2, 4, 2);
			pyramid.checkScales();
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException ignore) {}
	}

	private static class Dummy extends ImagePyramidBase {
		int[] scales;

		public Dummy( Class imageType, boolean saveOriginalReference ) {
			super(ImageType.single(imageType), saveOriginalReference);
		}

		public void setScaleFactors( int... scales ) {
			this.scales = scales;
		}

		@Override
		public void process( ImageBase input ) {}

		@Override
		public double getScale( int layer ) {
			return scales[layer];
		}

		@Override
		public int getNumLayers() {
			return scales.length;
		}

		@Override
		public double getSampleOffset( int layer ) {return 0;}

		@Override
		public double getSigma( int layer ) {return 0;}

		@Override
		public ImagePyramid copyStructure() { return null; }
	}
}
