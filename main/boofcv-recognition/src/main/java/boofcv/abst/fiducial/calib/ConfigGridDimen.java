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

import boofcv.struct.Configuration;

/**
 * Generic class that specifies the physical dimensions of a grid. Rows and columns count the number of shapes
 * in a grid. If chessboard it's the number of squares, including the space between them.
 *
 * @author Peter Abeles
 */
public class ConfigGridDimen implements Configuration {
	/**
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * Physical size of each shape. Side length for squares. Diameter for a circle.
	 */
	public double shapeSize;

	/**
	 * Space between shapes. For squares this would be the distance between the sides. For circles is the distance
	 * between the circle's center. Not used for chessboard.
	 */
	public double shapeDistance;

	public ConfigGridDimen( int numRows, int numCols, double shapeSize ) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.shapeSize = shapeSize;
	}

	public ConfigGridDimen( int numRows, int numCols, double shapeSize, double shapeDistance ) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.shapeSize = shapeSize;
		this.shapeDistance = shapeDistance;
	}

	public ConfigGridDimen() {
	}

	public ConfigGridDimen setTo( int numRows, int numCols, double shapeSize, double shapeDistance  ) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.shapeSize = shapeSize;
		this.shapeDistance = shapeDistance;
		return this;
	}

	public ConfigGridDimen setTo( ConfigGridDimen src ) {
		this.numRows = src.numRows;
		this.numCols = src.numCols;
		this.shapeSize = src.shapeSize;
		this.shapeDistance = src.shapeDistance;
		return this;
	}

	public double getSpacetoSizeRatio() {
		return shapeSize/shapeDistance;
	}

	@Override
	public void checkValidity() {
		if (numCols <= 0 || numRows <= 0)
			throw new IllegalArgumentException("Must specify then number of rows and columns in the target");
	}
}
