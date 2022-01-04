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

import boofcv.abst.fiducial.MicroQrCodeDetector;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigMicroQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Shows you how to detect a Micro QR Code inside an image and process the extracted data. Much of the information that
 * is computed while detecting and decoding a QR Code is saved inside the {@link MicroQrCode} class. This can be useful
 * for application developers.
 *
 * @author Peter Abeles
 */
public class ExampleDetectMicroQrCode {
	public static void main( String[] args ) {
		BufferedImage input = UtilImageIO.loadImageNotNull(UtilIO.pathExample("fiducial/microqr/image01.jpg"));
		GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);

		var config = new ConfigMicroQrCode();
//		config.considerTransposed = false; // by default, it will consider incorrectly encoded markers. Faster if false
		MicroQrCodeDetector<GrayU8> detector = FactoryFiducial.microqr(config, GrayU8.class);

		detector.process(gray);

		// Gets a list of all the qr codes it could successfully detect and decode
		List<MicroQrCode> detections = detector.getDetections();

		Graphics2D g2 = input.createGraphics();
		int strokeWidth = Math.max(4, input.getWidth()/200); // in large images the line can be too thin
		g2.setColor(Color.GREEN);
		g2.setStroke(new BasicStroke(strokeWidth));
		for (MicroQrCode qr : detections) {
			// The message encoded in the marker
			System.out.println("message: '" + qr.message + "'");

			// Visualize its location in the image
			VisualizeShapes.drawPolygon(qr.bounds, true, 1, g2);
		}

		// List of objects it thinks might be a QR Code but failed for various reasons
		List<MicroQrCode> failures = detector.getFailures();
		g2.setColor(Color.RED);
		for (MicroQrCode qr : failures) {
			// If the 'cause' is ERROR_CORRECTION or later it might a real QR Code
			if (qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal())
				continue;

			VisualizeShapes.drawPolygon(qr.bounds, true, 1, g2);
		}

		ShowImages.showWindow(input, "Example Micro QR Codes", true);
	}
}
