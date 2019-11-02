/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.census.CensusNaive;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.createSamples;
import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.samplesToIndexes;

class TestImplCensusTransformInner_MT {
	final private int w = 50, h = 80;

	@Test
	void region3x3() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayU8 found = new GrayU8(w,h);
		GrayU8 expected = new GrayU8(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformInner_MT.dense3x3_U8(input,found);
		ImplCensusTransformInner.dense3x3_U8(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,1,1,1,1,false);
	}

	@Test
	void region5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayS32 found = new GrayS32(w,h);
		GrayS32 expected = new GrayS32(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformInner_MT.dense5x5_U8(input,found);
		ImplCensusTransformInner.dense5x5_U8(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}

	@Test
	void sample_compare5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		InterleavedU16 found = new InterleavedU16(w,h,2);
		InterleavedU16 expected = new InterleavedU16(w,h,2);

		ImageMiscOps.fillUniform(input,rand,0,255);

		FastQueue<Point2D_I32> samples5x5 = createSamples(2);
		GrowQueue_I32 indexes = samplesToIndexes(input,samples5x5);

		ImplCensusTransformInner_MT.sample_IU16(input,2,indexes,found);
		CensusNaive.sample(input,samples5x5,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}
}

