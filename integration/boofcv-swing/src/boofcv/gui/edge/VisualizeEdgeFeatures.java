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

package boofcv.gui.edge;

import boofcv.io.image.ConvertRaster;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;


/**
 * @author Peter Abeles
 */
public class VisualizeEdgeFeatures {
	public static BufferedImage renderOrientation(GrayU8 direction , BufferedImage out ) {

		if( out == null ) {
			out = new BufferedImage(direction.getWidth(),direction.getHeight(),BufferedImage.TYPE_INT_RGB);
		}

		WritableRaster raster = out.getRaster();
		if( raster.getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
			int colors[] = new int[4];
			colors[0] = Color.RED.getRGB();
			colors[1] = Color.GREEN.getRGB();
			colors[2] = Color.BLUE.getRGB();
			colors[3] = Color.BLACK.getRGB();

			int dataDst[] = ((DataBufferInt)raster.getDataBuffer()).getData();
			int strideDst = ConvertRaster.stride(raster);
			int offsetDst = ConvertRaster.getOffset(raster);

			int w = direction.getWidth();
			int h = direction.getHeight();

			for( int y = 0; y < h; y++ ) {
				int indexDst = offsetDst + y*strideDst;
				int indexSrc = direction.startIndex + y*direction.stride;
				for( int x = 0; x < w; x++ ) {
					dataDst[indexDst++] = colors[direction.data[indexSrc++]];
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

		WritableRaster raster = out.getRaster();
		if( raster.getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
			int colors[] = new int[4];
			colors[0] = Color.RED.getRGB();
			colors[1] = Color.GREEN.getRGB();
			colors[2] = Color.BLUE.getRGB();
			colors[3] = Color.BLACK.getRGB();
			int white = Color.WHITE.getRGB();

			int dataDst[] = ((DataBufferInt)raster.getDataBuffer()).getData();
			int strideDst = ConvertRaster.stride(raster);
			int offsetDst = ConvertRaster.getOffset(raster);

			int w = direction.getWidth();
			int h = direction.getHeight();

			for( int y = 0; y < h; y++ ) {
				int indexDst = offsetDst + y*strideDst;
				int indexSrc = direction.startIndex + y*direction.stride;
				int indexInten = intensity.startIndex + y*intensity.stride;
				for( int x = 0; x < w; x++ , indexInten++ , indexSrc++, indexDst++) {
					if( intensity.data[indexInten] >= threshold ) {
						dataDst[indexDst] = colors[direction.data[indexSrc]+1];
					} else {
						dataDst[indexDst] = white;
					}
				}
			}
		} else {
			throw new RuntimeException("Raster not supported yet");
		}
		return out;
	}
}
