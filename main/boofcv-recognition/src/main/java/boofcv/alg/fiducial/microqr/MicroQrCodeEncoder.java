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

import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.fiducial.qrcode.ReidSolomonCodes;
import org.ddogleg.struct.DogArray_I8;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.qrcode.QrCodeEncoder.flipBits8;

/**
 * Provides an easy to use interface for specifying QR-Code parameters and generating the raw data sequence. After
 * the QR Code has been created using this class it can then be rendered.
 *
 * By default it will select the qr code version based on the number of
 * bits and the error correction level based on the version and number of bits. If the error correction isn't specified
 * and the version isn't specified then error correction level M is used by default.
 *
 * @author Peter Abeles
 */
public class MicroQrCodeEncoder {
	/** All the possible values in alphanumeric mode. */
	public static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

	// used to compute error correction
	private final ReidSolomonCodes rscodes = new ReidSolomonCodes(8, 0b100011101);

	// output qr code
	private final MicroQrCode qr = new MicroQrCode();

	// If true it will automatically select the amount of error correction depending on the length of the data
	private boolean autoErrorCorrection;

	private boolean autoMask;

	// Encoding for the byte character set. UTF-8 isn't standard compliant but is the most widely used
	private Charset byteCharacterSet = StandardCharsets.UTF_8;

	// workspace variables
	PackedBits8 packed = new PackedBits8();
	// storage for the data message
	private final DogArray_I8 message = new DogArray_I8();
	// storage fot the message's ecc
	private final DogArray_I8 ecc = new DogArray_I8();

	// Since QR Code version might not be known initially and the size of the length byte depends on the
	// version, store the segments here until fixate is called.
	private final List<MessageSegment> segments = new ArrayList<>();

//	Set<Character.UnicodeBlock> japaneseUnicodeBlocks = new HashSet<>() {{
//		add(Character.UnicodeBlock.HIRAGANA);
//		add(Character.UnicodeBlock.KATAKANA);
//		add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
//	}};

	CharsetEncoder asciiEncoder = Charset.forName("ISO-8859-1").newEncoder();

	public MicroQrCodeEncoder() {
		reset();
	}

	public void reset() {
		qr.reset();
		qr.version = -1;
		packed.size = 0;
		autoMask = true;
		autoErrorCorrection = true;
		segments.clear();
	}

	public MicroQrCodeEncoder setVersion( int version ) {
		qr.version = version;
		return this;
	}

	public MicroQrCodeEncoder setError( @Nullable MicroQrCode.ErrorLevel level ) {
		autoErrorCorrection = level == null;
		if (level != null)
			qr.error = level;
		return this;
	}

	public MicroQrCodeEncoder setMask( MicroQrCodeMaskPattern pattern ) {
		autoMask = false;
		qr.mask = pattern;
		return this;
	}

	/**
	 * Select the encoding based on the letters in the message. A very simple algorithm is used internally.
	 */
	public MicroQrCodeEncoder addAutomatic( String message ) {
		// very simple coding algorithm. Doesn't try to compress by using multiple formats
		if (containsKanji(message)) {
			// split into kanji and non-kanji segments
			int start = 0;
			boolean kanji = isKanji(message.charAt(0));
			for (int i = 0; i < message.length(); i++) {
				if (isKanji(message.charAt(i))) {
					if (!kanji) {
						addAutomatic(message.substring(start, i));
						start = i;
						kanji = true;
					}
				} else {
					if (kanji) {
						addKanji(message.substring(start, i));
						start = i;
						kanji = false;
					}
				}
			}
			if (kanji) {
				addKanji(message.substring(start));
			} else {
				addAutomatic(message.substring(start));
			}
			return this;
		} else if (containsByte(message)) {
			return addBytes(message);
		} else if (containsAlphaNumeric(message)) {
			return addAlphanumeric(message);
		} else {
			return addNumeric(message);
		}
	}

	private boolean isKanji( char c ) {
		return !asciiEncoder.canEncode(c);
//		return japaneseUnicodeBlocks.contains(Character.UnicodeBlock.of(c));
	}

