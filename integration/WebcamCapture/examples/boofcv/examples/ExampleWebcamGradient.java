/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.BorderType;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.ImageFloat32;
import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Visualizes the image gradient in a video stream
 *
 * @author Peter Abeles
 */
public class ExampleWebcamGradient {

	public static void main(String[] args) {

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = UtilWebcamCapture.openDefault(640,480);

		// Create the panel used to display the image and
		ImagePanel gui = new ImagePanel();
		Dimension viewSize = webcam.getViewSize();
		gui.setPreferredSize(viewSize);

		// Predeclare storage for the gradient
		ImageFloat32 derivX = new ImageFloat32((int)viewSize.getWidth(),(int)viewSize.getHeight());
		ImageFloat32 derivY = new ImageFloat32((int)viewSize.getWidth(),(int)viewSize.getHeight());

		ShowImages.showWindow(gui,"Gradient");

		for(;;) {
			BufferedImage image = webcam.getImage();
			ImageFloat32 gray = ConvertBufferedImage.convertFrom(image,(ImageFloat32)null);

			// compute the gradient
			GImageDerivativeOps.sobel(gray, derivX, derivY, BorderType.EXTENDED);

			// visualize and display
			BufferedImage visualized = VisualizeImageData.colorizeGradient(derivX,derivY,-1);

			gui.setBufferedImageSafe(visualized);
		}
	}
}
