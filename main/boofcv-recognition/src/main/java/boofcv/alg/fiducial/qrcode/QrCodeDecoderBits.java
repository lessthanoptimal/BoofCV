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

import org.ddogleg.struct.GrowQueue_I8;

import java.io.UnsupportedEncodingException;

import static boofcv.alg.fiducial.qrcode.QrCode.Failure.JIS_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCode.Failure.KANJI_UNAVAILABLE;
import static boofcv.alg.fiducial.qrcode.QrCodeEncoder.valueToAlphanumeric;

/**
 * After the data bits have been read this will decode them and extract a meaningful message.
 *
 * @author Peter Abeles
 */
public class QrCodeDecoderBits {

	// used to compute error correction
	ReidSolomonCodes rscodes = new ReidSolomonCodes(8,0b100011101);
	// storage for the data message
	GrowQueue_I8 message = new GrowQueue_I8();
	// storage fot the message's ecc
	GrowQueue_I8 ecc = new GrowQueue_I8();

	StringBuilder workString = new StringBuilder();

	/**
	 * Reconstruct the data while applying error correction.
	 */
	public boolean applyErrorCorrection( QrCode qr) {
//		System.out.println("decoder ver   "+qr.version);
//		System.out.println("decoder mask  "+qr.mask);
//		System.out.println("decoder error "+qr.error);

		QrCode.VersionInfo info = QrCode.VERSION_INFO[qr.version];
		QrCode.BlockInfo block = info.levels.get(qr.error);

		int wordsBlockAllA = block.codewords;
		int wordsBlockDataA = block.dataCodewords;
		int wordsEcc = wordsBlockAllA-wordsBlockDataA;
		int numBlocksA = block.blocks;

		int wordsBlockAllB = wordsBlockAllA + 1;
		int wordsBlockDataB = wordsBlockDataA + 1;
		int numBlocksB = (info.codewords-wordsBlockAllA*numBlocksA)/wordsBlockAllB;

		int totalBlocks = numBlocksA + numBlocksB;
		int totalDataBytes = wordsBlockDataA*numBlocksA + wordsBlockDataB*numBlocksB;
		qr.corrected = new byte[totalDataBytes];

		ecc.resize(wordsEcc);
		rscodes.generator(wordsEcc);

		if( !decodeBlocks(qr,wordsBlockDataA,numBlocksA,0,0,totalDataBytes,totalBlocks) )
			return false;

		return decodeBlocks(qr,wordsBlockDataB,numBlocksB,numBlocksA*wordsBlockDataA,numBlocksA,totalDataBytes,totalBlocks);
	}

	private boolean decodeBlocks( QrCode qr, int bytesInDataBlock, int numberOfBlocks, int bytesDataRead,
							  int offsetBlock, int offsetEcc, int stride) {
		message.resize(bytesInDataBlock);

		for (int idxBlock = 0; idxBlock < numberOfBlocks; idxBlock++) {
			copyFromRawData(qr.rawbits,message,ecc,offsetBlock+idxBlock,stride,offsetEcc);

			QrCodeEncoder.flipBits8(message);
			QrCodeEncoder.flipBits8(ecc);

			if( !rscodes.correct(message,ecc) ) {
				return false;
			}

			QrCodeEncoder.flipBits8(message);
			System.arraycopy(message.data,0,qr.corrected,bytesDataRead,message.size);
			bytesDataRead += message.size;
		}
		return true;
	}

	private void copyFromRawData( byte[] input , GrowQueue_I8 message , GrowQueue_I8 ecc ,
								  int offsetBlock , int stride , int offsetEcc )
	{
		for (int i = 0; i < message.size; i++) {
			message.data[i] = input[i*stride+offsetBlock];
		}
		for (int i = 0; i < ecc.size; i++) {
			ecc.data[i] = input[i*stride+offsetBlock+offsetEcc];
		}
	}

	public boolean decodeMessage(QrCode qr) {
		PackedBits8 bits = new PackedBits8();
		bits.data = qr.corrected;
		bits.size = qr.corrected.length*8;

//		System.out.println("decoded message");
//		bits.print();System.out.println();

		workString.setLength(0);
		qr.message = null;

		// if there isn't enough bits left to read the mode it must be done
		int location = 0;
		while( location+4 <= bits.size ) {
			int modeBits = bits.read(location, 4, true);
			location += 4;
			if( modeBits == 0) // escape indicator
				break;
			QrCode.Mode mode = QrCode.Mode.lookup(modeBits);
			qr.mode = updateModeLogic(qr.mode,mode);
			switch (mode) {
				case NUMERIC: location = decodeNumeric(qr, bits,location); break;
				case ALPHANUMERIC: location = decodeAlphanumeric(qr, bits,location); break;
				case BYTE: location = decodeByte(qr, bits,location); break;
				case KANJI: location = decodeKanji(qr, bits,location); break;
				case ECI:
//					throw new RuntimeException("Not supported yet");
					qr.failureCause = QrCode.Failure.UNKNOWN_MODE;
					return false;
				case FNC1_FIRST:
				case FNC1_SECOND:
					// This isn't the proper way to handle this mode, but it
					// should still parse the data
					break;
				default:
					qr.failureCause = QrCode.Failure.UNKNOWN_MODE;
					return false;
			}

			if (location < 0) {
				// cause is set inside of decoding function
				return false;
			}
		}
		// ensure the length is byte aligned
		location = alignToBytes(location);
		int lengthBytes = location / 8;

		// sanity check padding
		if (!checkPaddingBytes(qr, lengthBytes)) {
			qr.failureCause = QrCode.Failure.READING_PADDING;
			return false;
		}

		qr.message = workString.toString();
		return true;
	}

