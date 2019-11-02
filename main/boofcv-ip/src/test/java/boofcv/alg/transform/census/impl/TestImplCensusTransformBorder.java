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
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU16;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.createSamples;

/**
 * @author Peter Abeles
 */
class TestImplCensusTransformBorder {
	int w = 20, h = 30;

	@Test
	void region3x3() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayU8 found = new GrayU8(w,h);
		GrayU8 expected = new GrayU8(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformBorder.dense3x3_U8((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),found);
		CensusNaive.region3x3(input,expected);

		BoofTesting.assertEqualsBorder(expected,found,0,1,1);
	}

	@Test
	void region5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayS32 found = new GrayS32(w,h);
		GrayS32 expected = new GrayS32(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformBorder.dense5x5_U8((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),found);
		CensusNaive.region5x5(input,expected);

		BoofTesting.assertEqualsBorder(expected,found,0,2,2);
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

		ImplCensusTransformBorder.sample_S64((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),r,samples,found);
		CensusNaive.sample(input,samples,expected);

		BoofTesting.assertEqualsBorder(expected,found,0,r,r);
	}

	@Test
	void sample_compare5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		InterleavedU16 found = new InterleavedU16(w,h,2);
		InterleavedU16 expected = new InterleavedU16(w,h,2);

		ImageMiscOps.fillUniform(input,rand,0,255);

		FastQueue<Point2D_I32> samples5x5 = createSamples(2);

		ImplCensusTransformBorder.sample_IU16((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),2,samples5x5,found);
		CensusNaive.sample(input,samples5x5,expected);

		BoofTesting.assertEqualsBorder(expected,found,0.0,2,2);
	}
}