/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to convert between different image types and visualize image data.
 *
 * @author Peter Abeles
 */
public class ExampleImageConvert {

	public static <T extends ImageBase, D extends ImageBase>
	void convertExample( BufferedImage input , Class<T> imageType , Class<D> derivType ) {
		// If the gray scale image has a pixel range that includes 0 to 255 then it can
		T gray = ConvertBufferedImage.convertFrom(input,null,imageType);

		// Can also pass in an image instead of having the function declare one each time
//		T gray = GeneralizedImageOps.createImage(imageType,input.getWidth(),input.getHeight());
//		ConvertBufferedImage.convertFrom(input,gray,imageType);

		D derivX = GeneralizedImageOps.createImage(derivType,input.getWidth(),input.getHeight());
		D derivY = GeneralizedImageOps.createImage(derivType,input.getWidth(),input.getHeight());

		GImageDerivativeOps.sobel(gray,derivX,derivY, BorderType.EXTENDED);

		// Gray scale images can be converted into buffered images for visualization purposes
		// The follow functions normalize the input by the maximum absolute value then either
		// show it as a gray scale image or a colorized image where different colors represent
		// positive and negative values
		BufferedImage grayX = VisualizeImageData.grayMagnitude(derivX,null,-1);
		BufferedImage colorX = VisualizeImageData.colorizeSign(derivX,null,-1);

		// If the input image's pixel are between 0 and 255 then it can be directly converted directly into
		// a buffered image efficiently
		BufferedImage grayBuff = ConvertBufferedImage.convertTo(gray,null);
	}

	/**
	 * Any BufferedImage can be converted quickly into a gray scale image.  The gray scale image must be capable of
	 * storing pixels with values from 0 to 255.
	 */
	public static <T extends ImageBase> void convertBufferedToGray( BufferedImage input , Class<T> imageType ) {
		// Type unknown at compile time using generics
		T gray = ConvertBufferedImage.convertFrom(input,null,imageType);

		// known Type
		ImageFloat32 gray_F32 = ConvertBufferedImage.convertFrom(input,null, ImageFloat32.class);

		// Into a previously declared image
		ConvertBufferedImage.convertFrom(input,gray_F32, ImageFloat32.class);
	}

	/**
	 * Gray scale images can be converted into a BufferedImage provided their pixels have a range of 0 to 255.
	 */
	public static <T extends ImageBase> void convertGrayToBuffered( T input , ImageFloat32 input_F32) {
		// The same function can convert any generic and specific types into a BufferedImage
		BufferedImage out1 = ConvertBufferedImage.convertTo(input,null);
		BufferedImage out2 = ConvertBufferedImage.convertTo(input_F32,null);

		// predeclared BufferedImages can also be used
		ConvertBufferedImage.convertTo(input,out1);
	}

	/**
	 * If the gray scale image has a range not between 0 and 255 visualize functions can be used instead to
	 * create a BufferedImage.
	 */
	public static <T extends ImageBase> void convertGrayToBuffered( T input ) {
		// This creates a gray scale image by taking the absolute value of each pixel and rescaling the values
		// to be between 0 and 255
		BufferedImage out = VisualizeImageData.grayMagnitude(input,null,-1);

		// A normalization value can also be specified and the output image too
		VisualizeImageData.grayMagnitude(input,out,600);

		// A color image can be created to show if a value is positive or negative
		BufferedImage out2 = VisualizeImageData.colorizeSign(input,null,-1);

		// Normalization value can be specified here too
		VisualizeImageData.colorizeSign(input,out2,-1);
	}

	/**
	 * Demonstration of how to convert a gray scale images from one type into another type when the type
	 * is known at compile time.
	 */
	public static void convertGrayToGray( ImageFloat32 input , ImageUInt8 output ) {
		// If the output type has a domain large enough to contain the input image values then directly converting
		// is possible
		ConvertImage.convert(input,output);

		// otherwise the input image will need to be rescaled to be in the domain of the output image
		PixelMath.abs(input,input);
		float max = PixelMath.max(input);
		PixelMath.multiply(input,input,255.0f/max);
		ConvertImage.convert(input,output);
	}

	/**
	 * It is also possible to convert between two gray scale images.  Just be sure that the pixel values are within
	 * range of each other.
	 */
	public static void convertGrayToGrayGeneric( ImageFloat32 input , ImageUInt8 output ) {
		// If the output type has a domain large enough to contain the input image values then directly converting
		// is possible
		ConvertImage.convert(input,output);

		// otherwise the input image will need to be rescaled to be in the domain of the output image
		PixelMath.abs(input,input);
		float max = PixelMath.max(input);
		PixelMath.multiply(input,input,255.0f/max);
		ConvertImage.convert(input,output);
	}

}
