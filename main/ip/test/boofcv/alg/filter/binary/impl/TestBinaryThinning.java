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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestBinaryThinning {

	Random rand = new Random(234);

	/**
	 * Run the overall algorithm and compare against a known result
	 */
	@Test
	public void thinning() {
		GrayU8 img = new GrayU8(20,25);

		ImageMiscOps.fill(img.subimage(1, 5, 19, 10), 1);

		BinaryThinning alg = new BinaryThinning();

		alg.apply(img, -1);

		// the tests below aren't really that great.  really just checks to see if the algorithm has changed
		assertEquals(24, ImageStatistics.sum(img));

		for (int i = 2; i <18; i++) {
			assertEquals(1,img.get(i,7));
		}
	}

	/**
	 * A line 2 pixels thick.  Should be left with a line 1 pixel thick
	 */
	@Test
	public void thinning_line2pixel() {
		GrayU8 img = new GrayU8(20,25);

		ImageMiscOps.fill(img.subimage(0, 5, 20, 7), 1);

		BinaryThinning alg = new BinaryThinning();

		alg.apply(img, -1);

		assertEquals(20, ImageStatistics.sum(img));

		for (int i = 1; i <19; i++) {
			assertEquals(1,img.get(i,6));
		}
	}

	@Test
	public void findBlackPixels() {

		GrayU8 img = new GrayU8(5,7);
		img.set(2, 3, 1);
		img.set(4, 1, 1);

		findBlackPixels(img);
		findBlackPixels(BoofTesting.createSubImageOf(img));
	}

	private void findBlackPixels(GrayU8 img) {
		GrowQueue_I32 marked = new GrowQueue_I32();
		BinaryThinning alg = new BinaryThinning();

		alg.binary = img;
		alg.findOnePixels(marked);

		assertEquals(2, marked.size());
		assertEquals(img.getIndex(4, 1), marked.get(0));
		assertEquals(img.getIndex(2, 3), marked.get(1));
	}

	@Test
	public void checkGenericMask() {
		// manually construct masks which will fit the first of each type of mask
		GrayU8 imgA = new GrayU8(3,3);
		imgA.data = new byte[]{0,0,0, 0,1,0, 1,1,1};

		GrayU8 imgB = new GrayU8(3,3);
		imgB.data = new byte[]{0,0,0, 1,1,0, 1,1,0};

		BinaryThinning alg = new BinaryThinning();

		// these are then rotated through all the perputations
		checkGenericMask(imgA, imgB, alg);
		checkGenericMask(BoofTesting.createSubImageOf(imgA), BoofTesting.createSubImageOf(imgB), alg);
	}

	private void checkGenericMask(GrayU8 imgA, GrayU8 imgB, BinaryThinning alg) {
		for (int maskIndex = 0; maskIndex < 8; maskIndex++) {
			BinaryThinning.Mask mask = alg.masks[maskIndex];
			if( maskIndex % 2 == 0 ) {
				alg.inputBorder.setImage(imgA);
				assertFalse(mask.borderMask(1, 1));
				alg.inputBorder.setImage(imgB);
				assertTrue(mask.borderMask(1, 1));
				ImageMiscOps.rotateCW(imgA);
			} else {
				alg.inputBorder.setImage(imgB);
				assertFalse(mask.borderMask(1, 1));
				alg.inputBorder.setImage(imgA);
				assertTrue(mask.borderMask(1, 1));
				ImageMiscOps.rotateCW(imgB);
			}
		}
	}

	@Test
	public void checkInnerMask() {
		// manually construct masks which will fit the first of each type of mask
		GrayU8 imgA = new GrayU8(3,3);
		imgA.data = new byte[]{0,0,0, 0,1,0, 1,1,1};

		GrayU8 imgB = new GrayU8(3,3);
		imgB.data = new byte[]{0,0,0, 1,1,0, 1,1,0};

		BinaryThinning alg = new BinaryThinning();

		// these are then rotated through all the perputations
		checkInnerMask(imgA, imgB, alg);
		checkInnerMask(BoofTesting.createSubImageOf(imgA), BoofTesting.createSubImageOf(imgB), alg);
	}

	private void checkInnerMask(GrayU8 imgA, GrayU8 imgB, BinaryThinning alg) {
		for (int maskIndex = 0; maskIndex < 8; maskIndex++) {
			BinaryThinning.Mask mask = alg.masks[maskIndex];
			if( maskIndex % 2 == 0 ) {
				alg.binary = imgA;
				assertFalse(mask.innerMask(imgA.getIndex(1, 1)));
				alg.binary = imgB;
				assertTrue(mask.innerMask(imgB.getIndex(1, 1)));
				ImageMiscOps.rotateCW(imgA);
			} else {
				alg.binary = imgB;
				assertFalse(mask.innerMask(imgB.getIndex(1, 1)));
				alg.binary = imgA;
				assertTrue(mask.innerMask(imgA.getIndex(1, 1)));
				ImageMiscOps.rotateCW(imgB);
			}
		}
	}

	/**
	 * Randomly generate an image to create a whole bunch of potential patterns then see if the border
	 * and inner algorithms produce the same result
	 */
	@Test
	public void checkSameInnerAndOuter() {
		GrayU8 img = new GrayU8(200,300);

		ImageMiscOps.fillUniform(img,rand,0,2);

		BinaryThinning alg = new BinaryThinning();
		alg.binary = img;
		alg.inputBorder.setImage(img);

		for (int maskIndex = 0; maskIndex < alg.masks.length; maskIndex++) {
			System.out.println("mask "+maskIndex);
			BinaryThinning.Mask mask = alg.masks[maskIndex];

			for (int i = 1; i < img.height - 1; i++) {
				for (int j = 1; j < img.width - 1; j++) {
					if( img.get(j,i) == 1 ) {
						boolean border = mask.borderMask(j, i);
						boolean inner = mask.innerMask(img.getIndex(j, i));
						assertEquals(i + " " + j, border, inner);
					}
				}
			}
		}
	}

}
