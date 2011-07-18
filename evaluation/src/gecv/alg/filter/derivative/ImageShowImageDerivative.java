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
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Displays detected corners in a video sequence
 *
 * @author Peter Abeles
 */
public class ImageShowImageDerivative {

	ImageGradient<ImageUInt8, ImageSInt16> gradient;
	ImageSInt16 derivX;
	ImageSInt16 derivY;

	ImagePanel panelX;
	ImagePanel panelY;

	public ImageShowImageDerivative(ImageGradient<ImageUInt8, ImageSInt16> gradient) {
		this.gradient = gradient;
	}


	public void process(ImageUInt8 image) {

		if( derivX == null ) {
			derivX = new ImageSInt16(image.width,image.height);
			derivY = new ImageSInt16(image.width,image.height);
		}

		gradient.process(image,derivX,derivY);
		ShowImages.showWindow(image, "Input");
		panelX = ShowImages.showWindow(derivX, "Derivative X-Axis");
		panelY = ShowImages.showWindow(derivY, "Derivative Y-Axis");
	}

	public static void main(String args[]) {
		String fileName;

		if (args.length == 0) {
			fileName = "evaluation/data/indoors01.jpg";
		} else {
			fileName = args[0];
		}
		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(input,(ImageUInt8)null);
//		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.sobel_I8();
//		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.three_I8();
		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.gaussian_I8(1);

		ImageShowImageDerivative display = new ImageShowImageDerivative(gradient);

		display.process(gray);
	}
}
