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

package boofcv.gui.edge;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * @author Peter Abeles
 */
public class VisualizeEdgeFeatures {
	public static BufferedImage renderOrientation(GrayU8 direction , BufferedImage out ) {

		if( out == null ) {
			out = new BufferedImage(direction.getWidth(),direction.getHeight(),BufferedImage.TYPE_INT_RGB);
		}

		if( out.getRaster() instanceof IntegerInterleavedRaster) {
			int colors[] = new int[4];
			colors[0] = Color.RED.getRGB();
			colors[1] = Color.GREEN.getRGB();
			colors[2] = Color.BLUE.getRGB();
			colors[3] = Color.BLACK.getRGB();

			IntegerInterleavedRaster raster = (IntegerInterleavedRaster)out.getRaster();

			int rasterIndex = 0;
			int data[] = raster.getDataStorage();

			int w = direction.getWidth();
			int h = direction.getHeight();

			for( int y = 0; y < h; y++ ) {
				int indexSrc = direction.startIndex + y*direction.stride;
				for( int x = 0; x < w; x++ ) {
					data[rasterIndex++] = colors[direction.data[indexSrc++]];
				}
			}
		} else {
			throw new RuntimeException("Raster not supported yet");
		}
		return out;
	}

	public static BufferedImage renderOrientation4(GrayS8 direction , GrayF32 intensity , float threshold , BufferedImage out ) {

		if( out == null ) {
			out = new BufferedImage(direction.getWidth(),direction.getHeight(),BufferedImage.TYPE_INT_RGB);
		}

		if( out.getRaster() instanceof IntegerInterleavedRaster) {
			int colors[] = new int[4];
			colors[0] = Color.RED.getRGB();
			colors[1] = Color.GREEN.getRGB();
			colors[2] = Color.BLUE.getRGB();
			colors[3] = Color.BLACK.getRGB();
			int white = Color.WHITE.getRGB();

			IntegerInterleavedRaster raster = (IntegerInterleavedRaster)out.getRaster();

			int rasterIndex = 0;
			int data[] = raster.getDataStorage();

			int w = direction.getWidth();
			int h = direction.getHeight();

			for( int y = 0; y < h; y++ ) {
				int indexSrc = direction.startIndex + y*direction.stride;
				int indexInten = intensity.startIndex + y*intensity.stride;
				for( int x = 0; x < w; x++ , indexInten++ , indexSrc++, rasterIndex++) {
					if( intensity.data[indexInten] >= threshold ) {
						data[rasterIndex] = colors[direction.data[indexSrc]+1];
					} else {
						data[rasterIndex] = white;
					}
				}
			}
		} else {
			throw new RuntimeException("Raster not supported yet");
		}
		return out;
	}
}
