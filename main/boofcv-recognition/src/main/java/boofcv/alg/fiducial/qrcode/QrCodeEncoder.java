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
 * Used to create QR Codes given a message
 *
 * @author Peter Abeles
 */
public class QrCodeEncoder {

	QrCode qr = new QrCode();

	public QrCodeEncoder() {
		qr.version = -1;
	}

	public void setVersion(int version ) {
		qr.version = version;
	}
	
	public QrCode encodeNumeric( int[] numbers ) {
		int versionBits;

		if (qr.version < 10)
			versionBits = 10;
		else if (qr.version < 27)
			versionBits = 12;
		else
			versionBits = 14;

		int totalBits = versionBits + numbers.length;

//
//
//		int count = take_bits(ds, versionBits);
//		if (data->payload_len + count + 1 > QUIRC_MAX_PAYLOAD)
//			return QUIRC_ERROR_DATA_OVERFLOW;
//
//		while (count >= 3) {
//			if (numeric_tuple(data, ds, 10, 3) < 0)
//				return QUIRC_ERROR_DATA_UNDERFLOW;
//			count -= 3;
//		}
//
//		if (count >= 2) {
//			if (numeric_tuple(data, ds, 7, 2) < 0)
//				return QUIRC_ERROR_DATA_UNDERFLOW;
//			count -= 2;
//		}
//
//		if (count) {
//			if (numeric_tuple(data, ds, 4, 1) < 0)
//				return QUIRC_ERROR_DATA_UNDERFLOW;
//			count--;
//		}
//
//		return QUIRC_SUCCESS;

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

}
