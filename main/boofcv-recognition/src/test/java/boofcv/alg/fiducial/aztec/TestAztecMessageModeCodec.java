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

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAztecMessageModeCodec extends BoofStandardJUnit {
	/**
	 * Known results from ISO Figure G.6. Manually read bits from image
	 */
	@Test void encodeMode_Known0() {
		var marker = new AztecCode();
		marker.structure = AztecCode.Structure.COMPACT;
		marker.dataLayers = 1;
		marker.messageWordCount = 10;

		var found = new PackedBits8();
		var encoder = new AztecMessageModeCodec();
		encoder.encodeMode(marker, found);

		var expected = new PackedBits8();
		expected.append(0b0000100, 7, false);
		expected.append(0b1110000, 7, false);
		expected.append(0b1000110, 7, false);
		expected.append(0b0011001, 7, false);

		checkEqualsFirstBits(expected.size, found, expected);
	}

	/**
	 * Generate an error free message and see if it modifies anything
	 */
	@Test void correctDataBits() {
		var marker = new AztecCode();
		var encoder = new AztecMessageModeCodec();
		var original = new PackedBits8();
		var copy = new PackedBits8();

		// Go through different structures because the code is slightly different
		for (var structure : AztecCode.Structure.values()) {
			// Create the error free message
			marker.structure = structure;
			marker.dataLayers = 2;
			marker.messageWordCount = 10;
			encoder.encodeMode(marker, original);

			// With perfect data there should be no change
			copy.setTo(original);
			assertTrue(encoder.correctDataBits(copy, structure));

			// how many data bits
			int numBits = structure == AztecCode.Structure.COMPACT ? 8 : 16;

			checkEqualsFirstBits(numBits, copy, original);

			// Flip a bit and see if it gets corrected
			copy.setTo(original);
			copy.set(7, copy.get(7)^1);
			assertTrue(encoder.correctDataBits(copy, structure));
			checkEqualsFirstBits(numBits, copy, original);
		}
	}

	private void checkEqualsFirstBits( int numBits, PackedBits8 copy, PackedBits8 original ) {
		assertEquals(numBits, copy.size);
		for (int bitIdx = 0; bitIdx < copy.size; bitIdx++) {
			assertEquals(original.get(bitIdx), copy.get(bitIdx));
		}
	}

	/**
	 * Simple test to see if it decodes the original mode correctly.
	 */
	@Test void decodeMode() {
		var marker = new AztecCode();
		var found = new AztecCode();
		var alg = new AztecMessageModeCodec();
		var original = new PackedBits8();

		// Go through different structures because the code is slightly different
		for (var structure : AztecCode.Structure.values()) {
			// Create the error free message
			marker.structure = structure;
			marker.dataLayers = 2;
			marker.messageWordCount = 10;
			alg.encodeMode(marker, original);

			// Decode it and see if it extracted the correct values
			found.structure = structure;
			assertTrue(alg.decodeMode(original, found));

			assertEquals(marker.dataLayers, found.dataLayers);
			assertEquals(marker.messageWordCount, found.messageWordCount);
		}
	}
}
