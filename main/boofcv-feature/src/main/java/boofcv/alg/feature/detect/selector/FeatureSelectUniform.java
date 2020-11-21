/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigGridUniform;
import boofcv.struct.ImageGrid;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Attempts to select features uniformly across the image. This is done by breaking the image up into a grid and
 * selecting a specific number randomly from each grid cell.
 *
 * @author Peter Abeles
 */
public abstract class FeatureSelectUniform<Point> implements FeatureSelectLimit<Point> {

	/** Configuration for uniformly selecting a grid */
	public ConfigGridUniform configUniform = new ConfigGridUniform();

	// Used to select points randomly inside a cell
	public Random rand = new Random(0xDEADBEEF);

	// Grid for storing a set of objects
	ImageGrid<Info<Point>> grid = new ImageGrid<>(Info::new, Info::reset);

	@Override
	public void select( int imageWidth, int imageHeight,
						@Nullable FastAccess<Point> prior, FastAccess<Point> detected, int limit,
						FastArray<Point> selected ) {
		BoofMiscOps.checkTrue(limit > 0);
		selected.reset();

		// the limit is more than the total number of features. Return them all!
		if ((prior == null || prior.size == 0) && detected.size <= limit) {
			// make a copy of the results with no pruning since it already has the desired number, or less
			selected.addAll(detected);
			return;
		}

		// Adjust the grid to the requested limit and image shape
		int targetCellSize = configUniform.selectTargetCellSize(limit, imageWidth, imageHeight);
		grid.initialize(targetCellSize, imageWidth, imageHeight);

		// Note all the prior features
		if (prior != null) {
			for (int i = 0; i < prior.size; i++) {
				Point p = prior.data[i];
				getGridCell(p).priorCount++;
			}
		}

		// Add all detected points to the grid
		for (int i = 0; i < detected.size; i++) {
			Point p = detected.data[i];
			getGridCell(p).detected.add(p);
		}

		// Add points until the limit has been reached or there are no more cells to add
		final FastAccess<Info<Point>> cells = grid.cells;

		// predeclare the output list
		selected.resize(limit);
		selected.reset();
		while (selected.size < limit) {
			boolean change = false;
			for (int cellidx = 0; cellidx < cells.size && selected.size < limit; cellidx++) {
				Info<Point> info = cells.get(cellidx);

				// if there's a prior feature here, note it and move on
				if (info.priorCount > 0) {
					info.priorCount--;
					change = true;
					continue;
				}
				// Are there any detected features remaining?
				if (info.detected.isEmpty())
					continue;

				// Randomly select one and add it tot he grid
				selected.add(info.detected.remove(rand.nextInt(info.detected.size())));
				change = true;
			}
			if (!change)
				break;
		}
	}

	/**
	 * Returns the grid cell that contains the point
	 */
	protected abstract Info<Point> getGridCell( Point p );

	/**
	 * Info for each cell
	 */
	public static class Info<Point> {
		// Number of features in the cell from the prior list
		int priorCount = 0;
		// Sorted list of detected features by intensity
		List<Point> detected = new ArrayList<>();

		public void reset() {
			priorCount = 0;
			detected.clear();
		}
	}

	/**
	 * Implementation for {@link Point2D_I16}
	 */
	public static class I16 extends FeatureSelectUniform<Point2D_I16> {
		@Override
		protected Info<Point2D_I16> getGridCell( Point2D_I16 p ) {
			return grid.getCellAtPixel(p.x, p.y);
		}
	}

	/**
	 * Implementation for {@link Point2D_F32}
	 */
	public static class F32 extends FeatureSelectUniform<Point2D_F32> {
		@Override
		protected Info<Point2D_F32> getGridCell( Point2D_F32 p ) {
			return grid.getCellAtPixel((int)p.x, (int)p.y);
		}
	}

	/**
	 * Implementation for {@link Point2D_F64}
	 */
	public static class F64 extends FeatureSelectUniform<Point2D_F64> {
		@Override
		protected Info<Point2D_F64> getGridCell( Point2D_F64 p ) {
			return grid.getCellAtPixel((int)p.x, (int)p.y);
		}
	}
}
