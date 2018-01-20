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

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I8;
import org.ejml.ops.CommonOps_BDRM;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	// TODO support ECI
public class QrCodeEncoder {

	/**
	 * All the possible values in alphanumeric mode.
	 */
	public static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

	// used to compute error correction
	private ReidSolomonCodes rscodes = new ReidSolomonCodes(8, 0b100011101);

	// output qr code
	private QrCode qr = new QrCode();

	// If true it will automatically select the amount of error correction depending on the length of the data
	private boolean autoErrorCorrection;

	private boolean autoMask;

	// workspace variables
	PackedBits8 packed = new PackedBits8();
	// storage for the data message
	private GrowQueue_I8 message = new GrowQueue_I8();
	// storage fot the message's ecc
	private GrowQueue_I8 ecc = new GrowQueue_I8();

	// Since QR Code version might not be known initially and the size of the length byte depends on the
	// version, store the segments here until fixate is called.
	private List<MessageSegment> segments = new ArrayList<>();

	Set<Character.UnicodeBlock> japaneseUnicodeBlocks = new HashSet<Character.UnicodeBlock>() {{
		add(Character.UnicodeBlock.HIRAGANA);
		add(Character.UnicodeBlock.KATAKANA);
		add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
	}};

	CharsetEncoder asciiEncoder = Charset.forName("ISO-8859-1").newEncoder();


