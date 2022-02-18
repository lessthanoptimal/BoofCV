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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestAztecEncoderAutomatic extends BoofStandardJUnit {
	/**
	 * Ensure that when it should do a shift it does a shift
	 */
	@Test void shiftCharacters() {
		var alg = new AztecEncoderAutomatic();
		for (AztecCode.Mode modeA : AztecEncoderAutomatic.modes) {
			if (modeA == AztecCode.Mode.BYTE)
				continue;
			String stringA = getStringForMode(modeA);
			for (AztecCode.Mode modeB : AztecEncoderAutomatic.modes) {
				if (modeB == AztecCode.Mode.BYTE || modeA == modeB)
					continue;
				String stringB = getCharacterForMode(modeB);

				// If a shift is possible it will make sense to do it for a character
				String text = stringA + stringB + stringA;
				byte[] characters = text.getBytes(StandardCharsets.ISO_8859_1);

				// Process the results
				alg.initialize();
				alg.encodeCharacters(characters);

				// Use shift look up table to determine if it should have generated a shift or not
				boolean shouldShift = AztecEncoderAutomatic.shiftlen[modeA.ordinal()][modeB.ordinal()] < 10;

				// Examine the results
				AztecEncoderAutomatic.State state = alg.selectBestState();

				// First and last mode should be modeA
				assertEquals(modeA, state.sequence.get(modeA.ordinal() == 0 ? 0 : 1).mode);
				assertEquals(modeA, state.sequence.getTail(0).mode);

				// length when latches are done
				int latchLength = 2*stringA.length()*modeA.wordSize + modeB.wordSize;
				latchLength += AztecEncoderAutomatic.latlen[0][modeA.ordinal()];
				latchLength += AztecEncoderAutomatic.latlen[modeA.ordinal()][modeB.ordinal()];
				latchLength += AztecEncoderAutomatic.latlen[modeB.ordinal()][modeA.ordinal()];

				if (shouldShift)
					assertTrue(state.curLen < latchLength);
				else {
					// There are situations where it makes more sense to jump into byte mode than the original
					// encoding. This checks for that
					if (AztecCode.Mode.BYTE == state.sequence.getTail(1).mode) {
						assertTrue(state.curLen < latchLength);
					} else {
						assertEquals(latchLength, state.curLen);
					}
				}
			}
		}
	}

	/**
	 * Make sure the transition out of byte mode is handled correctly
	 */
	@Test void transitionByteMode() {
		var alg = new AztecEncoderAutomatic();
		for (AztecCode.Mode modeA : AztecEncoderAutomatic.modes) {
			if (modeA == AztecCode.Mode.BYTE)
				continue;
			String stringA = getStringForMode(modeA);
			String stringB = getStringForMode(AztecCode.Mode.BYTE);

			String message = stringA + stringB + stringA;

			alg.initialize();
			alg.encodeCharacters(message.getBytes(StandardCharsets.ISO_8859_1));
			AztecEncoderAutomatic.State state = alg.selectBestState();

			// Check to see if it has the expected length
			assertEquals(message.length(), state.characterCount);

			// See if it has the expected mode transitions
			var matches = new ArrayList<AztecEncoderAutomatic.Group>();
			assertTrue(state.sequence.findAll(matches, grp -> grp.count != 0));
			assertEquals(modeA, matches.get(0).mode);
			assertEquals(AztecCode.Mode.BYTE, matches.get(1).mode);
			assertEquals(modeA, matches.get(2).mode);
		}
	}

	@Test void pureUpper() {
		String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ ";
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text.getBytes(StandardCharsets.ISO_8859_1));
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(0), state);
		assertEquals(1, state.sequence.size);
		assertEquals(text.length(), state.sequence.get(0).count);
		assertEquals(text.length()*5, state.curLen);
	}

	@Test void pureLower() {
		String text = "abcdefghijklmnopqrstuvwxyz ";
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text.getBytes(StandardCharsets.ISO_8859_1));
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(1), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length(), state.sequence.get(1).count);
		assertEquals(5 + text.length()*5, state.curLen);
	}

	@Test void pureMixed() {
		byte[] text = new byte[]{32, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 27, 28, 29, 30, 31, 64,
				92, 95, 96, 124, 126, 127};
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text);
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(2), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length, state.sequence.get(1).count);
		assertEquals(5 + text.length*5, state.curLen);
	}

	@Test void purePunctuation() {
		// this includes the two character sequences.
		byte[] text = new byte[]{13, 13, 10, 46, 32, 44, 32, 58, 32,
				33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 91, 93, 123, 125};
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text);
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(3), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length, state.sequence.get(1).count);
		// Make sure the length takes in account the special two-character sequences
		assertEquals(10 + (text.length - 4)*5, state.curLen);
	}

	@Test void pureDigit() {
		byte[] text = new byte[]{32, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 44, 46};
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text);
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(4), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length, state.sequence.get(1).count);
		assertEquals(5 + text.length*4, state.curLen);
	}

	@Test void pureByte() {
		byte[] text = new byte[]{(byte)200, (byte)201, (byte)202};
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text);
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(5), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length, state.sequence.get(1).count);
		assertEquals(10 + text.length*8, state.curLen);
	}

	/** Make sure it counts the size of long byte sequences correctly */
	@Test void pureByte_long() {
		// extended length starts at 32 bytes
		byte[] text = new byte[50];
		Arrays.fill(text, (byte)200);
		var alg = new AztecEncoderAutomatic();
		alg.initialize();
		alg.encodeCharacters(text);
		AztecEncoderAutomatic.State state = alg.selectBestState();
		assertSame(alg.states.get(5), state);
		assertEquals(2, state.sequence.size);
		assertEquals(text.length, state.sequence.get(1).count);
		assertEquals(10 + 11 + text.length*8, state.curLen);
	}

	/**
	 * Exhaustively go through all transitions. See if it selects the correct mode. Strings are long enough so that
	 * there will be no shifts and only latches
	 */
	@Test void allTransitionsAllLatches() {
		var alg = new AztecEncoderAutomatic();

		for (AztecCode.Mode modeA : AztecEncoderAutomatic.modes) {
			String start = getStringForMode(modeA);
			for (AztecCode.Mode modeB : AztecEncoderAutomatic.modes) {
				if (modeA == modeB)
					continue;
				String end = getStringForMode(modeB);
				String message = start + end;

				alg.initialize();
				alg.encodeCharacters(message.getBytes(StandardCharsets.ISO_8859_1));

				AztecEncoderAutomatic.State state = alg.selectBestState();
				// it should always start in UPPER
				assertEquals(AztecCode.Mode.UPPER, state.sequence.get(0).mode);
				// first non-zero mode should be modeA
				for (var block : state.sequence.toList()) {
					if (block.count == 0)
						continue;
					assertEquals(modeA, block.mode);
					assertEquals(start.length(), block.count);
					break;
				}
				// Then the last sequence must be not zero
				assertEquals(modeB, state.sequence.getTail().mode);
				assertEquals(end.length(), state.sequence.getTail().count);
			}
		}
	}

	private String getStringForMode( AztecCode.Mode mode ) {
		return switch (mode) {
			case UPPER -> "ABCDEFG";
			case LOWER -> "abcdefg";
			case MIXED -> "\\^|~_";
			case PUNCT -> "()*+:;{}";
			case DIGIT -> "1234567890";
			case BYTE -> new String(new byte[]{(byte)200, (byte)201, (byte)202}, StandardCharsets.ISO_8859_1);
			default -> throw new IllegalArgumentException("Not supported");
		};
	}

	private String getCharacterForMode( AztecCode.Mode mode ) {
		return switch (mode) {
			case UPPER -> "A";
			case LOWER -> "a";
			case MIXED -> "~";
			case PUNCT -> "(";
			case DIGIT -> "1";
			case BYTE -> new String(new byte[]{(byte)200}, StandardCharsets.ISO_8859_1);
			default -> throw new IllegalArgumentException("Not supported");
		};
	}
}
