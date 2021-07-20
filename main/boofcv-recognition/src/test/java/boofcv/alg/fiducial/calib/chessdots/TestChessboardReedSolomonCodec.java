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

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_I8;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.calib.chessdots.ChessboardReedSolomonCodec.Multiplier.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestChessboardReedSolomonCodec extends BoofStandardJUnit {

	// Storage for encoded message
	PackedBits8 encoded = new PackedBits8();
	
	@Test void configure() {
		var alg = new ChessboardReedSolomonCodec();
		alg.configure(LEVEL_0, 100);

		// Hand compute solution:
		// 7 bits to encode a number up to 100
		// 14 bits for two-numbers + 4 for header + ecc. then round up
		assertEquals(7, alg.gridBitLength);

		// Same deal bu divide coordinates by 2
		alg.configure(LEVEL_1, 100);
		// 6-bits for a number up to 50
		assertEquals(7, alg.gridBitLength);

		// Same deal bu divide coordinates by 4
		alg.configure(LEVEL_3, 20);
		// 4-bits for a number up to 13
		assertEquals(6, alg.gridBitLength);
	}

	/**
	 * Encode and decode with different settings and numbers
	 */
	@Test void encode_decode() {
		var found = new Point2D_I32();

		var alg = new ChessboardReedSolomonCodec();

		alg.configure(LEVEL_0, 2000);
		alg.encode(10, 50, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(10, found.y);
		assertEquals(50, found.x);

		alg.configure(LEVEL_2, 400);
		alg.encode(40, 88, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(40, found.y);
		assertEquals(88, found.x);
	}

	/**
	 * Make sure the error correction works by introducing single bit errors
	 */
	@Test void singleBitError() {
		var alg = new ChessboardReedSolomonCodec();

		alg.configure(LEVEL_0, 100);
		alg.encode(10, 51, encoded);


		var found = new Point2D_I32();
		var copy = new PackedBits8();
		for (int bit = 0; bit < encoded.size; bit++) {
			copy.setTo(encoded);
			int byteIndex = bit/8;
			copy.data[byteIndex] ^= (byte)(1 << bit%8);

			// clear previous history so that it has to write to it for this to pass
			found.setTo(-1,-1);

			// Decode and check results
			assertTrue(alg.decode(copy, found), "bit=" + bit);
			assertEquals(10, found.y);
			assertEquals(51, found.x);
		}
	}

	@Test void multipleWordErrors() {
		var alg = new ChessboardReedSolomonCodec();

		// make it so it can recover from a bit error in all the words
		alg.setMaxErrorFraction(1.0);

		alg.configure(LEVEL_0, 20000);
		alg.encode(10, 51, encoded);

		var modified = new PackedBits8();
		modified.setTo(encoded);

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
		var alg = new ChessboardReedSolomonCodec();

		alg.configure(LEVEL_0, 20);
		alg.encode(1, 8, encoded);

		var noise = new DogArray_I8();
		noise.resize(encoded.size);

		int numTrials = 10_000;
		int numFalsePositive=0;
		var found = new Point2D_I32();
		for (int trial = 0; trial < numTrials; trial++) {
			rand.nextBytes(encoded.data);
			if (alg.decode(encoded, found)) {
				numFalsePositive++;
			}
		}

		// I'm not sure what the theoretical limit should be, but this should have a very low false positive rate
		assertTrue(numFalsePositive < numTrials*0.001, "false_positive="+numFalsePositive);
	}
}
