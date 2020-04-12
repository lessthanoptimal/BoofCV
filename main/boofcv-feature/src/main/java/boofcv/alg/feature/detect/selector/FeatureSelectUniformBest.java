/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSort_F32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.annotation.Nullable;
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
public class FeatureSelectUniformBest implements FeatureSelectLimit {

	/** Configuration for uniformly selecting a grid */
	public ConfigGridUniform configUniform = new ConfigGridUniform();

	// Grid for storing a set of objects
	ImageGrid<Info> grid = new ImageGrid<>(Info::new,Info::reset);

	// Workspace variables for sorting the cells
	GrowQueue_F32 pointIntensity = new GrowQueue_F32();
	QuickSort_F32 sorter = new QuickSort_F32();
	GrowQueue_I32 indexes = new GrowQueue_I32();
	List<Point2D_I16> workList = new ArrayList<>();

	@Override
	public void select(GrayF32 intensity, boolean positive,
					   @Nullable FastAccess<Point2D_I16> prior, FastAccess<Point2D_I16> detected, int limit,
					   FastQueue<Point2D_I16> selected)
	{
		selected.reset();

		// the limit is more than the total number of features. Return them all!
		if( (prior == null || prior.size==0) && detected.size <= limit ) {
			BoofMiscOps.copyAll_2D_I16(detected,selected);
			return;
		}

		// Adjust the grid to the requested limit and image shape
		int targetCellSize = configUniform.selectTargetCellSize(limit,intensity.width,intensity.height);
		grid.initialize(targetCellSize,intensity.width,intensity.height);

		// Note all the prior features
		if( prior != null ) {
			for (int i = 0; i < prior.size; i++) {
				Point2D_I16 p = prior.data[i];
				grid.getCellAtPixel(p.x, p.y).priorCount++;
			}
		}

		// Add all detected points to the grid
		for (int i = 0; i < detected.size; i++) {
			Point2D_I16 p = detected.data[i];
			grid.getCellAtPixel(p.x,p.y).detected.add(p);
		}

		// Sort elements in each cell in order be inverse preference
		sortCellLists(intensity, positive);

		// Add points until the limit has been reached or there are no more cells to add
		final FastAccess<Info> cells = grid.cells;
		while( selected.size < limit ) {
			boolean change = false;
			for (int cellidx = 0; cellidx < cells.size && selected.size < limit; cellidx++) {
				Info info = cells.get(cellidx);

				// if there's a prior feature here, note it and move on
				if( info.priorCount > 0 ) {
					info.priorCount--;
					change = true;
					continue;
				}
				// Are there any detected features remaining?
				if (info.detected.isEmpty())
					continue;
				selected.grow().set( info.detected.remove( info.detected.size()-1) );
				change = true;
			}
			if( !change )
				break;
		}
	}

	/**
	 * Sort points in cells based on their intensity
	 */
	private void sortCellLists(GrayF32 intensity, boolean positive) {
		// Add points to the grid elements and sort them based feature intensity
		final FastAccess<Info> cells = grid.cells;
		for (int cellidx = 0; cellidx < cells.size; cellidx++) {
			final List<Point2D_I16> cellPoints = cells.get(cellidx).detected;
			if( cellPoints.isEmpty() )
				continue;
			final int N = cellPoints.size();
			pointIntensity.resize(N);
			indexes.resize(N);

			// select the score's sign so that the most desirable is at the end of the list
			// That way elements can be removed from the top of the list, which is less expensive.
			if( positive ) {
				for (int pointIdx = 0; pointIdx < N; pointIdx++) {
					Point2D_I16 p = cellPoints.get(pointIdx);
					pointIntensity.data[pointIdx] = intensity.unsafe_get(p.x, p.y);
				}
			} else {
				for (int pointIdx = 0; pointIdx < N; pointIdx++) {
					Point2D_I16 p = cellPoints.get(pointIdx);
					pointIntensity.data[pointIdx] = -intensity.unsafe_get(p.x, p.y);
				}
			}
			sorter.sort(pointIntensity.data,0,N,indexes.data);

			// Extract an ordered list of points based on intensity and swap out the cell list to avoid a copy
			workList.clear();
			for (int i = 0; i < N; i++) {
				workList.add( cellPoints.get(indexes.data[i]));
			}
			List<Point2D_I16> tmp = cells.data[cellidx].detected;
			cells.data[cellidx].detected = workList;
			workList = tmp;
		}
	}

	/**
	 * Info for each cell
	 */
	public static class Info
	{
		// Number of features in the cell from the prior list
		int priorCount = 0;
		// Sorted list of detected features by intensity
		List<Point2D_I16> detected = new ArrayList<>();
		public void reset() {
			priorCount = 0;
			detected.clear();
		}
	}
}
