/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericThresholdCommon<T extends ImageGray<T>>
{

	Class<T> imageType;
	Random rand = new Random(234);

	public GenericThresholdCommon(Class<T> imageType) {
		this.imageType = imageType;
	}

	public abstract InputToBinary<T> createAlg(int requestedBlockWidth,
											   double scale , boolean down );

	/**
	 * Make sure it's doing something resembling a proper thresholding of a random image. About 1/2 the pixels should
	 * be true or false. There was a bug where this wasn't happening and wasn't caught
	 */
	@Test
	public void sanityCheckThreshold() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);
		GImageMiscOps.fillUniform(input,rand,0,255);

		GrayU8 down = new GrayU8(100,120);
		GrayU8 up = new GrayU8(100,120);

		// turn off texture so that the output's can be the inverse of each other
		createAlg(6,1.0,true).process(input,down);
		createAlg(6,1.0,false).process(input,up);

		assertTrue(ImageStatistics.sum(down)>down.data.length/4);
		assertTrue(ImageStatistics.sum(up)>down.data.length/4);
	}

	@Test
	public void toggleDown() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);
		GImageMiscOps.fillUniform(input,rand,0,255);

		GrayU8 down = new GrayU8(100,120);
		GrayU8 up = new GrayU8(100,120);

		// turn off texture so that the output's can be the inverse of each other
		createAlg(6,1.0,true).process(input,down);
		createAlg(6,1.0,false).process(input,up);

		for (int y = 0; y < down.height; y++) {
			for (int x = 0; x < down.width; x++) {
				assertTrue(x+" "+y,(down.get(x,y)==0) == !(up.get(x,y)==0));
			}
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void widthLargerThanImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType,10,12);
		GrayU8 output = new GrayU8(10,12);

		InputToBinary<T> alg = createAlg(20,1.0,true);
		alg.process(input,output);
	}

	@Test
	public void subImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);
		GImageMiscOps.fillUniform(input,rand,0,255);

		GrayU8 expected = new GrayU8(100,120);

		T sub_input = BoofTesting.createSubImageOf(input);
		GrayU8 sub_output = BoofTesting.createSubImageOf(expected);

		InputToBinary<T> alg = createAlg(14,1.0,true);

		alg.process(input,expected);
		alg.process(sub_input,sub_output);

		BoofTesting.assertEquals(expected,sub_output,0);
	}
}
