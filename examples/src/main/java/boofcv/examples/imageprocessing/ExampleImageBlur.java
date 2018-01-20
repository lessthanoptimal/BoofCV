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

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;

/**
 * This example shows you can can apply different standard image blur filters to an input image using different
 * interface.  For repeat calls using the filter interface has some advantages, but the procedural can be
 * simple to code up.
 *
 * @author Peter Abeles
 */
public class ExampleImageBlur {

	public static void main(String[] args) {
		ListDisplayPanel panel = new ListDisplayPanel();
		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample("sunflowers.jpg"));

		panel.addImage(buffered,"Original");

		Planar<GrayU8> input = ConvertBufferedImage.convertFrom(buffered, true, ImageType.pl(3, GrayU8.class));
		Planar<GrayU8> blurred = input.createSameShape();

		// size of the blur kernel. square region with a width of radius*2 + 1
		int radius = 8;

		// Apply gaussian blur using a procedural interface
		GBlurImageOps.gaussian(input,blurred,-1,radius,null);
		panel.addImage(ConvertBufferedImage.convertTo(blurred, null, true),"Gaussian");

		// Apply a mean filter using an object oriented interface.  This has the advantage of automatically
		// recycling memory used in intermediate steps
		BlurFilter<Planar<GrayU8>> filterMean = FactoryBlurFilter.mean(input.getImageType(),radius);
		filterMean.process(input, blurred);
		panel.addImage(ConvertBufferedImage.convertTo(blurred, null, true),"Mean");

		// Apply a median filter using image type specific procedural interface.  Won't work if the type
		// isn't known at compile time
		BlurImageOps.median(input,blurred,radius);
		panel.addImage(ConvertBufferedImage.convertTo(blurred, null, true),"Median");

		ShowImages.showWindow(panel,"Image Blur Examples",true);
	}
}
