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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.detect.selector.SampleIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * Adds the ability to specify the maximum number of points that you wish to return. The selected
 * points will be sorted by feature intensity. If maximums and minimums are found then the total
 * number refers to the total combined number of features. The intensity that it sorts by is the absolute value.
 *
 * @author Peter Abeles
 */
public class NonMaxLimiter {
	@Getter NonMaxSuppression nonmax;

	/** Maximum number of features it can return. If %le; 0 then there will be no limit */
	@Getter @Setter int maxTotalFeatures;

	// Detected minimums and maximums
	QueueCorner originalMin = new QueueCorner();
	QueueCorner originalMax = new QueueCorner();

	// Selects features when too many are detected
	FeatureSelectLimitIntensity<LocalExtreme> selector;
	// All detected features
	DogArray<LocalExtreme> foundAll = new DogArray<>(LocalExtreme::new);
	// Just the selected features
	FastArray<LocalExtreme> foundSelected = new FastArray<>(LocalExtreme.class);

	/**
	 * Configures the limiter
	 *
	 * @param nonmax Non-maximum suppression algorithm
	 * @param maxTotalFeatures The total number of allowed features it can return. Set to a value &le; 0 to disable.
	 */
	public NonMaxLimiter( NonMaxSuppression nonmax,
						  FeatureSelectLimitIntensity<LocalExtreme> selector,
						  int maxTotalFeatures ) {
		this.nonmax = nonmax;
		this.selector = selector;
		this.maxTotalFeatures = maxTotalFeatures;

		selector.setSampler(new SampleIntensity<>() {
			@Override
			public float sample( @Nullable GrayF32 intensity, int index, LocalExtreme p ) {return p.intensity;}

			@Override public int getX( LocalExtreme p ) {return p.location.x;}

			@Override public int getY( LocalExtreme p ) {return p.location.y;}
		});
	}

	/**
	 * Extracts local max and/or min from the intensity image. If more than the maximum features are found then
	 * only the most intense ones will be returned
	 *
	 * @param intensity Feature image intensity
	 */
	public void process( GrayF32 intensity ) {

		originalMin.reset();
		originalMax.reset();
		nonmax.process(intensity, null, null, originalMin, originalMax);

		foundAll.reset();
		for (int i = 0; i < originalMin.size; i++) {
			Point2D_I16 p = originalMin.get(i);
			float val = intensity.unsafe_get(p.x, p.y);
			foundAll.grow().set(-val, false, p);
		}

		for (int i = 0; i < originalMax.size; i++) {
			Point2D_I16 p = originalMax.get(i);
			float val = intensity.unsafe_get(p.x, p.y);
			foundAll.grow().set(val, true, p);
		}

		if (maxTotalFeatures > 0) {
			selector.select(intensity, -1, -1, true, null, foundAll, maxTotalFeatures, foundSelected);
		} else {
			foundSelected.clear();
			foundSelected.addAll(foundAll);
		}
	}

	public FastAccess<LocalExtreme> getFeatures() {
		return foundSelected;
	}

	/**
	 * Data structure which provides information on a local extremum.
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class LocalExtreme implements Comparable<LocalExtreme> {
		/**
		 * Absolute value of image intensity
		 */
		public float intensity;
		/** true if it was a maximum (positive) or minimum (negative intensity) */
		public boolean max;
		public Point2D_I16 location;

		public LocalExtreme set( float intensity, boolean max, Point2D_I16 location ) {
			this.intensity = intensity;
			this.max = max;
			this.location = location;
			return this;
		}

		/**
		 * Returns the value of the feature in the intensity image. Adds the sign back
		 */
		public float getIntensitySigned() {
			if (max)
				return intensity;
			else
				return -intensity;
		}

		@Override
		public int compareTo( LocalExtreme o ) {
			if (intensity < o.intensity) {
				return 1;
			} else if (intensity > o.intensity) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
