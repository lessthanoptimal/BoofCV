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
import georegression.struct.GeoTuple;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;

import javax.annotation.Nullable;


/**
 * Selects and sorts up to the N best features based on their intensity.
 *
 * @author Peter Abeles
 */
public abstract class FeatureSelectNBest<Point extends GeoTuple<Point>> implements FeatureSelectLimitIntensity<Point> {

	// list of the found best corners
	int[] indexes = new int[1];
	float[] indexIntensity = new float[1];

	@Override
	public void select(GrayF32 intensity , boolean positive, @Nullable FastAccess<Point> prior,
					   FastAccess<Point> detected, int limit , FastQueue<Point> selected) {
		selected.reset();

		if (detected.size <= limit) {
			// make a copy of the results with no pruning since it already
			// has the desired number, or less
			BoofMiscOps.copyAll(detected,selected);
			return;
		}

		// grow internal data structures
		if( detected.size > indexes.length ) {
			indexes = new int[detected.size];
			indexIntensity = new float[detected.size];
		}

		// extract the intensities for each corner
		Point[] points = detected.data;

		if( positive ) {
			for (int i = 0; i < detected.size; i++) {
				Point pt = points[i];
				// quick select selects the k smallest
				// I want the k-biggest so the negative is used
				indexIntensity[i] = -getIntensity(intensity,pt);
			}
		} else {
			for (int i = 0; i < detected.size; i++) {
				Point pt = points[i];
				indexIntensity[i] = getIntensity(intensity,pt);
			}
		}

		QuickSelect.selectIndex(indexIntensity,limit,detected.size,indexes);

		selected.resize(limit);
		for (int i = 0; i < limit; i++) {
			selected.get(i).setTo(detected.data[indexes[i]]);
		}
	}

	/**
	 * Looks up the feature intensity given the point
	 */
	protected abstract float getIntensity( GrayF32 intensity, Point p );

	/**
	 * Implementation for {@link Point2D_I16}
	 */
	public static class I16 extends FeatureSelectNBest<Point2D_I16> {
		@Override
		protected float getIntensity(GrayF32 intensity, Point2D_I16 p) {
			return intensity.unsafe_get(p.x,p.y);
		}
	}

	/**
	 * Implementation for {@link Point2D_F32}
	 */
	public static class F32 extends FeatureSelectNBest<Point2D_F32> {
		@Override
		protected float getIntensity(GrayF32 intensity, Point2D_F32 p) {
			return intensity.unsafe_get((int)p.x,(int)p.y);
		}
	}

	/**
	 * Implementation for {@link Point2D_F64}
	 */
	public static class F64 extends FeatureSelectNBest<Point2D_F64> {
		@Override
		protected float getIntensity(GrayF32 intensity, Point2D_F64 p) {
			return intensity.unsafe_get((int)p.x,(int)p.y);
		}
	}
}
