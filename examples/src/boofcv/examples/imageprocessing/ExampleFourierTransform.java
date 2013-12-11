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

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.InterleavedF32;

import java.awt.image.BufferedImage;

/**
 * Example demonstrating how to compute the Discrete Fourier Transform, visualize the transform, and apply
 * a filter frequency domain.
 *
 * @author Peter Abeles
 */
public class ExampleFourierTransform {

	/**
	 * Demonstration of how to apply a box filter in the frequency domain and compares the results
	 * to a box filter which has been applied in the spatial domain
	 */
	public static void applyBoxFilter( ImageFloat32 input ) {

		// declare storage
		ImageFloat32 boxImage = new ImageFloat32(input.width, input.height);
		InterleavedF32 boxTransform = new InterleavedF32(input.width,input.height,2);
		InterleavedF32 transform = new InterleavedF32(input.width,input.height,2);
		ImageFloat32 blurredImage = new ImageFloat32(input.width, input.height);
		ImageFloat32 spatialBlur = new ImageFloat32(input.width, input.height);

		DiscreteFourierTransform<ImageFloat32,InterleavedF32> dft =
				DiscreteFourierTransformOps.createTransformF32();


		// Make the image scaled from 0 to 1 to reduce overflow issues
		PixelMath.divide(input,255.0f,input);

		// compute the Fourier Transform
		dft.forward(input,transform);

		// create the box filter which is centered around the pixel.  Note that the filter gets wrapped around
		// the image edges
		for( int y = 0; y < 15; y++ ) {
			int yy = y-7 < 0 ? boxImage.height+(y-7) : y - 7;
			for( int x = 0; x < 15; x++ ) {
				int xx = x-7 < 0 ? boxImage.width+(x-7) : x - 7;
				// Set the value such that it doesn't change the image intensity
				boxImage.set(xx,yy,1.0f/(15*15));
			}
		}
		// compute the DFT for the box filter
		dft.forward(boxImage,boxTransform);

		// Visualize the Fourier Transform for the input image and the box filter
		displayTransform(transform,"Input Image");
		displayTransform(boxTransform,"Box Filter");

		// apply the filter. convolution in spacial domain is the same as multiplication in the frequency domain
		DiscreteFourierTransformOps.multiplyComplex(transform,boxTransform,transform);

		// convert the image back and display the results
		dft.inverse(transform,blurredImage);
		// undo change of scale
		PixelMath.multiply(blurredImage,255.0f,blurredImage);
		PixelMath.multiply(input,255.0f,input);

		// For sake of comparison, let's compute the box blur filter in the spatial domain
		// NOTE: The image border will be different since the frequency domain wraps around and this implementation
		// of the spacial domain adapts the kernel size
		BlurImageOps.mean(input,spatialBlur,7,null);

		// Convert to BufferedImage for output
		BufferedImage originOut = ConvertBufferedImage.convertTo(input, null);
		BufferedImage spacialOut = ConvertBufferedImage.convertTo(spatialBlur, null);
		BufferedImage blurredOut = ConvertBufferedImage.convertTo(blurredImage, null);

		ListDisplayPanel listPanel = new ListDisplayPanel();
		listPanel.addImage(originOut,"Original Image");
		listPanel.addImage(spacialOut,"Spacial Domain Box");
		listPanel.addImage(blurredOut,"Frequency Domain Box");

		ShowImages.showWindow(listPanel,"Box Blur in Spacial and Frequency Domain of Input Image");
	}

	/**
	 * Display the fourier transform's magnitude and phase.
	 */
	public static void displayTransform( InterleavedF32 transform , String name ) {

		// declare storage
		ImageFloat32 magnitude = new ImageFloat32(transform.width,transform.height);
		ImageFloat32 phase = new ImageFloat32(transform.width,transform.height);

		// Make a copy so that you don't modify the input
		transform = transform.clone();

		// shift the zero-frequency into the image center, as is standard in image processing
		DiscreteFourierTransformOps.shiftZeroFrequency(transform,true);

		// Compute the transform's magnitude and phase
		DiscreteFourierTransformOps.magnitude(transform,magnitude);
		DiscreteFourierTransformOps.phase(transform, phase);

		// Convert it to a log scale for visibility
		PixelMath.log(magnitude,magnitude);

		// Display the results
		BufferedImage visualMag = VisualizeImageData.grayMagnitude(magnitude, null, -1);
		BufferedImage visualPhase = VisualizeImageData.colorizeSign(phase, null, Math.PI);

		ImageGridPanel dual = new ImageGridPanel(1,2,visualMag,visualPhase);
		ShowImages.showWindow(dual,"Magnitude and Phase of "+name);
	}

	public static void main( String args[] ) {

		ImageFloat32 input = UtilImageIO.loadImage("../data/evaluation/standard/lena512.bmp", ImageFloat32.class);

		applyBoxFilter(input.clone());
	}
}
