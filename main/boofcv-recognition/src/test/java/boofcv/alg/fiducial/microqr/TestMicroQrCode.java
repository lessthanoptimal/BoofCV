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

package boofcv.alg.fiducial.microqr;

import boofcv.alg.fiducial.microqr.MicroQrCode.ErrorLevel;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMicroQrCode extends BoofStandardJUnit {

	@Test void totalModules() {
		assertEquals(11, MicroQrCode.totalModules(1));
		assertEquals(17, MicroQrCode.totalModules(4));
	}

	/**
	 * Go through all possible version and error levels and see if they are correctly decoded
	 */
	@Test void encodeAndDecodeFormat() {
		var qr = new MicroQrCode();
		var found = new MicroQrCode();
		for (int version = 1; version <= 4; version++) {
			qr.version = version;
			ErrorLevel[] levels = MicroQrCode.allowedErrorCorrection(version);
			for (ErrorLevel error : levels) {
				qr.error = error;

				assertTrue(found.decodeFormatBits(qr.encodeFormatBits()));

				assertEquals(qr.version, found.version);
				assertEquals(qr.error, found.error);
			}
		}
	}
}
