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

package boofcv.alg.distort;

import boofcv.BoofTesting;
import boofcv.alg.interpolate.BilinearPixelS;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_F32;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonImageDistort_SB extends BoofStandardJUnit {
	DummyInterpolate interp = new DummyInterpolate();

	float offX=0,offY=0;

	PixelTransform<Point2D_F32> tran = new PixelTransform<Point2D_F32>() {
		@Override
		public void compute(int x, int y, Point2D_F32 output ) {
			output.x = x+offX;
			output.y = y+offY;
		}

		@Override
		public PixelTransform<Point2D_F32> copyConcurrent() {
			return null;
		}
	};

	@Test void applyRenderAll_true() {
		ImageDistortHelper alg = createAlg(interp);
		alg.setRenderAll(true);

		offX= offY=0;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10, 15), new GrayF32(10, 15));
		assertEquals(150, alg.getTotal());

		offX=offY =0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10, 15), new GrayF32(10, 15));
		assertEquals(150, alg.getTotal());

		offX=offY = -0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10, 15), new GrayF32(10,15));
		assertEquals(150,alg.getTotal());
	}

	@Test void applyRenderAll_False() {
		ImageDistortHelper alg = createAlg(interp);
		alg.setRenderAll(false);

		offX=offY=0;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10, 15), new GrayF32(10, 15));
		assertEquals(150,alg.getTotal());

		offX=offY=0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10,15),new GrayF32(10,15));
		assertEquals(9*14,alg.getTotal());

		offX=offY=-0.1f;
		alg.reset();
		alg.setModel(tran);
		alg.apply(new GrayF32(10, 15), new GrayF32(10, 15));
		assertEquals(9*14,alg.getTotal());
	}

	/**
	 * Makes sure the mask is resized to match the input image
	 */
	@Test void resize_mask_fit_dst() {
		GrayF32 src = new GrayF32(10,15);
		ImageMiscOps.fillUniform(src,rand,0,2);
		GrayF32 dst = new GrayF32(14,18);
		GrayU8 mask = new GrayU8(1,1);

		ImageDistort alg = createAlg(interp);

		offX=offY=2;
		alg.setModel(tran);
		alg.apply(src,dst,mask);

		assertEquals(dst.width,mask.width);
		assertEquals(dst.height,mask.height);
	}

	@Test void renderAll_mask() {
		checkMask(true);
	}

	@Test void applyOnlyInside_mask() {
		checkMask(false);
	}

	/**
	 * Give it an invalid region and see if it does nothing
	 */
	@Test void invalidRegion() {
		GrayF32 src = new GrayF32(10,15);
		ImageMiscOps.fillUniform(src,rand,0,2);
		GrayF32 dst = new GrayF32(14,18);

		ImageDistort alg = createAlg(interp);
		offX=offY=2;
		alg.setModel(tran);
		// bad X
		alg.apply(src,dst,2,3,2,6);
		assertEquals(0, GImageStatistics.sum(dst));
		// bad X
		alg.apply(src,dst,2,3,7,2);
		assertEquals(0, GImageStatistics.sum(dst));
	}

	public void checkMask( boolean renderAll ) {
		GrayF32 src = new GrayF32(10,15);
		ImageMiscOps.fillUniform(src,rand,0,2);
		GrayF32 dst1 = new GrayF32(10,15);
		GrayF32 dst2 = new GrayF32(10,15);
		GrayU8 mask = new GrayU8(10,15);

		ImageDistort alg = createAlg(interp);

		offX=offY=2;
		alg.setRenderAll(renderAll);
		alg.setModel(tran);
		alg.apply(src,dst1);
		alg.apply(src,dst2,mask);

		// the output image should be identical
		BoofTesting.assertEquals(dst1,dst2,1e-8);

		// make sure it's not zeros
		assertTrue(ImageStatistics.sum(dst1)>=10);

		for (int y = 0; y < mask.height; y++) {
			for (int x = 0; x < mask.width; x++) {
				if( y < mask.height-2 && x < mask.width-2 ) {
					assertEquals(1,mask.get(x,y));
				} else {
					// all the pixels at and outside the boundary will be 1.1
					if( renderAll )
						assertEquals(1.1,dst1.get(x,y),1e-4);
					assertEquals(0,mask.get(x,y));
				}
			}
		}
	}

	protected abstract ImageDistortHelper createAlg(BilinearPixelS<GrayF32> interp);

	protected interface ImageDistortHelper extends ImageDistort {
		void reset();

		int getTotal();
	}

	protected static class DummyInterpolate extends ImplBilinearPixel_F32 {

		@Override
		public float get_border(float x, float y) {
			return 1.1f;
		}
	}
}
