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

package boofcv.abst.feature.detdesc;

import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSurf_DetectDescribe extends GenericTestsDetectDescribePoint<GrayF32,TupleDesc_F64>
{

	static {
		BoofConcurrency.USE_CONCURRENT = false;
	}

	TestSurf_DetectDescribe() {
		super(true, true, ImageType.single(GrayF32.class), TupleDesc_F64.class);
	}

	@Override
	public DetectDescribePoint<GrayF32, TupleDesc_F64> createDetDesc() {
		return FactoryDetectDescribe.surfStable(null,null,null, GrayF32.class);
	}

	/**
	 * More rigorous test to see if sets is done correctly specific to SURF
	 */
	@Test
	void setsRigorous() {
		DetectDescribePoint<GrayF32,TupleDesc_F64> alg = createDetDesc();

		assertEquals(2, alg.getNumberOfSets());
		alg.detect(image);
		int[] counts = new int[2];
		for (int i = 0; i < alg.getNumberOfFeatures(); i++) {
			counts[alg.getSet(i)]++;
		}
		assertTrue(counts[0]>0);
		assertTrue(counts[1]>0);
	}
}
