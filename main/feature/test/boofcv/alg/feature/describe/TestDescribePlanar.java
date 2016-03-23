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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDescribePlanar {

	boolean ori,scale;

	/**
	 * Sanity check to see if input image and number of descriptors is the same
	 */
	@Test(expected=IllegalArgumentException.class)
	public void checkNumBands() {

		DescribeRegionPoint[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		alg.setImage(new Planar(GrayS8.class,1,1,2));
	}

	@Test
	public void checkRequires() {
		DescribeRegionPoint[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		scale = true;
		ori = false;
		assertTrue(alg.requiresRadius());
		assertFalse(alg.requiresOrientation());
		scale = false;
		ori = true;
		assertFalse(alg.requiresRadius());
		assertTrue(alg.requiresOrientation());
	}

	@Test
	public void various() {
		DescribeRegionPoint[] descs = new DummyDesc[3];
		descs[0] = new DummyDesc();
		descs[1] = new DummyDesc();
		descs[2] = new DummyDesc();

		DummyAlg alg = new DummyAlg(descs);

		alg.setImage(new Planar(GrayS8.class,1,1,3));
		alg.process(0, 1, 2, 3, alg.createDescription());

		assertEquals(30, alg.createDescription().size());
		assertEquals(1,alg.numCombined);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(1,((DummyDesc)descs[i]).numProcess);
		}
	}

	private class DummyAlg extends DescribePlanar {

		int numCombined = 0;

		public DummyAlg(DescribeRegionPoint[] describers) {
			super(describers);
		}

		@Override
		protected void combine(TupleDesc description) {
			numCombined++;
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(length);
		}

		@Override
		public ImageType getImageType() {return null;}
	}

	private class DummyDesc implements DescribeRegionPoint {

		ImageBase image;
		int numProcess = 0;

		@Override
		public void setImage(ImageBase image) {
			this.image = image;
		}

		@Override
		public boolean process(double x, double y, double orientation, double radius, TupleDesc description) {
			numProcess++;
			return true;
		}

		@Override
		public boolean requiresRadius() {
			return scale;
		}

		@Override
		public boolean requiresOrientation() {
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
