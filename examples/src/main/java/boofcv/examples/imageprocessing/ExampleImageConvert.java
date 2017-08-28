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

package boofcv.examples.imageprocessing;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to convert between different BoofCV image types.
 *
 * @author Peter Abeles
 */
public class ExampleImageConvert {

	// image loaded from a file
	BufferedImage image;
	// gray scale image with element values from 0 to 255
	GrayU8 gray;
	// Derivative of gray image.  Elements are 16-bit signed integers
	GrayS16 derivX,derivY;

	void convert() {
		// Converting between BoofCV image types is easy with ConvertImage.  ConvertImage copies
		// the value of a pixel in one image into another image.  When doing so you need to take
		// in account the storage capabilities of these different class types.

		// Going from an unsigned 8-bit image to unsigned 16-bit image is no problem
		GrayU16 imageU16 = new GrayU16(gray.width,gray.height);
		ConvertImage.convert(gray,imageU16);

		// You can convert back into the 8-bit image from the 16-bit image with no problem
		// in this situation because imageU16 does not use the full range of 16-bit values
		ConvertImage.convert(imageU16,gray);

		// Here is an example where you over flow the image after converting
		// There won't be an exception or any error messages but the output image will be corrupted
		GrayU8 imageBad = new GrayU8(derivX.width,derivX.height);
		ConvertImage.convert(derivX,imageBad);

		// One way to get around this problem rescale and adjust the pixel values so that they
		// will be within a valid range.
		GrayS16 scaledAbs = new GrayS16(derivX.width,derivX.height);
		GPixelMath.abs(derivX,scaledAbs);
		GPixelMath.multiply(scaledAbs, 255.0 / ImageStatistics.max(scaledAbs), scaledAbs);

		// If you just want to see the values of a 16-bit image there are built in utility functions
		// for visualizing their values too
		BufferedImage colorX = VisualizeImageData.colorizeSign(derivX, null, -1);

		// Let's see what all the bad image looks like
		// ConvertBufferedImage is similar to ImageConvert in that it does a direct coversion with out
		// adjusting the pixel's value
		BufferedImage outBad = new BufferedImage(imageBad.width,imageBad.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage outScaled = new BufferedImage(imageBad.width,imageBad.height,BufferedImage.TYPE_INT_RGB);

		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addImage(ConvertBufferedImage.convertTo(scaledAbs,outScaled),"Scaled");
		panel.addImage(colorX,"Visualized");
		panel.addImage(ConvertBufferedImage.convertTo(imageBad,outBad),"Bad");
		ShowImages.showWindow(panel,"Image Convert",true);
	}

	/**
	 * Load and generate images
	 */
	public void createImages() {
		image = UtilImageIO.loadImage(UtilIO.pathExample("standard/barbara.jpg"));

		gray = ConvertBufferedImage.convertFromSingle(image, null, GrayU8.class);
		derivX = GeneralizedImageOps.createSingleBand(GrayS16.class, gray.getWidth(), gray.getHeight());
		derivY = GeneralizedImageOps.createSingleBand(GrayS16.class, gray.getWidth(), gray.getHeight());

		GImageDerivativeOps.gradient(DerivativeType.SOBEL, gray, derivX, derivY, BorderType.EXTENDED);
	}

	public static void main( String args[] ) {
		ExampleImageConvert app = new ExampleImageConvert();

		app.createImages();
		app.convert();
	}
}
