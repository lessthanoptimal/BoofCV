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
import boofcv.struct.feature.*;
import org.ddogleg.clustering.PointDistance;

/**
 * Euclidean squared distance between Tuple descriptors for {@link PointDistance}.
 *
 * @author Peter Abeles
 */
public abstract class TuplePointDistanceEuclideanSq<T extends TupleDesc<T>> implements PointDistance<T> {
	@Override public PointDistance<T> newInstanceThread() {return this;}

	public static class F64 extends TuplePointDistanceEuclideanSq<TupleDesc_F64> {
		@Override public double distance( TupleDesc_F64 a, TupleDesc_F64 b ) {
			return DescriptorDistance.euclideanSq(a, b);
		}
	}

	public static class F32 extends TuplePointDistanceEuclideanSq<TupleDesc_F32> {
		@Override public double distance( TupleDesc_F32 a, TupleDesc_F32 b ) {
			return DescriptorDistance.euclideanSq(a, b);
		}
	}

	public static class U8 extends TuplePointDistanceEuclideanSq<TupleDesc_U8> {
		@Override public double distance( TupleDesc_U8 a, TupleDesc_U8 b ) {
			return DescriptorDistance.euclideanSq(a, b);
		}
	}

	public static class S8 extends TuplePointDistanceEuclideanSq<TupleDesc_S8> {
		@Override public double distance( TupleDesc_S8 a, TupleDesc_S8 b ) {
			return DescriptorDistance.euclideanSq(a, b);
		}
	}
}
