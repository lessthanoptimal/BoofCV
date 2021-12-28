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
import boofcv.struct.image.GrayF32;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * Selects and sorts up to the N best features based on their intensity.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FeatureSelectNBest<Point> implements FeatureSelectLimitIntensity<Point> {

	// list of the found best corners
	int[] indexes = new int[1];
	float[] indexIntensity = new float[1];

	SampleIntensity<Point> sampler;

	public FeatureSelectNBest( SampleIntensity<Point> sampler ) {this.sampler = sampler;}

	public FeatureSelectNBest() {}

	@Override
	public void select( @Nullable GrayF32 intensity, int width, int height, boolean positive,
						@Nullable FastAccess<Point> prior, FastAccess<Point> detected, int limit,
						FastArray<Point> selected ) {
		BoofMiscOps.checkTrue(limit > 0);
		selected.reset();

		if (detected.size <= limit) {
			// make a copy of the results with no pruning since it already has the desired number, or less
			selected.addAll(detected);
			return;
		}

		// grow internal data structures
		if (detected.size > indexes.length) {
			indexes = new int[detected.size];
			indexIntensity = new float[detected.size];
		}

		// extract the intensities for each corner
		Point[] points = detected.data;

		if (positive) {
			for (int i = 0; i < detected.size; i++) {
				Point pt = points[i];
				// quick select selects the k smallest
				// I want the k-biggest so the negative is used
				indexIntensity[i] = -sampler.sample(intensity, i, pt);
			}
		} else {
			for (int i = 0; i < detected.size; i++) {
				Point pt = points[i];
				indexIntensity[i] = sampler.sample(intensity, i, pt);
			}
		}

		QuickSelect.selectIndex(indexIntensity, limit, detected.size, indexes);

		selected.resize(limit);
		for (int i = 0; i < limit; i++) {
			selected.set(i, detected.data[indexes[i]]);
		}
	}

	@Override
	public void setSampler( SampleIntensity<Point> sampler ) {
		this.sampler = sampler;
	}
}
