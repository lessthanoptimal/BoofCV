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

/**
 * Provides an easy to use interface for specifying QR-Code parameters and generating the raw data sequence. After
 * the QR Code has been created using this class it can then be rendered.
 *
 * By default it will select the qr code version based on the number of
 * bits and the error correction level based on the version and number of bits. If the error correction isn't specified
 * and the version isn't specified then error correction level Q is used by default.
 *
 * @author Peter Abeles
 */
// TODO autoselect version
	// TODO autoselect error correction
	// TODO autoselect mask
public class QrCodeEncoder {

	// used to compute error correction
	ReidSolomonCodes codes = new ReidSolomonCodes(8,0b100011101);

	// output qr code
	QrCode qr = new QrCode();

	// If true it will automatically select the amount of error correction depending on the length of the data
	boolean autoErrorCorrection = true;

	boolean autoMask = true;

	// workspace variables
	GrowQueue_I8 input = new GrowQueue_I8();
	GrowQueue_I8 ecc = new GrowQueue_I8();

	public QrCodeEncoder() {
		qr.version = -1;
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

	public QrCode numeric(String message ) {
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

	public QrCode numeric(int[] numbers ) {
		for (int i = 0; i < numbers.length; i++) {
			if( numbers[i] < 0 || numbers[i] > 9 )
				throw new IllegalArgumentException("All numbers must have a value from 0 to 9");
		}
		int lengthBits;

		if (qr.version < 10)
			lengthBits = 10;
		else if (qr.version < 27)
			lengthBits = 12;
		else
			lengthBits = 14;

		int totalBits = lengthBits + 10*(numbers.length/3);

		PackedBits8 packed = new PackedBits8(totalBits);
		packed.size = 0; // set size to 0 so that append() starts from the front

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

		// add the terminator to the bit stream
		packed.append(0b0000,4,false);

		bitsToMessage(packed);

		return qr;
	}

	public QrCode alphaNumeric( String alphaNumeric ) {
		return null;
	}

	public QrCode bytes( byte[] data ) {
		return null;
	}

	public QrCode kanji( short[] kanji ) {
		return null;
	}

	protected void bitsToMessage( PackedBits8 stream ) {
		// add padding to make it align to 8
		stream.append(0,(8-(stream.size%8))%8,false);

		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];
		QrCode.ErrorBlock block = info.levels.get(qr.error);

		qr.dataRaw = new byte[info.codewords];

		// there are some times two different sizes of blocks. The smallest is written to first and the second
		// is larger and can be derived from the size of the first
		int wordsBlockAllA = block.codewords;
		int wordsBlockDataA = block.dataCodewords;
		int wordsEcc = wordsBlockAllA-wordsBlockDataA;
		int numBlocksA = block.eccBlocks;

		int wordsBlockAllB = wordsBlockAllA + 1;
		int wordsBlockDataB = wordsBlockDataA + 1;
		int numBlocksB = (info.codewords-wordsBlockAllA*numBlocksA)/wordsBlockAllB;

		input.resize(wordsBlockDataA+1);

		int startEcc = numBlocksA*wordsBlockDataA + numBlocksB*wordsBlockDataB;
		int totalBlocks = numBlocksA + numBlocksB;

		codes.generator(wordsEcc);
		ecc.resize(wordsEcc);
		encodeBlocks(stream, wordsBlockDataA, numBlocksA, 0, 0,startEcc, totalBlocks);
		encodeBlocks(stream, wordsBlockDataB, numBlocksB, wordsBlockDataA*numBlocksA,numBlocksA, startEcc, totalBlocks);
	}

	private void encodeBlocks(PackedBits8 stream, int bytesInDataBlock, int numberOfBlocks, int streamOffset,
							  int blockOffset, int startEcc, int stride) {
		input.size = bytesInDataBlock;

		for (int idxBlock = 0; idxBlock < numberOfBlocks; idxBlock++) {
			// copy the portion of the stream that's being processed
			int length = Math.min(bytesInDataBlock,Math.max(0,stream.arrayLength()-streamOffset));
			if( length > 0 )
				System.arraycopy(stream.data,streamOffset,input.data,0,length);
			addPadding(input,length,0b00110111,0b10001000);
			flipBits8(input);

			// compute the ecc
			codes.computeECC(input,ecc);
			flipBits8(input); flipBits8(ecc);

			// write it into the output array
			copyIntoRawData(input,ecc,idxBlock+blockOffset,stride,startEcc,qr.dataRaw);

			streamOffset += input.size;
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
