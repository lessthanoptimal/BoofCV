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

import boofcv.alg.filter.binary.ThresholdSquareBlockMinMax;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericThresholdSquareBlockMinMaxChecks
		<T extends ImageGray>
{

	Class<T> imageType;
	Random rand = new Random(234);

	public GenericThresholdSquareBlockMinMaxChecks(Class<T> imageType) {
		this.imageType = imageType;
	}

	public abstract ThresholdSquareBlockMinMax<T,?> createAlg( double textureThreshold, int requestedBlockWidth,
														  double scale , boolean down );

	@Test
	public void thresholdSquare() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);

		GImageMiscOps.fill(input,200);
		GImageMiscOps.fillRectangle(input,20,40,45,30,32);

		ThresholdSquareBlockMinMax<T,?> alg = createAlg(10,6,1.0,true);

		GrayU8 output = new GrayU8(input.width,input.height);

		alg.process(input,output);

		// the entire square should be 1
		assertEquals(30*32, ImageStatistics.sum(output.subimage(40,45,70,77)));

		// the border surrounding the square should be 0
		for (int x = 39; x < 72; x++) {
			assertEquals(0,output.get(x,44));
			assertEquals(0,output.get(x,78));
		}
		for (int y = 45; y < 77; y++) {
			assertEquals(0,output.get(39,y));
			assertEquals(0,output.get(71,y));
		}
	}

	@Test
	public void toggleDown() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);
		GImageMiscOps.fillUniform(input,rand,0,255);

		GrayU8 down = new GrayU8(100,120);
		GrayU8 up = new GrayU8(100,120);

		// turn off texture so that the output's can be the inverse of each other
		createAlg(-1,6,1.0,true).process(input,down);
		createAlg(-1,6,1.0,false).process(input,up);

		for (int y = 0; y < down.height; y++) {
			for (int x = 0; x < down.width; x++) {
				assertTrue((down.get(x,y)==0) == !(up.get(x,y)==0));
			}
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void widthLargerThanImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType,10,12);
		GrayU8 output = new GrayU8(10,12);

		ThresholdSquareBlockMinMax<T,?> alg = createAlg(10,20,1.0,true);
		alg.process(input,output);
	}

	@Test
	public void subImage() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);
		GImageMiscOps.fillUniform(input,rand,0,255);

		GrayU8 expected = new GrayU8(100,120);

		T sub_input = BoofTesting.createSubImageOf(input);
		GrayU8 sub_output = BoofTesting.createSubImageOf(expected);

		ThresholdSquareBlockMinMax<T,?> alg = createAlg(10,14,1.0,true);

		alg.process(input,expected);
		alg.process(sub_input,sub_output);

		BoofTesting.assertEquals(expected,sub_output,0);
	}
}
