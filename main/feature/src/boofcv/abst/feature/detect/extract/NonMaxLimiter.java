/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastQueue;

/**
 * Adds the ability to specify the maximum number of points that you wish to return.  The selected
 * points will be sorted by feature intensity.  If maximums and minimums are found then the total
 * number refers to the total combined number of features.  The intensity that it sorts by is the absolute value.
 *
 * @author Peter Abeles
 */
public class NonMaxLimiter {
	NonMaxSuppression nonmax;

	int maxTotalFeatures;

	QueueCorner originalMin = new QueueCorner();
	QueueCorner originalMax = new QueueCorner();

	FastQueue<LocalExtreme> localExtreme = new FastQueue<>(LocalExtreme.class, true);

	/**
	 * Configures the limiter
	 * @param nonmax Non-maximum suppression algorithm
	 * @param maxTotalFeatures The total number of allowed features it can return.  Set to a value &le; 0 to disable.
	 */
	public NonMaxLimiter(NonMaxSuppression nonmax, int maxTotalFeatures) {
		this.nonmax = nonmax;
		if( maxTotalFeatures <= 0 )
			this.maxTotalFeatures = Integer.MAX_VALUE;
		else
			this.maxTotalFeatures = maxTotalFeatures;
	}

	/**
	 * Extracts local max and/or min from the intensity image.  If more than the maximum features are found then
	 * only the most intense ones will be returned
	 * @param intensity Feature image intensity
	 */
	public void process(GrayF32 intensity ) {

		originalMin.reset();
		originalMax.reset();
		nonmax.process(intensity,null,null,originalMin,originalMax);

		localExtreme.reset();
		for (int i = 0; i < originalMin.size; i++) {
			Point2D_I16 p = originalMin.get(i);
			float val = intensity.unsafe_get(p.x,p.y);
			localExtreme.grow().set(-val,false,p);
		}
		for (int i = 0; i < originalMax.size; i++) {
			Point2D_I16 p = originalMax.get(i);
			float val = intensity.unsafe_get(p.x, p.y);
			localExtreme.grow().set(val,true,p);
		}

		if( localExtreme.size > maxTotalFeatures ) {
			QuickSelect.select(localExtreme.data, maxTotalFeatures, localExtreme.size);
			localExtreme.size = maxTotalFeatures;
		}
	}

	public FastQueue<LocalExtreme> getLocalExtreme() {
		return localExtreme;
	}

	public int getMaxTotalFeatures() {
		return maxTotalFeatures;
	}

	public void setMaxTotalFeatures(int maxTotalFeatures) {
		this.maxTotalFeatures = maxTotalFeatures;
	}

	public NonMaxSuppression getNonmax() {
		return nonmax;
	}

	/**
	 * Data structure which provides information on a local extremum.
	 */
	public static class LocalExtreme implements Comparable<LocalExtreme>{
		/**
		 * Absolute value of image intensity
		 */
		public float intensity;
		public boolean max;
		public Point2D_I16 location;

		public LocalExtreme set( float intensity , boolean max , Point2D_I16 location ){
			this.intensity = intensity;
			this.max = max;
			this.location = location;
			return this;
		}

		/**
		 * Returns the value of the feature in the intensity image.  Adds the sign back
		 */
		public float getValue() {
			if( max )
				return intensity;
			else
				return -intensity;
		}

		@Override
		public int compareTo(LocalExtreme o) {
			if (intensity < o.intensity) {
				return 1;
			} else if( intensity > o.intensity ) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
