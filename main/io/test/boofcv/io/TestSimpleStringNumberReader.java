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

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSimpleStringNumberReader {

	@Test
	public void nocomments() throws IOException {
		String input = "  12384 342.123  233e-1 sdf  23\n424 94";

		StringReader reader = new StringReader(input);

		SimpleStringNumberReader alg = new SimpleStringNumberReader('e');
		
		assertTrue(alg.read(reader));

		assertEquals(7, alg.remainingTokens());
		
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(12384.0,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(342.123,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(233e-1,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(alg.isString());
		assertTrue("sdf".compareTo(alg.getString()) == 0);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(23.0,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(424.0,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(94.0,alg.getDouble(),1e-8);

		assertFalse(alg.next());
		assertEquals(0,alg.remainingTokens());
	}

	@Test
	public void withComments() throws IOException {
		String input = "#  12384 342.123  233e-1   23\n424 sdf\n# sdfsdf \n4   ";

		StringReader reader = new StringReader(input);

		SimpleStringNumberReader alg = new SimpleStringNumberReader('#');

		assertTrue(alg.read(reader));


		assertEquals(3, alg.remainingTokens());

		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(424.0,alg.getDouble(),1e-8);
		assertTrue(alg.next());
		assertTrue(alg.isString());
		assertTrue("sdf".compareTo(alg.getString()) == 0);
		assertTrue(alg.next());
		assertTrue(!alg.isString());
		assertEquals(4.0,alg.getDouble(),1e-8);


		assertFalse(alg.next());
		assertEquals(0,alg.remainingTokens());
	}
}
