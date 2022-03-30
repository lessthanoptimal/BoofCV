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

package boofcv.alg.geo.calibration;

import boofcv.struct.ConfigLength;
import boofcv.struct.ImageGrid;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;

/**
 * Scores how will points are distributed across the inner image. The image is broken up into even grid cells
 * and the fraction of cells that are filled is the score
 */
public class ScoreCalibrationInnerFill {

	/** Used to specify how large a cell in the image grid should be. If relative, then relative with (w+h)/2 */
	@Getter public final ConfigLength cellSize = ConfigLength.relative(0.05, 10);

	/** Fill score. 0 = empty. 1.0 completely filled */
	@Getter double score;

	// Stores the grid. each cell indicates if a calibration point appears inside it or not
	final ImageGrid<Cell> grid = new ImageGrid<>(Cell::new, Cell::reset);

	/**
	 * Call this first to initialize and reset. Pass in the image size.
	 */
	public void initialize( int width, int height ) {
		int target = cellSize.computeI((width + height)/2.0);
		grid.initialize(target, width, height);
		score = 0.0;
	}

	/**
	 * Add a new set of observations. Updates the score
	 */
	public void add( CalibrationObservation obs ) {
		// Mark all cells with observed points as filled
		for (int obsIdx = 0; obsIdx < obs.size(); obsIdx++) {
			Point2D_F64 p = obs.get(obsIdx).p;
			grid.getCellAtPixel((int)p.x, (int)p.y).filled = true;
		}

		// Compute fraction filled
		int totalFilled = 0;
		for (int cellIdx = 0; cellIdx < grid.cells.size; cellIdx++) {
			if (grid.cells.get(cellIdx).filled) {
				totalFilled++;
			}
		}
		score = totalFilled/(double)grid.cells.size;
	}

	/** Used to keep track of which cells have an observation */
	private static class Cell {
		boolean filled;

		public void reset() {
			filled = false;
		}
	}
}
