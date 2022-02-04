/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAztecDecoder {
	@Test void test0() {
		AztecCode marker = new AztecEncoder().
				addLower("moo").addUpper("A").addPunctuation("!").addDigit("312").fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));

		assertEquals("mooA!312", marker.message);
		assertEquals(0, marker.totalBitErrors);
	}

	/** Multiple punctuations need to be handled correctly */
	@Test void test1() {
		AztecCode marker = new AztecEncoder().addUpper("C").addPunctuation("!!!").addDigit("0").addPunctuation("!!").fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));

		assertEquals("C!!!0!!", marker.message);
		assertEquals(0, marker.totalBitErrors);
	}

	/** Multiple punctuations need to be handled correctly */
	@Test void test2() {
		AztecCode marker = new AztecEncoder().addLower("c").addPunctuation("!!").addMixed("^").addPunctuation("!!").fixate();

		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));

		assertEquals("c!!^!!", marker.message);
		assertEquals(0, marker.totalBitErrors);
	}

	/** A few cases where the same mode is entered twice */
	@Test void test3() {
		AztecCode marker = new AztecEncoder().
				addPunctuation("?").addPunctuation("?").
				addUpper("ABC").addUpper("CDEF").
				addLower("ab").addLower("erf").fixate();

		// clear the old data
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));

		assertEquals("??ABCCDEFaberf", marker.message);
		assertEquals(0, marker.totalBitErrors);
	}

	/**
	 * A longer message which will not have a word size of 6
	 */
	@Test void longerMessage() {
		String message = " ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ" +
				" ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ";
		AztecCode marker = new AztecEncoder().addUpper(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	/**
	 * Call the same decoder multiple times to make sure it clears correctly.
	 */
	@Test void multipleCalls() {
		AztecCode marker = new AztecEncoder().
				addPunctuation("?").addPunctuation("?").
				addUpper("ABC").addUpper("CDEF").
				addLower("ab").addLower("erf").fixate();
		var decoder = new AztecDecoder();

		for (int trial = 0; trial < 3; trial++) {
			clearMarker(marker);
			assertTrue(decoder.process(marker));

			assertEquals("??ABCCDEFaberf", marker.message);
			assertEquals(0, marker.totalBitErrors);
		}
	}

	@Test void upper() {
		String message = " ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		AztecCode marker = new AztecEncoder().addUpper(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	@Test void lower() {
		String message = " abcdefghijklmnopqrstuvwxyz";
		AztecCode marker = new AztecEncoder().addLower(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	@Test void punctuation() {
		String message = "\r\r\n. , : !\"#$%'()*+,-./:;<=>?[]{}";
		AztecCode marker = new AztecEncoder().addPunctuation(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	@Test void digits() {
		String message = " 0123456789,.";
		AztecCode marker = new AztecEncoder().addDigit(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	@Test void mixed() {
		String message = " @\\^_`|~";
		AztecCode marker = new AztecEncoder().addMixed(message).fixate();
		clearMarker(marker);

		assertTrue(new AztecDecoder().process(marker));
		assertEquals(message, marker.message);
	}

	/**
	 * Remove ground truth data from the marker, making sure everything found has been decoded
	 */
	private void clearMarker( AztecCode marker ) {
		marker.corrected = new byte[0];
		marker.message = "";
		marker.totalBitErrors = -1;
	}
}
