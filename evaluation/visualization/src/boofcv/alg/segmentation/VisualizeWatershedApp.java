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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.segmentation.watershed.RemoveWatersheds;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class VisualizeWatershedApp {

	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/berkeley_horses.jpg");
		ImageUInt8 gray = new ImageUInt8(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFrom(image, gray);

		ImageMiscOps.fill(gray,255);
		gray.set(10, 15, 10);
		gray.set(100, 200, 50);
		gray.set(200,250,20);

		WatershedVincentSoille1991 alg = FactorySegmentationAlg.watershed(ConnectRule.FOUR);
		alg.process(gray);

		ImageSInt32 pixelToRegion = alg.getOutput();
		int numRegions = alg.getTotalRegions();

		RemoveWatersheds remove = new RemoveWatersheds();
		remove.remove(alg.getOutputBorder());
		numRegions--;

		BufferedImage outWatersheds = new BufferedImage(gray.width,gray.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage outRegions = new BufferedImage(gray.width,gray.height,BufferedImage.TYPE_INT_RGB);

		Random rand = new Random(234);
		int colors[] = new int[numRegions ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}
		colors[0] = 0xFF0000;

		for( int y = 0; y < gray.height; y++ ) {
			for( int x = 0; x < gray.width; x++ ) {
				int index = pixelToRegion.unsafe_get(x, y);
				if( index == 0 )
					outWatersheds.setRGB(x, y, 0xFF0000);
				else
					outWatersheds.setRGB(x, y, 0x00);
				outRegions.setRGB(x, y, colors[index]);
			}
		}

		ShowImages.showWindow(outWatersheds, "Watershed");
		ShowImages.showWindow(outRegions, "Regions");
	}
}
