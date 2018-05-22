/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;

import java.awt.image.BufferedImage;

/**
 * A simple API is provided for creating your own QR Codes. Used extensively in BoofCV for testing purposes.
 * It's also easy to extending the rendering tools to support other file formats.
 *
 * @see boofcv.alg.fiducial.qrcode.QrCodeGenerator
 *
 * @author Peter Abeles
 */
public class ExampleRenderQrCode
{
	public static void main(String[] args) {

		// Uses a flow pattern to specify the QR Code. You can control all aspects of the QR
		// like specifying the version, mask, and message types or let it select all of that for you.
		QrCode qr = new QrCodeEncoder().
				setError(QrCode.ErrorLevel.M).
				addAutomatic("This is a Test ん鞠").fixate();
		// NOTE: The final function you call must be fixate() that's how it knows its done

		// QrCodeGenerator is the base class with all the logic and the children tell it how to
		// write in a specific format. QrCodeGeneratorImage is included with BoofCV and is used
		// to creates images
		QrCodeGeneratorImage render = new QrCodeGeneratorImage(20);

		render.render(qr);

		// Convert it to a BufferedImage for display purposes
		BufferedImage image = ConvertBufferedImage.convertTo(render.getGray(),null);
		ShowImages.showWindow(image,"Rendered QR Code", true);

		// You can also save it to disk by uncommenting the line below
//		UtilImageIO.saveImage(image,"qrcode.png");
	}
}
