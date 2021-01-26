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

import boofcv.alg.misc.ImageStatistics;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayS32;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Encodes a labeled image using Run Line Encoding (RLE) to reduce file size. This is a BoofCV format.
 */
public class LabeledImageRleCodec {
	/**
	 * Saves a labeled image in RLE format.
	 *
	 * @param labeled (Input) The image
	 * @param writer (Output) Where the image is written to
	 * @param comments (Input) Optional comments to be added to the image
	 * @throws IOException Thrown if anything goes wrong
	 */
	public static void encode( GrayS32 labeled, OutputStream writer, String... comments ) throws IOException {
		int numLabels = ImageStatistics.max(labeled) + 1;
		writer.write(String.format("LabeledRLE,w=%d,h=%d,labels=%d,format=txt,version=1\n",
				labeled.width, labeled.height, numLabels).getBytes(UTF_8));
		for (String comment : comments) {
			writer.write(("# " + comment + "\n").getBytes(UTF_8));
		}
		int value = labeled.get(0, 0);
		int length = 0;
		for (int y = 0; y < labeled.height; y++) {
			int index = labeled.startIndex + y*labeled.stride;
			for (int x = 0; x < labeled.width; x++) {
				int v = labeled.data[index++];
				if (v == value)
					length++;
				else {
					writer.write((length + "," + value + "\n").getBytes(UTF_8));
					value = v;
					length = 1;
				}
			}
		}
		writer.write((length + "," + value + "\n").getBytes(UTF_8));
	}

	/**
	 * Decodes the stream and reads the labeled image
	 *
	 * @param reader Stream containing RLE encoded image
	 * @param labeled (Output) decoded labeled image
	 * @throws IOException Thrown if anything goes wrong
	 */
	public static void decode( InputStream reader, GrayS32 labeled ) throws IOException {
		StringBuilder buffer = new StringBuilder(1024);
		String line = UtilIO.readLine(reader, buffer);
		if (!line.startsWith("LabeledRLE"))
			throw new IOException("Invalid. Does not start with LabeledRLE");

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

		labeled.reshape(width, height);
		int index = 0;

		while (reader.available() > 0) {
			line = UtilIO.readLine(reader, buffer);
			// this is probably bad formatting, but we will just ignore it
			if (line.length() == 0)
				continue;
			// skip comments
			if (line.charAt(0) == '#')
				continue;
			words = line.split(",");
			if (words.length != 2)
				throw new IOException("Unexpected: " + line);
			int length = Integer.parseInt(words[0]);
			int value = Integer.parseInt(words[1]);
			for (int i = 0; i < length; i++) {
				labeled.data[index++] = value;
			}
		}
	}
}
