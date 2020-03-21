/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block.score;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestDisparitySparseRectifiedScoreBM {

	Random rand = BoofTesting.createRandom(0);

	int width = 40;
	int height = 30;

	int radiusX = 1;
	int radiusY = 2;
	int sampleX = 3;
	int sampleY = 4;

	ImageBorder_S32<GrayU8> border = (ImageBorder_S32)FactoryImageBorder.generic(BorderType.EXTENDED, ImageType.SB_U8);

	/**
	 * Make sure it saves/computes values derived when calling configure
	 */
	@Test
	void configure() {
		var alg = new Mock(sampleX,sampleY);
		alg.setSampleRegion(0,0);
		alg.configure(0,15);
		assertEquals(0,alg.getDisparityMin());
		assertEquals(15,alg.getDisparityRange());
		assertEquals(14,alg.getDisparityMax());

		alg.configure(2,15);
		assertEquals(2,alg.getDisparityMin());
		assertEquals(15,alg.getDisparityRange());
		assertEquals(16,alg.getDisparityMax());
	}

	/**
	 * See if it copies the correct region and truncates the maximum local disparity range
	 */
	@Test
	void process() {
		var leftImage = new GrayU8(width,height);
		var rightImage = new GrayU8(width,height);
		ImageMiscOps.fillUniform(leftImage,rand,0,200);
		ImageMiscOps.fillUniform(rightImage,rand,0,200);

		var alg = new Mock(sampleX,sampleY);
		alg.setImages(leftImage,rightImage);
		alg.setBorder(border);
		alg.setSampleRegion(radiusX,radiusY);

		int range = 15;
		alg.configure(2,range);

		// Test it all the way inside
		assertTrue(alg.process(30,12));
		assertEquals(range,alg.foundRange);
		checkRegion(leftImage,30,12,1,alg.patchLeft);
		checkRegion(rightImage,30-1-range,12,range,alg.patchRight);

		// Test a few negative cases
		assertFalse(alg.process(0,0));
		assertFalse(alg.process(1,0));

		// Test partially outside
		assertTrue(alg.process(2,0));
		assertEquals(1,alg.foundRange);
		checkRegion(leftImage,2,0,1,alg.patchLeft);
		checkRegion(rightImage,0,0,1,alg.patchRight);
		assertTrue(alg.process(width-1,height-1));
		assertEquals(range,alg.foundRange);
		checkRegion(leftImage,width-1,height-1,1,alg.patchLeft);
		checkRegion(rightImage,width-2-range,height-1,range,alg.patchRight);
	}

	/**
	 * Copy two regions. One entirely inside and one inside and outside.
	 */
	@Test
	void copy() {
		var image = new GrayU8(width,height);
		ImageMiscOps.fillUniform(image,rand,0,200);

		var alg = new Mock(sampleX,sampleY);
		alg.setBorder(border);
		alg.setSampleRegion(radiusX,radiusY);

		alg.configure(2,15);
		alg.patchRight.reshape(alg.sampledWidth+14,alg.sampledHeight);

		// Test entirely inside the image
		alg.copy(10,12,5,image,alg.patchRight);
		checkRegion(image,10,12,5,alg.patchRight);

		// Test where it touches the border
		alg.copy(1,height-3,6,image,alg.patchRight);
		checkRegion(image,1,height-3,6,alg.patchRight);
	}

	private void checkRegion( GrayU8 input, int cx , int cy , int length , GrayU8 patch ) {
		border.setImage(input);
		int y0 = cy-radiusY-sampleY;
		int y1 = cy+radiusY+sampleY+1;
		int x0 = cx - radiusX - sampleX;
		int x1 = cx + radiusX + sampleX + length;

		for (int y = 0; y < y1-y0; y++) {
			for (int x = 0; x < x1-x0; x++) {
				assertEquals(border.get(x+x0, y+y0), patch.get(x,y));
			}
		}
	}

	private class Mock extends DisparitySparseRectifiedScoreBM<int[],GrayU8> {

		public int foundRange;

		public Mock(int radiusX, int radiusY) {
			super(radiusX, radiusY, GrayU8.class);
		}

		@Override
		protected void scoreDisparity(int disparityRange) {
			this.foundRange = disparityRange;
		}

		@Override
		public int[] getScore() {
			return null;
		}
	}
}