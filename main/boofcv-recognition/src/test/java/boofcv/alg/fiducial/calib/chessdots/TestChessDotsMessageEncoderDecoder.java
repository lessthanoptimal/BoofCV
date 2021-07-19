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

package boofcv.alg.fiducial.calib.chessdots;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_I8;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.calib.chessdots.ChessDotsMessageEncoderDecoder.Multiplier.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestChessDotsMessageEncoderDecoder extends BoofStandardJUnit {
	@Test void configure() {
		var alg = new ChessDotsMessageEncoderDecoder();
		alg.configure(LEVEL_0, 100);

		// Hand compute solution:
		// 7 bits to encode a number up to 100
		// 14 bits for two-numbers + 2 for header + ecc. then round up
		assertEquals(6, alg.squareLength);

		// Same deal bu divide coordinates by 2
		alg.configure(LEVEL_1, 100);
		// 6-bits for a number up to 50
		assertEquals(6, alg.squareLength);

		// Same deal bu divide coordinates by 4
		alg.configure(LEVEL_3, 20);
		// 4-bits for a number up to 13
		assertEquals(5, alg.squareLength);
	}

	/**
	 * Encode and decode with different settings and numbers
	 */
	@Test void encode_decode() {
		var found = new Point2D_I32();

		var alg = new ChessDotsMessageEncoderDecoder();

		alg.configure(LEVEL_0, 2000);
		alg.encode(10, 50);
		assertTrue(alg.decode(alg.getRawData(), found));
		assertEquals(10, found.y);
		assertEquals(50, found.x);

		alg.configure(LEVEL_2, 400);
		alg.encode(40, 88);
		assertTrue(alg.decode(alg.getRawData(), found));
		assertEquals(40, found.y);
		assertEquals(88, found.x);
	}

	/**
	 * Make sure the error correction works by introducing single bit errors
	 */
	@Test void singleBitError() {
		var alg = new ChessDotsMessageEncoderDecoder();

		alg.configure(LEVEL_0, 100);
		alg.encode(10, 51);

		var original = new DogArray_I8();
		original.setTo(alg.getRawData());

		var found = new Point2D_I32();
		var copy = new DogArray_I8();
		int numBits = original.size*8;
		for (int bit = 0; bit < numBits; bit++) {
			copy.setTo(original);
			int byteIndex = bit/8;
			copy.data[byteIndex] ^= (byte)(1 << bit%8);

			assertTrue(alg.decode(copy, found));
			assertEquals(10, found.y);
			assertEquals(51, found.x);
		}
	}

	@Test void multipleWordErrors() {
		var alg = new ChessDotsMessageEncoderDecoder();

		// make it so it can recover from a bit error in all the words
		alg.setMaxErrorFraction(1.0);

		alg.configure(LEVEL_0, 20000);
		alg.encode(10, 51);

		var modified = new DogArray_I8();
		modified.setTo(alg.getRawData());

		// This has been configured to recover from bit errors in all the message words
		for (int i = 0; i < alg.message.size; i++) {
			modified.data[i] ^= (byte)0x13;
		}

		var found = new Point2D_I32();
		assertTrue(alg.decode(modified, found));
		assertEquals(10, found.y);
		assertEquals(51, found.x);
	}

	/**
	 * Make sure it doesn't accept pure noise
	 */
	@Test void failWhenPureNoise() {
		var alg = new ChessDotsMessageEncoderDecoder();

		// This will fail if the error fraction is 0.5
		alg.maxErrorFraction = 1.0;

		alg.configure(LEVEL_0, 100);
		alg.encode(10, 51);

		var noise = new DogArray_I8();
		noise.resize(alg.getRawData().size);

		var found = new Point2D_I32();
		for (int trial = 0; trial < 1000; trial++) {
			rand.nextBytes(noise.data);
			assertFalse(alg.decode(noise, found), "trial=" + trial);
		}
	}
}
