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

import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeEncoder;
import boofcv.alg.fiducial.microqr.MicroQrCodeGenerator;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;

/**
 * A simple API is provided for creating your own Micro QR Code. Used extensively in BoofCV for testing purposes.
 * It's also easy to extending the rendering tools to support other file formats.
 *
 * @author Peter Abeles
 * @see boofcv.alg.fiducial.microqr.MicroQrCodeGenerator
 */
public class ExampleRenderMicroQrCode {
	public static void main( String[] args ) {
		// Uses a flow pattern to specify the QR Code. You can control all aspects of the Micro QR
		// like specifying the version, mask, and message types or let it select all of that for you.
		MicroQrCode qr = new MicroQrCodeEncoder()
//				.setError(MicroQrCode.ErrorLevel.L) <-- 99% you should let it automatically select
				.addAutomatic("Test ん鞠").fixate();
		// NOTE: The final function you call must be fixate(), that's how it knows it's done

		// Render the QR as an image. It's also possible to render as a PDF or your own custom format
		GrayU8 rendered = MicroQrCodeGenerator.renderImage(/* pixel per module */ 10, /* border modules*/ 1, qr);

		// Convert it to a BufferedImage for display purposes
		BufferedImage output = ConvertBufferedImage.convertTo(rendered, null);

		// You can also save it to disk by uncommenting the line below
//		UtilImageIO.saveImage(output, "microqr.png");

		// Display the image
		ShowImages.showWindow(output, "Rendered Micro QR Code", true);
	}
}
