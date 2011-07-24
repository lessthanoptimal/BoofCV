/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.derivative;

import gecv.abst.filter.derivative.FactoryDerivative;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.misc.PixelMath;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

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
			int maxX = PixelMath.maxAbs(derivX);
			int maxY = PixelMath.maxAbs(derivY);

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
			fileName = "/home/pja/uav_video.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageUInt8.class);

		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.sobel_I8();

		VideoShowImageDerivative display = new VideoShowImageDerivative(sequence, gradient);

		display.process();
	}
}
