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

package boofcv.abst.fiducial.calib;

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
	 * 0 = no error correction. 10 = maximum amount.
	 */
	public int errorCorrectionLevel = 3;

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
	 * Parses the standard compact name string and converts it into a configuration.
	 */
	public static ConfigECoCheckMarkers parse( String description, double squareSize ) {
		String[] words = description.split("e");
		String[] shape = words[0].split("x");

		int rows = Integer.parseInt(shape[0]);
		int cols = Integer.parseInt(shape[1]);
		int numMarkers = Integer.parseInt(words[1].split("n")[1]);

		ConfigECoCheckMarkers config = singleShape(rows, cols, numMarkers, squareSize);
		config.errorCorrectionLevel = Integer.parseInt(words[1].charAt(0) + "");

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

	public void setTo( ConfigECoCheckMarkers src ) {
		this.dataBitWidthFraction = src.dataBitWidthFraction;
		this.dataBorderFraction = src.dataBorderFraction;
		this.firstTargetDuplicated = src.firstTargetDuplicated;
		this.errorCorrectionLevel = src.errorCorrectionLevel;
		this.markerShapes.clear();
		for (int i = 0; i < src.markerShapes.size(); i++) {
			MarkerShape s = new MarkerShape();
			s.setTo(src.markerShapes.get(i));
			this.markerShapes.add(s);
		}
	}

	/**
	 * String which compactly describes markers with duplicate shapes.
	 *
	 * Example: 9x7e3n1 means 9x7 pattern with error correction level of 3 and 1 possible marker.
	 */
	public String compactName() {
		BoofMiscOps.checkEq(1, markerShapes.size(), "Only one unique shape allowed");
		MarkerShape shape = markerShapes.get(0);
		return String.format("%dx%de%dn%d", shape.numRows, shape.numCols, errorCorrectionLevel, firstTargetDuplicated);
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkFraction(dataBitWidthFraction, "dataBitWidthFraction must be 0 to 1.0.");
		BoofMiscOps.checkFraction(dataBorderFraction, "dataBorderFraction must be 0 to 1.0.");
		BoofMiscOps.checkTrue(firstTargetDuplicated >= 1);
		BoofMiscOps.checkTrue(markerShapes.size() >= 1);
		BoofMiscOps.checkTrue(errorCorrectionLevel >= 0 && errorCorrectionLevel <= 10,
				"error correction must be from 0 to 10, inclusive.");
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
