/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;


import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Demonstrates how to create and process binary images.
 *
 * @author Peter Abeles
 */
public class ExampleBinaryImage {

	/**
	 * Thresholds and displays the image.
	 */
	public static void binaryExample( BufferedImage image )
	{
		// convert into a usable format
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		float mean = (float)ImageStatistics.mean(input);

		// create a binary image
		ThresholdImageOps.threshold(input,binary,mean,true);

		// Render the binary image for output and display it in a window
		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binary,null);
		ShowImages.showWindow(visualBinary,"Binary Image");
	}

	/**
	 * Thresholds and extracts clusters of blobs from the image.
	 */
	public static void labeledExample( BufferedImage image )
	{
		// convert into a usable format
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		ImageSInt32 blobs = new ImageSInt32(input.width,input.height);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		float mean = (float)ImageStatistics.sum(input);

		// create a binary image
		ThresholdImageOps.threshold(input,binary,mean,true);

		// remove small blobs through erosion and dilation
		// The null in the input indicates that it should internally declare the work image it needs
		// this is less efficient, but easier to code.
		binary = BinaryImageOps.erode8(binary,null);
		binary = BinaryImageOps.dilate8(binary, null);

		// Detect blobs inside the binary image and assign labels to them
		int numBlobs = BinaryImageOps.labelBlobs4(binary,blobs);

		// Render the binary image for output and display it in a window
		BufferedImage visualized = VisualizeBinaryData.renderLabeled(blobs, numBlobs, null);
		ShowImages.showWindow(visualized,"Labeled Image");
	}


	public static void main( String args[] ) {
		// load and convert the image into a unable format
		BufferedImage image = UtilImageIO.loadImage("../data/applet/particles01.jpg");

		binaryExample(image);
		labeledExample(image);
	}

}
