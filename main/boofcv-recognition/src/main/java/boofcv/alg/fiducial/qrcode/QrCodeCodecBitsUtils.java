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

package boofcv.alg.fiducial.qrcode;

import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static boofcv.alg.fiducial.qrcode.EciEncoding.guessEncoding;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.JIS_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.KANJI_UNAVAILABLE;

/**
 * Various functions to encode and decode QR and Micro QR data.
 *
 * @author Peter Abeles
 */
public class QrCodeCodecBitsUtils {
	/** All the possible values in alphanumeric mode. */
	public static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

	public QrCode.Failure failureCause = QrCode.Failure.NONE;
	public final StringBuilder workString = new StringBuilder();

	// If null the encoding of byte messages will attempt to be automatically determined, with a default
	// of UTF-8. Otherwise, this is the encoding used.
	@Nullable String forceEncoding;

	// Specified ECI encoding
	@Nullable String encodingEci;

//	Set<Character.UnicodeBlock> japaneseUnicodeBlocks = new HashSet<>() {{
//		add(Character.UnicodeBlock.HIRAGANA);
//		add(Character.UnicodeBlock.KATAKANA);
//		add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
//	}};

	final static CharsetEncoder asciiEncoder = Charset.forName("ISO-8859-1").newEncoder();

	public QrCodeCodecBitsUtils( @Nullable String forceEncoding ) {
		this.forceEncoding = forceEncoding;
	}

