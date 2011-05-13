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
import gecv.abst.filter.derivative.HessianDirectXY;
import gecv.alg.drawing.PixelMath;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoShowImageHessian extends ProcessImageSequence<ImageUInt8> {

	HessianDirectXY<ImageUInt8, ImageSInt16> hessian;
	ImageSInt16 derivXX;
	ImageSInt16 derivYY;
	ImageSInt16 derivXY;

	ImagePanel panelXX;
	ImagePanel panelYY;
	ImagePanel panelXY;

	public VideoShowImageHessian(SimpleImageSequence<ImageUInt8> sequence,
									HessianDirectXY<ImageUInt8, ImageSInt16> hessian) {
		super(sequence);

		this.hessian = hessian;
	}


	@Override
	public void processFrame(ImageUInt8 image) {

		if( derivXX == null ) {
			derivXX = new ImageSInt16(image.width,image.height);
			derivYY = new ImageSInt16(image.width,image.height);
			derivXY = new ImageSInt16(image.width,image.height);
		}

		hessian.process(image, derivXX, derivYY,derivXY);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, ImageUInt8 origImage) {

		if (panelXX == null) {
			panelXX = ShowImages.showWindow(derivXX, "Derivative XX");
			panelYY = ShowImages.showWindow(derivYY, "Derivative YY");
			panelXY = ShowImages.showWindow(derivXY, "Derivative XY");
			addComponent(panelXX);
			addComponent(panelYY);
			addComponent(panelXY);
		} else {
			int maxX = PixelMath.maxAbs(derivXX);
			int maxY = PixelMath.maxAbs(derivYY);
			int maxXY = PixelMath.maxAbs(derivXY);
			VisualizeImageData.colorizeSign(derivXX, panelXX.getImage(),maxX);
			VisualizeImageData.colorizeSign(derivYY, panelYY.getImage(),maxY);
			VisualizeImageData.colorizeSign(derivXY, panelXY.getImage(),maxXY);
			panelXX.repaint();
			panelYY.repaint();
			panelXY.repaint();
		}
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "/home/pja/uav_video.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageUInt8.class);

		HessianDirectXY<ImageUInt8, ImageSInt16> hessian = FactoryDerivative.hessianDirectSobel_I8();

		VideoShowImageHessian display = new VideoShowImageHessian(sequence, hessian);

		display.process();
	}
}
