/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Simple example showing you how to thin a binary image.  This is also known as skeletonalization.  Thinning
 * discards most of objects foreground (value one) pixels are leaves behind a "skinny" object which still
 * mostly describes the original objects shape.
 *
 * @author Peter Abeles
 */
public class ExampleMorphologicalThinning {
	public static void main(String[] args) {
		// load and convert the image into a usable format
		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("standard/fingerprint.jpg"));

		// convert into a usable format
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);

		// Adaptive threshold to better handle changes in local image intensity
		GThresholdImageOps.adaptiveGaussian(input,binary,10,0,true,null,null);

		// Tell it to thin the image until there are no more changes
		ImageUInt8 thinned = BinaryImageOps.thin(binary, -1, null);

		// display the results
		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binary, false, null);
		BufferedImage visualThinned = VisualizeBinaryData.renderBinary(thinned, false, null);

		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addImage(visualThinned, "Thinned");
		panel.addImage(visualBinary, "Binary");
		panel.addImage(image, "Original");

		ShowImages.showWindow(panel,"Thinned/Skeletonalized Image", true);
	}
}