	private boolean containsKanji( String message ) {
		for (int i = 0; i < message.length(); i++) {
			if (isKanji(message.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean containsByte( String message ) {
		for (int i = 0; i < message.length(); i++) {
			if (ALPHANUMERIC.indexOf(message.charAt(i)) == -1)
				return true;
		}
		return false;
	}

	private boolean containsAlphaNumeric( String message ) {
		for (int i = 0; i < message.length(); i++) {
			int c = (int)message.charAt(i) - '0';
			if (c < 0 || c > 9)
				return true;
		}
		return false;
	}

	/** Encodes into packed the mode. Number of bits vary depending on the version */
	private void encodeMode( MicroQrCode.Mode mode ) {
		int bits = MicroQrCode.modeIndicatorBits(qr.version);
		if (bits == 0)
			return;
		packed.append(mode.ordinal(), bits, false);
	}

	/**
	 * Creates a QR-Code which encodes a number sequence
	 *
	 * @param message String that specifies numbers and no other types. Each number has to be from 0 to 9 inclusive.
	 * @return The QR-Code
	 */
	public MicroQrCodeEncoder addNumeric( String message ) {
		byte[] numbers = new byte[message.length()];

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			int values = c - '0';
			if (values < 0 || values > 9)
				throw new RuntimeException("Expected each character to be a number from 0 to 9");
			numbers[i] = (byte)values;
		}
		return addNumeric(numbers);
	}

	/**
	 * Creates a QR-Code which encodes a number sequence
	 *
	 * @param numbers Array of numbers. Each number has to be from 0 to 9 inclusive.
	 * @return The QR-Code
	 */
	public MicroQrCodeEncoder addNumeric( byte[] numbers ) {
		for (int i = 0; i < numbers.length; i++) {
			if (numbers[i] < 0 || numbers[i] > 9)
				throw new IllegalArgumentException("All numbers must have a value from 0 to 9");
		}

		StringBuilder builder = new StringBuilder(numbers.length);
		for (int i = 0; i < numbers.length; i++) {
			builder.append(Integer.toString(numbers[i]));
		}
		MessageSegment segment = new MessageSegment();
		segment.message = builder.toString();
		segment.data = numbers;
		segment.length = numbers.length;
		segment.mode = MicroQrCode.Mode.NUMERIC;

		segment.encodedSizeBits += 10*(segment.length/3);
		if (segment.length%3 == 2) {
			segment.encodedSizeBits += 7;
		} else if (segment.length%3 == 1) {
			segment.encodedSizeBits += 4;
		}

		segments.add(segment);

		return this;
	}

	private void encodeNumeric( byte[] numbers, int length ) {
		qr.mode = MicroQrCode.Mode.NUMERIC;
		int lengthBits = getLengthBitsNumeric(qr.version);

		// specify the mode
		encodeMode(qr.mode);

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

	/**
	 * Creates a QR-Code which encodes data in the alphanumeric format
	 *
	 * @param alphaNumeric String containing only alphanumeric values.
	 * @return The QR-Code
	 */
	public MicroQrCodeEncoder addAlphanumeric( String alphaNumeric ) {
		if (qr.version < 2)
			throw new RuntimeException("Alphanumeric requires version >= 2");

		byte[] values = alphanumericToValues(alphaNumeric);

		MessageSegment segment = new MessageSegment();
		segment.message = alphaNumeric;
		segment.data = values;
		segment.length = values.length;
		segment.mode = MicroQrCode.Mode.ALPHANUMERIC;

		segment.encodedSizeBits += 11*(segment.length/2);
		if (segment.length%2 == 1) {
			segment.encodedSizeBits += 6;
		}

		segments.add(segment);

		return this;
	}

	private void encodeAlphanumeric( byte[] numbers, int length ) {
		qr.mode = MicroQrCode.Mode.ALPHANUMERIC;

		int lengthBits = getLengthBitsAlphanumeric(qr.version);

		// specify the mode
		encodeMode(qr.mode);

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

	public static char valueToAlphanumeric( int value ) {
		if (value < 0 || value >= ALPHANUMERIC.length())
			throw new RuntimeException("Value out of range");
		return ALPHANUMERIC.charAt(value);
	}

	public MicroQrCodeEncoder addBytes( String message ) {
		return addBytes(message.getBytes(byteCharacterSet));
	}

	/**
	 * Creates a QR-Code which encodes data in the byte format.
	 *
	 * @param data Data to be encoded
	 * @return The QR-Code
	 */
	public MicroQrCodeEncoder addBytes( byte[] data ) {
		if (qr.version < 3)
			throw new RuntimeException("Bytes requires version >= 3");

		StringBuilder builder = new StringBuilder(data.length);
		for (int i = 0; i < data.length; i++) {
			builder.append((char)data[i]);
		}

		MessageSegment segment = new MessageSegment();
		segment.message = builder.toString();
		segment.data = data;
		segment.length = data.length;
		segment.mode = MicroQrCode.Mode.BYTE;

		segment.encodedSizeBits += 8*segment.length;

		segments.add(segment);

		return this;
	}

	private void encodeBytes( byte[] data, int length ) {
		qr.mode = MicroQrCode.Mode.BYTE;

		int lengthBits = getLengthBitsBytes(qr.version);

		// specify the mode
		encodeMode(qr.mode);

		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		for (int i = 0; i < length; i++) {
			packed.append(data[i] & 0xff, 8, false);
		}
	}

	/**
	 * Creates a QR-Code which encodes Kanji characters
	 *
	 * @param message Data to be encoded
	 * @return The QR-Code
	 */
	public MicroQrCodeEncoder addKanji( String message ) {
		if (qr.version < 3)
			throw new RuntimeException("Kanji requires version >= 3");

		byte[] bytes;
		try {
			bytes = message.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalArgumentException(ex);
		}

		MessageSegment segment = new MessageSegment();
		segment.message = message;
		segment.data = bytes;
		segment.length = message.length();
		segment.mode = MicroQrCode.Mode.KANJI;

		segment.encodedSizeBits += MicroQrCode.modeIndicatorBits(qr.version);
		segment.encodedSizeBits += 13*segment.length;

		segments.add(segment);

		return this;
	}

	private void encodeKanji( byte[] bytes, int length ) {
		qr.mode = MicroQrCode.Mode.KANJI;

		int lengthBits = getLengthBitsKanji(qr.version);

		// specify the mode
		encodeMode(qr.mode);

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

	public static int getLengthBitsNumeric( int version ) {
		return 2 + version;
	}

	public static int getLengthBitsAlphanumeric( int version ) {
		return 1 + version;
	}

	public static int getLengthBitsBytes( int version ) {
		return 1 + version;
	}

	public static int getLengthBitsKanji( int version ) {
		return version;
	}

	/**
	 * Call this function after you are done adding to the QR code
	 *
	 * @return The generated QR Code
	 */
	public MicroQrCode fixate() {
		autoSelectVersionAndError();

		// sanity check of code
		int expectedBitSize = bitsAtVersion(qr.version);

		qr.message = "";
		for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
			MessageSegment m = segments.get(segIdx);
			qr.message += m.message;
			switch (m.mode) {
				case NUMERIC -> encodeNumeric(m.data, m.length);
				case ALPHANUMERIC -> encodeAlphanumeric(m.data, m.length);
				case BYTE -> encodeBytes(m.data, m.length);
				case KANJI -> encodeKanji(m.data, m.length);
				default -> throw new RuntimeException("Unknown");
			}
		}

		if (packed.size != expectedBitSize)
			throw new RuntimeException("Bad size code. " + packed.size + " vs " + expectedBitSize);

		int maxBits = MicroQrCode.maxDataBits(qr.version, qr.error);

		if (packed.size > maxBits) {
			throw new IllegalArgumentException("The message is longer than the max possible size");
		}

		// add the terminator bits to the bit stream
		int terminatorBits = qr.terminatorBits();
		if (packed.size + terminatorBits <= maxBits) {
			packed.append(0b0000, terminatorBits, false);
		}

		bitsToMessage(packed);

		// TODO implement properly later
		if (autoMask) {
			qr.mask = MicroQrCodeMaskPattern.M00;
		}

		return qr;
	}

	/**
	 * Checks to see if a request has been made to select version and/or error correction. If so it will pick something
	 */
	private void autoSelectVersionAndError() {
		if (qr.version == -1) {
			escape:
			// select the smallest version which can store all the data
			for (int version = 1; version <= 4; version++) {
				MicroQrCode.ErrorLevel[] errorsToTry;
				if (autoErrorCorrection) {
					errorsToTry = MicroQrCode.allowedErrorCorrection(version);
				} else {
					errorsToTry = new MicroQrCode.ErrorLevel[]{qr.error};
				}

				for (MicroQrCode.ErrorLevel error : errorsToTry) {
					qr.error = error;

					int maxDataBits = MicroQrCode.maxDataBits(version, error);
					int encodedDataBits = bitsAtVersion(version);

					// See if there's enough storage for this message
					if (encodedDataBits > maxDataBits)
						continue;
					qr.version = version;
					break escape;
				}
			}
			if (qr.version == -1) {
				throw new IllegalArgumentException("Packet too to be encoded in a qr code");
			}
		} else {
			// the version is set but the error correction level isn't. Pick the one with
			// the most error correction that can can store all the data
			if (qr.version == 1) {
				qr.error = MicroQrCode.ErrorLevel.DETECT;
			} else if (autoErrorCorrection) {
				@Nullable MicroQrCode.ErrorLevel selected = null;
				MicroQrCode.VersionInfo v = MicroQrCode.VERSION_INFO[qr.version];
				int encodedDataBits = bitsAtVersion(qr.version);

				for (MicroQrCode.ErrorLevel error : MicroQrCode.allowedErrorCorrection(qr.version)) {
					int maxDataBits = MicroQrCode.maxDataBits(qr.version, error);
					if (encodedDataBits <= maxDataBits) {
						selected = error;
					}
				}

				if (selected == null) {
					throw new IllegalArgumentException("You need to use a high version number to store the data. Tried " +
							"all error correction levels at version " + qr.version + ". Total Data " + (packed.size/8));
				}
				qr.error = selected;
			}
		}
		// Sanity check
		int dataBits = bitsAtVersion(qr.version);
		int maxDataBits = MicroQrCode.maxDataBits(qr.version, qr.error);
		if (dataBits > maxDataBits) {
			int encodedBits = totalEncodedBitsNoOverHead();
			throw new IllegalArgumentException("Version and error level can't encode all the data. " +
					"Version = " + qr.version + " , Error = " + qr.error +
					" , Encoded Bits = " + encodedBits + " , Overhead Bits = " + (dataBits - encodedBits) +
					" , Data bits = " + dataBits + " , Limit bits = " + maxDataBits);
		}
	}

	/**
	 * Computes how many bits it takes to encode the message at this version number
	 */
	private int bitsAtVersion( int version ) {
		int total = 0;
		for (int i = 0; i < segments.size(); i++) {
			total += segments.get(i).sizeInBits(version);
		}
		return total;
	}

	/**
	 * Returns the number of encoded bits without overhead
	 */
	private int totalEncodedBitsNoOverHead() {
		int total = 0;
		for (int i = 0; i < segments.size(); i++) {
			total += segments.get(i).encodedSizeBits;
		}
		return total;
	}

	protected void bitsToMessage( PackedBits8 stream ) {
		// add padding to make it align to 8
		stream.append(0, (8 - (stream.size%8))%8, false);

//		System.out.println("encoded message");
//		stream.print();System.out.println();

		MicroQrCode.VersionInfo version = MicroQrCode.VERSION_INFO[qr.version];
		MicroQrCode.DataInfo dataInfo = version.levels.get(qr.error);
		if (dataInfo == null)
			throw new IllegalArgumentException("Invalid error correction level selected for level");

		qr.rawbits = new byte[version.codewords];
		int wordsEcc = version.codewords - dataInfo.dataCodewords;

		message.resize(dataInfo.dataCodewords);

		rscodes.generator(wordsEcc);
		ecc.resize(wordsEcc);

		System.arraycopy(stream.data, 0, message.data, 0, stream.arrayLength());
		addPadding(message, stream.arrayLength(), 0b00110111, 0b10001000);
		flipBits8(message);

		// compute the ecc
		rscodes.computeECC(message, ecc);
		flipBits8(message);
		flipBits8(ecc);

		// write it into the output array
		System.arraycopy(message.data, 0, qr.rawbits, 0, message.size);
		System.arraycopy(ecc.data, 0, qr.rawbits, message.size, ecc.size);
	}

	private void addPadding( DogArray_I8 queue, int dataBytes, int padding0, int padding1 ) {
		boolean a = true;
		for (int i = dataBytes; i < queue.size; i++, a = !a) {
			if (a)
				queue.data[i] = (byte)padding0;
			else
				queue.data[i] = (byte)padding1;
		}
	}

	public Charset getByteCharacterSet() {
		return byteCharacterSet;
	}

	public void setByteCharacterSet( Charset byteCharacterSet ) {
		this.byteCharacterSet = byteCharacterSet;
	}

	@SuppressWarnings({"NullAway.Init"})
	private static class MessageSegment {
		MicroQrCode.Mode mode;
		String message;
		byte[] data;
		int length;
		// number of bits in the encoded message, excluding the length and mode bits
		int encodedSizeBits;

		public int sizeInBits( int version ) {
			int lengthBits = switch (mode) {
				case NUMERIC -> getLengthBitsNumeric(version);
				case ALPHANUMERIC -> getLengthBitsAlphanumeric(version);
				case BYTE -> getLengthBitsBytes(version);
				case KANJI -> getLengthBitsKanji(version);
				default -> throw new RuntimeException("Egads");
			};
			return encodedSizeBits + lengthBits + MicroQrCode.modeIndicatorBits(version);
		}
	}
}
