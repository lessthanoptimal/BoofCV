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

import boofcv.alg.fiducial.aztec.AztecCode.Mode;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestAztecEncoder extends BoofStandardJUnit {
	/**
	 * Compare to example in specification and see if it produces the same results. Page 37
	 */
	@Test void segmentsToEncodedBits_example0() {
		AztecEncoder encoder = new AztecEncoder().addUpper("C").addLower("ode").
				addDigit(" 2").addUpper("D").addPunctuation("!");

		// test the bit array and not encoded message since padding will be added later on
		encoder.segmentsToEncodedBits();

		// Expected encoded bits in order plus any padding to make it fill the last word
		String expected = "00100 11100 10000 00101 00110 11110 0001 0100 1111 00101 0000 00110";
		expected = expected.replace(" ", ""); // remove human readable spaces
		PackedBits8 bits = encoder.bits;

		assertEquals(bits.size, expected.length());

		for (int index = 0; index < expected.length(); index++) {
			char c = expected.charAt(index);
			assertEquals(bits.get(index) + "", c + "");
		}
		assertEquals("Code 2D!", encoder.workMarker.message);
	}

	@Test void automatic_example0() {
		String expected = "001001 110010 000001 101001 101111 000010 100111 100101 000001 011011";
		expected = expected.replace(" ", ""); // remove human readable spaces

		AztecCode code = new AztecEncoder().addAutomatic("Code 2D!").fixate();
		var bits = PackedBits8.wrap(code.rawbits, expected.length());

		for (int index = 0; index < expected.length(); index++) {
			char c = expected.charAt(index);
			assertEquals(bits.get(index) + "", c + "");
		}
		assertEquals("Code 2D!", code.message);
	}

	/**
	 * Make sure it respects the user decision when manually specifies the number of layers
	 */
	@Test void manualLayers() {
		AztecCode marker = new AztecEncoder().setLayers(10).addUpper("CODE").fixate();
		assertEquals(10, marker.dataLayers);
	}

	/**
	 * Exhaustively try all encoding transitions and see if it blows up
	 */
	@Test void allModeTransitions() {
		var encodings = new Mode[]{Mode.UPPER, Mode.LOWER, Mode.MIXED, Mode.PUNCT, Mode.DIGIT, Mode.BYTE};
		for (var a : encodings) {
			for (var b : encodings) {
				AztecCode marker = addEncoding(b, addEncoding(a, new AztecEncoder())).fixate();
				assertEquals(2, marker.message.length());
			}
		}
	}

	AztecEncoder addEncoding( Mode encoding, AztecEncoder encoder ) {
		return switch (encoding) {
			case UPPER -> encoder.addUpper("A");
			case LOWER -> encoder.addLower("a");
			case MIXED -> encoder.addMixed("^");
			case PUNCT -> encoder.addPunctuation("!");
			case DIGIT -> encoder.addDigit("1");
			case BYTE -> encoder.addBytes(new byte[]{1}, 0, 1);
			default -> throw new RuntimeException();
		};
	}

	@Test void selectNumberOfLayers() {
		assertEquals(1, new AztecEncoder().addUpper("A").fixate().dataLayers);
		assertEquals(1, new AztecEncoder().addUpper("ABCDEFGHIJ").fixate().dataLayers);
		assertEquals(2, new AztecEncoder().addUpper("ABCDEFGHIJABCDEFGHIJ").fixate().dataLayers);
	}

	@Test void bitsToWords_ZEROS() {
		var encoder = new AztecEncoder();
		encoder.workMarker.dataLayers = 4;
		// 8-bit words

		// have one set of zeros in the beginning and at the end
		encoder.bits.append(0b1010_0000, 8, false);
		encoder.bits.append(0b0000_0000, 8, false);
		encoder.bits.append(0b1100_0010, 8, false);
		encoder.bits.append(0b0000_0000, 8, false);
		encoder.bitsToWords();

		assertEquals(5, encoder.storageDataWords.size);
		assertEquals(0b1010_0000, encoder.storageDataWords.get(0));
		assertEquals(0b0000_0001, encoder.storageDataWords.get(1));
		assertEquals(0b0110_0001, encoder.storageDataWords.get(2));
		assertEquals(0b0000_0001, encoder.storageDataWords.get(3));
		assertEquals(0b0011_1111, encoder.storageDataWords.get(4)); // last 6-bits are filled with 1
	}

	@Test void bitsToWords_ONES() {
		var encoder = new AztecEncoder();
		encoder.workMarker.dataLayers = 4;
		// 8-bit words

		// have one set of ones in the beginning and at the end
		encoder.bits.append(0b1010_0000, 8, false);
		encoder.bits.append(0b1111_1111, 8, false);
		encoder.bits.append(0b1100_0011, 8, false);
		encoder.bits.append(0b1111_1111, 8, false);
		encoder.bitsToWords();

		assertEquals(5, encoder.storageDataWords.size);
		assertEquals(0b1010_0000, encoder.storageDataWords.get(0));
		assertEquals(0b1111_1110, encoder.storageDataWords.get(1));
		assertEquals(0b1110_0001, encoder.storageDataWords.get(2));
		assertEquals(0b1111_1110, encoder.storageDataWords.get(3));
		assertEquals(0b1111_1110, encoder.storageDataWords.get(4));
	}

	/** Example taken from G.3 page 38 */
	@Test void bitsToWords_Example0() {
		String input = "00100 11100 10000 00101 00110 11110 0001 0100 1111 00101 0000 00110";
		String[] expectedWords = "001001 110010 000001 101001 101111 000010 100111 100101 000001 011011".split(" ");

		var encoder = new AztecEncoder();
		encoder.bits.append(input.replace(" ", ""));
		encoder.workMarker.dataLayers = 1;
		encoder.workMarker.structure = AztecCode.Structure.COMPACT;
		encoder.bitsToWords();

		// to make it easier to check add ECC words onto the data words array
		encoder.storageDataWords.addAll(encoder.storageEccWords);

		// Compare against expected taken from the ISO
		assertEquals(encoder.storageDataWords.size, expectedWords.length);

		for (int index = 0; index < expectedWords.length; index++) {
			int expected = 0;
			String word = expectedWords[index];
			for (int i = 0; i < 6; i++) {
				if (word.charAt(i) == '1')
					expected |= 1 << (6 - i - 1);
			}
			assertEquals(expected, encoder.storageDataWords.get(index) & 0xFFFF);
		}
	}

	/**
	 * Number of bits will not be divisible by 8. Make sure that situation is handled correctly.
	 */
	@Test void copyIntoResults() {
		var alg = new AztecEncoder();
		alg.workMarker.dataLayers = 2; // 6-bit words

		// bits are not divisible by 8
		alg.bits.resize(8*7 + 2);

		// give each word a value
		alg.storageDataWords.resize(10);
		alg.workMarker.messageWordCount = alg.storageDataWords.size;
		for (int i = 0; i < alg.storageDataWords.size; i++) {
			alg.storageDataWords.data[i] = (short)(i + 1);
		}
		alg.storageEccWords.resize(7);

		AztecCode found = alg.copyIntoResults();

		// see if found has the expected state using hand computed values
		assertNotEquals(found, alg.workMarker);
		assertEquals(found.messageWordCount, alg.storageDataWords.size);
		assertEquals(8, found.corrected.length);
		assertEquals(13, found.rawbits.length);
	}
}
