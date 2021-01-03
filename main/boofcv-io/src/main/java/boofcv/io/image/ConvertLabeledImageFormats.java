/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Functions for converting between different labeled image formats.
 *
 * @author Peter Abeles
 */
public class ConvertLabeledImageFormats {
	/**
	 * Converts from a set of polygon regions into a labeled image. Regions must be simple polygons
	 *
	 * @param regions (Input) List of simple polygon regions
	 * @param width (Input) width of image
	 * @param height (Input) height of image
	 * @param output (Output) labeled image. Optional. If null a new image is created.
	 * @return The output image/
	 */
	public static GrayS32 convert( List<PolygonRegion> regions , int width, int height, @Nullable GrayS32 output ) {
		if (output==null) {
			output = new GrayS32(width,height);
		} else {
			output.reshape(width, height);
		}

		// use a buffered image for rending polygons since BoofCV don't have a rendering algorithm. This will be
		// inefficient...

		GrayU8 gray = new GrayU8(width, height);
		BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		Polygon polygon = new Polygon();
		for( PolygonRegion r : regions ) {
			Graphics2D g2 = buffered.createGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0, width,height);

			g2.setColor(Color.BLACK);

			polygon.reset();
			for (int i = 0; i < r.polygon.size(); i++) {
				Point2D_F64 p = r.polygon.get(i);
				polygon.addPoint((int)p.x, (int)p.y);
			}
			Point2D_F64 p = r.polygon.get(0);
			polygon.addPoint((int)p.x, (int)p.y);

			g2.fill(polygon);

			ConvertBufferedImage.convertFrom(buffered, gray);

			GrayS32 _output = output;
			gray.forEachXY((x,y)->{
				if (gray.unsafe_get(x,y)==0) {
					_output.unsafe_set(x,y, r.regionID);
				}
			});
		}

		return output;
	}
}
