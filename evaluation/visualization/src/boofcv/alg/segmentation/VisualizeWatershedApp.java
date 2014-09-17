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

package boofcv.alg.segmentation;

import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeWatershedApp {

	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/berkeley_horses.jpg");
		ImageUInt8 gray = new ImageUInt8(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFrom(image, gray);

		WatershedVincentSoille1991 alg = FactorySegmentationAlg.watershed(ConnectRule.FOUR);
		alg.process(gray);

		ImageSInt32 pixelToRegion = alg.getOutput();

		VisualizeRegions.watersheds(pixelToRegion,image,0);

		alg.removeWatersheds();
		int numRegions = alg.getTotalRegions();
		BufferedImage outRegions = VisualizeRegions.regions(pixelToRegion,numRegions,null);

		ShowImages.showWindow(image, "Watershed");
		ShowImages.showWindow(outRegions, "Regions");
	}
}
