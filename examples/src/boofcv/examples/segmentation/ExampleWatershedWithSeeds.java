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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Watershed image segmentation will often produce an excessive number of regions since each local minimum is the
 * seed which creates a new region.  To get around this problem you can provide the seeds manually.  The down side
 * to providing manual seeds is that it is no longer a general purpose algorithm and requires knowledge of the image
 * structure to provide the seeds.  This example demonstrates how to do this.
 *
 * @author Peter Abeles
 */
public class ExampleWatershedWithSeeds {
	public static void main(String[] args) {

		BufferedImage image = UtilImageIO.loadImage("../data/applet/particles01.jpg");
		ImageUInt8 input = ConvertBufferedImage.convertFromSingle(image, null, ImageUInt8.class);

		// declare working data
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		ImageSInt32 label = new ImageSInt32(input.width,input.height);

		// Try using the mean pixel value to create a binary image then erode it to separate the particles from
		// each other
		double mean = ImageStatistics.mean(input);
		ThresholdImageOps.threshold(input, binary, (int) mean, true);
		ImageUInt8 filtered = BinaryImageOps.erode8(binary, 2, null);
		int numRegions = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, label).size() + 1;
		// +1 to regions because contour only counts blobs and not the background

		// The labeled image can be used as is.  A precondition for seeded watershed is that all seeds have an
		// ID > 0.  Luckily, a value of 0 was used for background pixels in the contour algorithm.
		WatershedVincentSoille1991 watershed = FactorySegmentationAlg.watershed(ConnectRule.FOUR);

		watershed.process(input,label);

		ImageSInt32 output = watershed.getOutput();

		BufferedImage outLabeled = VisualizeBinaryData.renderLabeledBG(label, numRegions, null);
		VisualizeRegions.watersheds(output,image,1);

		// Removing the watersheds and update the region count
		// NOTE: watershed.getTotalRegions() does not return correct results if seeds are used!
		watershed.removeWatersheds();
		numRegions -= 1;
		BufferedImage outRegions = VisualizeRegions.regions(output,numRegions,null);

		ShowImages.showWindow(outLabeled,"Seeds");
		ShowImages.showWindow(outRegions,"Regions");
		ShowImages.showWindow(image,"Watersheds");

		// Additional processing would be needed for this example to be really useful.
		// The watersheds can be used to characterize the background while the seed binary image the particles
		// From this the particles could be more accurately classified by assigning each pixel one of the two
		// just mentioned groups based distance
	}
}
