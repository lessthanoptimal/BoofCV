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

import boofcv.abst.filter.derivative.ImageHessianDirect;
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

import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class VideoShowImageHessian extends ProcessImageSequence<ImageUInt8> {

	ImageHessianDirect<ImageUInt8, ImageSInt16> hessian;
	ImageSInt16 derivXX;
	ImageSInt16 derivYY;
	ImageSInt16 derivXY;

	ImagePanel panelXX;
	ImagePanel panelYY;
	ImagePanel panelXY;

	public VideoShowImageHessian(SimpleImageSequence<ImageUInt8> sequence,
									ImageHessianDirect<ImageUInt8, ImageSInt16> hessian) {
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
			int maxX = ImageStatistics.maxAbs(derivXX);
			int maxY = ImageStatistics.maxAbs(derivYY);
			int maxXY = ImageStatistics.maxAbs(derivXY);
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
			fileName = "../data/applet/zoom.mjpeg";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = BoofVideoManager.loadManagerDefault().load(fileName, ImageType.single(ImageUInt8.class));

		ImageHessianDirect<ImageUInt8, ImageSInt16> hessian = FactoryDerivative.hessianDirectSobel_I8();

		VideoShowImageHessian display = new VideoShowImageHessian(sequence, hessian);

		display.process();
	}
}
