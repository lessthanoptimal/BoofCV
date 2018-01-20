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

import boofcv.alg.color.ColorRgb;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;

/**
 * <p>
 * {@link Planar} images are one way in which color images can be stored and manipulated inside
 * of BoofCV.  Inside of a Planar image each color band is stored as an independent {@link ImageGray}.
 * This is unlike the more common interleaved format where color information is stored in adjacent bytes in
 * the same image.
 * </p>
 *
 * <p>
 * The main advantage of {@link Planar} is the ease at which gray scale operations can be applied to each
 * band independently with no additional code.  This is particularly useful in a library,
 * such as BoofCV, which is heavily focused on gray scale image processing and computer vision. The are also
 * situations for some scientific applications where processing each band independently makes more sense.
 * </p>
 *
 * @author Peter Abeles
 */
public class ExamplePlanarImages {

	public static ListDisplayPanel gui = new ListDisplayPanel();

	/**
	 * Many operations designed to only work on {@link ImageGray} can be applied
	 * to a Planar image by feeding in each band one at a time.
	 */
	public static void independent( BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(input,null,true,GrayU8.class);

		// declare the output blurred image
		Planar<GrayU8> blurred = image.createSameShape();
		
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

		gui.addImage(input,"Input");
		gui.addImage(output,"Gaussian Blur");
	}

	/**
	 * Values of pixels can be read and modified by accessing the internal {@link ImageGray}.
	 */
	public static void pixelAccess(  BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(input,null,true,GrayU8.class);

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
	 * images.  Two examples of how to convert a Planar image into a gray scale image are shown
	 * in this example.
	 */
	public static void convertToGray( BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(input,null,true,GrayU8.class);

		GrayU8 gray = new GrayU8( image.width,image.height);

		// creates a gray scale image by averaging intensity value across pixels
		GPixelMath.averageBand(image, gray);
		BufferedImage outputAve = ConvertBufferedImage.convertTo(gray,null);

		// convert to gray scale but weigh each color band based on how human vision works
		ColorRgb.rgbToGray_Weighted(image, gray);
		BufferedImage outputWeighted = ConvertBufferedImage.convertTo(gray,null);

		// create an output image just from the first band
		BufferedImage outputBand0 = ConvertBufferedImage.convertTo(image.getBand(0),null);

		gui.addImage(outputAve,"Gray Averaged");
		gui.addImage(outputWeighted,"Gray Weighted");
		gui.addImage(outputBand0,"Band 0");
	}
	
	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(UtilIO.pathExample("apartment_building_02.jpg"));

		// Uncomment lines below to run each example

		ExamplePlanarImages.independent(input);
		ExamplePlanarImages.pixelAccess(input);
		ExamplePlanarImages.convertToGray(input);

		ShowImages.showWindow(gui,"Color Planar Image Examples",true);
	}
}
