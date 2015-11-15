/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastQueue;

/**
 * Adds the ability to specify the maximum number of points that you wish to return.  The selected
 * points will be sorted by feature intensity
 *
 * @author Peter Abeles
 */
public class NonMaxLimiter {
	NonMaxSuppression nonmax;

	int maxTotalFeatures;

	QueueCorner originalMin = new QueueCorner();
	QueueCorner originalMax = new QueueCorner();

	FastQueue<LocalExtreme> localExtreme = new FastQueue<LocalExtreme>(LocalExtreme.class,true);

	public NonMaxLimiter(NonMaxSuppression nonmax, int maxTotalFeatures) {
		this.nonmax = nonmax;
		this.maxTotalFeatures = maxTotalFeatures;
	}

	public void process(ImageFloat32 intensity ) {

		originalMin.reset();
		originalMax.reset();
		nonmax.process(intensity,null,null,originalMin,originalMax);

		localExtreme.reset();
		for (int i = 0; i < originalMin.size; i++) {
			Point2D_I16 p = originalMin.get(i);
			float val = intensity.unsafe_get(p.x,p.y);
			localExtreme.grow().set(val,false,p);
		}
		for (int i = 0; i < originalMax.size; i++) {
			Point2D_I16 p = originalMax.get(i);
			float val = intensity.unsafe_get(p.x, p.y);
			localExtreme.grow().set(-val,true,p);
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

	public static class LocalExtreme implements Comparable<LocalExtreme>{
		public float intensity;
		public boolean max;
		public Point2D_I16 location;

		public LocalExtreme set( float intensity , boolean max , Point2D_I16 location ){
			this.intensity = intensity;
			this.max = max;
			this.location = location;
			return this;
		}

		public float getValue() {
			if( max )
				return -intensity;
			else
				return intensity;
		}

		@Override
		public int compareTo(LocalExtreme o) {
			if (intensity > o.intensity) {
				return 1;
			} else if( intensity < o.intensity ) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
