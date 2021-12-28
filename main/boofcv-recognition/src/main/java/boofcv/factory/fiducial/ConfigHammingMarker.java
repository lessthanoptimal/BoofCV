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
import org.ddogleg.struct.FastArray;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
@SuppressWarnings({"NullAway.Init"})
public class ConfigHammingMarker implements Configuration {
	/** How wide the border is relative to the total fiducial width. Typically, the width of one square. */
	public double borderWidthFraction = 0.25;

	/** Number of cells along each side in the binary grid */
	public int gridWidth = -1;

	/** The minimum hamming distance separating two markers */
	public int minimumHamming;

	/** How each marker is encoded */
	public FastArray<Marker> encoding = new FastArray<>(Marker.class);

	/** Which dictionary is this based off of. Typically, this will be pre-defined. */
	public HammingDictionary dictionary;

	/** Length of a targets size in world units. */
	public double targetWidth = 1;

	public ConfigHammingMarker() {}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(borderWidthFraction > 0.0);
		BoofMiscOps.checkTrue(gridWidth > 0);
		BoofMiscOps.checkTrue(minimumHamming >= 0);
		BoofMiscOps.checkTrue(targetWidth > 0);
		for (int i = 0; i < encoding.size(); i++) {
			encoding.get(i).checkValidity();
		}
	}

	@Override public void serializeInitialize() {
		// If it's custom then the dictionary was encoded
		if (dictionary == HammingDictionary.CUSTOM)
			return;

		// Otherwise, we need to load a pre-defined dictionary
		this.encoding = loadDictionary(dictionary).encoding;
	}

	@Override public List<String> serializeActiveFields() {
		List<String> active = new ArrayList<>();
		active.add("borderWidthFraction");
		active.add("gridWidth");
		active.add("minimumHamming");
		active.add("dictionary");
		if (dictionary == HammingDictionary.CUSTOM)
			active.add("encoding");

		return active;
	}

	public ConfigHammingMarker setTo( ConfigHammingMarker src ) {
		this.borderWidthFraction = src.borderWidthFraction;
		this.gridWidth = src.gridWidth;
		this.minimumHamming = src.minimumHamming;
		this.dictionary = src.dictionary;
		this.targetWidth = src.targetWidth;
		this.encoding.clear();
		encoding.addAll(src.encoding);
		return this;
	}

	public int bitsPerGrid() {
		return gridWidth*gridWidth;
	}

	/**
	 * Adds a new marker with the specified encoding number
	 */
	public void addMarker( long encoding ) {
		var m = new Marker();
		this.encoding.add(m);
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

		public void checkValidity() {
			BoofMiscOps.checkTrue(pattern.size > 0);
		}

		public void setTo( Marker src ) {
			this.pattern.setTo(src.pattern);
		}
	}

	/**
	 * Decodes a string that defined a dictionary in standard format
	 */
	public static ConfigHammingMarker decodeDictionaryString( String text ) {
		var config = new ConfigHammingMarker();

		String[] lines = text.split("\n");

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.isEmpty() || line.charAt(0) == '#')
				continue;

			String[] words = line.split("=");
			if (words.length != 2)
				throw new RuntimeException("Expected 2 words on line " + i);
			switch (words[0]) {
				case "grid_width" -> config.gridWidth = Integer.parseInt(words[1]);
				case "minimum_hamming" -> config.minimumHamming = Integer.parseInt(words[1]);
				case "dictionary" -> {
					String[] ids = words[1].split(",");
					for (int idIdx = 0; idIdx < ids.length; idIdx++) {
						if (ids[idIdx].startsWith("0x"))
							config.addMarker(Long.parseUnsignedLong(ids[idIdx].substring(2), 16));
						else
							config.addMarker(Long.parseUnsignedLong(ids[idIdx]));
					}
				}
				default -> throw new RuntimeException("Unknown key='" + words[0] + "'");
			}
		}

		// Border will be one square wide
		config.borderWidthFraction = 1.0/(config.gridWidth + 2.0);

		return config;
	}

	/**
	 * Loads a predefined dictionary stored in the the resources
	 */
	public static ConfigHammingMarker loadPredefined( String name ) {
		URL path = Objects.requireNonNull(ConfigHammingMarker.class.getResource(name + ".txt"));

		try (InputStream stream = path.openStream()) {
			String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
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
	public static ConfigHammingMarker loadDictionary( HammingDictionary dictionary ) {
		ConfigHammingMarker config = switch (dictionary) {
			case CUSTOM -> throw new IllegalArgumentException("Need to manually specify a custom dictionary");
			case ARUCO_ORIGINAL -> loadPredefined("aruco_original");
			case ARUCO_MIP_16h3 -> loadPredefined("aruco_mip_16h3");
			case ARUCO_MIP_25h7 -> loadPredefined("aruco_mip_25h7");
			case ARUCO_MIP_36h12 -> loadPredefined("aruco_mip_36h12");
			case ARUCO_OCV_4x4_1000 -> loadPredefined("aruco_ocv_4x4_1000");
			case ARUCO_OCV_5x5_1000 -> loadPredefined("aruco_ocv_5x5_1000");
			case ARUCO_OCV_6x6_1000 -> loadPredefined("aruco_ocv_6x6_1000");
			case ARUCO_OCV_7x7_1000 -> loadPredefined("aruco_ocv_7x7_1000");
			case APRILTAG_16h5 -> loadPredefined("apriltag_16h5");
			case APRILTAG_25h7 -> loadPredefined("apriltag_25h7");
			case APRILTAG_25h9 -> loadPredefined("apriltag_25h9");
			case APRILTAG_36h10 -> loadPredefined("apriltag_36h10");
			case APRILTAG_36h11 -> loadPredefined("apriltag_36h11");
		};
		config.dictionary = dictionary;
		return config;
	}
}
