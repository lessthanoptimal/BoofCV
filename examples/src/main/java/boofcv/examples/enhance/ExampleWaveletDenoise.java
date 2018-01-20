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

package boofcv.examples.enhance;

import boofcv.abst.denoise.FactoryImageDenoise;
import boofcv.abst.denoise.WaveletDenoiseFilter;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;

import java.util.Random;

/**
 * Example of how to "remove" noise from images using wavelet based algorithms.  A simplified interface is used
 * which hides most of the complexity.  Wavelet image processing is still under development and only floating point
 * images are currently supported.  Which is why the image  type is hard coded.
 */
public class ExampleWaveletDenoise {

	public static void main( String args[] ) {

		// load the input image, declare data structures, create a noisy image
		Random rand = new Random(234);
		GrayF32 input = UtilImageIO.loadImage(UtilIO.pathExample("standard/lena512.jpg"),GrayF32.class);

		GrayF32 noisy = input.clone();
		GImageMiscOps.addGaussian(noisy, rand, 20, 0, 255);
		GrayF32 denoised = noisy.createSameShape();

		// How many levels in wavelet transform
		int numLevels = 4;
		// Create the noise removal algorithm
		WaveletDenoiseFilter<GrayF32> denoiser =
				FactoryImageDenoise.waveletBayes(GrayF32.class,numLevels,0,255);

		// remove noise from the image
		denoiser.process(noisy,denoised);

		// display the results
		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(ConvertBufferedImage.convertTo(input,null),"Input");
		gui.addImage(ConvertBufferedImage.convertTo(noisy,null),"Noisy");
		gui.addImage(ConvertBufferedImage.convertTo(denoised,null),"Denoised");

		ShowImages.showWindow(gui,"Wavelet Noise Removal Example",true);
	}
}
