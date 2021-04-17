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

package boofcv.alg.misc;

import boofcv.struct.ConfigGridUniform;
import boofcv.struct.ImageGrid;
import lombok.Getter;

/**
 * Computes the fraction / percent of an image which is covered by image features. Converage is computed by overlaying
 * a grid on top of the image. The size of a cell depends on the maximum number of possible features which can be
 * detected.
 *
 * @author Peter Abeles
 * @see ConfigGridUniform
 * @see ImageGrid
 */
public class ImageCoverage {

	/** Configuration for overlaying a grid. You probably want to leave this as is */
	public final ConfigGridUniform configUniform = new ConfigGridUniform(1.5, 1);

	// grid cells. Stored in row major format
	public final ImageGrid<Cell> grid = new ImageGrid<>(Cell::new, Cell::reset);

	/** Fraction of the image covered by image features */
	public @Getter double fraction;
	/** Automatically computed. The targeted cell size in pixels. See {@link ImageGrid} */
	public int targetCellPixels;

	/**
	 * Resets and adjusts the grid size
	 *
	 * @param maxFeatures Maximum number of features which can be inside an image
	 * @param width image width
	 * @param height image height
	 */
	public void reset( int maxFeatures, int width, int height ) {
		targetCellPixels = configUniform.selectTargetCellSize(maxFeatures, width, height);
		grid.initialize(targetCellPixels, width, height);
		fraction = 0.0;
//		System.out.println("Coverage reset tl="+targetLength+" mf="+maxFeatures+" w="+width+" h="+height);
	}

	/**
	 * Marks a pixel in the image as having contained a feature. The pixel must lie inside the image. Outside
	 * pixels have undefined and likely very bad behavior.
	 */
	public void markPixel( int pixelX, int pixelY ) {
		grid.getCellAtPixel(pixelX, pixelY).covered = true;
	}

	/**
	 * Given the filled in grid it computes the fraction of cells with coverage
	 */
	public void process() {
		int total = 0;
		for (int i = 0; i < grid.cells.size; i++) {
			if (grid.cells.data[i].covered)
				total++;
		}
		fraction = total/(double)grid.cells.size;
	}

	/**
	 * Specifies if each cell in the grid contains at least one feature
	 */
	protected static class Cell {
		boolean covered;

		public void reset() {
			covered = false;
		}
	}
}
