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

import java.util.Arrays;

/**
 * Used to create QR Codes given a message. By default it will select the qr code version based on the number of
 * bits and the error correction level based on the version and number of bits. If the error correction isn't specified
 * and the version isn't specified then error correction level Q is used by default.
 *
 * @author Peter Abeles
 */
public class QrCodeEncoder {

	QrCode qr = new QrCode();

	// If true it will automatically select the amount of error correction depending on the length of the data
	boolean autoErrorCorrection = true;

	public QrCodeEncoder() {
		qr.version = -1;
	}

	public QrCodeEncoder setVersion(int version ) {
		qr.version = version;
		return this;
	}

	public QrCodeEncoder setErrorCorrection(QrCode.ErrorCorrectionLevel level ) {
		autoErrorCorrection = false;
		qr.errorCorrection = level;
		return this;
	}

	public QrCode encodeNumeric( String message ) {
		int[] numbers = new int[ message.length() ];

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			int values = c - '0';
			if( values < 0 || values > 9 )
				throw new RuntimeException("Expected each character to be a number from 0 to 9");
			numbers[i] = values;
		}
		return encodeNumeric(numbers);
	}

	public QrCode encodeNumeric( int[] numbers ) {
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

	public QrCode encodeAlphaNumeric( String alphaNumeric ) {
		return null;
	}

	public QrCode encodeBytes( byte[] data ) {
		return null;
	}

	public QrCode encodeKanji( short[] kanji ) {
		return null;
	}

	protected void bitsToMessage( PackedBits8 stream ) {
		ReidSolomonCodes codes = new ReidSolomonCodes(8,0b100011101);

		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];
		QrCode.ErrorBlock block = info.levels.get(qr.errorCorrection);

		qr.dataRaw = new byte[info.codewords];

		int blockCodeWordsA = block.codewords;
		int blockDataA = block.dataCodewords;
		int numBlocksA = block.eccBlocks;

		int blockCodeWordsB = blockCodeWordsA + 1;
		int blockDataB = blockDataA + 1;
		int numBlocksB = (info.codewords-blockCodeWordsA*numBlocksA)/blockCodeWordsB;

		int streamOffset = 0;

		GrowQueue_I8 input = new GrowQueue_I8();
		input.resize(blockCodeWordsA+1);
		input.size = blockCodeWordsA;
		GrowQueue_I8 ecc = new GrowQueue_I8();
		ecc.resize(blockCodeWordsA-blockDataA);
		codes.generator(ecc.size);

		int startEcc = numBlocksA*blockDataA + numBlocksB*blockDataB;
		int totalBlocks = numBlocksA + numBlocksB;

		for (int i = 0; i < numBlocksA; i++) {
			// copy the portion of the stream that's being processed
			int length = Math.min(blockCodeWordsA,Math.max(0,stream.arrayLength()-streamOffset));
			System.arraycopy(stream.data,streamOffset,input.data,0,length);
			Arrays.fill(input.data,length,input.size,(byte)0);

			// compute the ecc
			codes.computeECC(input,ecc);

			// write it into the output array
			copyIntoRawData(input,ecc,i,totalBlocks,startEcc,qr.dataRaw);

			streamOffset += input.size;
		}

		input.size = blockCodeWordsB;
		for (int i = 0; i < numBlocksB; i++) {

			// todo write
			streamOffset += input.size;
		}
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
