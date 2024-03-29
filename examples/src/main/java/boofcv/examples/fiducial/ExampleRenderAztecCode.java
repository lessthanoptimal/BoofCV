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

import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecEncoder;
import boofcv.alg.fiducial.aztec.AztecGenerator;
import boofcv.alg.fiducial.microqr.MicroQrCodeGenerator;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;

/**
 * An easy-to-use API is provided for creating your own Aztec Code marker. You can render it as an image or PDF
 * document.
 *
 * @author Peter Abeles
 * @see MicroQrCodeGenerator
 */
public class ExampleRenderAztecCode {
	public static void main( String[] args ) {
		// Create a marker to render. Almost everything about how the marker is constructed can be manually specified
		// or you can let it automatically select everything
		AztecCode marker = new AztecEncoder().addAutomatic("Code 2D!").fixate();
		// NOTE: The final function you call must be fixate(), that's how it knows it's done

		// Render the marker as an image. It's also possible to render as a PDF or your own custom format
		GrayU8 rendered = AztecGenerator.renderImage(/* pixel per square */ 10, /* border squares */ 1, marker);

		// Convert it to a BufferedImage for display purposes
		BufferedImage output = ConvertBufferedImage.convertTo(rendered, null);

		// You can also save it to disk by uncommenting the line below
//		UtilImageIO.saveImage(output, "aztec.png");

		// Display the rendered marker
		ShowImages.showWindow(output, "Rendered Aztec Code", true);
	}
}