	public QrCodeEncoder() {
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

	public QrCodeEncoder setVersion(int version) {
		qr.version = version;
		return this;
	}

	public QrCodeEncoder setError(QrCode.ErrorLevel level) {
		autoErrorCorrection = false;
		qr.error = level;
		return this;
	}

	public QrCodeEncoder setMask(QrCodeMaskPattern pattern) {
		autoMask = false;
		qr.mask = pattern;
		return this;
	}

	/**
	 * Select the encoding based on the letters in the message. A very simple algorithm is used internally.
	 *
	 */
	public QrCodeEncoder addAutomatic(String message) {
		// very simple coding algorithm. Doesn't try to compress by using multiple formats
		if(containsKanji(message)) {
			// split into kanji and non-kanji segments
			int start = 0;
			boolean kanji = isKanji(message.charAt(0));
			for (int i = 0; i < message.length(); i++) {
				if( isKanji(message.charAt(i))) {
					if( !kanji ) {
						addAutomatic(message.substring(start,i));
						start = i;
						kanji = true;
					}
				} else {
					if( kanji ) {
						addKanji(message.substring(start,i));
						start = i;
						kanji = false;
					}
				}
			}
			if( kanji ) {
				addKanji(message.substring(start,message.length()));
			} else {
				addAutomatic(message.substring(start,message.length()));
			}
			return this;
		} else if( containsByte(message)) {
			return addBytes(message);
		} else if( containsAlphaNumeric(message)) {
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
			if( ALPHANUMERIC.indexOf(message.charAt(i)) == -1)
				return true;
		}
		return false;
	}

	private boolean containsAlphaNumeric( String message ) {
		for (int i = 0; i < message.length(); i++) {
			int c = (int)message.charAt(i) - '0';
			if( c < 0 || c > 9  )
				return true;
		}
		return false;
	}

	/**
	 * Creates a QR-Code which encodes a number sequence
	 *
	 * @param message String that specifies numbers and no other types. Each number has to be from 0 to 9 inclusive.
	 * @return The QR-Code
	 */
	public QrCodeEncoder addNumeric(String message) {
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
	public QrCodeEncoder addNumeric(byte[] numbers) {
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
		segment.mode = QrCode.Mode.NUMERIC;

		segment.encodedSizeBits += 4;
		segment.encodedSizeBits += 10*(segment.length/3);
		if( segment.length%3 == 2 ) {
			segment.encodedSizeBits += 7;
		} else if( segment.length%3 == 1 ) {
			segment.encodedSizeBits += 4;
		}

		segments.add(segment);

		return this;
	}

	private void encodeNumeric(byte[] numbers , int length ) {
		qr.mode = QrCode.Mode.NUMERIC;
		int lengthBits = getLengthBitsNumeric(qr.version);

		// specify the mode
		packed.append(0b0001, 4, false);

		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		int index = 0;
		while (length - index >= 3) {
			int value = numbers[index] * 100 + numbers[index + 1] * 10 + numbers[index + 2];
			packed.append(value, 10, false);
			index += 3;
		}
		if (length - index == 2) {
			int value = numbers[index] * 10 + numbers[index + 1];
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
	public QrCodeEncoder addAlphanumeric(String alphaNumeric) {
		byte values[] = alphanumericToValues(alphaNumeric);

		MessageSegment segment = new MessageSegment();
		segment.message = alphaNumeric;
		segment.data = values;
		segment.length = values.length;
		segment.mode = QrCode.Mode.ALPHANUMERIC;

		segment.encodedSizeBits += 4;
		segment.encodedSizeBits += 11*(segment.length/2);
		if( segment.length%2 == 1 ) {
			segment.encodedSizeBits += 6;
		}

		segments.add(segment);

		return this;
	}

	private void encodeAlphanumeric( byte[] numbers , int length ) {
		qr.mode = QrCode.Mode.ALPHANUMERIC;

		int lengthBits = getLengthBitsAlphanumeric(qr.version);

		// specify the mode
		packed.append(0b0010, 4, false);

		// Specify the number of digits
		packed.append(length, lengthBits, false);

		// Append the digits
		int index = 0;
		while (length - index >= 2) {
			int value = numbers[index] * 45 + numbers[index + 1];
			packed.append(value, 11, false);
			index += 2;
		}
		if (length - index == 1) {
			int value = numbers[index];
			packed.append(value, 6, false);
		}
	}

	public static byte[] alphanumericToValues(String data) {
		byte[] output = new byte[data.length()];

		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			int value = ALPHANUMERIC.indexOf(c);
			if (value < 0)
				throw new IllegalArgumentException("Unsupported character '" + c + "' = " + (int) c);
			output[i] = (byte) value;
		}
		return output;
	}

	public static char valueToAlphanumeric(int value) {
		if (value < 0 || value >= ALPHANUMERIC.length())
			throw new RuntimeException("Value out of range");
		return ALPHANUMERIC.charAt(value);
	}

	public QrCodeEncoder addBytes(String message) {
		try {
			return addBytes(message.getBytes("JIS"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a QR-Code which encodes data in the byte format.
	 *
	 * @param data Data to be encoded
	 * @return The QR-Code
	 */
	public QrCodeEncoder addBytes(byte[] data) {
		StringBuilder builder = new StringBuilder(data.length);
		for (int i = 0; i < data.length; i++) {
			builder.append((char)data[i]);
		}

		MessageSegment segment = new MessageSegment();
		segment.message = builder.toString();
		segment.data = data;
		segment.length = data.length;
		segment.mode = QrCode.Mode.BYTE;

		segment.encodedSizeBits += 4;
		segment.encodedSizeBits += 8*segment.length;

		segments.add(segment);

		return this;
	}

	private void encodeBytes(byte[] data, int length) {
		qr.mode = QrCode.Mode.BYTE;

		int lengthBits = getLengthBitsBytes(qr.version);

		// specify the mode
		packed.append(0b0100, 4, false);

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
	public QrCodeEncoder addKanji(String message) {
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
		segment.mode = QrCode.Mode.KANJI;

		segment.encodedSizeBits += 4;
		segment.encodedSizeBits += 13*segment.length;

		segments.add(segment);

		return this;
	}

	private void encodeKanji( byte []bytes , int length ) {
		qr.mode = QrCode.Mode.KANJI;

		int lengthBits = getLengthBitsKanji(qr.version);

		// specify the mode
		packed.append(0b1000, 4, false);

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
				throw new IllegalArgumentException("Invalid byte sequence. At " +(i / 2));
			}
			int encoded = ((adjusted >> 8) * 0xc0) + (adjusted & 0xff);
			packed.append(encoded, 13, false);
		}
	}

	public static int getLengthBitsNumeric(int version) {
		return getLengthBits(version, 10, 12, 14);
	}

	public static int getLengthBitsAlphanumeric(int version) {
		return getLengthBits(version, 9, 11, 13);
	}

	public static int getLengthBitsBytes(int version) {
		return getLengthBits(version, 8, 16, 16);
	}

	public static int getLengthBitsKanji(int version) {
		return getLengthBits(version, 8, 10, 12);
	}

	/**
	 * Returns the length of the message length variable in bits. Dependent on version
	 */
	private static int getLengthBits(int version, int bitsA, int bitsB, int bitsC) {
		int lengthBits;

		if (version < 10)
			lengthBits = bitsA;
		else if (version < 27)
			lengthBits = bitsB;
		else
			lengthBits = bitsC;
		return lengthBits;
	}

	// todo implement
	public QrCode eci() {
		return null;
	}

	/**
	 * Call this function after you are done adding to the QR code
	 *
	 * @return The generated QR Code
	 */
	public QrCode fixate() {
		autoSelectVersionAndError();

		// sanity check of code
		int expectedBitSize = bitsAtVersion(qr.version);

		qr.message = "";
		for( MessageSegment m : segments ) {
			qr.message += m.message;
			switch( m.mode ) {
				case NUMERIC:encodeNumeric(m.data,m.length);break;
				case ALPHANUMERIC:encodeAlphanumeric(m.data,m.length);break;
				case BYTE:encodeBytes(m.data,m.length);break;
				case KANJI:encodeKanji(m.data,m.length);break;
				default:
					throw new RuntimeException("Unknown");
			}
		}

		if( packed.size != expectedBitSize )
			throw new RuntimeException("Bad size code. "+packed.size+" vs "+expectedBitSize);

		int maxBits = QrCode.VERSION_INFO[qr.version].codewords * 8;

		if (packed.size > maxBits) {
			throw new IllegalArgumentException("The message is longer than the max possible size");
		}

		if (packed.size + 4 <= maxBits) {
			// add the terminator to the bit stream
			packed.append(0b0000, 4, false);
		}

		bitsToMessage(packed);

		if (autoMask) {
			qr.mask = selectMask(qr);
		}

		return qr;
	}

	/**
	 * Selects a mask by minimizing the appearance of certain patterns. This is inspired by
	 * what was described in the reference manual. I had a hard time understanding some
	 * of the specifics so I improvised.
	 */
	static QrCodeMaskPattern selectMask( QrCode qr ) {
		int N = qr.getNumberOfModules();
		int totalBytes = QrCode.VERSION_INFO[qr.version].codewords;
		List<Point2D_I32> locations = QrCode.LOCATION_BITS[qr.version];

		QrCodeMaskPattern bestMask = null;
		double bestScore = Double.MAX_VALUE;

		PackedBits8 bits = new PackedBits8();
		bits.size = totalBytes * 8;
		bits.data = qr.rawbits;

		if (bits.size > locations.size())
			throw new RuntimeException("BUG in code");

		// Bit value of 0 = white. 1 = black
		QrCodeCodeWordLocations matrix = new QrCodeCodeWordLocations(qr.version);
		for (QrCodeMaskPattern mask : QrCodeMaskPattern.values()) {
			double score = scoreMask(N, locations, bits, matrix, mask);
			if (score < bestScore) {
				bestScore = score;
				bestMask = mask;
			}
		}
		return bestMask;
	}

	/**
	 * Features inside the QR code that could confuse a detector
	 */
	static class FoundFeatures {
		int adjacent = 0;
		int sameColorBlock = 0;
		int position = 0;
	}

	private static double scoreMask(int N, List<Point2D_I32> locations,
									PackedBits8 bits,
									QrCodeCodeWordLocations matrix,
									QrCodeMaskPattern mask) {
		FoundFeatures features = new FoundFeatures();

		// write the bits plus mask into the matrix
		int blackInBlock = 0;
		for (int i = 0; i < bits.size; i++) {
			Point2D_I32 p = locations.get(i);
			boolean v = mask.apply(p.y, p.x, bits.get(i)) == 1;
			matrix.unsafe_set(p.y, p.x, v);

			if (v) {
				blackInBlock++;
			}
			if ((i + 1) % 8 == 0) {
				if (blackInBlock == 0 || blackInBlock == 8) {
					features.sameColorBlock++;
				}
				blackInBlock = 0;
			}
		}
		// look for adjacent blocks that are the same color as well as patterns that
		// could be confused for position patterns 1,1,3,1,1
		// in vertical and horizontal directions
		detectAdjacentAndPositionPatterns(N, matrix, features);

		// penalize it if it's heavily skewed towards one color
		// this is a more significant deviation
		double scale = matrix.sum() / (double) (N * N);
		scale = scale < 0.5 ? 0.5 - scale : scale - 0.5;

//		System.out.println("adjacent "+features.adjacent+"  block "+features.sameColorBlock+" "+features.position+"  s "+(N*N*scale));
		return features.adjacent + 3 * features.sameColorBlock + 40 * features.position +  N*N*scale;
	}

	/**
	 * Look for adjacent blocks that are the same color as well as patterns that
	 * could be confused for position patterns 1,1,3,1,1
	 * in vertical and horizontal directions
	 */
	static void detectAdjacentAndPositionPatterns(int N, QrCodeCodeWordLocations matrix, FoundFeatures features) {

		for (int foo = 0; foo < 2; foo++) {
			for (int row = 0; row < N; row++) {
				int index = row * N;
				for (int col = 1; col < N; col++, index++) {
					if (matrix.data[index] == matrix.data[index + 1])
						features.adjacent++;
				}
				index = row * N;
				for (int col = 6; col < N; col++, index++) {
					if (matrix.data[index] && !matrix.data[index + 1] &&
							matrix.data[index + 2] && matrix.data[index + 3] && matrix.data[index + 4] &&
							!matrix.data[index + 5] && matrix.data[index + 6])
						features.position++;
				}
			}
			CommonOps_BDRM.transposeSquare(matrix);
		}

		// subtract out false positives by the mask applied to position patterns, timing, mode
		features.adjacent -= 8*9 + 2*9*8 + (N-18)*2;
	}

	/**
	 * Checks to see if a request has been made to select version and/or error correction. If so it will pick something
	 */
	private void autoSelectVersionAndError() {
		if (qr.version == -1) {
			QrCode.ErrorLevel levelsToTry[];
			if (autoErrorCorrection) {
				// this is a reasonable compromise between robustness and data storage
				levelsToTry = new QrCode.ErrorLevel[]{QrCode.ErrorLevel.M,QrCode.ErrorLevel.L};
			} else {
				levelsToTry = new QrCode.ErrorLevel[]{qr.error};
			}
			escape:for( QrCode.ErrorLevel error : levelsToTry ) {
				qr.error = error;
				// select the smallest version which can store all the data
				for (int i = 1; i <= 40; i++) {
					int dataBits = bitsAtVersion(i);
					int totalBytes = dataBits / 8 + (dataBits % 8) % 8;
					if (totalBytes <= QrCode.VERSION_INFO[i].totalDataBytes(qr.error)) {
						qr.version = i;
						break escape;
					}
				}
			}
			if (qr.version == -1) {
				throw new IllegalArgumentException("Packet too to be encoded in a qr code");
			}
		} else {
			// the version is set but the error correction level isn't. Pick the one with
			// the most error correction that can which can store all the data
			if (autoErrorCorrection) {
				qr.error = null;
				QrCode.VersionInfo v = QrCode.VERSION_INFO[qr.version];
				int dataBits = bitsAtVersion(qr.version);
				int totalBytes = dataBits / 8 + (dataBits % 8) % 8;
				for (QrCode.ErrorLevel level : QrCode.ErrorLevel.values()) {
					if (totalBytes <= v.totalDataBytes(level)) {
						qr.error = level;
					}
				}
				if (qr.error == null) {
					throw new IllegalArgumentException("You need to use a high version number to store the data. Tried " +
							"all error correction levels at version " + qr.version + ". Total Data " + (packed.size / 8));
				}
			}
		}
	}

	/**
	 * Computes how many bits it takes to encode the message at this version number
	 * @param version
	 * @return
	 */
	private int bitsAtVersion( int version ) {
		int total = 0;
		for (int i = 0; i < segments.size(); i++) {
			total += segments.get(i).sizeInBits(version);
		}
		return total;
	}

	protected void bitsToMessage(PackedBits8 stream) {
		// add padding to make it align to 8
		stream.append(0, (8 - (stream.size % 8)) % 8, false);

//		System.out.println("encoded message");
//		stream.print();System.out.println();

		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];
		QrCode.BlockInfo block = info.levels.get(qr.error);

		qr.rawbits = new byte[info.codewords];

		// there are some times two different sizes of blocks. The smallest is written to first and the second
		// is larger and can be derived from the size of the first
		int wordsBlockAllA = block.codewords;
		int wordsBlockDataA = block.dataCodewords;
		int wordsEcc = wordsBlockAllA - wordsBlockDataA;
		int numBlocksA = block.blocks;

		int wordsBlockAllB = wordsBlockAllA + 1;
		int wordsBlockDataB = wordsBlockDataA + 1;
		int numBlocksB = (info.codewords - wordsBlockAllA * numBlocksA) / wordsBlockAllB;

		message.resize(wordsBlockDataA + 1);

		int startEcc = numBlocksA * wordsBlockDataA + numBlocksB * wordsBlockDataB;
		int totalBlocks = numBlocksA + numBlocksB;

		rscodes.generator(wordsEcc);
		ecc.resize(wordsEcc);
		encodeBlocks(stream, wordsBlockDataA, numBlocksA, 0, 0, startEcc, totalBlocks);
		encodeBlocks(stream, wordsBlockDataB, numBlocksB, wordsBlockDataA * numBlocksA, numBlocksA, startEcc, totalBlocks);
	}

	private void encodeBlocks(PackedBits8 stream, int bytesInDataBlock, int numberOfBlocks, int streamOffset,
							  int blockOffset, int startEcc, int stride) {
		message.size = bytesInDataBlock;

		for (int idxBlock = 0; idxBlock < numberOfBlocks; idxBlock++) {
			// copy the portion of the stream that's being processed
			int length = Math.min(bytesInDataBlock, Math.max(0, stream.arrayLength() - streamOffset));
			if (length > 0)
				System.arraycopy(stream.data, streamOffset, message.data, 0, length);
			addPadding(message, length, 0b00110111, 0b10001000);
			flipBits8(message);

			// compute the ecc
			rscodes.computeECC(message, ecc);
			flipBits8(message);
			flipBits8(ecc);

			// write it into the output array
			copyIntoRawData(message, ecc, idxBlock + blockOffset, stride, startEcc, qr.rawbits);

			streamOffset += message.size;
		}
	}

	public static void flipBits8(byte[] array, int size) {
		for (int j = 0; j < size; j++) {
			array[j] = flipBits8(array[j] & 0xFF);
		}
	}

	public static void flipBits8(GrowQueue_I8 array) {
		flipBits8(array.data, array.size);
	}

	public static byte flipBits8(int x) {
		int b = 0;
		for (int i = 0; i < 8; i++) {
			b <<= 1;
			b |= (x & 1);
			x >>= 1;
		}
		return (byte) b;
	}

	private void addPadding(GrowQueue_I8 queue, int dataBytes, int padding0, int padding1) {

		boolean a = true;
		for (int i = dataBytes; i < queue.size; i++, a = !a) {
			if (a)
				queue.data[i] = (byte) padding0;
			else
				queue.data[i] = (byte) padding1;
		}
	}

	private void print(String name, GrowQueue_I8 queue) {
		PackedBits8 bits = new PackedBits8();
		bits.size = queue.size * 8;
		bits.data = queue.data;
		System.out.print(name + "  ");
		bits.print();
	}

	private void copyIntoRawData(GrowQueue_I8 message, GrowQueue_I8 ecc, int offset, int stride,
								 int startEcc, byte[] output) {
		for (int i = 0; i < message.size; i++) {
			output[i * stride + offset] = message.data[i];
		}
		for (int i = 0; i < ecc.size; i++) {
			output[i * stride + offset + startEcc] = ecc.data[i];
		}
	}

	private static class MessageSegment
	{
		QrCode.Mode mode;
		String message;
		byte data[];
		int length;
		// number of bits in the encoded message, excluding the length bits
		int encodedSizeBits;

		public int sizeInBits( int version ) {
			int lengthBits;
			switch( mode ) {
				case NUMERIC:lengthBits = getLengthBitsNumeric(version);break;
				case ALPHANUMERIC:lengthBits = getLengthBitsAlphanumeric(version);break;
				case BYTE:lengthBits = getLengthBitsBytes(version);break;
				case KANJI:lengthBits = getLengthBitsKanji(version);break;
				default:throw new RuntimeException("Egads");
			}
			return encodedSizeBits + lengthBits;
		}
	}
}

