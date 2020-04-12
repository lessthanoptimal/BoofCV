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
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;


/**
 * Selects and sorts up to the N best features based on their intensity.
 *
 * @author Peter Abeles
 */
public class FeatureSelectNBest implements FeatureSelectLimit {

	// list of the found best corners
	int[] indexes = new int[1];
	float[] indexIntensity = new float[1];

	@Override
	public void select(GrayF32 intensity , boolean positive, FastAccess<Point2D_I16> prior,
					   FastAccess<Point2D_I16> detected, int limit , FastQueue<Point2D_I16> selected) {
		selected.reset();

		if (detected.size <= limit) {
			// make a copy of the results with no pruning since it already
			// has the desired number, or less
			BoofMiscOps.copyAll_2D_I16(detected,selected);
			return;
		}

		// grow internal data structures
		if( detected.size > indexes.length ) {
			indexes = new int[detected.size];
			indexIntensity = new float[detected.size];
		}

		// extract the intensities for each corner
		Point2D_I16[] points = detected.data;

		if( positive ) {
			for (int i = 0; i < detected.size; i++) {
				Point2D_I16 pt = points[i];
				// quick select selects the k smallest
				// I want the k-biggest so the negative is used
				indexIntensity[i] = -intensity.get(pt.x, pt.y);
			}
		} else {
			for (int i = 0; i < detected.size; i++) {
				Point2D_I16 pt = points[i];
				indexIntensity[i] = intensity.get(pt.x, pt.y);
			}
		}

		QuickSelect.selectIndex(indexIntensity,limit,detected.size,indexes);

		for (int i = 0; i < limit; i++) {
			selected.grow().set(detected.data[indexes[i]]);
		}
	}
}
