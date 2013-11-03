/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ProcessImageSequence;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.BoofVideoManager;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoShowImageDerivative extends ProcessImageSequence<ImageUInt8> {

	ImageGradient<ImageUInt8, ImageSInt16> gradient;
	ImageSInt16 derivX;
	ImageSInt16 derivY;

	ImagePanel panelX;
	ImagePanel panelY;
	ImagePanel original;

	boolean drawGray = false;

	public VideoShowImageDerivative(SimpleImageSequence<ImageUInt8> sequence,
									ImageGradient<ImageUInt8, ImageSInt16> gradient) {
		super(sequence);

		this.gradient = gradient;
	}


	@Override
	public void processFrame(ImageUInt8 image) {

		if( derivX == null ) {
			derivX = new ImageSInt16(image.width,image.height);
			derivY = new ImageSInt16(image.width,image.height);
		}

		gradient.process(image,derivX,derivY);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, ImageUInt8 origImage) {

		if (panelX == null) {
			panelX = ShowImages.showWindow(derivX, "Derivative X-Axis");
			panelY = ShowImages.showWindow(derivY, "Derivative Y-Axis");
			original = ShowImages.showWindow( guiImage , "Original" );
			addComponent(panelX);
			addComponent(panelY);
		} else {
			original.setBufferedImage(guiImage);
			int maxX = ImageStatistics.maxAbs(derivX);
			int maxY = ImageStatistics.maxAbs(derivY);

			if( drawGray ) {
				VisualizeImageData.grayMagnitude(derivX,panelX.getImage(),maxX);
				VisualizeImageData.grayMagnitude(derivY,panelY.getImage(),maxY);
			} else {
				VisualizeImageData.colorizeSign(derivX,panelX.getImage(),maxX);
				VisualizeImageData.colorizeSign(derivY,panelY.getImage(),maxY);
			}
			panelX.repaint();
			panelY.repaint();
			original.repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if( e.getButton() == 1 )
			super.mouseClicked(e);
		else
			drawGray = !drawGray;
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "../data/applet/zoom.mjpeg";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = BoofVideoManager.loadManagerDefault().load(fileName, ImageType.single(ImageUInt8.class));

		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.sobel_I8();

		VideoShowImageDerivative display = new VideoShowImageDerivative(sequence, gradient);

		display.process();
	}
}
