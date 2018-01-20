/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.ThresholdBlockCommon;
import boofcv.alg.filter.binary.ThresholdBlockMinMax;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericThresholdBlockMinMaxChecks
		<T extends ImageGray<T>> extends GenericThresholdCommon<T>
{

	public GenericThresholdBlockMinMaxChecks(Class<T> imageType) {
		super(imageType);
	}

	public abstract ThresholdBlockMinMax<T,?> createAlg(double textureThreshold, int requestedBlockWidth,
														double scale , boolean down );

	@Override
	public ThresholdBlockCommon<T, ?> createAlg(int requestedBlockWidth, double scale, boolean down) {
		return createAlg(1.0,requestedBlockWidth, scale, down);
	}

	@Test
	public void thresholdSquare() {
		T input = GeneralizedImageOps.createSingleBand(imageType,100,120);

		GImageMiscOps.fill(input,200);
		GImageMiscOps.fillRectangle(input,20,40,45,30,32);

		ThresholdBlockMinMax<T,?> alg = createAlg(10,6,1.0,true);

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
}
