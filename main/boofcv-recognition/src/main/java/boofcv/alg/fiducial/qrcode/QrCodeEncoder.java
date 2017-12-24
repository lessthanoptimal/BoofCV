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

import org.ddogleg.struct.GrowQueue_I8;

import java.io.UnsupportedEncodingException;

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
	// TODO autoselect mask
public class QrCodeEncoder {

	/**
	 * All the possible values in alphanumeric mode.
	 */
	public static final String ALPHANUMERIC= "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

	// used to compute error correction
	private ReidSolomonCodes rscodes = new ReidSolomonCodes(8,0b100011101);

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

	public QrCodeEncoder() {
		reset();
	}

	public void reset() {
		qr.reset();
		qr.version = -1;
		packed.size = 0;
		autoMask = true;
		autoErrorCorrection = true;
	}

	public QrCodeEncoder setVersion(int version ) {
		qr.version = version;
		return this;
	}

	public QrCodeEncoder setError(QrCode.ErrorLevel level ) {
		autoErrorCorrection = false;
		qr.error = level;
		return this;
	}

	public QrCodeEncoder setMask( QrCodeMaskPattern pattern ) {
		autoMask = false;
		qr.mask = pattern;
		return this;
	}

	/**
	 * Creates a QR-Code which encodes a number sequence
	 * @param message String that specifies numbers and no other types. Each number has to be from 0 to 9 inclusive.
	 * @return The QR-Code
	 */
	public QrCodeEncoder numeric(String message ) {
		int[] numbers = new int[ message.length() ];

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			int values = c - '0';
			if( values < 0 || values > 9 )
				throw new RuntimeException("Expected each character to be a number from 0 to 9");
			numbers[i] = values;
		}
		return numeric(numbers);
	}

	/**
	 * Creates a QR-Code which encodes a number sequence
	 * @param numbers Array of numbers. Each number has to be from 0 to 9 inclusive.
	 * @return The QR-Code
	 */
	public QrCodeEncoder numeric(int[] numbers ) {
		for (int i = 0; i < numbers.length; i++) {
			if( numbers[i] < 0 || numbers[i] > 9 )
				throw new IllegalArgumentException("All numbers must have a value from 0 to 9");
		}

		qr.mode = QrCode.Mode.NUMERIC;
		int lengthBits = getLengthBitsNumeric(qr.version);

		// specify the mode
		packed.append(0b0001,4,false);

		// Specify the number of digits
		packed.append(numbers.length,lengthBits,false);

		// Append the digits
		int index = 0;
		while( numbers.length-index >= 3 ) {
			int value = numbers[index] * 100 + numbers[index+1]*10 + numbers[index+2];
			packed.append(value,10,false);
			index += 3;
		}
		if( numbers.length-index == 2 ) {
			int value = numbers[index]*10 + numbers[index+1];
			packed.append(value,7,false);
		} else if( numbers.length-index == 1 ) {
			int value = numbers[index];
			packed.append(value,4,false);
		}

		return this;
	}

	/**
	 * Creates a QR-Code which encodes data in the alphanumeric format
	 * @param alphaNumeric String containing only alphanumeric values.
	 * @return The QR-Code
	 */
	public QrCodeEncoder alphanumeric( String alphaNumeric ) {
		byte values[] = alphanumericToValues(alphaNumeric);

		qr.mode = QrCode.Mode.ALPHANUMERIC;

		int lengthBits = getLengthBitsAlphanumeric(qr.version);

		// specify the mode
		packed.append(0b0010,4,false);

		// Specify the number of digits
		packed.append(values.length,lengthBits,false);

		// Append the digits
		int index = 0;
		while( values.length-index >= 2 ) {
			int value = values[index] * 45 + values[index+1];
			packed.append(value,11,false);
			index += 2;
		}
		if( values.length-index == 1 ) {
			int value = values[index];
			packed.append(value,6,false);
		}

		return this;
	}

	public static byte[] alphanumericToValues( String data ) {
		byte[] output = new byte[ data.length() ];

		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			int value = ALPHANUMERIC.indexOf(c);
			if( value < 0 )
				throw new IllegalArgumentException("Unsupported character '"+c+"' = "+(int)c);
			output[i] = (byte)value;
		}
		return output;
	}

	public static char valueToAlphanumeric( int value ) {
		if( value < 0 || value >= ALPHANUMERIC.length() )
			throw new RuntimeException("Value out of range");
		return ALPHANUMERIC.charAt(value);
	}

	public QrCodeEncoder bytes( String message ) {
		try {
			return bytes(message.getBytes("JIS"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a QR-Code which encodes data in the byte format.
	 * @param data Data to be encoded
	 * @return The QR-Code
	 */
	public QrCodeEncoder bytes( byte[] data ) {
		qr.mode = QrCode.Mode.BYTE;

		int lengthBits = getLengthBitsBytes(qr.version);

		// specify the mode
		packed.append(0b0100,4,false);

		// Specify the number of digits
		packed.append(data.length,lengthBits,false);

		// Append the digits
		for (int i = 0; i < data.length; i++) {
			packed.append(data[i]&0xff,8,false);
		}

		return this;
	}

	/**
	 * Creates a QR-Code which encodes Kanji characters
	 * @param message Data to be encoded
	 * @return The QR-Code
	 */
	public QrCodeEncoder kanji( String message ) {
		qr.mode = QrCode.Mode.KANJI;

		int lengthBits = getLengthBitsKanji(qr.version);

		byte[] bytes;
		try {
			bytes = message.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalArgumentException(ex);
		}

		// specify the mode
		packed.append(0b1000,4,false);

		// Specify the number of characters
		packed.append(message.length(),lengthBits,false);

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
				throw new IllegalArgumentException("Invalid byte sequence. "+message.charAt(i/2));
			}
			int encoded = ((adjusted >> 8) * 0xc0) + (adjusted & 0xff);
			packed.append(encoded, 13, false);
		}

		return this;
	}

	public static int getLengthBitsNumeric( int version ) {
		return getLengthBits( version, 10, 12, 14);
	}

	public static int getLengthBitsAlphanumeric( int version ) {
		return getLengthBits( version, 9, 11, 13);
	}

	public static int getLengthBitsBytes( int version ) {
		return getLengthBits( version, 8, 16, 16);
	}

	public static int getLengthBitsKanji(int version) {
		return getLengthBits( version, 8, 10, 12);
	}

	/**
	 * Returns the length of the message length variable in bits. Dependent on version
	 */
	private static int getLengthBits( int version , int bitsA , int bitsB , int bitsC ) {
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
	 * @return The generated QR Code
	 */
	public QrCode fixate() {
		autoSelectVersionAndError();

		if( autoMask ) {
			// todo
		}

		int maxBits = QrCode.VERSION_INFO[qr.version].codewords*8;

		if( packed.size > maxBits ) {
			throw new IllegalArgumentException("The message is longer than the max possible size");
		}

		if( packed.size+4 <= maxBits) {
			// add the terminator to the bit stream
			packed.append(0b0000, 4, false);
		}
		bitsToMessage(packed);
		return qr;
	}

	/**
	 * Checks to see if a request has been made to select version and/or error correction. If so it will pick something
	 */
	private void autoSelectVersionAndError() {
		if( qr.version == -1 ) {
			if( autoErrorCorrection ) {
				// this is a reasonable compromise between robustness and data storage
				qr.error = QrCode.ErrorLevel.M;
			}
			// select the smallest version which can store all the data
			for (int i = 1; i <= 40; i++) {
				if( packed.size/8 <= QrCode.VERSION_INFO[i].totalDataBytes(qr.error) ) {
					qr.version = i;
					break;
				}
			}
			if( qr.version == -1 ) {
				throw new IllegalArgumentException("Packet too large to store at error level "+qr.error);
			}
		} else {
			// the version is set but the error correction level isn't. Pick the one with
			// the most error correction that can which can store all the data
			if( autoErrorCorrection ) {
				qr.error = null;
				QrCode.VersionInfo v = QrCode.VERSION_INFO[qr.version];
				for(QrCode.ErrorLevel level : QrCode.ErrorLevel.values() ) {
					if( packed.size/8 <= v.totalDataBytes(level)) {
						qr.error = level;
					}
				}
				if( qr.error == null ) {
					throw new IllegalArgumentException("You need to use a high version number to store the data. Tried" +
							"all error correction levels at version "+qr.version+". Total Data "+(packed.size/8));
				}
			}
		}
	}

	protected void bitsToMessage( PackedBits8 stream ) {
		// add padding to make it align to 8
		stream.append(0,(8-(stream.size%8))%8,false);

//		System.out.println("encoded message");
//		stream.print();System.out.println();

		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];
		QrCode.BlockInfo block = info.levels.get(qr.error);

		qr.rawbits = new byte[info.codewords];

		// there are some times two different sizes of blocks. The smallest is written to first and the second
		// is larger and can be derived from the size of the first
		int wordsBlockAllA = block.codewords;
		int wordsBlockDataA = block.dataCodewords;
		int wordsEcc = wordsBlockAllA-wordsBlockDataA;
		int numBlocksA = block.blocks;

		int wordsBlockAllB = wordsBlockAllA + 1;
		int wordsBlockDataB = wordsBlockDataA + 1;
		int numBlocksB = (info.codewords-wordsBlockAllA*numBlocksA)/wordsBlockAllB;

		message.resize(wordsBlockDataA+1);

		int startEcc = numBlocksA*wordsBlockDataA + numBlocksB*wordsBlockDataB;
		int totalBlocks = numBlocksA + numBlocksB;

		rscodes.generator(wordsEcc);
		ecc.resize(wordsEcc);
		encodeBlocks(stream, wordsBlockDataA, numBlocksA, 0, 0,startEcc, totalBlocks);
		encodeBlocks(stream, wordsBlockDataB, numBlocksB, wordsBlockDataA*numBlocksA,numBlocksA, startEcc, totalBlocks);
	}

	private void encodeBlocks(PackedBits8 stream, int bytesInDataBlock, int numberOfBlocks, int streamOffset,
							  int blockOffset, int startEcc, int stride) {
		message.size = bytesInDataBlock;

		for (int idxBlock = 0; idxBlock < numberOfBlocks; idxBlock++) {
			// copy the portion of the stream that's being processed
			int length = Math.min(bytesInDataBlock,Math.max(0,stream.arrayLength()-streamOffset));
			if( length > 0 )
				System.arraycopy(stream.data,streamOffset, message.data,0,length);
			addPadding(message,length,0b00110111,0b10001000);
			flipBits8(message);

			// compute the ecc
			rscodes.computeECC(message,ecc);
			flipBits8(message); flipBits8(ecc);

			// write it into the output array
			copyIntoRawData(message,ecc,idxBlock+blockOffset,stride,startEcc,qr.rawbits);

			streamOffset += message.size;
		}
	}

	public static void flipBits8(byte[] array, int size ) {
		for (int j = 0; j < size; j++) {
			array[j] = flipBits8(array[j]&0xFF);
		}
	}

	public static void flipBits8(GrowQueue_I8 array ) {
		flipBits8(array.data,array.size);
	}

	public static byte flipBits8(int x ) {
		int b=0;
		for (int i = 0; i < 8; i++) {
			b<<=1;
			b|=( x &1);
			x>>=1;
		}
		return (byte)b;
	}

	private void addPadding(GrowQueue_I8 queue , int dataBytes, int padding0 , int padding1 ) {

		boolean a = true;
		for (int i = dataBytes; i < queue.size; i++,a=!a) {
			if( a )
				queue.data[i] = (byte)padding0;
			else
				queue.data[i] = (byte)padding1;
		}
	}

	private void print( String name , GrowQueue_I8 queue ) {
		PackedBits8 bits = new PackedBits8();
		bits.size = queue.size*8;
		bits.data = queue.data;
		System.out.print(name+"  ");
		bits.print();
	}

	private void copyIntoRawData( GrowQueue_I8 message , GrowQueue_I8 ecc , int offset , int stride ,
								  int startEcc , byte[] output )
	{
		for (int i = 0; i < message.size; i++) {
			output[i*stride+offset] = message.data[i];
		}
		for (int i = 0; i < ecc.size; i++) {
			output[i*stride+offset+startEcc] = ecc.data[i];
		}
	}
}
