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
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.qrcode.QrCodeCodecBitsUtils.flipBits8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestMicroQrCodeEncoder extends BoofStandardJUnit {
	/**
	 * In the qr code specification an example is given. This compares the computed results to that example
	 */
	@Test void numeric_specification_NoMask() {
		MicroQrCode qr = new MicroQrCodeEncoder().setVersion(2).
				setError(MicroQrCode.ErrorLevel.L).
				setMask(MicroQrCodeMaskPattern.NONE).
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

	@Test void automatic() {
		var encoder = new MicroQrCodeEncoder();
		var decoder = new MicroQrCodeDecoderBits(EciEncoding.UTF8); // used to validate the message
		MicroQrCode qr = encoder.addAutomatic("142f阿ん鞠≦Ｋ").fixate();
		assertEquals(qr.message, "142f阿ん鞠≦Ｋ");
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.MIXED, qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123ASDdf").fixate();
		assertEquals(qr.message, "123ASDdf");
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.BYTE, qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123ASD").fixate();
		assertEquals(qr.message, "123ASD");
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.ALPHANUMERIC, qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123").fixate();
		assertEquals(qr.message, "123");
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.NUMERIC, qr.mode);
	}

	@Test void messageTooLong() {
		assertThrows(IllegalArgumentException.class, () ->
				new MicroQrCodeEncoder().setVersion(1).
						setError(MicroQrCode.ErrorLevel.M).
						setMask(MicroQrCodeMaskPattern.M10).
						addNumeric("0123123123123123123123").fixate());
	}

	/**
	 * Encodes and then decodes it for several different lengths
	 */
	@Test void encodeThenDecode() {
		for (int length = 1; length < 15; length++) {
			encodeThenDecodeBytes(length);
		}
		for (int length = 1; length < 35; length++) {
			encodeThenDecodeNumeric(length);
		}
	}

	private void encodeThenDecodeNumeric( int length ) {
		String message = "";
		for (int i = 0; i < length; i++) {
			message += "" + (rand.nextInt(10));
		}

		MicroQrCode qr = new MicroQrCodeEncoder().setMask(MicroQrCodeMaskPattern.M10).addNumeric(message).fixate();

		qr.message = "";
		var decoder = new MicroQrCodeDecoderBits(EciEncoding.UTF8);
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));

		assertEquals(message, qr.message);
	}

	private void encodeThenDecodeBytes( int length ) {
		String message = "";
		for (int i = 0; i < length; i++) {
			message += (char)(0x21 + rand.nextInt(50));
		}

		MicroQrCode qr = new MicroQrCodeEncoder().setMask(MicroQrCodeMaskPattern.M10).addBytes(message).fixate();

		qr.message = "";
		var decoder = new MicroQrCodeDecoderBits(EciEncoding.UTF8);
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));

		assertEquals(message, qr.message);
	}

	/**
	 * See if it blows up when encoding using multiple encoding methods
	 */
	@Test void multipleModes() {
		MicroQrCode qr = new MicroQrCodeEncoder().
				setVersion(4).setError(MicroQrCode.ErrorLevel.L).
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("1234").
				addKanji("阿ん鞠").fixate();

		assertEquals("1234阿ん鞠", qr.message);
	}

	@Test void tooMuchData() {
		var encoder = new MicroQrCodeEncoder()
				.setVersion(2).setError(MicroQrCode.ErrorLevel.M).
				setMask(MicroQrCodeMaskPattern.M10).
				addAlphanumeric("ASDASDJASD983405983094580SDF:LOEFLAWEQR");

		assertThrows(RuntimeException.class, encoder::fixate);
	}

	@Test void autoSelectVersion() {
		MicroQrCode qr = new MicroQrCodeEncoder().
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("12123").fixate();

		assertEquals(1, qr.version);

		qr = new MicroQrCodeEncoder().
				setError(MicroQrCode.ErrorLevel.M).
				setMask(MicroQrCodeMaskPattern.M10).
				addAlphanumeric("123132123123132122").fixate();

		assertEquals(4, qr.version);
	}

	@Test void autoSelectErrorCorrection() {
		MicroQrCode qr = new MicroQrCodeEncoder().
				setVersion(2).
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("1").fixate();

		assertEquals(MicroQrCode.ErrorLevel.M, qr.error);

		qr = new MicroQrCodeEncoder().
				setVersion(4).
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("1231").fixate();

		assertEquals(MicroQrCode.ErrorLevel.Q, qr.error);

		qr = new MicroQrCodeEncoder().
				setVersion(4).
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("123132123123132122983848374923223").fixate();

		assertEquals(MicroQrCode.ErrorLevel.L, qr.error);
	}

	@Test void autoSelectVersionAndError() {
		MicroQrCode qr = new MicroQrCodeEncoder().
				setMask(MicroQrCodeMaskPattern.M10).
				addNumeric("123").fixate();
		assertEquals(1, qr.version);
		assertEquals(MicroQrCode.ErrorLevel.DETECT, qr.error);

		// alphanumeric isn't supported by version 1
		qr = new MicroQrCodeEncoder().
				setMask(MicroQrCodeMaskPattern.M10).
				addAlphanumeric("1").fixate();
		assertEquals(2, qr.version);
		assertEquals(MicroQrCode.ErrorLevel.M, qr.error);

		qr = new MicroQrCodeEncoder().
				setMask(MicroQrCodeMaskPattern.M10).
				addAlphanumeric("123123213AAE324SDASD").fixate();

		assertEquals(4, qr.version);
		assertEquals(MicroQrCode.ErrorLevel.L, qr.error);
	}

	@Test void autoSelectMask() {
		MicroQrCode qr = new MicroQrCodeEncoder().addAlphanumeric("123").fixate();
		assertNotNull(qr.mask);
	}
}
