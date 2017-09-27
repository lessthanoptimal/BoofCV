/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodePolynomialMath {
	@Test
	public void encodeFormatMessage() {

		int found = QrCodePolynomialMath.encodeFormatMessage(QrCode.ErrorCorrectionLevel.M,0b101);
		int expected = 0b100000011001110;

		assertEquals(expected,found);
	}

	@Test
	public void checkFormatmessage() {
		for( QrCode.ErrorCorrectionLevel error : QrCode.ErrorCorrectionLevel.values()) {
			int found = QrCodePolynomialMath.encodeFormatMessage(error,0b101);

			assertTrue( QrCodePolynomialMath.checkFormatMessage(found));

			// introduce a single bit flip
			for ( int i = 0; i < 15; i++ ) {
				int mod = found ^ (1<<i);
				assertFalse(QrCodePolynomialMath.checkFormatMessage(mod));
			}
		}
	}

	@Test
	public void decodeFormatMessage() {
		QrCode qr = new QrCode();
		for( QrCode.ErrorCorrectionLevel error : QrCode.ErrorCorrectionLevel.values()) {
			int message = QrCodePolynomialMath.encodeFormatMessage(error,0b101);

			QrCodePolynomialMath.decodeFormat(message^QrCodePolynomialMath.FORMAT_MASK,qr);

			assertEquals(error,qr.errorCorrection);
			assertEquals(0b101,qr.maskPattern);
		}
	}

	/**
	 * Test against an example from the QR code reference manual for format information
	 */
	@Test
	public void bitPolyDivide() {
		int message = 0b00101 << 10;
		int divisor = 0b10100110111;

		int found = QrCodePolynomialMath.bitPolyDivide(message,divisor,15,5);
		int expected = 0b0011011100;

		assertEquals(expected,found);
	}
}