/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.segmentation;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Demonstration of different techniques for automatic thresholding an image to create a binary image.  The binary
 * image can then be used for shape analysis and other applications.  Global methods apply the same threshold
 * to the entire image.  Local/adaptive methods compute a local threshold around each pixel and can handle uneven
 * lighting, but produce noisy results in regions with uniform lighting.
 *
 * @see boofcv.examples.imageprocessing.ExampleBinaryOps
 *
 * @author Peter Abeles
 */
public class ExampleThresholding {

	public static void threshold( String imageName ) {
		BufferedImage image = UtilImageIO.loadImage(imageName);

		// convert into a usable format
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);

		// Display multiple images in the same window
		ListDisplayPanel gui = new ListDisplayPanel();

		// Global Methods
		GThresholdImageOps.threshold(input, binary, ImageStatistics.mean(input), true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Global: Mean");
		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 256), true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Global: Otsu");
		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeEntropy(input, 0, 256), true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Global: Entropy");

		// Local method
		GThresholdImageOps.adaptiveSquare(input, binary, 28, 0, true, null, null);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Adaptive: Square");
		GThresholdImageOps.adaptiveGaussian(input, binary, 42, 0, true, null, null);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Adaptive: Gaussian");
		GThresholdImageOps.adaptiveSauvola(input, binary, 5, 0.30f, true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, null),"Adaptive: Sauvola");

		// Sauvola is tuned for text image.  Change radius to make it run better in others.

		// Show the image image for reference
		gui.addImage(ConvertBufferedImage.convertTo(input,null),"Input Image");

		String fileName =  imageName.substring(imageName.lastIndexOf('/')+1);
		ShowImages.showWindow(gui,fileName);
	}

	public static void main(String[] args) {
		// example in which global thresholding works best
		threshold("../data/applet/particles01.jpg");
		// example in which adaptive/local thresholding works best
		threshold("../data/applet/segment/uneven_lighting_squares.jpg");
		// hand written text with non-uniform stained background
		threshold("../data/applet/segment/stained_handwriting.jpg");
	}
}
