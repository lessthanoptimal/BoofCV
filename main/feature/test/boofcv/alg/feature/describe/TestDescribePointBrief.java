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

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDescribePointBrief {

	Random rand = new Random(234);

	/**
	 * Checks to see if it blows up when processing features
	 */
	@Test
	public void basicSanityCheck() {
		GrayU8 input = new GrayU8(30,40);
		GImageMiscOps.fillUniform(input,rand,0,100);

		BlurFilter<GrayU8> filterBlur = FactoryBlurFilter.gaussian(GrayU8.class, -1, 1);
		Helper helper = new Helper();
		DescribePointBrief<GrayU8> alg = new DescribePointBrief<>(helper,filterBlur);

		alg.setImage(input);
		alg.process(15,20,null);
		assertEquals(1,helper.numInside);
		assertEquals(0,helper.numOutside);

		alg.process(0,0,null);
		assertEquals(1,helper.numInside);
		assertEquals(1,helper.numOutside);

	}

	protected static class Helper extends DescribePointBinaryCompare<GrayU8> {

		int numInside = 0;
		int numOutside = 0;

		public Helper() {
			super(new BinaryCompareDefinition_I32(2,10,5));
		}

		@Override
		public void processInside(int c_x, int c_y, TupleDesc_B feature) {
			numInside++;
		}

		@Override
		public void processBorder(int c_x, int c_y, TupleDesc_B feature) {
			numOutside++;
		}
	}
}
