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

import java.util.ArrayList;
import java.util.List;

/**
 * Specifies the grid shape and physical sizes for one or more {@link ConfigChessboardBits} type markers.
 * In most use cases, having a single marker shape is desirable. However, the detector supports arbitrary
 * shapes for each marker. This configuration can do both by allowing the first marker in the list to be
 * duplicated.
 *
 * @author Peter Abeles
 */
public class ConfigChessboardBitsMarkers implements Configuration {

	/** Number of times the first marker is duplicated. */
	public int firstTargetDuplicated = 1;

	/** Shapes of individual markers. The first marker might be duplicated. */
	public final List<MarkerShape> markerShapes = new ArrayList<>();

	/**
	 * Configures N markers with the same shape
	 */
	public static ConfigChessboardBitsMarkers singleShape( int rows, int cols, double squareSize, int numMarkers ) {
		var config = new ConfigChessboardBitsMarkers();
		config.firstTargetDuplicated = numMarkers;
		config.markerShapes.add(new MarkerShape(rows, cols, squareSize));
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

	public void setTo( ConfigChessboardBitsMarkers src ) {
		this.firstTargetDuplicated = src.firstTargetDuplicated;
		this.markerShapes.clear();
		for (int i = 0; i < src.markerShapes.size(); i++) {
			MarkerShape s = new MarkerShape();
			s.setTo(src.markerShapes.get(i));
			this.markerShapes.add(s);
		}
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(firstTargetDuplicated >= 1);
		BoofMiscOps.checkTrue(markerShapes.size() >= 1);
	}

	public static class MarkerShape {
		/** Number of squares tall the grid is */
		public int numRows = -1;

		/** Number of squares wide the grid is */
		public int numCols = -1;

		/** Length of a squares size */
		public double squareSize;

		public MarkerShape( int numRows, int numCols, double squareSize ) {
			this.numRows = numRows;
			this.numCols = numCols;
			this.squareSize = squareSize;
		}

		public MarkerShape() {}

		public void setTo( MarkerShape src ) {
			this.numRows = src.numRows;
			this.numCols = src.numCols;
			this.squareSize = src.squareSize;
		}
	}
}
