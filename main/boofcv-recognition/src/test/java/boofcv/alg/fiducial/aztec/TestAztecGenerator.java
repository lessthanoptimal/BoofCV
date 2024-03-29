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

import boofcv.struct.packed.PackedArrayPoint2D_I16;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAztecGenerator extends BoofStandardJUnit {
	/**
	 * Generate coordinates for all possible markers and see if there are the expected number of bits
	 */
	@Test void computeDataBitCoordinates() {
		var marker = new AztecCode();
		var coordinates = new PackedArrayPoint2D_I16();
		for (var structure : AztecCode.Structure.values()) {
			marker.structure = structure;
			for (int dataLayers = 1; dataLayers <= structure.maxDataLayers; dataLayers++) {
				marker.dataLayers = dataLayers;
				AztecGenerator.computeDataBitCoordinates(marker, coordinates);

				int expected = marker.getCapacityBits();
				int found = coordinates.size();

				// only full codewords can be encoded
				found -= found%marker.getWordBitCount();
				assertEquals(expected, found, structure + " " + dataLayers);
			}
		}
	}
}
