/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSimpleNumberSequenceReader {

	@Test
	public void nocomments() throws IOException {
		String input = "  12384 342.123  233e-1   23\n424 94";

		StringReader reader = new StringReader(input);
		
		SimpleNumberSequenceReader alg = new SimpleNumberSequenceReader('e');
		
		List<Double> found = alg.read(reader);
		
		assertEquals(6,found.size());
		assertEquals(12384.0,found.get(0),1e-8);
		assertEquals(342.123,found.get(1),1e-8);
		assertEquals(233e-1,found.get(2),1e-8);
		assertEquals(23.0,found.get(3),1e-8);
		assertEquals(424.0,found.get(4),1e-8);
		assertEquals(94.0,found.get(5),1e-8);
	}

	@Test
	public void withComments() throws IOException {
		String input = "#  12384 342.123  233e-1   23\n424 94\n# sdfsdf \n4   ";

		StringReader reader = new StringReader(input);

		SimpleNumberSequenceReader alg = new SimpleNumberSequenceReader('#');

		List<Double> found = alg.read(reader);

		assertEquals(3,found.size());
		assertEquals(424.0,found.get(0),1e-8);
		assertEquals(94.0,found.get(1),1e-8);
		assertEquals(4.0,found.get(2),1e-8);
	}
}
