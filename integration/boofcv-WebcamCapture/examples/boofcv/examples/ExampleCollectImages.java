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

package boofcv.examples;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * View images from a webcam and save them by clicking on the image with a mouse
 *
 * @author Peter Abeles
 */
public class ExampleCollectImages {

	public static volatile boolean saveImage = false;

	public static void main(String[] args) {

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = Webcam.getWebcams().get(0);
		UtilWebcamCapture.adjustResolution(webcam,640,480);
		webcam.open();

		// Create the panel used to display the image
		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(webcam.getViewSize());
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				saveImage = true;
			}
		});

		ShowImages.showWindow(gui,"Webcam",true);

		int total = 0;
		while( true ) {
			BufferedImage image = webcam.getImage();

			if( saveImage ) {
				System.out.println("Saving image "+total);
				saveImage = false;
				UtilImageIO.saveImage(image,String.format("image%04d.png",(total++)));
			}

			gui.setImageUI(image);
		}
	}
}
