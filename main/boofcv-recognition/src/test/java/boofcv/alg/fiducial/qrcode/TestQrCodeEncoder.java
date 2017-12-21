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
public class TestQrCodeEncoder {
	/**
	 * In the qr code specification an example is given. This compares the computed results
	 * to that example
	 */
	@Test
	public void numeric_specification() {
		QrCode qr = new QrCodeEncoder().setVersion(1).
				setError(QrCode.ErrorLevel.M).
				setMask(new QrCodeMaskPattern.NONE(0b011)).
				numeric("01234567");

		byte[] expected = new byte[]{0b00010000,
		0b00100000, 0b00001100, 0b01010110, 0b01100001 ,(byte)0b10000000, (byte)0b11101100, 0b00010001,
				(byte)0b11101100, 0b00010001, (byte)0b11101100, 0b00010001, (byte)0b11101100, 0b00010001,
				(byte)0b11101100, 0b00010001, (byte)0b10100101, 0b00100100,
				(byte)0b11010100, (byte)0b11000001, (byte)0b11101101, 0b00110110,
				(byte)0b11000111, (byte)0b10000111, 0b00101100, 0b01010101};

		QrCodeEncoder.flipBits8(expected,expected.length);

		assertEquals(qr.dataRaw.length,expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(qr.dataRaw[i],expected[i]);
		}
	}

	/**
	 * Compare only the data portion against an example from the specification
	 */
	@Test
	public void alphanumeric_specification() {
		QrCodeEncoder encoder = new QrCodeEncoder();
		encoder.setVersion(1).
				setError(QrCode.ErrorLevel.H).
				setMask(new QrCodeMaskPattern.NONE(0b011)).
				alphanumeric("AC-42");

		byte[] expected = new byte[]{0b00100000, 0b00101001, (byte)0b11001110, (byte)0b11100111, 0b00100001,0};

		QrCodeEncoder.flipBits8(expected,expected.length);

		assertEquals(encoder.packed.size/8,expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(encoder.packed.data[i],expected[i]);
		}
	}

	@Test
	public void alphanumericToValues() {
		byte found[] = QrCodeEncoder.alphanumericToValues("14AE%*+-./:");
		byte expected[] = new byte[]{1,4,10,14,38,39,40,41,42,43,44};

		assertArrayEquals(expected,found);
	}

	/**
	 * Test comparing against a data stream that was successfully decoded by another qr-code reader
	 */
	@Test
	public void kanji() {
		QrCodeEncoder encoder = new QrCodeEncoder();
		encoder.setVersion(1).setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				kanji("阿ん鞠ぷへ≦Ｋ");

		byte expected[] = new byte[]{
				0x01,0x4E,(byte)0x8B,(byte)0xA0,0x23,
				0x2F,(byte)0x83,(byte)0xAA,0x50,0x0D,(byte)0x88,(byte)0x82,0x2B,0x00};

		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i],encoder.packed.data[i]);
		}
	}


	@Test
	public void autoSelectVersion() {
		fail("Implement");
	}

	@Test
	public void autoSelectMask() {
		fail("Implement");
	}

	@Test
	public void autoSelectErrorCorrection() {
		fail("Implement");
	}

	@Test
	public void manuialVersion() {
		fail("Implement");
	}

	@Test
	public void manuialMask() {
		fail("Implement");
	}

	@Test
	public void manuialErrorCorrection() {
		fail("Implement");
	}

}