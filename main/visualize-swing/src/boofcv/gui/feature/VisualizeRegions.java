/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.feature;

import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;

/**
 * Code for visualizing regions and superpixels
 *
 * @author Peter Abeles
 */
public class VisualizeRegions {

	/**
	 * Sets the pixels of each watershed as red in the output image.  Watersheds have a value of 0
	 * @param segments Conversion from pixel to region
	 * @param output Storage for output image.  Can be null.
	 * @param radius Thickness of watershed.  0 is 1 pixel wide. 1 is 3 pixels wide.
	 * @return Output image.
	 */
	public static BufferedImage watersheds(GrayS32 segments , BufferedImage output , int radius ) {
		if( output == null )
			output = new BufferedImage(segments.width,segments.height,BufferedImage.TYPE_INT_RGB);

		if( radius <= 0 ) {
			for (int y = 0; y < segments.height; y++) {
				for (int x = 0; x < segments.width; x++) {
					int index = segments.unsafe_get(x, y);
					if (index == 0)
						output.setRGB(x, y, 0xFF0000);
				}
			}
		} else {
			for (int y = 0; y < segments.height; y++) {
				for (int x = 0; x < segments.width; x++) {
					int index = segments.unsafe_get(x, y);
					if (index == 0) {
						for (int i = -radius; i <= radius; i++) {
							int yy = y + i;
							for (int j = -radius; j <= radius; j++) {
								int xx = x + j;

								if (segments.isInBounds(xx, yy)) {
									output.setRGB(xx, yy, 0xFF0000);
								}
							}
						}
					}
				}
			}
		}

		return output;
	}

	/**
	 * Draws each region with a random color
	 * @param pixelToRegion Conversion from pixel to region
	 * @param numRegions Total number of regions.
	 * @param output Storage for output image.  Can be null.
	 * @return Output image.
	 */
	public static BufferedImage regions(GrayS32 pixelToRegion , int numRegions , BufferedImage output ) {
		return VisualizeBinaryData.renderLabeled(pixelToRegion,numRegions,output);
	}

	/**
	 * Draws each region using the provided color
	 * @param pixelToRegion Conversion from pixel to region
	 * @param segmentColor Color of each region
	 * @param output Storage for output image.  Can be null.
	 * @return Output image.
	 */
	public static BufferedImage regionsColor( GrayS32 pixelToRegion ,
											  FastQueue<float[]> segmentColor ,
											  BufferedImage output ) {
		if( output == null )
			output = new BufferedImage(pixelToRegion.width,pixelToRegion.height,BufferedImage.TYPE_INT_RGB);

		for( int y = 0; y < pixelToRegion.height; y++ ) {
			for( int x = 0; x < pixelToRegion.width; x++ ) {
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

				output.setRGB(x, y, rgb);
			}
		}

		return output;
	}

	/**
	 * Draws border pixels of each region using the specified color.
	 *
	 * @param pixelToRegion Conversion from pixel to region
	 * @param borderColor RGB value of border pixel
	 * @param output Storage for output image.  Can be null.
	 * @return Output image.
	 */
	public static BufferedImage regionBorders( GrayS32 pixelToRegion ,
											   int borderColor ,
											   BufferedImage output ) {
		if( output == null )
			output = new BufferedImage(pixelToRegion.width,pixelToRegion.height,BufferedImage.TYPE_INT_RGB);

		GrayU8 binary = new GrayU8(pixelToRegion.width,pixelToRegion.height);
		ImageSegmentationOps.markRegionBorders(pixelToRegion, binary);
		for( int y = 0; y < binary.height; y++ ) {
			for( int x = 0; x < binary.width; x++ ) {
				if( binary.unsafe_get(x,y) == 1 )  {
					output.setRGB(x,y,borderColor);
				}
			}
		}

		return output;
	}
}
