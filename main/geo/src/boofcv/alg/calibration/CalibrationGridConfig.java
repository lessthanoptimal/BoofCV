/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.calibration;

import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CalibrationGridConfig {

	// how many elements wide is the grid
	int gridWidth;
	// how many elements tall is the grid
	int gridHeight;

	// width of each square element in the grid in world units
	double cellSize;

	public CalibrationGridConfig(int gridWidth, int gridHeight, double cellSize) {
		this.gridWidth = gridWidth;
		this.gridHeight = gridHeight;
		this.cellSize = cellSize;
	}

	/**
	 * Computes the grid coordinates in 3D space.  The z-axis is assumed to be zero
	 * so only a 2D point is returned.
	 *
	 * @return List of grid points in 3D space, with z assumed to be zero.
	 */
	public List<Point2D_F64> computeGridPoints() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for( int y = 0; y < gridHeight; y++ ) {
			for( int x = 0; x < gridWidth; x++ ) {
				ret.add( new Point2D_F64(x*cellSize,y*cellSize));
			}
		}

		return ret;
	}
}
