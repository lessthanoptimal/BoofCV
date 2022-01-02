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

import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.qrcode.QrCodeCodecBitsUtils.flipBits8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMicroQrCodeEncoder {
	/**
	 * In the qr code specification an example is given. This compares the computed results to that example
	 */
	@Test void numeric_specification() {
		MicroQrCode qr = new MicroQrCodeEncoder().setVersion(2).
				setError(MicroQrCode.ErrorLevel.L).
				setMask(new MicroQrCodeMaskPattern.NONE(0b011)).
				addNumeric("01234567").fixate();

		byte[] expected = new byte[]{
				(byte)0b0100_0000, 0b0001_1000, (byte)0b1010_1100, (byte)0b1100_0011, 0b0000_0000,
				(byte)0b1000_0110, 0b0000_1101, (byte)0b0010_0010, (byte)0b1010_1110, 0b0011_0000};

		flipBits8(expected, expected.length);

		assertEquals(qr.rawbits.length, expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], qr.rawbits[i], "index=" + i);
		}
	}
}
