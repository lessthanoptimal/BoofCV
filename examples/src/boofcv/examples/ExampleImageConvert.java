/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to convert between different image types.
 *
 * @author Peter Abeles
 */
public class ExampleImageConvert {

	/**
	 * Computes the image derivative to demonstrate how to convert to and from BufferedImages.  When a gray scale
	 * image hsa intensity values from 0 to 255 then the faster operations inside of ConvertBufferedImage can be used.
	 * Otherwise alternative techniques (shown below) are used which rescale the image.
	 *
	 * @param input Input BufferedImage.
	 * @param imageType Image type for work image.
	 * @param derivType Image type for image derivative.
	 */
	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	void convertBufferedImage(BufferedImage input, Class<T> imageType, Class<D> derivType) {
		// If the gray scale image has a pixel range that includes 0 to 255 then it can
		T gray = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		// Can also pass in an image instead of having the function declare one each time
		// T gray = GeneralizedImageOps.createSingleBand(imageType,input.getWidth(),input.getHeight());
		// ConvertBufferedImage.convertFromSingle(input,gray,imageType);

		D derivX = GeneralizedImageOps.createSingleBand(derivType, input.getWidth(), input.getHeight());
		D derivY = GeneralizedImageOps.createSingleBand(derivType, input.getWidth(), input.getHeight());

		GImageDerivativeOps.sobel(gray,derivX,derivY, BorderType.EXTENDED);

		// Gray scale images can be converted into buffered images for visualization.
		// The follow functions normalize the input by the maximum absolute value then either
		// show it as a gray scale image or a colorized image where different colors represent
		// positive and negative values
		BufferedImage grayX = VisualizeImageData.grayMagnitude(derivX,null,-1);
		BufferedImage colorX = VisualizeImageData.colorizeSign(derivX,null,-1);

		// alternatively the derivative could be rescaled and then convert into a BufferedImage
		GPixelMath.abs(derivX,derivX);
		GPixelMath.multiply(derivX,derivX,255/GPixelMath.max(derivX));
		BufferedImage scaledX = ConvertBufferedImage.convertTo(derivX,null);

		// If the input image's pixel are between 0 and 255 then it can be directly converted directly into
		// a buffered image efficiently
		BufferedImage grayBuff = ConvertBufferedImage.convertTo(gray,null);

		// display the results
		ShowImages.showWindow(grayX,"X-derivative Gray");
		ShowImages.showWindow(colorX,"X-derivative Color");
		ShowImages.showWindow(scaledX,"X-derivative Scaled");
		ShowImages.showWindow(grayBuff,"Input Image");
	}

	// todo example showing  ConvertImage
	// edge detect -> uint8 -> corner detect


	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage("data/standard/barbara.png");

//		convertBufferedImage(image, ImageFloat32.class, ImageFloat32.class);
		convertBufferedImage(image, ImageUInt8.class, ImageSInt16.class);
	}

}