	/**
	 * Set the mode to the most complex character encoding.
	 */
	private QrCode.Mode updateModeLogic( QrCode.Mode current , QrCode.Mode candidate )
	{
		if( candidate.ordinal() <= QrCode.Mode.KANJI.ordinal() ) {
			current = current.ordinal() > candidate.ordinal() ? current : candidate;
		}
		return current;
	}

	public static int alignToBytes(int lengthBits) {
		return lengthBits + (8-lengthBits%8)%8;
	}

	/**
	 * Makes sure the used bytes have the expected values
	 *
	 * @param lengthBytes Number of bytes that data should be been written to and not filled with padding.
	 */
	boolean checkPaddingBytes(QrCode qr, int lengthBytes) {
		boolean a = true;

		for (int i = lengthBytes; i < qr.corrected.length; i++) {
			if (a) {
				if (0b00110111 != (qr.corrected[i] & 0xFF))
					return false;
			} else {
				if (0b10001000 != (qr.corrected[i] & 0xFF)) {
					// the pattern starts over at the beginning of a block. Strictly enforcing the standard
					// requires knowing size of a data chunk and where it starts. Possible but
					// probably not worth the effort the implement as a strict requirement.
					if (0b00110111 == (qr.corrected[i] & 0xFF)) {
						a = true;
					} else {
						return false;
					}
				}
			}
			a = !a;
		}

		return true;
	}

	/**
	 * Decodes a numeric message
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeNumeric( QrCode qr , PackedBits8 data, int bitLocation ) {
		int lengthBits = QrCodeEncoder.getLengthBitsNumeric(qr.version);

		int length = data.read(bitLocation,lengthBits,true);
		bitLocation += lengthBits;

		while( length >= 3 ) {
			if( data.size < bitLocation+10 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation,10,true);
			bitLocation += 10;

			int valA = chunk/100;
			int valB = (chunk-valA*100)/10;
			int valC = chunk-valA*100-valB*10;

			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
			workString.append((char)(valC + '0'));

			length -= 3;
		}

		if( length == 2 ) {
			if( data.size < bitLocation+7 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation,7,true);
			bitLocation += 7;

			int valA = chunk/10;
			int valB = chunk-valA*10;
			workString.append((char)(valA + '0'));
			workString.append((char)(valB + '0'));
		} else if( length == 1 ) {
			if( data.size < bitLocation+4 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation,4,true);
			bitLocation += 4;
			workString.append((char)(valA + '0'));
		}
		return bitLocation;
	}

	/**
	 * Decodes alphanumeric messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeAlphanumeric( QrCode qr , PackedBits8 data, int bitLocation ) {
		int lengthBits = QrCodeEncoder.getLengthBitsAlphanumeric(qr.version);

		int length = data.read(bitLocation,lengthBits,true);
		bitLocation += lengthBits;

		while( length >= 2 ) {
			if( data.size < bitLocation+11 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int chunk = data.read(bitLocation,11,true);
			bitLocation += 11;

			int valA = chunk/45;
			int valB = chunk-valA*45;

			workString.append(valueToAlphanumeric(valA));
			workString.append(valueToAlphanumeric(valB));
			length -= 2;
		}

		if( length == 1 ) {
			if( data.size < bitLocation+6 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int valA = data.read(bitLocation,6,true);
			bitLocation += 6;
			workString.append(valueToAlphanumeric(valA));
		}
		return bitLocation;
	}

	/**
	 * Decodes byte messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeByte( QrCode qr , PackedBits8 data, int bitLocation ) {
		int lengthBits = QrCodeEncoder.getLengthBitsBytes(qr.version);

		int length = data.read(bitLocation,lengthBits,true);
		bitLocation += lengthBits;

		if( length*8 > data.size-bitLocation ) {
			qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
			return -1;
		}

		byte rawdata[] = new byte[ length ];

		for (int i = 0; i < length; i++) {
			rawdata[i] = (byte)data.read(bitLocation,8,true);
			bitLocation += 8;
		}
		try {
			workString.append( new String(rawdata, "JIS") );
		} catch (UnsupportedEncodingException ignored) {
			qr.failureCause = JIS_UNAVAILABLE;
			return -1;
		}
		return bitLocation;
	}

	/**
	 * Decodes Kanji messages
	 *
	 * @param qr QR code
	 * @param data encoded data
	 * @return Location it has read up to in bits
	 */
	private int decodeKanji( QrCode qr , PackedBits8 data, int bitLocation ) {
		int lengthBits = QrCodeEncoder.getLengthBitsKanji(qr.version);

		int length = data.read(bitLocation,lengthBits,true);
		bitLocation += lengthBits;

		byte rawdata[] = new byte[ length*2 ];

		for (int i = 0; i < length; i++) {
			if( data.size < bitLocation+13 ) {
				qr.failureCause = QrCode.Failure.MESSAGE_OVERFLOW;
				return -1;
			}
			int letter = data.read(bitLocation,13,true);
			bitLocation += 13;

			letter = ((letter/0x0C0) << 8) | (letter%0x0C0);

			if (letter < 0x01F00) {
				// In the 0x8140 to 0x9FFC range
				letter += 0x08140;
			} else {
				// In the 0xE040 to 0xEBBF range
				letter += 0x0C140;
			}
			rawdata[i*2] = (byte) (letter >> 8);
			rawdata[i*2 + 1] = (byte) letter;
		}

		// Shift_JIS may not be supported in some environments:
		try {
			workString.append( new String(rawdata, "Shift_JIS") );
		} catch (UnsupportedEncodingException ignored) {
			qr.failureCause = KANJI_UNAVAILABLE;
			return -1;
		}

		return bitLocation;
	}
}
