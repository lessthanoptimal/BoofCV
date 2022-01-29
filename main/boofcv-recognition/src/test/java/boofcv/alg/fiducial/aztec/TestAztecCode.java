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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAztecCode {
	/** Compare against table in specification */
	@Test void getSizeSquares() {
		var marker = new AztecCode();
		marker.dataLayers = 1;
		marker.structure = AztecCode.Structure.COMPACT;
		assertEquals(15, marker.getMarkerSquareCount());
		marker.structure = AztecCode.Structure.FULL;
		assertEquals(19, marker.getMarkerSquareCount());

		marker.dataLayers = 4;
		marker.structure = AztecCode.Structure.COMPACT;
		assertEquals(27, marker.getMarkerSquareCount());
		marker.structure = AztecCode.Structure.FULL;
		assertEquals(31, marker.getMarkerSquareCount());

		marker.dataLayers = 5;
		assertEquals(37, marker.getMarkerSquareCount());
		marker.dataLayers = 6;
		assertEquals(41, marker.getMarkerSquareCount());
		marker.dataLayers = 16;
		assertEquals(83, marker.getMarkerSquareCount());
		marker.dataLayers = 32;
		assertEquals(151, marker.getMarkerSquareCount());
	}

	@Test void getLocatorRingCount() {
		var marker = new AztecCode();
		marker.dataLayers = 10; // distraction. Shouldn't change results
		marker.structure = AztecCode.Structure.COMPACT;
		assertEquals(2, marker.getLocatorRingCount());
		marker.structure = AztecCode.Structure.FULL;
		assertEquals(3, marker.getLocatorRingCount());
	}

	@Test void getLocatorSquareCount() {
		var marker = new AztecCode();
		marker.dataLayers = 10; // distraction. Shouldn't change results
		marker.structure = AztecCode.Structure.COMPACT;
		assertEquals(5, marker.getLocatorSquareCount());
		marker.structure = AztecCode.Structure.FULL;
		assertEquals(9, marker.getLocatorSquareCount());
	}
}
