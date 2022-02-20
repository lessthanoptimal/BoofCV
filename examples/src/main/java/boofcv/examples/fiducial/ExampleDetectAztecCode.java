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

import boofcv.abst.fiducial.AztecCodePreciseDetector;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.factory.fiducial.ConfigAztecCode;
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
 * Shows you how to detect an Aztec Code inside an image. BoofCV provides a lot of information about the marker and
 * provides an accurate location of the finder pattern. For example, you can get the number of bit errors which
 * were encountered while reading and will return failed detections with the reason they failed.
 *
 * @author Peter Abeles
 */
public class ExampleDetectAztecCode {
	public static void main( String[] args ) {
		BufferedImage input = UtilImageIO.loadImageNotNull(UtilIO.pathExample("fiducial/aztec/image01.jpg"));
		GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);

		var config = new ConfigAztecCode();
//		config.considerTransposed = false; // by default, it will consider incorrectly encoded markers. Faster if false
		AztecCodePreciseDetector<GrayU8> detector = FactoryFiducial.aztec(config, GrayU8.class);

		detector.process(gray);

		// Gets a list of all the qr codes it could successfully detect and decode
		List<AztecCode> detections = detector.getDetections();

		// Print the encoded messages
		for (AztecCode marker : detections) {
			System.out.println("message: '" + marker.message + "'");
		}

		// Visualize the found markers in the image
		Graphics2D g2 = input.createGraphics();
		int strokeWidth = Math.max(4, input.getWidth()/200); // in large images the line can be too thin
		g2.setColor(Color.GREEN);
		g2.setStroke(new BasicStroke(strokeWidth));
		for (AztecCode marker : detections) {
			VisualizeShapes.drawPolygon(marker.bounds, true, 1, g2);
		}

		// List of objects it thinks might be a QR Code but failed for various reasons
		List<AztecCode> failures = detector.getFailures();
		g2.setColor(Color.RED);
		for (AztecCode marker : failures) {
			// If it failed to decode the mode then there's a decent change of it being a false negative
			if (marker.failure.ordinal() < AztecCode.Failure.MODE_ECC.ordinal())
				continue;

			VisualizeShapes.drawPolygon(marker.bounds, true, 1, g2);
		}

		ShowImages.showWindow(input, "Example Aztec Codes", true);
	}
}
