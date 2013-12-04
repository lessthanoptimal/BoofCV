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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageDistortCache {

	InterpolatePixelS interp = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
	DummyBorder border = new DummyBorder();

	float offX=0,offY=0;

	PixelTransform_F32 tran = new PixelTransform_F32() {
		@Override
		public void compute(int x, int y) {
			distX = x+offX;
			distY = y+offY;
		}
	};

	@Test
	public void applyBorder() {
		Helper alg = new Helper(interp,border);

		offX=offY=0;
		alg.reset();
		border.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10,15),new ImageFloat32(10,15));
		assertEquals(150,alg.getTotal());
		assertEquals(0,border.getTotal());

		offX=offY=0.1f;
		alg.reset();
		border.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10,15),new ImageFloat32(10,15));
		assertEquals(150,alg.getTotal());
		assertEquals(10+14,border.getTotal());

		offX=offY=-0.1f;
		alg.reset();
		border.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10,15),new ImageFloat32(10,15));
		assertEquals(150,alg.getTotal());
		assertEquals(10+14,border.getTotal());
	}

	@Test
	public void applyNoBorder() {
		Helper alg = new Helper(interp,null);

		offX=offY=0;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10, 15), new ImageFloat32(10, 15));
		assertEquals(150,alg.getTotal());

		offX=offY=0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10,15),new ImageFloat32(10,15));
		assertEquals(9*14,alg.getTotal());

		offX=offY=-0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new ImageFloat32(10, 15), new ImageFloat32(10, 15));
		assertEquals(9*14,alg.getTotal());
	}

	private static class Helper extends ImageDistortCache {

		int total = 0;

		public Helper(InterpolatePixelS interp, ImageBorder border) {
			super(interp, border);
		}

		public void reset() {
			total = 0;
		}

		private int getTotal() {
			return total;
		}

		@Override
		protected void assign(int indexDst, float value) {
			total++;
			int x = (indexDst - dstImg.startIndex)%dstImg.stride;
			int y = (indexDst - dstImg.startIndex)/dstImg.stride;
			assertTrue(dstImg.isInBounds(x,y));
		}
	}

	private static class DummyBorder extends ImageBorder {

		int total = 0;

		public void reset() {
			total = 0;
		}

		public int getTotal() {
			return total;
		}

		@Override
		public double getGeneral(int x, int y) {
			total++;
			return 0;
		}
	}

}