	/**
	 * Decodes a numeric message
	 *
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	public int decodeNumeric( PackedBits8 data, int bitLocation, int lengthBits ) {
		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		while (length >= 3) {
			if (data.size < bitLocation + 10) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 10, true);
			bitLocation += 10;

			int valA = chunk/100;
			int valB = (chunk - valA*100)/10;
			int valC = chunk - valA*100 - valB*10;

			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
			workString.append((char)(valC + '0'));

			length -= 3;
		}

		if (length == 2) {
			if (data.size < bitLocation + 7) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 7, true);
			bitLocation += 7;

			int valA = chunk/10;
			int valB = chunk - valA*10;
			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
		} else if (length == 1) {
			if (data.size < bitLocation + 4) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation, 4, true);
			bitLocation += 4;
			workString.append((char)(valA + '0'));
		}
		return bitLocation;
	}

	/**
	 * Decodes alphanumeric messages
	 *
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	public int decodeAlphanumeric( PackedBits8 data, int bitLocation, int lengthBits ) {

		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		while (length >= 2) {
			if (data.size < bitLocation + 11) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation, 11, true);
			bitLocation += 11;

			int valA = chunk/45;
			int valB = chunk - valA*45;

			workString.append(valueToAlphanumeric(valA));
			workString.append(valueToAlphanumeric(valB));
			length -= 2;
		}

		if (length == 1) {
			if (data.size < bitLocation + 6) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation, 6, true);
			bitLocation += 6;
			workString.append(valueToAlphanumeric(valA));
		}
		return bitLocation;
	}

	/**
	 * Decodes byte messages
	 *
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	public int decodeByte( PackedBits8 data, int bitLocation, int lengthBits ) {
		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		if (length*8 > data.size - bitLocation) {
			failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
			return -1;
		}

		byte[] rawdata = new byte[length];

		for (int i = 0; i < length; i++) {
			rawdata[i] = (byte)data.read(bitLocation, 8, true);
			bitLocation += 8;
		}

		// If ECI encoding is not specified use the default encoding. Unfortunately the specification is ignored
		// by most people here and UTF-8 is used. If an encoding is specified then that is used.
		String encoding = encodingEci == null ? (forceEncoding != null ? forceEncoding : guessEncoding(rawdata))
				: encodingEci;
		try {
			workString.append(new String(rawdata, encoding));
		} catch (UnsupportedEncodingException ignored) {
			failureCause = JIS_UNAVAILABLE;
			return -1;
		}
		return bitLocation;
	}

	/**
	 * Decodes Kanji messages
	 *
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	public int decodeKanji( PackedBits8 data, int bitLocation, int lengthBits ) {
		int length = data.read(bitLocation, lengthBits, true);
		bitLocation += lengthBits;

		byte[] rawdata = new byte[length*2];

		for (int i = 0; i < length; i++) {
			if (data.size < bitLocation + 13) {
				failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int letter = data.read(bitLocation, 13, true);
			bitLocation += 13;

			letter = ((letter/0x0C0) << 8) | (letter%0x0C0);

			if (letter < 0x01F00) {
				// In the 0x8140 to 0x9FFC range
				letter += 0x08140;
			} else {
				// In the 0xE040 to 0xEBBF range
				letter += 0x0C140;
			}
			rawdata[i*2] = (byte)(letter >> 8);
			rawdata[i*2 + 1] = (byte)letter;
		}

		// Shift_JIS may not be supported in some environments:
		try {
			workString.append(new String(rawdata, "Shift_JIS"));
		} catch (UnsupportedEncodingException ignored) {
			failureCause = KANJI_UNAVAILABLE;
			return -1;
		}

		return bitLocation;
	}

	public static boolean isKanji( char c ) {
		return !asciiEncoder.canEncode(c);
//		return japaneseUnicodeBlocks.contains(Character.UnicodeBlock.of(c));
	}

	public static boolean containsKanji( String message ) {
		for (int i = 0; i < message.length(); i++) {
			if (isKanji(message.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static boolean containsByte( String message ) {
		for (int i = 0; i < message.length(); i++) {
			if (ALPHANUMERIC.indexOf(message.charAt(i)) == -1)
				return true;
		}
		return false;
	}

	public static boolean containsAlphaNumeric( String message ) {
		for (int i = 0; i < message.length(); i++) {
			int c = (int)message.charAt(i) - '0';
			if (c < 0 || c > 9)
				return true;
		}
		return false;
	}

	public static byte[] alphanumericToValues( String data ) {
		byte[] output = new byte[data.length()];

		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			int value = ALPHANUMERIC.indexOf(c);
			if (value < 0)
				throw new IllegalArgumentException("Unsupported character '" + c + "' = " + (int)c);
			output[i] = (byte)value;
		}
		return output;
	}

	public static void flipBits8( byte[] array, int size ) {
		for (int j = 0; j < size; j++) {
			array[j] = flipBits8(array[j] & 0xFF);
		}
	}

	public static void flipBits8( DogArray_I8 array ) {
		flipBits8(array.data, array.size);
	}

	public static byte flipBits8( int x ) {
		int b = 0;
		for (int i = 0; i < 8; i++) {
			b <<= 1;
			b |= (x & 1);
			x >>= 1;
		}
		return (byte)b;
	}

	public static char valueToAlphanumeric( int value ) {
		if (value < 0 || value >= ALPHANUMERIC.length())
			throw new RuntimeException("Value out of range");
		return ALPHANUMERIC.charAt(value);
	}

	public static void encodeNumeric( byte[] numbers, int length, int lengthBits, PackedBits8 packed ) {
		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		int index = 0;
		while (length - index >= 3) {
			int value = numbers[index]*100 + numbers[index + 1]*10 + numbers[index + 2];
			packed.append(value, 10, false);
			index += 3;
		}
		if (length - index == 2) {
			int value = numbers[index]*10 + numbers[index + 1];
			packed.append(value, 7, false);
		} else if (length - index == 1) {
			int value = numbers[index];
			packed.append(value, 4, false);
		}
	}

	public static void encodeAlphanumeric( byte[] numbers, int length, int lengthBits, PackedBits8 packed ) {
		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		int index = 0;
		while (length - index >= 2) {
			int value = numbers[index]*45 + numbers[index + 1];
			packed.append(value, 11, false);
			index += 2;
		}
		if (length - index == 1) {
			int value = numbers[index];
			packed.append(value, 6, false);
		}
	}

	public static void encodeBytes( byte[] data, int length, int lengthBits, PackedBits8 packed ) {
		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		for (int i = 0; i < length; i++) {
			packed.append(data[i] & 0xff, 8, false);
		}
	}

	public static void encodeKanji( byte[] bytes, int length, int lengthBits, PackedBits8 packed ) {
		// Specify the number of characters
		packed.append(length, lengthBits, false);

		for (int i = 0; i < bytes.length; i += 2) {
			int byte1 = bytes[i] & 0xFF;
			int byte2 = bytes[i + 1] & 0xFF;
			int code = (byte1 << 8) | byte2;
			int adjusted;
			if (code >= 0x8140 && code <= 0x9ffc) {
				adjusted = code - 0x8140;
			} else if (code >= 0xe040 && code <= 0xebbf) {
				adjusted = code - 0xc140;
			} else {
				throw new IllegalArgumentException("Invalid byte sequence. At " + (i/2));
			}
			int encoded = ((adjusted >> 8)*0xc0) + (adjusted & 0xff);
			packed.append(encoded, 13, false);
		}
	}
}
