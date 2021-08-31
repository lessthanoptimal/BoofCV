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

package boofcv.factory.fiducial;

import boofcv.alg.fiducial.qrcode.PackedBits32;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import org.ddogleg.struct.DogArray;

import java.io.*;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Defines the dictionary and how they are encoded in a Hamming distance marker.
 *
 * <p>Values for each pre-defined dictionary comes from ArUco marker 3 source code. [1]</p>
 *
 * <p>[1] <a href="boofcv.alg.fiducial.square.DetectFiducialSquareHamming">ArUco 3</a></p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.fiducial.square.DetectFiducialSquareHamming
 */
public class ConfigHammingDictionary implements Configuration {
	/**
	 * How wide the border is relative to the total fiducial width. Typically the width of one square.
	 */
	public double borderWidthFraction = 0.25;

	/** Number of cells along each side in the binary grid */
	public int gridWidth = -1;

	/** The minimum hamming distance separating two markers */
	public int minimumHamming;

	/** How each marker is encoded */
	public DogArray<Marker> encoding = new DogArray<>(Marker::new);

	/** Which dictionary is this based off of. Typically, this will be pre-defined. */
	public Dictionary dictionary;

	public ConfigHammingDictionary() {}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(borderWidthFraction > 0.0);
		BoofMiscOps.checkTrue(gridWidth > 0);
		BoofMiscOps.checkTrue(minimumHamming >= 0);
		for (int i = 0; i < encoding.size; i++) {
			encoding.get(i).checkValidity();
		}
	}

	public void setTo( ConfigHammingDictionary src ) {
		this.borderWidthFraction = src.borderWidthFraction;
		this.gridWidth = src.gridWidth;
		this.minimumHamming = src.minimumHamming;
		this.encoding.resize(src.encoding.size);
		this.dictionary = src.dictionary;
		for (int i = 0; i < src.encoding.size; i++) {
			encoding.get(i).setTo(src.encoding.get(i));
		}
	}

	public int bitsPerGrid() {
		return gridWidth*gridWidth;
	}

	/**
	 * Adds a new marker with the specified encoding number
	 */
	public void addMarker( long encoding ) {
		if (encoding >= (long)gridWidth*(long)gridWidth)
			throw new IllegalArgumentException("ID is larger than the number of bits available");

		Marker m = this.encoding.grow();
		m.id = this.encoding.size - 1;
		m.pattern.resize(gridWidth*gridWidth);
		for (int bit = 0; bit < m.pattern.size; bit++) {
			m.pattern.set(bit, (int)((encoding >> bit) & 1L));
		}
	}

	/**
	 * Defines a marker
	 */
	public static class Marker {
		/** Expected binary bit pattern */
		public final PackedBits32 pattern = new PackedBits32();
		/** Unique ID that this marker represents */
		public int id;

		public void checkValidity() {
			BoofMiscOps.checkTrue(pattern.size > 0);
		}

		public void setTo( Marker src ) {
			this.id = src.id;
			this.pattern.setTo(src.pattern);
		}
	}

	/**
	 * Decodes a string that defined a dictionary in standard format
	 */
	public static ConfigHammingDictionary decodeDictionaryString( String text ) {
		var config = new ConfigHammingDictionary();

		String[] lines = text.split("\n");

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.isEmpty() || line.charAt(0) == '#')
				continue;

			String[] words = line.split("=");
			if (words.length != 2)
				throw new RuntimeException("Expected 2 words on line " + i);
			if (words[0].equals("grid_width")) {
				config.gridWidth = Integer.parseInt(words[1]);
			} else if (words[0].equals("minimum_hamming")) {
				config.minimumHamming = Integer.parseInt(words[1]);
			} else if (words[0].equals("dictionary")) {
				String[] ids = words[1].split(",");
				for (int idIdx = 0; idIdx < ids.length; idIdx++) {
					if (ids[idIdx].startsWith("0x"))
						config.addMarker(Long.parseUnsignedLong(ids[idIdx].substring(2), 16));
					else
						config.addMarker(Long.parseUnsignedLong(ids[idIdx]));
				}
			} else {
				throw new RuntimeException("Unknown key='" + words[0] + "'");
			}
		}

		// Border will be one square wide
		config.borderWidthFraction = 1.0/(config.gridWidth + 2.0);

		return config;
	}

	/**
	 * Loads a predefined dictionary stored in the the resources
	 */
	public static ConfigHammingDictionary loadPredefined( String name ) {
		URL path = ConfigHammingDictionary.class.getResource(name + ".txt");

		try (InputStream stream = path.openStream()) {
			String text = new BufferedReader(new InputStreamReader(stream))
					.lines().collect(Collectors.joining("\n"));
			return decodeDictionaryString(text);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates a predefined dictionary
	 *
	 * @param dictionary Which dictionary it should create
	 * @return The specified dictionary
	 */
	public static ConfigHammingDictionary define( Dictionary dictionary ) {
		ConfigHammingDictionary config = switch (dictionary) {
			case CUSTOM -> throw new IllegalArgumentException("Need to manually specify a custom dictionary");
			case ARUCO_ORIGINAL -> loadPredefined("aruco_original");
			case ARUCO_MIP_16h3 -> loadPredefined("aruco_mip_16h3");
			case ARUCO_MIP_25h7 -> loadPredefined("aruco_mip_25h7");
			case ARUCO_MIP_36h12 -> loadPredefined("aruco_mip_36h12");
		};
		config.dictionary = dictionary;
		return config;
	}

	/**
	 * List of pre-generated dictionaries
	 */
	public enum Dictionary {
		/** Custom dictionary */
		CUSTOM,
		ARUCO_ORIGINAL,
		ARUCO_MIP_16h3,
		ARUCO_MIP_25h7,
		ARUCO_MIP_36h12,
	}
}
