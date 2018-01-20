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

import boofcv.alg.descriptor.DescriptorDistance;

/**
 * Contains all the formulas for encoding and decoding BCH and Reid-Solomon codes. For a more accessible
 * introduction to this material see [1] and [2].
 *
 * <pre>
 * [1] S. A. Tretter "Introduction to Bose Chaudhuri Hocquenghem codes" Goddard Space Flight Center, September 1967
 * [2] https://en.wikiversity.org/wiki/Reed–Solomon_codes_for_coders
 * </pre>
 *
 * @author Peter Abeles
 */
// TODO replaced correctDCH() with a non-brute force method.
public class QrCodePolynomialMath {

	public static final int FORMAT_GENERATOR = 0b10100110111;
	public static final int FORMAT_MASK = 0b101010000010010;
	public static final int VERSION_GENERATOR = 0b1111100100101;

	/**
	 * Encodes the version bits. BCH(18,6)
	 * @param version QR code version. 7 to 40
	 * @return encoded bit field
	 */
	public static int encodeVersionBits(int version) {
		int message = version << 12;
		return message ^ bitPolyModulus(message, VERSION_GENERATOR,18,6);
	}

	/**
	 * Encodes the version bits. BCH(18,6)
	 * @param bits Read in bits. Should encode 18-bits
	 * @return encoded bit field
	 */
	public static boolean checkVersionBits(int bits) {
		return bitPolyModulus(bits, VERSION_GENERATOR,18,6) == 0;
	}

	/**
	 * Attempts to correct version bit sequence.
	 * @param bits Read in bits after removing the mask
	 * @return If the message could be corrected, th 5-bit format message. -1 if it couldn't
	 */
	public static int correctVersionBits(int bits ) {
		return correctDCH(64,bits,VERSION_GENERATOR,18,6);
	}

	/**
	 * Encodes the format bits. BCH(15,5)
	 * @param level Error correction level
	 * @param mask The type of mask that is applied to the qr code
	 * @return encoded bit field
	 */
	public static int encodeFormatBits(QrCode.ErrorLevel level , int mask ) {
		int message = (level.value << 3) | (mask & 0xFFFFFFF7);
		message = message << 10;
		return message ^ bitPolyModulus(message, FORMAT_GENERATOR,15,5);
	}

	/**
	 * Check the format bits. BCH(15,5) code.
	 */
	public static boolean checkFormatBits(int bitsNoMask ) {
		return bitPolyModulus(bitsNoMask, FORMAT_GENERATOR,15,5) == 0;
	}

	/**
	 * Assumes that the format message has no errors in it and decodes its data and saves it into the qr code
	 *
	 * @param message format data bits after the mask has been remove and shifted over 10 bits
	 * @param qr Where the results are written to
	 */
	public static void decodeFormatMessage(int message , QrCode qr ) {
		int error = message >> 3;

		qr.error = QrCode.ErrorLevel.lookup(error);
		qr.mask = QrCodeMaskPattern.lookupMask(message&0x07);
	}

	/**
	 * Attempts to correct format bit sequence.
	 * @param bitsNoMask Read in bits after removing the mask
	 * @return If the message could be corrected, th 5-bit format message. -1 if it couldn't
	 */
	public static int correctFormatBits(int bitsNoMask ) {
		return correctDCH(32,bitsNoMask,FORMAT_GENERATOR,15,5);
	}

	/**
	 * Applies a brute force algorithm to find the message which has the smallest hamming distance. if two
	 * messages have the same distance -1 is returned.
	 * @param N Number of possible messages. 32 or 64
	 * @param messageNoMask The observed message with mask removed
	 * @param generator Generator polynomial
	 * @param totalBits Total number of bits in the message.
	 * @param dataBits Total number of data bits in the message
	 * @return The error corrected message or -1 if it can't be determined.
	 */
	public static int correctDCH( int N , int messageNoMask , int generator , int totalBits, int dataBits) {
		int bestHamming = 255;
		int bestMessage = -1;

		int errorBits = totalBits-dataBits;

		// exhaustively check all possibilities
		for (int i = 0; i < N; i++) {
			int test = i << errorBits;
			test = test ^ bitPolyModulus(test,generator,totalBits,dataBits);

			int distance = DescriptorDistance.hamming(test^messageNoMask);

			// see if it found a better match
			if( distance < bestHamming ) {
				bestHamming = distance;
				bestMessage = i;
			} else if( distance == bestHamming ) {
				// ambiguous so reject
				bestMessage = -1;
			}
		}
		return bestMessage;
	}

	/**
	 * Performs division using xcr operators on the encoded polynomials. used in BCH encoding/decoding
	 * @param data Data being checked
	 * @param generator Generator polynomial
	 * @param totalBits Total number of bits in data
	 * @param dataBits Number of data bits. Rest are error correction bits
	 * @return Remainder after polynomial division
	 */
	public static int bitPolyModulus(int data , int generator , int totalBits, int dataBits) {
		int errorBits = totalBits-dataBits;
		for (int i = dataBits-1; i >= 0; i--) {
			if( (data & (1 << (i+errorBits))) != 0 ) {
				data ^= generator << i;
			}
		}
		return data;
	}
}
