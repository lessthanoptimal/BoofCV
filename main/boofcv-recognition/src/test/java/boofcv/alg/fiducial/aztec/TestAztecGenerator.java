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

import boofcv.gui.image.ShowImages;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAztecGenerator {
	@Test void foo() {
		var marker = new AztecCode();
		marker.dataLayers = 12;
		marker.messageLength = 10;
		marker.structure = AztecCode.Structure.FULL;

		GrayU8 image = AztecGenerator.renderImage(5, 0, marker);
		ShowImages.showBlocking(image, "Aztec Code", 120_000, true);
	}

	/**
	 * Generate coordinates for all possible markers and see if there are the expected number of bits
	 */
	@Test void computeDataBitCoordinates() {
		var layerStartsAtBit = new DogArray_I32();
		var marker = new AztecCode();
		var coordinates = new DogArray_I16();
		for (var structure : AztecCode.Structure.values()) {
			marker.structure = structure;
			for (int dataLayers = 1; dataLayers <= structure.maxDataLayers; dataLayers++) {
				marker.dataLayers = dataLayers;
				AztecGenerator.computeDataBitCoordinates(marker, coordinates, layerStartsAtBit);

				int expected = marker.getMaxBitEncoded();
				int found = coordinates.size/2;

				// only full codewords can be encoded
				found -= found%marker.getCodewordBitCount();
				assertEquals(expected, found, structure + " " + dataLayers);
			}
		}
	}
}
