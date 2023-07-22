/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;

public class ExampleWebcamJavaCV {
	public static void main( String[] args ) {

		var webcam = new WebcamOpenCV();

		SimpleImageSequence<Planar<GrayU8>> sequence =
				webcam.open("0", 1280, 960, ImageType.pl(3, GrayU8.class));

		var output = new BufferedImage(
				sequence.getWidth(), sequence.getHeight(), BufferedImage.TYPE_INT_RGB);

		var gui = new ImagePanel(output);
		ShowImages.showWindow(gui, "Webam using JavaCV", true);

		while (sequence.hasNext()) {
			Planar<GrayU8> gray = sequence.next();
			ConvertBufferedImage.convertTo(gray, output, true);

			gui.repaint();
		}
	}
}
