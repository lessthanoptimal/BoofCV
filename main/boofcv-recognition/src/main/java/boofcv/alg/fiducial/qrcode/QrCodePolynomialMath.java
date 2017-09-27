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

/**
 * Contains all the formulas for encoding and decoding BCH and Reid-Solomon codes.
 *
 * @author Peter Abeles
 */
public class QrCodePolynomialMath {

	public static final int FORMAT_GENERATOR = 0b10100110111;
	public static final int FORMAT_MASK = 0b101010000010010;

	/**
	 * Encodes the format bits. BCH(15,5)
	 * @param level Error correction level
	 * @return encoded bit field
	 */
	public static int encodeFormatMessage(QrCode.ErrorCorrectionLevel level , int mask ) {
		int message = (level.value << 3) | (mask & 0xFFFFFFF7);
		message = message << 10;
		int tmp = message ^ bitPolyDivide(message, FORMAT_GENERATOR,15,5);
		return tmp ^ FORMAT_MASK;
	}

	/**
	 * Check the format bits. BCH(15,5) code.
	 */
	public static boolean checkFormatMessage(int value ) {
		return bitPolyDivide(value^FORMAT_MASK, FORMAT_GENERATOR,15,5) == 0;
	}

	/**
	 * Assumes that the format message has no errors in it and decodes its data and saves it into the qr code
	 *
	 * @param message format data bits after the mask has been applied
	 * @param qr Where the results are written to
	 */
	public static void decodeFormat( int message , QrCode qr ) {
		int error = message >> 13;

		qr.errorCorrection = QrCode.ErrorCorrectionLevel.lookup(error);
		qr.maskPattern = (message >> 10)&0x07;
	}

	/**
	 * Performs division using xcr operators on the encoded polynomials. used in BCH encoding/decoding
	 * @param data Data being checked
	 * @param generator Generator polynomial
	 * @param totalBits Total number of bits in data
	 * @param dataBits Number of data bits. Rest are error correction bits
	 * @return Remainder after polynomial division
	 */
	public static int bitPolyDivide(int data , int generator , int totalBits, int dataBits) {
		int errorBits = totalBits-dataBits;
		for (int i = dataBits-1; i >= 0; i--) {
			if( (data & (1 << (i+errorBits))) != 0 ) {
				data ^= generator << i;
			}
		}
		return data;
	}
}
