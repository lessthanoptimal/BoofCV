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
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
class TestImplCensusTransformInner {
	final private int w = 25, h = 40;

	@Test
	void region3x3() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayU8 found = new GrayU8(w,h);
		GrayU8 expected = new GrayU8(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformInner.dense3x3_U8(input,found);
		CensusNaive.region3x3(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,1,1,1,1,false);
	}

	@Test
	void region5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayS32 found = new GrayS32(w,h);
		GrayS32 expected = new GrayS32(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformInner.dense5x5_U8(input,found);
		CensusNaive.region5x5(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}

	@Test
	void sample_U64() {
		Random rand = new Random(234);
		int r = 3;

		GrayU8 input = new GrayU8(w,h);
		GrayS64 found = new GrayS64(w,h);
		GrayS64 expected = new GrayS64(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		FastQueue<Point2D_I32> samples = createSamples(r);
		GrowQueue_I32 indexes = samplesToIndexes(input,samples);

		ImplCensusTransformInner.sample_S64(input,r,indexes,found);
		CensusNaive.sample(input,samples,expected);

		BoofTesting.assertEqualsInner(expected,found,0,r,r,r,r,false);
	}

	@Test
	void sample_IU16_compare5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		InterleavedU16 found = new InterleavedU16(w,h,2);
		InterleavedU16 expected = new InterleavedU16(w,h,2);

		ImageMiscOps.fillUniform(input,rand,0,255);

		FastQueue<Point2D_I32> samples5x5 = createSamples(2);
		GrowQueue_I32 indexes = samplesToIndexes(input,samples5x5);

		ImplCensusTransformInner.sample_IU16(input,2,indexes,found);
		CensusNaive.sample(input,samples5x5,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}

	static GrowQueue_I32 samplesToIndexes( GrayU8 input , FastQueue<Point2D_I32> samples ) {
		GrowQueue_I32 indexes = new GrowQueue_I32();
		for (int i = 0; i < samples.size; i++) {
			Point2D_I32 p = samples.get(i);
			indexes.add(p.y*input.stride+p.x);
		}
		return indexes;
	}

	static FastQueue<Point2D_I32> createSamples( int r ) {
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32.class,true);
		for (int y = -r; y <= r; y++) {
			for (int x = -r; x <= r; x++) {
				samples.grow().set(x,y);
			}
		}
		return samples;
	}
}