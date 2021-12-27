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

package boofcv.alg.feature.detect.selector;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigGridUniform;
import boofcv.struct.ImageGrid;
import boofcv.struct.image.GrayF32;
import org.ddogleg.sorting.QuickSort_F32;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to select features uniformly across the image with a preference for locally more intense features. This
 * is done by breaking the image up into a grid. Then features are added by selecting the most intense feature from
 * each grid. If a cell has a prior feature in it then it is skipped for that iteration and the prior counter is
 * decremented. This is repeated until the limit has been reached or there are no more features to add.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FeatureSelectUniformBest<Point> implements FeatureSelectLimitIntensity<Point> {

	/** Configuration for uniformly selecting a grid */
	public ConfigGridUniform configUniform = new ConfigGridUniform();

	// Grid for storing a set of objects
	final ImageGrid<Info<Point>> grid = new ImageGrid<>(Info::new, Info::reset);

	SampleIntensity<Point> sampler;

	// Workspace variables for sorting the cells
	final DogArray_F32 pointIntensity = new DogArray_F32();
	final QuickSort_F32 sorter = new QuickSort_F32();
	final DogArray_I32 indexes = new DogArray_I32();
	List<Point> workList = new ArrayList<>();

	public FeatureSelectUniformBest( SampleIntensity<Point> sampler ) {this.sampler = sampler;}

	public FeatureSelectUniformBest() {}

	@Override
	public void select( @Nullable GrayF32 intensity, int width, int height, boolean positive,
						@Nullable FastAccess<Point> prior, FastAccess<Point> detected, int limit,
						FastArray<Point> selected ) {
		BoofMiscOps.checkTrue(limit > 0);
		selected.reset();

		// Get the image shape from whatever source is available
		width = intensity == null ? width : intensity.width;
		height = intensity == null ? height : intensity.height;

		// the limit is more than the total number of features. Return them all!
		if ((prior == null || prior.size == 0) && detected.size <= limit) {
			// make a copy of the results with no pruning since it already has the desired number, or less
			selected.addAll(detected);
			return;
		}

		// Adjust the grid to the requested limit and image shape
		int targetCellSize = configUniform.selectTargetCellSize(limit, width, height);
		grid.initialize(targetCellSize, width, height);

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

		// Sort elements in each cell in order be inverse preference
		sortCellLists(intensity, positive);

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
				selected.add(info.detected.remove(info.detected.size() - 1));
				change = true;
			}
			if (!change)
				break;
		}
	}

	@Override
	public void setSampler( SampleIntensity<Point> sampler ) {
		this.sampler = sampler;
	}

	/**
	 * Sort points in cells based on their intensity
	 */
	private void sortCellLists( @Nullable GrayF32 intensity, boolean positive ) {
		// Add points to the grid elements and sort them based feature intensity
		final FastAccess<Info<Point>> cells = grid.cells;
		for (int cellidx = 0; cellidx < cells.size; cellidx++) {
			final List<Point> cellPoints = cells.get(cellidx).detected;
			if (cellPoints.isEmpty())
				continue;
			final int N = cellPoints.size();
			pointIntensity.resize(N);
			indexes.resize(N);

			// select the score's sign so that the most desirable is at the end of the list
			// That way elements can be removed from the top of the list, which is less expensive.
			if (positive) {
				for (int pointIdx = 0; pointIdx < N; pointIdx++) {
					Point p = cellPoints.get(pointIdx);
					pointIntensity.data[pointIdx] = sampler.sample(intensity, pointIdx, p);
				}
			} else {
				for (int pointIdx = 0; pointIdx < N; pointIdx++) {
					Point p = cellPoints.get(pointIdx);
					pointIntensity.data[pointIdx] = -sampler.sample(intensity, pointIdx, p);
				}
			}
			sorter.sort(pointIntensity.data, 0, N, indexes.data);

			// Extract an ordered list of points based on intensity and swap out the cell list to avoid a copy
			workList.clear();
			for (int i = 0; i < N; i++) {
				workList.add(cellPoints.get(indexes.data[i]));
			}
			List<Point> tmp = cells.data[cellidx].detected;
			cells.data[cellidx].detected = workList;
			workList = tmp;
		}
	}

	/**
	 * Returns the grid cell that contains the point
	 */
	protected Info<Point> getGridCell( Point p ) {
		return grid.getCellAtPixel(sampler.getX(p), sampler.getY(p));
	}

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
}
