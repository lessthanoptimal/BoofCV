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

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodeEncoder {
	Random rand = new Random(234);

	/**
	 * In the qr code specification an example is given. This compares the computed results
	 * to that example
	 */
	@Test
	public void numeric_specification() {
		QrCode qr = new QrCodeEncoder().setVersion(1).
				setError(QrCode.ErrorLevel.M).
				setMask(new QrCodeMaskPattern.NONE(0b011)).
				addNumeric("01234567").fixate();

		byte[] expected = new byte[]{0b00010000,
		0b00100000, 0b00001100, 0b01010110, 0b01100001 ,(byte)0b10000000, (byte)0b11101100, 0b00010001,
				(byte)0b11101100, 0b00010001, (byte)0b11101100, 0b00010001, (byte)0b11101100, 0b00010001,
				(byte)0b11101100, 0b00010001, (byte)0b10100101, 0b00100100,
				(byte)0b11010100, (byte)0b11000001, (byte)0b11101101, 0b00110110,
				(byte)0b11000111, (byte)0b10000111, 0b00101100, 0b01010101};

		QrCodeEncoder.flipBits8(expected,expected.length);

		assertEquals(qr.rawbits.length,expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i],qr.rawbits[i]);
		}
	}

	@Test
	public void checkAlphaNumericLookUpTable() {
		assertEquals(45,QrCodeEncoder.ALPHANUMERIC.length());
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
				addAlphanumeric("AC-42").fixate();

		byte[] expected = new byte[]{0b00100000, 0b00101001, (byte)0b11001110, (byte)0b11100111, 0b00100001,0};

		QrCodeEncoder.flipBits8(expected,expected.length);

		assertEquals(encoder.packed.size/8,expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i],encoder.packed.data[i]);
		}
	}

	@Test
	public void alphanumericToValues() {
		byte found[] = QrCodeEncoder.alphanumericToValues("14AE%*+-./:");
		byte expected[] = new byte[]{1,4,10,14,38,39,40,41,42,43,44};

		assertArrayEquals(expected,found);
	}

	@Test
	public void valueToAlphanumeric() {
		byte input[] = new byte[]{1,4,10,14,38,39,40,41,42,43,44};
		String expected = "14AE%*+-./:";
		for (int i = 0; i < input.length; i++) {
			char c = QrCodeEncoder.valueToAlphanumeric(input[i]);
			assertTrue(c == expected.charAt(i));
		}
	}

	/**
	 * Test comparing against a data stream that was successfully decoded by another qr-code reader
	 */
	@Test
	public void kanji() {
		QrCodeEncoder encoder = new QrCodeEncoder();
		encoder.setVersion(1).setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addKanji("阿ん鞠ぷへ≦Ｋ").fixate();

		byte expected[] = new byte[]{
				0x01,0x4E,(byte)0x8B,(byte)0xA0,0x23,
				0x2F,(byte)0x83,(byte)0xAA,0x50,0x0D,(byte)0x88,(byte)0x82,0x2B,0x00};

		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i],encoder.packed.data[i]);
		}
	}

	@Test
	public void automatic() {
		QrCodeEncoder encoder = new QrCodeEncoder();
		QrCodeDecoderBits decoder = new QrCodeDecoderBits(); // used to validate the message
		QrCode qr = encoder.addAutomatic("123ASDdf阿ん鞠ぷへ≦Ｋ").fixate();
		assertTrue("123ASDdf阿ん鞠ぷへ≦Ｋ".equals(qr.message));

		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.KANJI,qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123ASDdf").fixate();
		assertTrue("123ASDdf".equals(qr.message));
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.BYTE,qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123ASD").fixate();
		assertTrue("123ASD".equals(qr.message));
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.ALPHANUMERIC,qr.mode);

		encoder.reset();
		qr = encoder.addAutomatic("123").fixate();
		assertTrue("123".equals(qr.message));
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));
		assertEquals(QrCode.Mode.NUMERIC,qr.mode);
	}

	@Test(expected = IllegalArgumentException.class)
	public void messageTooLong() {
		QrCode qr = new QrCodeEncoder().setVersion(1).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("01234567890123456789012345678901234567890123456789012345678901234567890123456789").fixate();

		assertTrue(qr.rawbits.length==26);
	}

	/**
	 * Encodes and then decodes it for several different lengths
	 */
	@Test
	public void encodeThenDecode() {
		for (int length = 1; length < 30; length++) {
			encodeThenDecode(length);
		}
		encodeThenDecode(1000);
		encodeThenDecode(2000);
		encodeThenDecode(2800);
	}

	private void encodeThenDecode(int length) {
		String message = "";
		for (int i = 0; i < length; i++) {
			message += (char)(0x21+rand.nextInt(50));
		}

		QrCode qr = new QrCodeEncoder().setMask(QrCodeMaskPattern.M011).addBytes(message).fixate();

		qr.message = null;
		QrCodeDecoderBits decoder = new QrCodeDecoderBits();
		assertTrue(decoder.applyErrorCorrection(qr));
		assertTrue(decoder.decodeMessage(qr));

		assertEquals(message, qr.message.toString());
	}

	/**
	 * See if it blows up when encoding using multiple encoding methods
	 */
	@Test
	public void multipleModes() {
		new QrCodeEncoder()
				.setVersion(1).setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addNumeric("1234").
				addKanji("阿ん鞠ぷへ≦Ｋ").fixate();
	}

	@Test
	public void autoSelectVersion() {
		QrCode qr = new QrCodeEncoder().
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123132123").fixate();

		assertEquals(1,qr.version);

		qr = new QrCodeEncoder().
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123132123123132123123132123").fixate();

		assertEquals(2,qr.version);
	}

	@Test
	public void autoSelectErrorCorrection() {
		QrCode qr = new QrCodeEncoder().
				setVersion(1).
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123").fixate();

		assertEquals(QrCode.ErrorLevel.H,qr.error);

		qr = new QrCodeEncoder().
				setVersion(1).
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123123213AADE32425434985").fixate();

		assertEquals(QrCode.ErrorLevel.L,qr.error);
	}

	@Test
	public void autoSelectVersionAndError() {
		QrCode qr = new QrCodeEncoder().
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123").fixate();

		assertEquals(1,qr.version);
		assertEquals(QrCode.ErrorLevel.M,qr.error);

		qr = new QrCodeEncoder().
				setMask(QrCodeMaskPattern.M011).
				addAlphanumeric("123123213AADE324254349985ASDASD").fixate();

		assertEquals(2,qr.version);
		assertEquals(QrCode.ErrorLevel.M,qr.error);
	}

	@Test
	public void autoSelectMask() {
		QrCode qr = new QrCodeEncoder().addAlphanumeric("123").fixate();
		assertTrue(qr.mask != null);
	}

	/**
	 * Sanity check on qr code with no data encoded.
	 */
	@Test
	public void detectAdjacentAndPositionPatterns_test0() {
		int version = 1;
		QrCodeEncoder.FoundFeatures features = new QrCodeEncoder.FoundFeatures();
		int N = QrCode.totalModules(version);
		QrCodeCodeWordLocations matrix = new QrCodeCodeWordLocations(version);

		QrCodeEncoder.detectAdjacentAndPositionPatterns(N,matrix,features);

		// the matrix masks out pixels which are not data bits
		assertEquals(0,features.position);
		// lots of squares will have adjacent values be the same in this situation

		// the matrix is modified. Make sure it's the same on output
		QrCodeCodeWordLocations test = new QrCodeCodeWordLocations(version);
		for (int i = 0; i < N*N; i++) {
			assertEquals(test.data[i],matrix.data[i]);
		}
	}

	/**
	 * Insert a fake position pattern and see if it is found
	 */
	@Test
	public void detectAdjacentAndPositionPatterns_test1() {
		int version = 1;
		QrCodeEncoder.FoundFeatures features = new QrCodeEncoder.FoundFeatures();
		int N = QrCode.totalModules(version);
		QrCodeCodeWordLocations matrix = new QrCodeCodeWordLocations(version);

		matrix.set(10,10,true);
		matrix.set(11,10,false);
		matrix.set(12,10,true);
		matrix.set(13,10,true);
		matrix.set(14,10,true);
		matrix.set(15,10,false);
		matrix.set(16,10,true);

		QrCodeEncoder.detectAdjacentAndPositionPatterns(N,matrix,features);
		assertEquals(1, features.position);

		matrix.set(10,10,true);
		matrix.set(10,11,false);
		matrix.set(10,12,true);
		matrix.set(10,13,true);
		matrix.set(10,14,true);
		matrix.set(10,15,false);
		matrix.set(10,16,true);

		features.position = 0;
		QrCodeEncoder.detectAdjacentAndPositionPatterns(N,matrix,features);
		assertEquals(2, features.position);
	}

	/**
	 * Test adjacent counter by making a very specific pattern
	 */
	@Test
	public void detectAdjacentAndPositionPatterns_test2() {
		int version = 1;
		QrCodeEncoder.FoundFeatures features = new QrCodeEncoder.FoundFeatures();
		int N = QrCode.totalModules(version);
		QrCodeCodeWordLocations matrix = new QrCodeCodeWordLocations(version);

		matrix.fill(false);
		for (int i = 0; i < N; i++) {
			boolean v = true;
			for (int j = i%2; j < N; j++, v=!v) {
				matrix.set(i,j,v);
			}
		}

		int a = -222;
		QrCodeEncoder.detectAdjacentAndPositionPatterns(N,matrix,features);
		assertEquals(a,features.adjacent);

		features.adjacent = 0;
		matrix.set(0,1,true);
		QrCodeEncoder.detectAdjacentAndPositionPatterns(N,matrix,features);
		assertEquals(a+3,features.adjacent);
	}


}