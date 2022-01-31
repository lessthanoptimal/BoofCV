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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAztecCodecMode {
	/**
	 * Known results from ISO Figure G.6. Manually read bits from image
	 */
	@Test void encodeMode_Known0() {
		var marker = new AztecCode();
		marker.structure = AztecCode.Structure.COMPACT;
		marker.dataLayers = 1;
		marker.messageWordCount = 10;

		var found = new PackedBits8();
		var encoder = new AztecCodecMode();
		encoder.encodeMode(marker, found);

		var expected = new PackedBits8();
		expected.append(0b0000100, 7, false);
		expected.append(0b1110000, 7, false);
		expected.append(0b1000110, 7, false);
		expected.append(0b0011001, 7, false);

		assertEquals(expected.size, found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(expected.get(i), found.get(i));
		}
	}
}
