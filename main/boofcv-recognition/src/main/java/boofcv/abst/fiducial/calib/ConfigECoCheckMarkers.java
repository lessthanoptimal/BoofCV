/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial.calib;

import boofcv.alg.fiducial.calib.ecocheck.ECoCheckCodec;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import boofcv.struct.GridShape;
import org.ddogleg.struct.FastArray;

import java.util.List;

/**
 * Specifies the grid shape and physical sizes for one or more {@link ConfigECoCheckDetector} type markers.
 * In most use cases, having a single marker shape is desirable. However, the detector supports arbitrary
 * shapes for each marker. This configuration can do both by allowing the first marker in the list to be
 * duplicated.
 *
 * @author Peter Abeles
 */
public class ConfigECoCheckMarkers implements Configuration {
	public static final int DEFAULT_CHECKSUM = 6;
	public static final int DEFAULT_ECC = 3;

	/** Fraction of a cell's length the data bit is */
	public double dataBitWidthFraction = 0.7;

	/** Fraction of the length the quite-zone is around data bits */
	public double dataBorderFraction = 0.15;

	/** Number of times the first marker is duplicated. */
	public int firstTargetDuplicated = 1;

	/** Shapes of individual markers. The first marker might be duplicated. */
	public final FastArray<MarkerShape> markerShapes = new FastArray<>(MarkerShape.class);

	/**
	 * <p>
	 * Amount of error correction applied. Larger values increase the number of data bits that need to be encoded, but
	 * increase the number of errors it can recover from. More bits means smaller dots and more errors. If there are
	 * extra bits in the square they will be allocated to error correction automatically. If two targets have
	 * different amounts of error correction they will be incompatible.
	 * </p>
	 *
	 * 0 = no error correction. 9 = maximum amount.
	 */
	public int errorCorrectionLevel = DEFAULT_ECC;

	/**
	 * Number of bits allocated to the checksum. There can be at most 8 bits.
	 */
	public int checksumBits = DEFAULT_CHECKSUM;

	/**
	 * Configures N markers with the same shape
	 */
	public static ConfigECoCheckMarkers singleShape( int rows, int cols, int numMarkers, double squareSize ) {
		var config = new ConfigECoCheckMarkers();
		config.firstTargetDuplicated = numMarkers;
		config.markerShapes.add(new MarkerShape(rows, cols, squareSize));
		return config;
	}

	/**
	 * Parses the standard compact name string and converts it into a configuration. Note that error correction
	 * 'e' and checksum 'c' are optional. See {@link #compactName} for a summary of this string format.
	 */
	public static ConfigECoCheckMarkers parse( String description, double squareSize ) {
		// 9x7n1e3c6

		int locN = -1;
		int locE = -1;
		int locC = -1;

		for (int i = 0; i < description.length(); i++) {
			switch (description.charAt(i)) {
				case 'n' -> locN = i;
				case 'e' -> locE = i;
				case 'c' -> locC = i;
			}
		}

		String[] shape = description.substring(0, locN).split("x");

		int rows = Integer.parseInt(shape[0]);
		int cols = Integer.parseInt(shape[1]);
		int numMarkers;
		if (locE == -1)
			numMarkers = Integer.parseInt(description.substring(locN + 1));
		else
			numMarkers = Integer.parseInt(description.substring(locN + 1, locE));

		ConfigECoCheckMarkers config = singleShape(rows, cols, numMarkers, squareSize);
		if (locE != -1)
			if (locC == -1)
				config.errorCorrectionLevel = Integer.parseInt(description.substring(locE + 1));
			else
				config.errorCorrectionLevel = Integer.parseInt(description.substring(locE + 1, locC));
		if (locC != -1)
			config.checksumBits = Integer.parseInt(description.substring(locC + 1));

		return config;
	}

