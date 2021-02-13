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

import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 **/
public class TestConvertTupleDesc_F64_F32 extends BoofStandardJUnit {
	@Test void createOutput() {
		var alg = new ConvertTupleDesc_F64_F32(5);
		TupleDesc_F32 found = alg.createOutput();
		assertEquals(found.data.length, 5);
	}

	@Test void convert() {
		var alg = new ConvertTupleDesc_F64_F32(5);

		var input = new TupleDesc_F64(5);
		input.data = new double[]{2.5, 3, 20, -43.45, 2.123};

		TupleDesc_F32 found = alg.createOutput();
		alg.convert(input, found);


		for (int i = 0; i < 5; i++) {
			assertEquals(input.data[i], found.data[i], UtilEjml.TEST_F32);
		}
	}

	@Test void getOutputType() {
		var alg = new ConvertTupleDesc_F64_F32(5);
		assertSame(TupleDesc_F32.class, alg.getOutputType());
	}
}
