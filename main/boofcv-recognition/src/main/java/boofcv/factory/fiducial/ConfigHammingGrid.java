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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Defines the calibration pattern based on {@link ConfigHammingMarker hamming square fiducials} where each square
 * is a marker that can be uniquely identified. These are typically used for multi camera calibration and are
 * robust against partial occlusions. Aruco Grids are a member of this family.
 *
 * @author Peter Abeles
 */
public class ConfigHammingGrid implements Configuration {
	/** Number of squares tall the grid is */
	public int numRows = -1;

	/** Number of squares wide the grid is */
	public int numCols = -1;

	/** Size of a square in document units */
	public double squareSize = 1.0;

	/** How wide the space is between squares relative to the length of a square */
	public double spaceToSquare = 0.4;

	/** The first marker will have this ID */
	public int markerOffset = 0;

	/** Encoding dictionary for binary patterns */
	public ConfigHammingMarker markers;

	public ConfigHammingGrid( ConfigHammingMarker dictionary ) {
		this.markers = dictionary;
	}

	public ConfigHammingGrid() {
		markers = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_25h7);
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(numRows > 0);
		BoofMiscOps.checkTrue(numCols > 0);
		BoofMiscOps.checkTrue(spaceToSquare > 0);
		BoofMiscOps.checkTrue(markerOffset > 0);
		markers.checkValidity();
	}

	public ConfigHammingGrid setTo( ConfigHammingGrid src ) {
		this.numRows = src.numRows;
		this.numCols = src.numCols;
		this.squareSize = src.squareSize;
		this.spaceToSquare = src.spaceToSquare;
		this.markerOffset = src.markerOffset;
		this.markers.setTo(src.markers);
		return this;
	}

	public double getMarkerWidth() {
		return (numCols - 1)*(1.0 + spaceToSquare)*squareSize + squareSize;
	}

	public double getMarkerHeight() {
		return (numRows - 1)*(1.0 + spaceToSquare)*squareSize + squareSize;
	}

	/**
	 * Create from a pre-defined dictionary
	 */
	public static ConfigHammingGrid create( HammingDictionary dictionary,
											int rows, int cols, double squareSize, double spaceToSquare ) {
		ConfigHammingMarker configDictionary = ConfigHammingMarker.loadDictionary(dictionary);

		var config = new ConfigHammingGrid(configDictionary);
		config.numRows = rows;
		config.numCols = cols;
		config.squareSize = squareSize;
		config.spaceToSquare = spaceToSquare;

		return config;
	}
}
