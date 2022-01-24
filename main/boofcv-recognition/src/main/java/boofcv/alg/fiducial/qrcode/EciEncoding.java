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

/**
 * Information on different character encodings and ECI character sets
 *
 * @author Peter Abeles
 */
public class EciEncoding {
	// BINARY is specific to BoofCV and is used to indicate that there should be no encoding done
	public static final String BINARY = "binary";

	// Standard QR Code string encodings
	public static final String UTF8 = "UTF8";
	public static final String ISO8859_1 = "ISO8859_1";
	public static final String JIS = "JIS";

	public static boolean isValidUTF8( byte[] message ) {
		int index = 0;
		while (index < message.length) {
			// determine the number of bytes per letter
			int letterSize;
			int value = message[index] & 0xFF;
			if (value >> 3 == 0b1111_0) {
				letterSize = 4;
			} else if (value >> 4 == 0b1110) {
				letterSize = 3;
			} else if (value >> 5 == 0b110) {
				letterSize = 2;
			} else if ((value >> 7) == 0) {
				letterSize = 1;
			} else {
				return false;
			}
			// all multibyte UTF-9 characters start with 0b10xx_xxx
			for (int i = 1; i < letterSize; i++) {
				if ((message[index + i] & 0xFF) >> 6 != 0b10)
					return false;
			}
			index += letterSize;
			if (index == message.length)
				return true;
		}
		return false;
	}

	/**
	 * ECI designator to character set. Take from ZXing. The easily available, and out of date, QR Code specification
	 * is missing this information as far as I can tell. The latest ISO is available for $50 on ISO's website.
	 */
	public static String getEciCharacterSet( int designator ) {
		return switch (designator) {
			case 0, 2 -> "Cp437";
			case 1, 3 -> "ISO8859_1";
			case 4 -> "ISO8859_2";
			case 5 -> "ISO8859_3";
			case 6 -> "ISO8859_4";
			case 7 -> "ISO8859_5";
			case 8 -> "ISO8859_6";
			case 9 -> "ISO8859_7";
			case 10 -> "ISO8859_8";
			case 11 -> "ISO8859_9";
			case 12 -> "ISO8859_10";
			case 13 -> "ISO8859_11";
			case 14 -> "ISO8859_12";
			case 15 -> "ISO8859_13";
			case 16 -> "ISO8859_14";
			case 17 -> "ISO8859_15";
			case 18 -> "ISO8859_16";
			case 20 -> "SJIS";
			case 21 -> "Cp1250";
			case 22 -> "Cp1251";
			case 23 -> "Cp1252";
			case 24 -> "Cp1256";
			case 25 -> "UnicodeBigUnmarked";
			case 26 -> "UTF8";
			case 27, 170 -> "ASCII";
			case 28 -> "Big5";
			case 29 -> "GB18030";
			case 30 -> "EUC_KR";
			default -> throw new IllegalArgumentException("Unknown ECI designator " + designator);
		};
	}
}
