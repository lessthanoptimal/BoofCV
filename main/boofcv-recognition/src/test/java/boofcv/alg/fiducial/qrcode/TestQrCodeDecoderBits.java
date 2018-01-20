/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
public class TestQrCodeDecoderBits {

	@Test
	public void applyErrorCorrection() {
		QrCode qr = new QrCodeEncoder().addNumeric("923492348985").fixate();

		QrCodeDecoderBits alg = new QrCodeDecoderBits();

		byte original[] = new byte[qr.rawbits.length];
		System.arraycopy(qr.rawbits,0,original,0,original.length);

		// perfect message with no errors
		assertTrue(alg.applyErrorCorrection(qr));

		int dataSize = qr.getNumberOfDataBytes();
		for (int i = 0; i < dataSize; i++) {
			assertEquals(original[i],qr.corrected[i]);
		}

		// add noise and try again
		qr.rawbits[5] ^= 0b1101001011;
		qr.corrected = null;

		assertTrue(alg.applyErrorCorrection(qr));

		for (int i = 0; i < dataSize; i++) {
			assertEquals(original[i],qr.corrected[i]);
		}
		// add too much noise and it should fail
		for (int i = 0; i < 10; i++) {
			qr.rawbits[6+i] = (byte)(qr.rawbits[i]+234);
		}
		qr.corrected = null;

		assertFalse(alg.applyErrorCorrection(qr));
	}

	@Test
	public void alignToBytes() {
		assertEquals(0, QrCodeDecoderBits.alignToBytes(0));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(1));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(7));
		assertEquals(8, QrCodeDecoderBits.alignToBytes(8));
		assertEquals(16, QrCodeDecoderBits.alignToBytes(9));
	}

	@Test
	public void checkPaddingBytes() {
		QrCode qr = new QrCodeEncoder().addNumeric("923492348985").fixate();

		QrCodeDecoderBits alg = new QrCodeDecoderBits();

		qr.corrected = new byte[50];

		assertFalse(alg.checkPaddingBytes(qr,2));

		// fill it with the pattern
		for (int i = 2; i < qr.corrected.length; i++) {
			if( i%2 == 0 ) {
				qr.corrected[i] = 0b00110111;
			} else {
				qr.corrected[i] = (byte)0b10001000;
			}
		}

		assertTrue(alg.checkPaddingBytes(qr,2));

		// Test failure conditions now
		assertFalse(alg.checkPaddingBytes(qr,3));
		qr.corrected[8] ^= 0x1101;
		assertFalse(alg.checkPaddingBytes(qr,2));


	}
}
