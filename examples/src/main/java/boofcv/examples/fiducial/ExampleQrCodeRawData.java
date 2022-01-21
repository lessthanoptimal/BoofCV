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

/**
 * When dealing with raw data embedded in a QR code things do get more complicated. You will need to convert
 * the string message into a byte array. There is also the possibility that BoofCV incorrectly determined how
 * the BYTE data was encoded. There is no standard that people follow so BoofCV does its best job to guess
 * the encoding by looking at the bit format.
 *
 * @author Peter Abeles
 */
public class ExampleQrCodeRawData {
	public static void main( String[] args ) {
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
		// If you force the encoding to "raw" then you turn off the logic where it tries to determine the format
		// This will make it impossible for BoofCV to corrupt the data. 99% of the time you don't need to do this...
		config.forceEncoding = "raw";
		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(config, GrayU8.class);
		detector.process(gray);

		for (QrCode qr : detector.getDetections()) {
			// Convert the message from a String to byte data. This only works if the 'raw' encoding is used
			byte[] found = BoofMiscOps.stringToByteArray(qr.message);

			// See if the data got corrupted or not
			boolean perfectMatch = found.length == originalData.length;
			for (int i = 0; i < found.length; i++) {
				perfectMatch &= found[i] == originalData[i];
			}

			// Print the results
			System.out.println("Matched: " + perfectMatch);
		}
	}
}