	/**
	 * Converts this configuration into a list of GridShapes
	 *
	 * @param markers (Output) Storage for converted shapes
	 */
	public void convertToGridList( List<GridShape> markers ) {
		markers.clear();
		MarkerShape first = markerShapes.get(0);
		for (int i = 0; i < firstTargetDuplicated; i++) {
			markers.add(new GridShape(first.numRows, first.numCols));
		}

		// Add all the others now
		for (int i = 1; i < markerShapes.size(); i++) {
			MarkerShape s = markerShapes.get(i);
			markers.add(new GridShape(s.numRows, s.numCols));
		}
	}

	public ConfigECoCheckMarkers setTo( ConfigECoCheckMarkers src ) {
		this.dataBitWidthFraction = src.dataBitWidthFraction;
		this.dataBorderFraction = src.dataBorderFraction;
		this.firstTargetDuplicated = src.firstTargetDuplicated;
		this.errorCorrectionLevel = src.errorCorrectionLevel;
		this.checksumBits = src.checksumBits;
		this.markerShapes.clear();
		for (int i = 0; i < src.markerShapes.size(); i++) {
			MarkerShape s = new MarkerShape();
			s.setTo(src.markerShapes.get(i));
			this.markerShapes.add(s);
		}
		return this;
	}

	/**
	 * <p>String which compactly describes markers with duplicate shapes.</p>
	 *
	 * <ul>
	 *     <li>Example: 9x7n1e3c6 means 9x7 pattern, 1 possible marker, with error correction level of 3,
	 *     checksum with 6 bits.</li>
	 * 	 <li>Example: 9x7n1 is the same, but ecc and checksum have default values.</li>
	 * </ul>
	 */
	public String compactName() {
		BoofMiscOps.checkEq(1, markerShapes.size(), "Only one unique shape allowed");
		MarkerShape shape = markerShapes.get(0);
		String text = String.format("%dx%dn%de%dc%d", shape.numRows, shape.numCols,
				firstTargetDuplicated, errorCorrectionLevel, checksumBits);

		// Truncate if using default values
		if (checksumBits == DEFAULT_CHECKSUM) {
			text = text.substring(0, text.length() - 2);
			if (errorCorrectionLevel == DEFAULT_ECC)
				text = text.substring(0, text.length() - 2);
		}


		return text;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkFraction(dataBitWidthFraction, "dataBitWidthFraction must be 0 to 1.0.");
		BoofMiscOps.checkFraction(dataBorderFraction, "dataBorderFraction must be 0 to 1.0.");
		BoofMiscOps.checkTrue(firstTargetDuplicated >= 1, "firstTargetDuplicated <= 0");
		BoofMiscOps.checkTrue(markerShapes.size() >= 1, "Shapes not defined");
		BoofMiscOps.checkTrue(errorCorrectionLevel >= 0 && errorCorrectionLevel <= ECoCheckCodec.MAX_ECC_LEVEL,
				"error correction must be from 0 to 9, inclusive.");
		BoofMiscOps.checkTrue(checksumBits >= 0 && checksumBits <= ECoCheckCodec.MAX_CHECKSUM_BITS,
				"checksum bits must be from 0 to 8, inclusive.");
	}

	public static class MarkerShape implements Configuration {
		/** Number of squares tall the grid is */
		public int numRows = -1;

		/** Number of squares wide the grid is */
		public int numCols = -1;

		/** Length of the square's side */
		public double squareSize;

		public MarkerShape( int numRows, int numCols, double squareSize ) {
			this.numRows = numRows;
			this.numCols = numCols;
			this.squareSize = squareSize;
		}

		public MarkerShape() {}

		public int getNumCorners() {
			return (numCols - 1)*(numRows - 1);
		}

		/**
		 * Returns marker's width
		 */
		public double getWidth() {
			return (numCols - 1)*squareSize;
		}

		/**
		 * Returns marker's width
		 */
		public double getHeight() {
			return (numRows - 1)*squareSize;
		}

		public void setTo( MarkerShape src ) {
			this.numRows = src.numRows;
			this.numCols = src.numCols;
			this.squareSize = src.squareSize;
		}

		@Override public void checkValidity() {
			BoofMiscOps.checkTrue(numRows >= 0);
			BoofMiscOps.checkTrue(numCols >= 0);
			BoofMiscOps.checkTrue(squareSize > 0);
		}
	}
}
