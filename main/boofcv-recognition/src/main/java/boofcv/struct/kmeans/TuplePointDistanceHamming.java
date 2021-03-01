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

package boofcv.struct.kmeans;

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.feature.TupleDesc_B;
import org.ddogleg.clustering.PointDistance;

/**
 * Computes hamming distance for binary descriptors
 *
 * @author Peter Abeles
 */
public class TuplePointDistanceHamming implements PointDistance<TupleDesc_B> {
	@Override public double distance( TupleDesc_B a, TupleDesc_B b ) {
		// divide by the size to keep ensure the value returned is from 0 to 1.0. This tends to have less numerical
		// issues are things scale up
		return DescriptorDistance.hamming(a,b)/(double)a.size();
	}

	@Override public PointDistance<TupleDesc_B> newInstanceThread() {
		return this;
	}
}
