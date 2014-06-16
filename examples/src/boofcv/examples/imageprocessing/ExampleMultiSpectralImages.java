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

package boofcv.examples.imageprocessing;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;

/**
 * <p>
 * {@link MultiSpectral} images are one way in which color images can be stored and manipulated inside
 * of BoofCV.  Inside of a MultiSpectral image each color band is stored as an independent {@link boofcv.struct.image.ImageSingleBand}.
 * This is unlike the more common interleaved format where color information is stored in the same image.
 * </p>
 *
 * <p>
 * The main advantage of {@link MultiSpectral} is the ease at which gray scale operations can be applied to each
 * band independently with no additional code.  This is particularly useful in a library,
 * such as BoofCV, which is heavily focused on gray scale image processing and computer vision. The are also
 * situations for some scientific applications where processing each band independently makes more sense.
 * </p>
 *
 * @author Peter Abeles
 */
public class ExampleMultiSpectralImages {

	/**
	 * Many operations designed to only work on {@link boofcv.struct.image.ImageSingleBand} can be applied
	 * to a MultiSpectral image by feeding in each band one at a time.
	 */
	public static void independent( BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,true,ImageUInt8.class);

		// declare the output blurred image
		MultiSpectral<ImageUInt8> blurred =
				new MultiSpectral<ImageUInt8>(ImageUInt8.class,image.width,image.height,image.getNumBands());
		
		// Apply Gaussian blur to each band in the image
		for( int i = 0; i < image.getNumBands(); i++ ) {
			// note that the generalized version of BlurImageOps is not being used, but the type
			// specific version.
			BlurImageOps.gaussian(image.getBand(i),blurred.getBand(i),-1,5,null);
		}
		
		// Declare the BufferedImage manually to ensure that the color bands have the same ordering on input
		// and output
		BufferedImage output = new BufferedImage(image.width,image.height,input.getType());
		ConvertBufferedImage.convertTo(blurred, output,true);
		ShowImages.showWindow(input,"Input");
		ShowImages.showWindow(output,"Ouput");
	}

	/**
	 * Values of pixels can be read and modified by accessing the internal {@link boofcv.struct.image.ImageSingleBand}.
	 */
	public static void pixelAccess(  BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,true,ImageUInt8.class);

		int x = 10, y = 10;

		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Original "+i+" = "+image.getBand(i).get(x,y));

		// change the value in each band
		for( int i = 0; i < image.getNumBands(); i++ )
			image.getBand(i).set(x, y, 100 + i);

		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Result   "+i+" = "+image.getBand(i).get(x,y));
	}

	/**
	 * There is no real perfect way that everyone agrees on for converting color images into gray scale
	 * images.  Two examples of how to convert a MultiSpectral image into a gray scale image are shown 
	 * in this example.
	 */
	public static void convertToGray( BufferedImage input ) {
		// convert the BufferedImage into a MultiSpectral
		MultiSpectral<ImageUInt8> image = ConvertBufferedImage.convertFromMulti(input,null,true,ImageUInt8.class);
		
		ImageUInt8 gray = new ImageUInt8( image.width,image.height);
		
		// creates a gray scale image by averaging intensity value across pixels
		GPixelMath.averageBand(image, gray);
		BufferedImage outputAve = ConvertBufferedImage.convertTo(gray,null);

		// create an output image just from the first band
		BufferedImage outputBand0 = ConvertBufferedImage.convertTo(image.getBand(0),null);

		ShowImages.showWindow(outputAve,"Average");
		ShowImages.showWindow(outputBand0,"Band 0");
	}
	
	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage("../data/applet/apartment_building_02.jpg");

		// Uncomment lines below to run each example

		ExampleMultiSpectralImages.independent(input);
//		ExampleMultiSpectralImages.pixelAccess(input);
//		ExampleMultiSpectralImages.convertToGray(input);
	}
}
