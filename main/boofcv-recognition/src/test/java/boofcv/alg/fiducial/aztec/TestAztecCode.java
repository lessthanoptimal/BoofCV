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
		marker.structure = AztecCode.Structure.COMPACT;
		checkMarkerSquareCount(1, 15, marker);
		marker.structure = AztecCode.Structure.FULL;
		checkMarkerSquareCount(1, 19, marker);

		marker.structure = AztecCode.Structure.COMPACT;
		checkMarkerSquareCount(4, 27, marker);
		marker.structure = AztecCode.Structure.FULL;
		checkMarkerSquareCount(4, 31, marker);

		checkMarkerSquareCount(5, 37, marker);
		checkMarkerSquareCount(6, 41, marker);
		checkMarkerSquareCount(7, 45, marker);
		checkMarkerSquareCount(8, 49, marker);
		checkMarkerSquareCount(9, 53, marker);
		checkMarkerSquareCount(10, 57, marker);
		checkMarkerSquareCount(11, 61, marker);
		checkMarkerSquareCount(12, 67, marker);
		checkMarkerSquareCount(13, 71, marker);
		checkMarkerSquareCount(14, 75, marker);
		checkMarkerSquareCount(15, 79, marker);
		checkMarkerSquareCount(16, 83, marker);
		checkMarkerSquareCount(17, 87, marker);
		checkMarkerSquareCount(18, 91, marker);
		checkMarkerSquareCount(19, 95, marker);
		checkMarkerSquareCount(20, 101, marker);
		checkMarkerSquareCount(25, 121, marker);
		checkMarkerSquareCount(29, 139, marker);
		checkMarkerSquareCount(32, 151, marker);
	}

	private void checkMarkerSquareCount( int layers, int expected, AztecCode marker ) {
		marker.dataLayers = layers;
		assertEquals(expected, marker.getMarkerWidthSquares());
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
		assertEquals(5, marker.getLocatorWidthSquares());
		marker.structure = AztecCode.Structure.FULL;
		assertEquals(9, marker.getLocatorWidthSquares());
	}

	@Test void countCodewords() {
		assertEquals(4, AztecCode.Structure.COMPACT.getMaxDataLayers());
		assertEquals(32, AztecCode.Structure.FULL.getMaxDataLayers());
	}
}
