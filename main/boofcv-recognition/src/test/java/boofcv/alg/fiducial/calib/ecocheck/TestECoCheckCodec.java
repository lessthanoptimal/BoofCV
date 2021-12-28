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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestECoCheckCodec extends BoofStandardJUnit {

	// Storage for encoded message
	PackedBits8 encoded = new PackedBits8();

	@Test void configure() {
		var alg = new ECoCheckCodec();

		// Hand compute solution:
		// marker = 0, cell = 4 bits. checksum=6, total=0+4+6=10. then convert to words. add in ecc
		alg.configure(1, 12);
		assertEquals(0, alg.markerBitCount);
		assertEquals(4, alg.cellBitCount);
		assertEquals(6, alg.gridBitLength);

		// Describing multiple markers shouldn't push it over the edge yet
		alg.configure(3, 12);
		assertEquals(6, alg.gridBitLength);

		// Add more possible cell values
		alg.configure(1, 100);
		assertEquals(6, alg.gridBitLength);

		// Try a larger constellation
		// marker = 10bits, cell = 16 bits. 5-words to encode the message
		alg.configure(1020, 30000);
		assertEquals(10, alg.markerBitCount);
		assertEquals(15, alg.cellBitCount);
		assertEquals(8, alg.gridBitLength);

		// Another hand solution with more checksum bits
		// marker = 0, cell = 4bits. total=4+4=8. then convert to words. add in ecc
		alg.checksumBitCount = 4;
		alg.errorCorrectionLevel = 0;
		alg.configure(1, 12);
		assertEquals(0, alg.markerBitCount);
		assertEquals(4, alg.cellBitCount);
		assertEquals(3, alg.gridBitLength);
	}

	/**
	 * Encode and decode with different settings and numbers
	 */
	@Test void encode_decode() {
		var found = new CellValue();

		var alg = new ECoCheckCodec();

		alg.configure(1, 15);
		alg.encode(0, 14, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(0, found.markerID);
		assertEquals(14, found.cellID);

		alg.configure(12, 400);
		alg.encode(6, 335, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(6, found.markerID);
		assertEquals(335, found.cellID);

		// Try it with no ECC
		alg.errorCorrectionLevel = 0;
		alg.configure(12, 400);
		alg.encode(6, 335, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(6, found.markerID);
		assertEquals(335, found.cellID);
	}

	/**
	 * There are no checksum bits. Does it still work?
	 */
	@Test void encode_decode_NoChecksum() {
		var found = new CellValue();

		var alg = new ECoCheckCodec();
		alg.checksumBitCount = 0;

		alg.configure(1, 15);
		alg.encode(0, 14, encoded);
		assertTrue(alg.decode(encoded, found));
		assertEquals(0, found.markerID);
		assertEquals(14, found.cellID);
	}

	/**
	 * Make sure the error correction works by introducing single bit errors.
	 */
	@Test void singleBitError() {
		// Smallest possible marker
		singleBitError(1, 10);
		// A larger marker
		singleBitError(500, 60_000);
	}

	void singleBitError( int numMarkers, int numCells ) {
		var alg = new ECoCheckCodec();

		int marker = numMarkers-1;
		int cell = numCells/2+1;

		alg.configure(numMarkers, numCells);
		alg.encode(marker, cell, encoded);

		var found = new CellValue();
		var copy = new PackedBits8();
		for (int bit = 0; bit < encoded.size; bit++) {
			copy.setTo(encoded);
			int byteIndex = bit/8;
			copy.data[byteIndex] ^= (byte)(1 << bit%8);

			// clear previous history so that it has to write to it for this to pass
			found.setTo(-1,-1);

			// Decode and check results
			assertTrue(alg.decode(copy, found), "bit=" + bit);
			assertEquals(marker, found.markerID);
			assertEquals(cell, found.cellID);
		}
	}

	@Test void multipleWordErrors() {
		var alg = new ECoCheckCodec();

		// make it so it can recover from a bit error in all the words
		alg.setErrorCorrectionLevel(9);

		alg.configure(10, 20000);
		alg.encode(5, 51, encoded);

		var modified = new PackedBits8();
		modified.setTo(encoded);

		// This has been configured to recover from bit errors in all the message words
		for (int i = 0; i < alg.message.size; i++) {
			modified.data[i] ^= (byte)0x13;
		}

		var found = new CellValue();
		assertTrue(alg.decode(modified, found));
		assertEquals(5, found.markerID);
		assertEquals(51, found.cellID);
	}

	/**
	 * Make sure it doesn't accept pure noise
	 */
	@Test void failWhenPureNoise() {
		var alg = new ECoCheckCodec();

		alg.configure(1, 20);
		alg.encode(1, 8, encoded);

		var noise = new DogArray_I8();
		noise.resize(encoded.size);

		int numTrials = 10_000;
		int numFalsePositive=0;
		var found = new CellValue();
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
