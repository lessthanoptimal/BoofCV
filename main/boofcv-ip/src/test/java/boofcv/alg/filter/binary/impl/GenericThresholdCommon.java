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

package boofcv.alg.filter.binary.impl;

import boofcv.BoofTesting;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericThresholdCommon<T extends ImageGray<T>> extends BoofStandardJUnit {

	Class<T> imageType;

	protected GenericThresholdCommon( Class<T> imageType ) {
		this.imageType = imageType;
	}

	public abstract InputToBinary<T> createAlg( int requestedBlockWidth,
												double scale, boolean down );

	/**
	 * Make sure it's doing something resembling a proper thresholding of a random image. About 1/2 the pixels should
	 * be true or false. There was a bug where this wasn't happening and wasn't caught
	 */
	@Test void sanityCheckThreshold() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		GrayU8 down = new GrayU8(100, 120);
		GrayU8 up = new GrayU8(100, 120);

		// turn off texture so that the output's can be the inverse of each other
		createAlg(6, 1.0, true).process(input, down);
		createAlg(6, 1.0, false).process(input, up);

		assertTrue(ImageStatistics.sum(down) > down.data.length/4);
		assertTrue(ImageStatistics.sum(up) > down.data.length/4);
	}

	@Test void toggleDown() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		GrayU8 down = new GrayU8(100, 120);
		GrayU8 up = new GrayU8(100, 120);

		// turn off texture so that the output's can be the inverse of each other
		createAlg(6, 1.0, true).process(input, down);
		createAlg(6, 1.0, false).process(input, up);

		for (int y = 0; y < down.height; y++) {
			for (int x = 0; x < down.width; x++) {
				assertEquals(!(up.get(x, y) == 0), (down.get(x, y) == 0), x + " " + y);
			}
		}
	}

	/**
	 * Should process the image just fine. If a local region the block/region is adjusted for the image
	 */
	@Test void widthLargerThanImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 10, 12);
		GImageMiscOps.fillUniform(input, rand, 0, 255);
		GrayU8 output = new GrayU8(10, 12);

		InputToBinary<T> alg = createAlg(20, 1.0, true);
		alg.process(input, output);

		assertTrue(ImageStatistics.sum(output) > output.data.length/4);
	}

	@Test void subImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		GrayU8 expected = new GrayU8(100, 120);

		T sub_input = BoofTesting.createSubImageOf(input);
		GrayU8 sub_output = BoofTesting.createSubImageOf(expected);

		InputToBinary<T> alg = createAlg(14, 1.0, true);

		alg.process(input, expected);
		alg.process(sub_input, sub_output);

		BoofTesting.assertEquals(expected, sub_output, 0);
	}

	@Test void resize_output() {
		T input = GeneralizedImageOps.createSingleBand(imageType, 100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		GrayU8 output = new GrayU8(80, 20);

		createAlg(6, 1.0, true).process(input, output);

		assertEquals(input.width, output.width);
		assertEquals(input.height, output.height);
	}
}
