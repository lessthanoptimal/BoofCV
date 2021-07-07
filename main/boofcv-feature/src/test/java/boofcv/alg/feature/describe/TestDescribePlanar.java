/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestDescribePlanar extends BoofStandardJUnit {

	boolean ori, scale;

	/**
	 * Sanity check to see if input image and number of descriptors is the same
	 */
	@Test void checkNumBands() {

		DescribePointRadiusAngle[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		assertThrows(IllegalArgumentException.class, () -> alg.setImage(new Planar(GrayS8.class, 1, 1, 2)));
	}

	@Test void checkRequires() {
		DescribePointRadiusAngle[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		scale = true;
		ori = false;
		assertTrue(alg.isScalable());
		assertFalse(alg.isOriented());
		scale = false;
		ori = true;
		assertFalse(alg.isScalable());
		assertTrue(alg.isOriented());
	}

	@Test void various() {
		DescribePointRadiusAngle[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		alg.setImage(new Planar(GrayS8.class, 1, 1, 3));
		alg.process(0, 1, 2, 3, alg.createDescription());

		assertEquals(30, alg.createDescription().size());
		assertEquals(1, alg.numCombined);

		for (int i = 0; i < 3; i++) {
			assertEquals(1, ((DummyDesc)descs[i]).numProcess);
		}
	}

	private static class DummyAlg extends DescribePlanar {
		int numCombined = 0;

		public DummyAlg( DescribePointRadiusAngle[] describers ) {
			super(describers);
		}

		@Override
		protected void combine( TupleDesc description ) {
			numCombined++;
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(length);
		}

		@Override
		public ImageType getImageType() {return null;}
	}

	private class DummyDesc implements DescribePointRadiusAngle {
		ImageBase image;
		int numProcess = 0;

		@Override
		public void setImage( ImageBase image ) {
			this.image = image;
		}

		@Override
		public boolean process( double x, double y, double orientation, double radius, TupleDesc description ) {
			numProcess++;
			return true;
		}

		@Override
		public boolean isScalable() {
			return scale;
		}

		@Override
		public boolean isOriented() {
			return ori;
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(10);
		}

		@Override
		public Class getDescriptionType() {
			return TupleDesc_F64.class;
		}

		@Override
		public ImageType getImageType() {return null;}

		@Override
		public double getCanonicalWidth() {
			throw new RuntimeException("Foo");
		}
	}
}
