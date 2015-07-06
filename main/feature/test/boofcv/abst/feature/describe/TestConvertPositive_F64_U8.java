/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_U8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvertPositive_F64_U8 {

	@Test
	public void createOutput() {
		ConvertPositive_F64_U8 alg = new ConvertPositive_F64_U8(5);

		TupleDesc_U8 found = alg.createOutput();

		assertTrue(5 == found.value.length);
	}

	@Test
	public void convert() {
		ConvertPositive_F64_U8 alg = new ConvertPositive_F64_U8(5);

		TupleDesc_F64 input = new TupleDesc_F64(5);
		input.value = new double[]{2.5,3,20,-43.45};

		TupleDesc_U8 found = alg.createOutput();
		alg.convert(input,found);

		TupleDesc_U8 expected = alg.createOutput();
		ConvertDescriptors.positive(input, expected);

		for( int i = 0; i < 5; i++ ) {
			assertEquals(expected.value[i],found.value[i]);
		}
	}

	@Test
	public void getOutputType() {
		ConvertPositive_F64_U8 alg = new ConvertPositive_F64_U8(5);
		assertTrue(TupleDesc_U8.class == alg.getOutputType());
	}
}
