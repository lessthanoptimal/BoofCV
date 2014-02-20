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

import boofcv.abst.segmentation.ImageSegmentation;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.ConfigFh04;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Example demonstrating high level image segmentation interface.  An image segmented using this
 * interface will have each pixel assigned a unique label from 0 to N-1, where N is the number of regions.  All
 * pixels which belong to the same region are connected.
 *
 * @author Peter Abeles
 */
public class ExampleImageSegmentation {

	/**
	 * Segments and visualizes the image
	 */
	public static <T extends ImageBase> void performSegmentation( ImageSegmentation<T> alg ,
																  T color )
	{
		// Segmentation often works better after blurring the image.  Reduces high frequency image components which
		// can cause over segmentation
		GBlurImageOps.gaussian(color, color, 0.5, -1, null);

		// Storage for segmented image.  Each pixel will be assigned a label from 0 to N-1, where N is the number
		// of segments in the image
		ImageSInt32 pixelToSegment = new ImageSInt32(color.width,color.height);

		// Segmentation magic happens here
		alg.segment(color,pixelToSegment);

		// Displays the results
		visualize(pixelToSegment,color,alg.getTotalSegments());
	}

	/**
	 * Visualizes results three ways.  1) Colorized segmented image where each region is given a random color.
	 * 2) Each pixel is assigned the mean color through out the region. 3) Black pixels represent the border
	 * between regions.
	 */
	public static <T extends ImageBase> void visualize( ImageSInt32 pixelToRegion ,
														T color ,
														int numSegments  ) {

		// Declare output buffered images
		BufferedImage outColor = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage outSegments = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage outBorder = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);

		// Computes the mean color inside each region
		ImageType<T> type = color.getImageType();
		ComputeRegionMeanColor<T> colorize = FactorySegmentationAlg.regionMeanColor(type);

		FastQueue<float[]> segmentColor = new ColorQueue_F32(type.getNumBands());
		segmentColor.resize(numSegments);

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.resize(numSegments);

		ImageSegmentationOps.countRegionPixels(pixelToRegion, numSegments, regionMemberCount.data);
		colorize.process(color,pixelToRegion,regionMemberCount,segmentColor);

		// Select random colors for each region
		Random rand = new Random(234);
		int colors[] = new int[ segmentColor.size ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		// Assign colors to each pixel depending on their region for mean color image and random segment color image
		for( int y = 0; y < color.height; y++ ) {
			for( int x = 0; x < color.width; x++ ) {
				int index = pixelToRegion.unsafe_get(x,y);
				float []cv = segmentColor.get(index);

				int r,g,b;

				if( cv.length == 3 ) {
					r = (int)cv[0];
					g = (int)cv[1];
					b = (int)cv[2];
				} else {
					r = g = b = (int)cv[0];
				}

				int rgb = r << 16 | g << 8 | b;

				outColor.setRGB(x, y, colors[index]);
				outSegments.setRGB(x, y, rgb);
			}
		}

		// Make edges appear black
		ConvertBufferedImage.convertTo(color,outBorder,true);
		ImageUInt8 binary = new ImageUInt8(pixelToRegion.width,pixelToRegion.height);
		ImageSegmentationOps.markRegionBorders(pixelToRegion,binary);
		for( int y = 0; y < binary.height; y++ ) {
			for( int x = 0; x < binary.width; x++ ) {
				if( binary.unsafe_get(x,y) == 1 )  {
					outBorder.setRGB(x,y,0);
				}
			}
		}

		// Show the visualization results
		ShowImages.showWindow(outColor, "Regions");
		ShowImages.showWindow(outSegments,"Color of Segments");
		ShowImages.showWindow(outBorder,"Region Borders");
	}

	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/berkeley_horses.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/berkeley_kangaroo.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/berkeley_man.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/mountain_pines_people.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/particles01.jpg");

		// Select input image type.  Some algorithms behave different depending on image type
		ImageType<MultiSpectral<ImageFloat32>> imageType = ImageType.ms(3,ImageFloat32.class);
//		ImageType<MultiSpectral<ImageUInt8>> imageType = ImageType.ms(3,ImageUInt8.class);
//		ImageType<ImageFloat32> imageType = ImageType.single(ImageFloat32.class);
//		ImageType<ImageUInt8> imageType = ImageType.single(ImageUInt8.class);

//		ImageSegmentation alg = FactoryImageSegmentation.meanShift(null, imageType);
//		ImageSegmentation alg = FactoryImageSegmentation.slic(new ConfigSlic(800), imageType);
		ImageSegmentation alg = FactoryImageSegmentation.fh04(new ConfigFh04(100,30), imageType);
//		ImageSegmentation alg = FactoryImageSegmentation.watershed(ConnectRule.EIGHT);

		// Convert image into BoofCV format
		ImageBase color = imageType.createImage(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image, color, true);

		// Segment and display results
		performSegmentation(alg,color);
	}
}
