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

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 **/
public class TestConvertTupleDoNothing extends BoofStandardJUnit {
	@Test void basic() {
		var alg = new ConvertTupleDoNothing<>(() -> new TupleDesc_F64(5));

		// Check to see if the output type and create both work
		assertSame(TupleDesc_F64.class, alg.getOutputType());
		assertEquals(5 , alg.createOutput().size());

		// See if convert works and just copies
		var expected = new TupleDesc_F64(1,2,3,4,5);
		TupleDesc_F64 found = alg.createOutput();
		alg.convert(expected, found);

		for (int i = 0; i < expected.size(); i++) {
			assertEquals(expected.get(i), found.get(i));
		}
	}
}
