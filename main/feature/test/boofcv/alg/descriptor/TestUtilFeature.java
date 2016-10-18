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

package boofcv.alg.descriptor;

import boofcv.struct.feature.TupleDesc_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUtilFeature {

	@Test
	public void combine() {
		TupleDesc_F64 feature0 = new TupleDesc_F64(64);
		TupleDesc_F64 feature1 = new TupleDesc_F64(32);

		feature0.value[5] = 10;
		feature1.value[3] = 13;

		List<TupleDesc_F64> list = new ArrayList<>();
		list.add(feature0);
		list.add(feature1);

		TupleDesc_F64 combined = UtilFeature.combine(list,null);

		assertEquals(10,combined.getDouble(5),1e-8);
		assertEquals(13,combined.getDouble(67),1e-8);
	}

	@Test
	public void normalizeL2_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		feature.value[5] = 2;
		feature.value[10] = 4;
		UtilFeature.normalizeL2(feature);
		assertEquals(0.44721,feature.value[5],1e-3);
		assertEquals(0.89443, feature.value[10], 1e-3);
	}

	/**
	 * The descriptor is all zeros.  See if it handles this special case.
	 */
	@Test
	public void normalizeL2_zeros_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		UtilFeature.normalizeL2(feature);
		for( int i = 0; i < feature.value.length; i++ )
			assertEquals(0,feature.value[i],1e-4);
	}

	@Test
	public void normalizeSumOne_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		feature.value[5] = 2;
		feature.value[10] = 4;
		UtilFeature.normalizeSumOne(feature);

		double total = 0;
		for (int i = 0; i < feature.size(); i++) {
			total += feature.getDouble(i);
		}
		assertEquals(1,total,1e-8);
	}

	/**
	 * The descriptor is all zeros.  See if it handles this special case.
	 */
	@Test
	public void normalizeSumOne_zeros_F64() {
		TupleDesc_F64 feature = new TupleDesc_F64(64);
		UtilFeature.normalizeSumOne(feature);
		for( int i = 0; i < feature.value.length; i++ )
			assertEquals(0,feature.value[i],1e-4);
	}
}