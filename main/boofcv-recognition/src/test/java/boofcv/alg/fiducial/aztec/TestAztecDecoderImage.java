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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestAztecDecoderImage {
	@Test void decodeMode() {
		fail("Implement");
	}

	@Test void readModeBits() {
		fail("Implement");
	}

	@Test void selectOrientation() {
		var alg = new AztecDecoderImage<>();

		for (int side = 0; side < 4; side++) {
			// Fill in mode bits with a known pattern with the specified side first
			modeImageBitsCompact(side, alg.imageBits);

			// see if it selects the correct side/orientation
			assertEquals(side, alg.selectOrientation(AztecCode.Structure.COMPACT));
		}
	}

	@Test void extractModeDataBits() {
		var alg = new AztecDecoderImage<>();

		for (int side = 0; side < 4; side++) {
			// Create image bits with a known pattern
			modeImageBitsCompact(side, alg.imageBits);

			// Extract the bits
			alg.extractModeDataBits(side, AztecCode.Structure.COMPACT);

			// Sanity check the results
			assertEquals(7*4, alg.bits.size);
			for (int i = 0; i < alg.bits.size; i++) {
				assertEquals((i + side)%2, alg.bits.get(i));
			}
		}
	}

	@Test void fixedFeatureReadErrors() {
		var alg = new AztecDecoderImage<>();

		int[] bitTypes = AztecDecoderImage.modeBitTypesComp;
		for (int startSide = 0; startSide < 4; startSide++) {
			modeImageBitsCompact(startSide, alg.imageBits);

			// when given the correct offset there should be no error
			assertEquals(0, alg.fixedFeatureReadErrors(startSide*10, bitTypes));

			// if the offset is wrong then there should be an error
			int incorrectSide = (startSide + 1)%4;
			assertNotEquals(0, alg.fixedFeatureReadErrors(incorrectSide*10, bitTypes));

			// Introduce a single bit error
			alg.imageBits.set(11, alg.imageBits.get(11) ^ 1);
			assertEquals(1, alg.fixedFeatureReadErrors(startSide*10, bitTypes));
		}
	}

	/**
	 * Add image bits for the mode of a target that's compact
	 */
	private void modeImageBitsCompact( int firstSide, PackedBits8 bits ) {
		bits.resize(0);
		for (int sideIdx = 0; sideIdx < 4; sideIdx++) {
			int side = (sideIdx + firstSide)%4;
			switch (side) {
				case 0 -> bits.append("1101010100");
				case 1 -> bits.append("1110101011");
				case 2 -> bits.append("0001010100");
				case 3 -> bits.append("0010101011");
			}
		}
	}
}