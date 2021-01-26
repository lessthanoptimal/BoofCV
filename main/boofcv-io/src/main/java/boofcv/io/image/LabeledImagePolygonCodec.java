/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.io.UtilIO;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For reading and writing images which have been labeled with polygon regions. The image size and location of each
 * polygon in pixels is saved in a text format. The format does provide the potential for a binary format to be
 * used in the future. This is a BoofCV specific file format.
 *
 * @author Peter Abeles
 */
public class LabeledImagePolygonCodec {
	/**
	 * Saves polygon regions which have been used to label an image.
	 *
	 * @param regions (Input) Regions which have been denoted inside the image
	 * @param width (Input) Image width
	 * @param height (Input) Image height
	 * @param writer (Output) Where the image is written to
	 * @param comments (Input) Optional comments to be added to the image
	 * @throws IOException Thrown if anything goes wrong
	 */
	public static void encode( List<PolygonRegion> regions, int width, int height,
							   OutputStream writer, String... comments ) throws IOException {
		writer.write(String.format("LabeledPolygon,w=%d,h=%d,labels=%d,format=txt,version=1\n",
				width, height, regions.size()).getBytes(UTF_8));
		for (String comment : comments) {
			writer.write(("# " + comment + "\n").getBytes(UTF_8));
		}
		for (PolygonRegion region : regions) {
			String line = region.regionID + "," + region.polygon.size();
			for (int i = 0; i < region.polygon.size(); i++) {
				Point2D_F64 p = region.polygon.get(i);
				line += String.format(",%.17g,%.17g", p.x, p.y);
			}
			writer.write((line + "\n").getBytes(UTF_8));
		}
	}

	/**
	 * Decodes the stream and reads the labeled image
	 *
	 * @param reader Stream containing RLE encoded image
	 * @throws IOException Thrown if anything goes wrong
	 */
	public static void decode( InputStream reader, ImageDimension shape, DogArray<PolygonRegion> regions )
			throws IOException {
		regions.reset();
		StringBuilder buffer = new StringBuilder(1024);
		String line = UtilIO.readLine(reader, buffer);
		if (!line.startsWith("LabeledPolygon"))
			throw new IOException("Invalid. Does not start with LabeledPolygon");

		String[] words = line.split(",");
		int width = 0, height = 0, version = -1;
		boolean txt = false;
		for (int i = 1; i < words.length; i++) {
			String[] values = words[i].split("=");
			if (values.length != 2)
				throw new IOException("Unexpected: " + words[i]);
			switch (values[0]) {
				case "w" -> width = Integer.parseInt(values[1]);
				case "h" -> height = Integer.parseInt(values[1]);
				case "version" -> version = Integer.parseInt(values[1]);
				case "format" -> txt = values[1].equalsIgnoreCase("txt");
			}
		}
		if (!txt)
			throw new IOException("Can only read text format");
		if (version <= 0)
			throw new IOException("Unknown version.");

		shape.setTo(width, height);
		while (reader.available() > 0) {
			line = UtilIO.readLine(reader, buffer);
			// this is probably bad formatting, but we will just ignore it
			if (line.length() == 0)
				continue;
			// skip comments
			if (line.charAt(0) == '#')
				continue;
			words = line.split(",");
			if (words.length < 2)
				throw new IOException("Unexpected: " + line);
			int regionID = Integer.parseInt(words[0]);
			int size = Integer.parseInt(words[1]);
			if (size*2 + 2 != words.length)
				throw new IOException("Unexpected number of words. " + line);

			PolygonRegion region = regions.grow();
			region.regionID = regionID;
			region.polygon.vertexes.resize(size);
			for (int i = 2; i < words.length; i += 2) {
				double x = Double.parseDouble(words[i]);
				double y = Double.parseDouble(words[i + 1]);
				region.polygon.vertexes.get((i - 2)/2).setTo(x, y);
			}
		}
	}
}
