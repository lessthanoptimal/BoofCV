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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.fiducial.qrcode.PackedBits32;
import boofcv.struct.StandardConfigurationChecks;
import org.junit.jupiter.api.Test;

import static boofcv.factory.fiducial.ConfigHammingMarker.loadDictionary;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConfigHammingMarker extends StandardConfigurationChecks {
	@Test void decodeDictionaryString() {
		String text = """
				# Ignore this line
				grid_width=4
				minimum_hamming=3
				dictionary=0x5867,0x8b03,0x2537,0x4d58dbea0
				""";

		ConfigHammingMarker found = ConfigHammingMarker.decodeDictionaryString(text);
		assertEquals(4, found.gridWidth);
		assertEquals(3, found.minimumHamming);
		assertEquals(4, found.encoding.size());

		checkEncoding(0x5867, found, 0);
		checkEncoding(0x8b03, found, 1);
		checkEncoding(0x2537, found, 2);
		checkEncoding(Long.parseUnsignedLong("4d58dbea0", 16), found, 3);
	}

	private void checkEncoding( long id, ConfigHammingMarker found, int markerIndex ) {
		ConfigHammingMarker.Marker marker = found.encoding.get(markerIndex);
		int bits = found.bitsPerGrid();
		for (int i = 0; i < bits; i++) {
			int expected = (int)((id >> i) & 1L);
			assertEquals(expected, marker.pattern.get(i));
		}
	}

	@Test void define_ARUCO_ORIGINAL() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_ORIGINAL);
		assertEquals(5, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(1024, found.encoding.size());
	}

	@Test void define_ARUCO_MIP_16h3() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_MIP_16h3);
		assertEquals(4, found.gridWidth);
		assertEquals(3, found.minimumHamming);
		assertEquals(250, found.encoding.size());
	}

	@Test void define_ARUCO_MIP_25h7() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_MIP_25h7);
		assertEquals(5, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(100, found.encoding.size());
	}

	@Test void define_ARUCO_MIP_36h12() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_MIP_36h12);
		assertEquals(6, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(250, found.encoding.size());
	}

	@Test void define_ARUCO_OCV_4x4_1000() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_OCV_4x4_1000);
		assertEquals(4, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(1000, found.encoding.size());
	}

	@Test void define_ARUCO_OCV_5x5_1000() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_OCV_5x5_1000);
		assertEquals(5, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(1000, found.encoding.size());
	}

	@Test void define_ARUCO_OCV_6x6_1000() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_OCV_6x6_1000);
		assertEquals(6, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(1000, found.encoding.size());
	}

	@Test void define_ARUCO_OCV_7x7_1000() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.ARUCO_OCV_7x7_1000);
		assertEquals(7, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(1000, found.encoding.size());
	}

	@Test void define_APRILTAG_16h5() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.APRILTAG_16h5);
		assertEquals(4, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(30, found.encoding.size());
	}

	@Test void define_APRILTAG_25h7() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.APRILTAG_25h7);
		assertEquals(5, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(242, found.encoding.size());
	}

	@Test void define_APRILTAG_25h9() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.APRILTAG_25h9);
		assertEquals(5, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(35, found.encoding.size());
	}

	@Test void define_APRILTAG_36h10() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.APRILTAG_36h10);
		assertEquals(6, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(2320, found.encoding.size());
	}

	@Test void define_APRILTAG_36h11() {
		ConfigHammingMarker found = loadDictionary(HammingDictionary.APRILTAG_36h11);
		assertEquals(6, found.gridWidth);
		assertEquals(bruteForceMinimum(found), found.minimumHamming);
		assertEquals(587, found.encoding.size());
	}

	private int bruteForceMinimum(ConfigHammingMarker found) {
		int distance = found.gridWidth*found.gridWidth;
		for (int i = 0; i < found.encoding.size(); i++) {
			PackedBits32 pi = found.encoding.get(i).pattern;
			for (int j = i+1; j < found.encoding.size(); j++) {
				PackedBits32 pj = found.encoding.get(j).pattern;

				int c = 0;
				for (int k = 0; k < pi.arrayLength(); k++) {
					c += DescriptorDistance.hamming(pi.data[k]^pj.data[k]);
				}

				if (c<distance) {
					distance = c;
				}
			}
		}
		return distance;
	}
}
