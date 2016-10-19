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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_IL_F32;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.InterleavedF32;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageDistortBasic_IL {
	private static final int NUM_BANDS = 2;
	DummyInterpolate interp = new DummyInterpolate();

	float offX=0,offY=0;

	PixelTransform2_F32 tran = new PixelTransform2_F32() {
		@Override
		public void compute(int x, int y) {
			distX = x+offX;
			distY = y+offY;
		}
	};

	@Test
	public void applyRenderAll_true() {
		Helper alg = new Helper(interp);
		alg.setRenderAll(true);

		offX= offY=0;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS), new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(150, alg.getTotal());

		offX=offY =0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS), new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(150, alg.getTotal());

		offX=offY = -0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS), new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(150,alg.getTotal());
	}

	@Test
	public void applyRenderAll_False() {
		Helper alg = new Helper(interp);
		alg.setRenderAll(false);

		offX=offY=0;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS),new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(150,alg.getTotal());

		offX=offY=0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS),new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(9*14,alg.getTotal());

		offX=offY=-0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new InterleavedF32(10, 15,NUM_BANDS),new InterleavedF32(10, 15,NUM_BANDS));
		assertEquals(9*14,alg.getTotal());
	}

	private static class Helper extends ImageDistortBasic_IL<InterleavedF32,InterleavedF32> {

		int total = 0;

		public Helper(InterpolatePixelMB<InterleavedF32> interp) {
			super(interp);
		}

		public void reset() {
			total = 0;
		}

		private int getTotal() {
			return total;
		}

		@Override
		protected void assign(int indexDst, float value[]) {
			total++;
			int numBand = value.length;
			int x = ((indexDst - dstImg.startIndex)%dstImg.stride)/numBand;
			int y = (indexDst - dstImg.startIndex)/dstImg.stride;
			assertTrue(dstImg.isInBounds(x,y));
		}
	}

	protected static class DummyInterpolate extends ImplBilinearPixel_IL_F32 {

		public DummyInterpolate() {
			super(NUM_BANDS);
		}

		@Override
		public void get_border(float x, float y, float value[] ) {
			Arrays.fill(value,1.1f);
		}
	}

}
