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

package boofcv.factory.fiducial;

import boofcv.factory.fiducial.ConfigHammingDictionary.Dictionary;
import boofcv.struct.StandardConfigurationChecks;
import org.junit.jupiter.api.Test;

import static boofcv.factory.fiducial.ConfigHammingDictionary.define;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConfigHammingDictionary extends StandardConfigurationChecks {
	@Test void decodeDictionaryString() {
		String text = """
				# Ignore this line
				grid_width=4
				minimum_hamming=3
				dictionary=0x5867,0x8b03,0x2537,0x4d58dbea0
				""";

		ConfigHammingDictionary found = ConfigHammingDictionary.decodeDictionaryString(text);
		assertEquals(4, found.gridWidth);
		assertEquals(3, found.minimumHamming);
		assertEquals(4, found.encoding.size);

		for (int i = 0; i < found.encoding.size; i++) {
			assertEquals(i, found.encoding.get(i).id);
		}

		checkEncoding(0x5867, found, 0);
		checkEncoding(0x8b03, found, 1);
		checkEncoding(0x2537, found, 2);
		checkEncoding(Long.parseUnsignedLong("4d58dbea0", 16), found, 3);
	}

	private void checkEncoding( long id, ConfigHammingDictionary found, int markerIndex ) {
		ConfigHammingDictionary.Marker marker = found.encoding.get(markerIndex);
		int bits = found.bitsPerGrid();
		for (int i = 0; i < bits; i++) {
			int expected = (int)((id >> i) & 1L);
			assertEquals(expected, marker.pattern.get(i));
		}
	}

	@Test void define_ARUCO_ORIGINAL() {
		ConfigHammingDictionary found = define(Dictionary.ARUCO_ORIGINAL);
		assertEquals(5, found.gridWidth);
		assertEquals(1, found.minimumHamming);
		assertEquals(1023, found.encoding.size);
	}

	@Test void define_ARUCO_MIP_16h3() {
		ConfigHammingDictionary found = define(Dictionary.ARUCO_MIP_16h3);
		assertEquals(4, found.gridWidth);
		assertEquals(3, found.minimumHamming);
		assertEquals(250, found.encoding.size);
	}

	@Test void define_ARUCO_MIP_25h7() {
		ConfigHammingDictionary found = define(Dictionary.ARUCO_MIP_25h7);
		assertEquals(5, found.gridWidth);
		assertEquals(7, found.minimumHamming);
		assertEquals(100, found.encoding.size);
	}

	@Test void define_ARUCO_MIP_36h12() {
		ConfigHammingDictionary found = define(Dictionary.ARUCO_MIP_36h12);
		assertEquals(6, found.gridWidth);
		assertEquals(12, found.minimumHamming);
		assertEquals(250, found.encoding.size);
	}
}
