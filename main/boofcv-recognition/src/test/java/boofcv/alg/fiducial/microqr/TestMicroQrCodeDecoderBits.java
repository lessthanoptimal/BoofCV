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

import boofcv.alg.fiducial.qrcode.EciEncoding;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestMicroQrCodeDecoderBits extends BoofStandardJUnit {
	@Test public void applyErrorCorrection() {
		MicroQrCode qr = new MicroQrCodeEncoder().addNumeric("923492348985").fixate();

		var alg = new MicroQrCodeDecoderBits(EciEncoding.UTF8);

		byte[] original = new byte[qr.rawbits.length];
		System.arraycopy(qr.rawbits, 0, original, 0, original.length);

		// perfect message with no errors
		assertTrue(alg.applyErrorCorrection(qr));

		assertEquals(0, qr.totalBitErrors);
		int dataSize = qr.getNumberOfDataCodeWords();
		for (int i = 0; i < dataSize; i++) {
			assertEquals(original[i], qr.corrected[i]);
		}

		// add noise and try again
		qr.rawbits[5] ^= (byte)0b1101001011;
		qr.corrected = null;

		assertTrue(alg.applyErrorCorrection(qr));
		assertTrue(qr.totalBitErrors > 0);

		for (int i = 0; i < dataSize; i++) {
			assertEquals(original[i], qr.corrected[i]);
		}
		// add too much noise and it should fail
		for (int i = 0; i < 10; i++) {
			qr.rawbits[6 + i] = (byte)(qr.rawbits[i] + 234);
		}
		qr.corrected = null;

		assertFalse(alg.applyErrorCorrection(qr));
	}

	/** Have it decode a simple message at different versions */
	@Test public void decodeKnownMessage() {
		var alg = new MicroQrCodeDecoderBits(EciEncoding.UTF8);

		for (int version = 1; version <= 4; version++) {
			MicroQrCode expected = new MicroQrCodeEncoder().setVersion(version).
					addNumeric("0167").fixate();

			var found = new MicroQrCode();
			found.version = expected.version;
			found.error = expected.error;
			// Just copy the message data
			found.corrected = new byte[expected.getNumberOfDataCodeWords()];
			System.arraycopy(expected.rawbits, 0, found.corrected, 0, found.corrected.length);
			assertTrue(alg.decodeMessage(found));

			assertEquals(expected.message, found.message);
		}
	}
}
