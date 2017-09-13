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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ThresholdBlockOtsu;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestLocalBlockOtsuBinaryFilter {
	Random rand = new Random(234);

	@Test
	public void compare() {
		GrayU8 input = new GrayU8(30,40);
		GrayU8 found = new GrayU8(30,40);
		GrayU8 expected = new GrayU8(30,40);

		GImageMiscOps.fillUniform(input, rand, 0, 200);

		ThresholdBlockOtsu compare = new ThresholdBlockOtsu(9,0,true);
		compare.process(input,expected);

		LocalBlockOtsuBinaryFilter<GrayU8> alg =
				new LocalBlockOtsuBinaryFilter<>(9,0,true, ImageType.single(GrayU8.class));
		alg.process(input,found);

		BoofTesting.assertEquals(found, expected, 0);
	}
}
