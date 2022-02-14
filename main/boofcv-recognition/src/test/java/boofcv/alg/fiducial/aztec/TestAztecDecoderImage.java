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
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestAztecDecoderImage extends BoofStandardJUnit {
	/**
	 * Sees if it can successfully decode multiple noise free markers
	 */
	@Test void process() {
		var alg = new AztecDecoderImage<>(GrayU8.class);
		for (var structure : AztecCode.Structure.values()) {
			AztecCode truth = new AztecEncoder().setStructure(structure).addUpper("TEST").fixate();
			GrayU8 image = AztecGenerator.renderImage(10, 0, truth);

			AztecCode found = new AztecCode();

			for (int rotation = 0; rotation < 4; rotation++) {
				// Process the image
				assertTrue(alg.process(truth.locator, image, found));

				// See if it was decoded correctly
				assertEquals(truth.message, found.message);
				assertEquals(0, found.totalBitErrors);
				assertFalse(found.transposed);

				// Rotate the locator pattern so that initially it will be wrong
				truth.locator.layers.forEach(l -> UtilPolygons2D_F64.shiftUp(l.square));
			}
		}
	}

	/**
	 * Render an image and test the results. Rotate the image to make sure orientation is correctly estimated
	 */
	@Test void decodeMode() {
		var alg = new AztecDecoderImage<>(GrayU8.class);
		for (var structure : AztecCode.Structure.values()) {
			AztecCode truth = new AztecEncoder().setStructure(structure).addUpper("TEST").fixate();
			GrayU8 image = AztecGenerator.renderImage(10, 0, truth);

			Point2D_F64 expectedCorner0 = truth.locator.layers.get(0).square.get(0).copy();

			for (int rotation = 0; rotation < 4; rotation++) {
				var found = new AztecCode();

				alg.interpolate.setImage(image);

				assertTrue(alg.decodeMode(truth.locator, found));

				// Check if it was decoded correctly
				assertEquals(structure, found.structure);
				assertEquals(truth.dataLayers, found.dataLayers);
				assertEquals(truth.messageWordCount, found.messageWordCount);

				// See if it compensated for the rotation
				assertTrue(expectedCorner0.isIdentical(found.locator.layers.get(0).square.get(0), 1e-8));

				// Rotate the locator pattern so that initially it will be wrong
				truth.locator.layers.forEach(l -> UtilPolygons2D_F64.shiftUp(l.square));
			}
		}
	}

	@Test void readModeBitsFromImage() {
		// Test both structures
		for (var structure : AztecCode.Structure.values()) {
			// Create a known target
			var truth = new AztecEncoder().setStructure(structure).addUpper("TEST").fixate();
			GrayU8 image = AztecGenerator.renderImage(10, 0, truth);

			// Render into and image and save coordinates of locator pattern
			var alg = new AztecDecoderImage<>(GrayU8.class);
			alg.interpolate.setImage(image);

			alg.readModeBitsFromImage(truth.locator);

			// Fixed structure should have no error
			assertEquals(0, alg.fixedFeatureReadErrors(0, AztecDecoderImage.getModeBitType(structure)));

			// See if the data is as expected by encoding it and comparing
			alg.extractModeDataBits(0, structure);

			var expectedBits = new PackedBits8();
			var modeCodec = new AztecMessageModeCodec();
			modeCodec.encodeMode(truth, expectedBits);
			assertTrue(expectedBits.isIdentical(alg.bits));
		}
	}

	@Test void selectOrientation() {
		var alg = new AztecDecoderImage<>(GrayU8.class);

		for (int side = 0; side < 4; side++) {
			// Fill in mode bits with a known pattern with the specified side first
			modeImageBitsCompact((4 - side)%4, alg.imageBits);

			// see if it selects the correct side/orientation
			assertEquals(side, alg.selectOrientation(AztecCode.Structure.COMPACT));
		}
	}

	@Test void extractModeDataBits() {
		var alg = new AztecDecoderImage<>(GrayU8.class);

		for (int side = 0; side < 4; side++) {
			int antiSide = (4 - side)%4;
			// Create image bits with a known pattern
			modeImageBitsCompact(antiSide, alg.imageBits);

			// Extract the bits
			alg.extractModeDataBits(side, AztecCode.Structure.COMPACT);

			// Sanity check the results
			assertEquals(7*4, alg.bits.size);
			for (int i = 0; i < alg.bits.size; i++) {
				assertEquals(i%2, alg.bits.get(i));
			}
		}
	}

	@Test void fixedFeatureReadErrors() {
		var alg = new AztecDecoderImage<>(GrayU8.class);

		int[] bitTypes = AztecDecoderImage.modeBitTypesComp;
		for (int startSide = 0; startSide < 4; startSide++) {
			int antiSide = (4 - startSide)%4;

			modeImageBitsCompact(antiSide, alg.imageBits);

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
