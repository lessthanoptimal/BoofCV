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

package boofcv.examples.fiducial;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;

import java.io.UnsupportedEncodingException;

/**
 * When dealing with binary data embedded in a QR code things do get more complicated. You will need to convert
 * the string message into a byte array. By default, it's assumed that BYTE data is a text string and it will
 * encode it into a string. In general the conversion to a string will not modify the data but it's not defined
 * how illegal characters are encoded. To be safe you can force the encoding to be "raw".
 *
 * @author Peter Abeles
 */
public class ExampleQrCodeRawData {
	public static void main( String[] args ) throws UnsupportedEncodingException {
		// Let's generate some random data. In the real world this could be a zip file or similar
		byte[] originalData = new byte[500];
		for (int i = 0; i < originalData.length; i++) {
			originalData[i] = (byte)i;
		}

		// Generate a marking that's encoded with this raw data
		QrCode original = new QrCodeEncoder().addBytes(originalData).fixate();
		var gray = new QrCodeGeneratorImage(/* pixel per module */ 5).render(original).getGray();

		// Let's detect and then decode the image
		var config = new ConfigQrCode();
		// If you force the encoding to "raw" then you turn off auto encoding you know the bit values will not
		// be modified. It's undefined how illegal values are handled in different encodings, but often still work.
		config.forceEncoding = "raw";
		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(config, GrayU8.class);
		detector.process(gray);

		for (QrCode qr : detector.getDetections()) {
			// Only consider QR codes that are encoded with BYTE data
			if (qr.mode != QrCode.Mode.BYTE)
				continue;

			// Convert the message from a String to byte data. This only works if the 'raw' encoding is used
			byte[] data;
			if (qr.byteEncoding.equals("raw")) {
				data = BoofMiscOps.stringRawToByteArray(qr.message);
			} else {
				// If it thought the byte data was UTF-8 you need to decode with UTF-8 because a single character
				// can be multiple bytes.
				data = qr.message.getBytes(qr.byteEncoding);
			}

			// See if the data got corrupted or not
			boolean perfectMatch = data.length == originalData.length;
			for (int i = 0; i < data.length; i++) {
				perfectMatch &= data[i] == originalData[i];
			}

			// Print the results
			System.out.println("Encoding: '" + qr.byteEncoding + "' Matched: " + perfectMatch);
		}
	}
}
