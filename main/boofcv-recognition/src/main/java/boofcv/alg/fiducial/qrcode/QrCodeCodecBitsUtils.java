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

import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_I8;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Set;

import static boofcv.alg.fiducial.qrcode.EciEncoding.guessEncoding;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.JIS_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.KANJI_UNAVAILABLE;

/**
 * Various functions to encode and decode QR and Micro QR data.
 *
 * @author Peter Abeles
 */
public class QrCodeCodecBitsUtils implements VerbosePrint {
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

	@Nullable PrintStream verbose = null;

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
				if (verbose != null) verbose.printf("overflow: numeric data.size=%d\n", data.size);
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
				if (verbose != null) verbose.printf("overflow: alphanumeric data.size=%d\n", data.size);
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
			if (verbose != null) verbose.printf("overflow: byte data.size=%d\n", data.size);
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
				if (verbose != null) verbose.printf("overflow: kanji data.size=%d\n", data.size);
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
			throw new RuntimeException("Value out of range. value=" + value);
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

	public static MessageSegment createSegmentNumeric( String message ) {
		byte[] numbers = new byte[message.length()];

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			int values = c - '0';
			if (values < 0 || values > 9)
				throw new RuntimeException("Expected each character to be a number from 0 to 9, not '" + c + "'");
			numbers[i] = (byte)values;
		}
		return createSegmentNumeric(numbers);
	}

	public static MessageSegment createSegmentNumeric( byte[] numbers ) {
		for (int i = 0; i < numbers.length; i++) {
			if (numbers[i] < 0 || numbers[i] > 9)
				throw new IllegalArgumentException("All numbers must have a value from 0 to 9");
		}

		var builder = new StringBuilder(numbers.length);
		for (int i = 0; i < numbers.length; i++) {
			builder.append(Integer.toString(numbers[i]));
		}

		var segment = new MessageSegment();
		segment.message = builder.toString();
		segment.data = numbers;
		segment.length = numbers.length;
		segment.mode = QrCode.Mode.NUMERIC;

		segment.encodedSizeBits += 10*(segment.length/3);
		if (segment.length%3 == 2) {
			segment.encodedSizeBits += 7;
		} else if (segment.length%3 == 1) {
			segment.encodedSizeBits += 4;
		}

		return segment;
	}

	public static MessageSegment createSegmentAlphanumeric( String alphaNumeric ) {
		byte[] values = alphanumericToValues(alphaNumeric);

		var segment = new MessageSegment();
		segment.message = alphaNumeric;
		segment.data = values;
		segment.length = values.length;
		segment.mode = QrCode.Mode.ALPHANUMERIC;

		segment.encodedSizeBits += 11*(segment.length/2);
		if (segment.length%2 == 1) {
			segment.encodedSizeBits += 6;
		}

		return segment;
	}

	public static MessageSegment createSegmentBytes( byte[] data ) {
		var builder = new StringBuilder(data.length);
		for (int i = 0; i < data.length; i++) {
			builder.append((char)data[i]);
		}

		MessageSegment segment = new MessageSegment();
		segment.message = builder.toString();
		segment.data = data;
		segment.length = data.length;
		segment.mode = QrCode.Mode.BYTE;

		segment.encodedSizeBits += 8*segment.length;

		return segment;
	}

	public static MessageSegment createSegmentKanji( String message ) {
		byte[] bytes;
		try {
			bytes = message.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalArgumentException(ex);
		}

		var segment = new MessageSegment();
		segment.message = message;
		segment.data = bytes;
		segment.length = message.length();
		segment.mode = QrCode.Mode.KANJI;

		segment.encodedSizeBits += 13*segment.length;
		return segment;
	}

	/**
	 * Select the encoding based on the letters in the message. A very simple algorithm is used internally.
	 */
	public static void addAutomatic( Charset byteCharacterSet,
									 String message, List<MessageSegment> segments ) {
		// very simple coding algorithm. Doesn't try to compress by using multiple formats
		if (containsKanji(message)) {
			// split into kanji and non-kanji segments
			int start = 0;
			boolean kanji = isKanji(message.charAt(0));
			for (int i = 0; i < message.length(); i++) {
				if (isKanji(message.charAt(i))) {
					if (!kanji) {
						addAutomatic(byteCharacterSet, message.substring(start, i), segments);
						start = i;
						kanji = true;
					}
				} else {
					if (kanji) {
						segments.add(createSegmentKanji(message.substring(start, i)));
						start = i;
						kanji = false;
					}
				}
			}
			if (kanji) {
				segments.add(createSegmentKanji(message.substring(start)));
			} else {
				addAutomatic(byteCharacterSet, message.substring(start), segments);
			}
		} else if (containsByte(message)) {
			segments.add(createSegmentBytes(message.getBytes(byteCharacterSet)));
		} else if (containsAlphaNumeric(message)) {
			segments.add(createSegmentAlphanumeric(message));
		} else {
			segments.add(createSegmentNumeric(message));
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class MessageSegment {
		public QrCode.Mode mode;
		public String message;
		public byte[] data;
		public int length;
		// number of bits in the encoded message, excluding the length bits
		public int encodedSizeBits;
	}
}
