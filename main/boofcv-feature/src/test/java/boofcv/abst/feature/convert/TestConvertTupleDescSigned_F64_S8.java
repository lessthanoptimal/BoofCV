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

package boofcv.abst.feature.convert;

import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
public class TestConvertTupleDescSigned_F64_S8 extends BoofStandardJUnit {

	@Test void createOutput() {
		var alg = new ConvertTupleDescSigned_F64_S8(5);
		var found = alg.createOutput();

		assertEquals(found.data.length, 5);
	}

	@Test void convert() {
		var alg = new ConvertTupleDescSigned_F64_S8(5);

		var input = new TupleDesc_F64(5);
		input.data = new double[]{-2.5, 3, 20, -243.45};

		TupleDesc_S8 found = alg.createOutput();
		alg.convert(input, found);

		TupleDesc_S8 expected = alg.createOutput();
		ConvertDescriptors.signed(input, expected);

		for (int i = 0; i < 5; i++) {
			assertEquals(expected.data[i], found.data[i]);
		}
	}

	@Test void getOutputType() {
		ConvertTupleDescSigned_F64_S8 alg = new ConvertTupleDescSigned_F64_S8(5);
		assertSame(TupleDesc_S8.class, alg.getOutputType());
	}
}
