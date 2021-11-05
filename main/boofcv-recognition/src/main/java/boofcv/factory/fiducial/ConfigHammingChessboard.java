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
 * Defines the calibration pattern based on {@link ConfigHammingMarker hamming checkerboard fiducials} where square
 * markers are embedded inside a chessboard/checkerboard pattern. Calibration features come from the inner chessboard
 * pattern. Charuco is a member of this family.
 *
 * @author Peter Abeles
 */
public class ConfigHammingChessboard implements Configuration {
	/** Number of squares tall the grid is */
	public int numRows = -1;

	/** Number of squares wide the grid is */
	public int numCols = -1;

	/** The first marker will have this ID */
	public int markerOffset = 0;

	/** Describes the markers are drawn inside the chessboard pattern */
	public ConfigHammingMarker markers;

	/** How much smaller the marker is relative to the chessboard squares */
	public double markerScale = 0.7;

	/** Size of a square in document units */
	public double squareSize = 1.0;

	/** If an even pattern then the top-left will always be a black square */
	public boolean chessboardEven = true;

	public ConfigHammingChessboard( ConfigHammingMarker markers ) {
		this.markers = markers;
	}

	public ConfigHammingChessboard() {
		markers = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_25h7);
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(numRows > 0);
		BoofMiscOps.checkTrue(numCols > 0);
		BoofMiscOps.checkTrue(markerOffset >= 0);
		markers.checkValidity();
		BoofMiscOps.checkTrue(markerScale > 0);
		BoofMiscOps.checkTrue(squareSize > 0);
	}

	public ConfigHammingChessboard setTo( ConfigHammingChessboard src ) {
		this.numRows = src.numRows;
		this.numCols = src.numCols;
		this.markerOffset = src.markerOffset;
		this.markers.setTo(src.markers);
		this.markerScale = src.markerScale;
		this.squareSize = src.squareSize;
		this.chessboardEven = src.chessboardEven;
		return this;
	}

	public double getMarkerWidth() {
		return squareSize*numCols;
	}

	public double getMarkerHeight() {
		return squareSize*numRows;
	}

	/**
	 * Create from a pre-defined dictionary
	 */
	public static ConfigHammingChessboard create( HammingDictionary dictionary,
												  int rows, int cols, double squareSize ) {
		ConfigHammingMarker configDictionary = ConfigHammingMarker.loadDictionary(dictionary);

		var config = new ConfigHammingChessboard(configDictionary);
		config.numRows = rows;
		config.numCols = cols;
		config.squareSize = squareSize;

		return config;
	}
}
