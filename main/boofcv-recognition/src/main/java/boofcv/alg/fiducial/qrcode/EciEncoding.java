/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
	public static final String UTF8="UTF8";
	public static final String ISO8859_1="ISO8859_1";
	public static final String JIS="JIS";

	/**
	 * The encoding for byte messages should be ISO8859_1 or JIS, depending on which version of the specification
	 * you follow. In reality people use whatever they want and expect it to magically work. This attempts to
	 * figure out if it's ISO8859_1, JIS, or UTF8. UTF-8 is the most common and is used if its ambiguous.
	 *
	 * @param message The raw byte message with an unknown encoding
	 * @return
	 */
	public static String guessEncoding( byte[] message ) {
		// this function is inspired by a similarly named function in ZXing, but was written from scratch
		// using specifications for each encoding since I didn't understand what they were doing

		boolean isUtf8 = true;
		boolean isJis = true;
		boolean isIso = true;

		for (int i = 0; i < message.length; i++) {
			int v = message[i]&0xFF;
			if( isUtf8 )
				isUtf8 = isValidUTF8(v);
			if( isJis )
				isJis = isValidJIS(v);
			if( isIso )
				isIso = isValidIso8869_1(v);
		}

//		System.out.printf("UTF-8=%s ISO=%s JIS=%s\n",isUtf8,isIso,isJis);

		// If there is ambiguity do it based on how common it is and what the specification says
		if( isUtf8 )
			return UTF8;
		if( isIso )
			return ISO8859_1;
		if( isJis )
			return JIS;

		return UTF8;
	}

	public static boolean isValidUTF8( int v ) {
		return (v >= 0 && v <= 0xBF) || (v >= 0xC4 && v <= 0xF4);
	}

	public static boolean isValidJIS( int v ) {
		return (v >= 0x20 && v <= 0x7E) || (v >= 0xA1 && v <= 0xDF);
	}

	public static boolean isValidIso8869_1( int v ) {
		return (v >= 0x20 && v <= 0x7E) || (v >= 0xA0 && v <= 0xFF);
	}

	/**
	 * ECI designator to character set. Take from ZXing. The easily available, and out of date, QR Code specification
	 * is missing this information as far as I can tell. The latest ISO is available for $50 on ISO's website.
	 */
	public static String getEciCharacterSet( int designator ) {
		switch( designator) {
			case 0:
			case 2: return "Cp437";
			case 1:
			case 3: return "ISO8859_1";
			case 4: return "ISO8859_2";
			case 5: return "ISO8859_3";
			case 6: return "ISO8859_4";
			case 7: return "ISO8859_5";
			case 8: return "ISO8859_6";
			case 9: return "ISO8859_7";
			case 10: return "ISO8859_8";
			case 11: return "ISO8859_9";
			case 12: return "ISO8859_10";
			case 13: return "ISO8859_11";
			case 14: return "ISO8859_12";
			case 15: return "ISO8859_13";
			case 16: return "ISO8859_14";
			case 17: return "ISO8859_15";
			case 18: return "ISO8859_16";
			case 20: return "SJIS";
			case 21: return "Cp1250";
			case 22: return "Cp1251";
			case 23: return "Cp1252";
			case 24: return "Cp1256";
			case 25: return "UnicodeBigUnmarked";
			case 26: return "UTF8";
			case 27:
			case 170: return "ASCII";
			case 28: return "Big5";
			case 29: return "GB18030";
			case 30: return "EUC_KR";
			default:
				throw new IllegalArgumentException("Unknown ECI designator "+designator);
		}
	}
}
