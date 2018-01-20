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

	/**
	 * Compare to QR code reference
	 */
	@Test
	public void encodeVersionBits() {
		int found = QrCodePolynomialMath.encodeVersionBits(7);
		int expected = 0b000111110010010100;

		assertEquals(expected,found);
	}

	@Test
	public void checkVersionBits() {
		for (int version = 7; version <= 40; version++) {
			int found = QrCodePolynomialMath.encodeVersionBits(version);
			assertTrue( QrCodePolynomialMath.checkVersionBits(found));

			// introduce a single bit flip
			for ( int i = 0; i < 18; i++ ) {
				int mod = found ^ (1<<i);
				assertFalse(QrCodePolynomialMath.checkVersionBits(mod));
			}
		}
	}


	/**
	 * Compare to QR code reference
	 */
	@Test
	public void encodeFormatBits() {

		int found = QrCodePolynomialMath.encodeFormatBits(QrCode.ErrorLevel.M,0b101);
		found ^= QrCodePolynomialMath.FORMAT_MASK;
		int expected = 0b100000011001110;

		assertEquals(expected,found);
	}

	@Test
	public void checkFormatBits() {
		for( QrCode.ErrorLevel error : QrCode.ErrorLevel.values()) {
			int found = QrCodePolynomialMath.encodeFormatBits(error,0b101);

			assertTrue( QrCodePolynomialMath.checkFormatBits(found));

			// introduce a single bit flip
			for ( int i = 0; i < 15; i++ ) {
				int mod = found ^ (1<<i);
				assertFalse(QrCodePolynomialMath.checkFormatBits(mod));
			}
		}
	}

	@Test
	public void decodeFormatMessage() {
		QrCode qr = new QrCode();
		for( QrCode.ErrorLevel error : QrCode.ErrorLevel.values()) {
			int message = QrCodePolynomialMath.encodeFormatBits(error,0b101);
			message >>= 10;

			QrCodePolynomialMath.decodeFormatMessage(message,qr);

			assertEquals(error,qr.error);
			assertTrue(QrCodeMaskPattern.M101==qr.mask);
		}
	}

	@Test
	public void correctDCH() {
		int data = 0b10101;
		int errorBits = 10;
		int dataBits = 5;
		int generator = QrCodePolynomialMath.FORMAT_GENERATOR;
		int message = (data<<errorBits)^QrCodePolynomialMath.bitPolyModulus(data<<errorBits,generator,
				errorBits+dataBits,dataBits);

		for (int i = 0; i < data; i++) {
			// single bit errors
			int corrupted = message ^ (1<<i);
			int corrected = QrCodePolynomialMath.correctDCH(
					32,corrupted,generator,errorBits+dataBits,dataBits);

			assertEquals(data,corrected);

			// two bit errors
			for (int j = 0; j < 32; j++) {
				int corrupted2 = corrupted ^ (1<<j);
				corrected = QrCodePolynomialMath.correctDCH(
						32,corrupted2,generator,errorBits+dataBits,dataBits);

				assertEquals(data,corrected);
			}
		}
	}

	/**
	 * Test against an example from the QR code reference manual for format information
	 */
	@Test
	public void bitPolyDivide() {
		int message = 0b00101 << 10;
		int divisor = 0b10100110111;

		int found = QrCodePolynomialMath.bitPolyModulus(message,divisor,15,5);
		int expected = 0b0011011100;

		assertEquals(expected,found);
	}
}