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

import boofcv.struct.feature.TupleDesc_B;
import org.ddogleg.clustering.PointDistance;

/**
 * @author Peter Abeles
 */
public class TestTuplePointDistanceHamming extends GenericPointDistanceChecks<TupleDesc_B> {
	@Override protected PointDistance<TupleDesc_B> createAlg() {
		return new TuplePointDistanceHamming();
	}

	@Override protected TupleDesc_B createRandomPoint() {
		var desc = new TupleDesc_B(512);
		for (int i = 0; i < desc.data.length; i++) {
			desc.data[i] = rand.nextInt();
		}
		return desc;
	}

	/**
	 * This is a bit tricky. Magnitude makes no sense for a binary descriptor. Instead we will flip more bits
	 */
	@Override protected TupleDesc_B addToPoint( TupleDesc_B src, double magnitude ) {
		TupleDesc_B dst = src.newInstance();
		System.arraycopy(src.data, 0, dst.data, 0, src.data.length);

		// decide on how many bits to flip based on the magnitude of the requested change
		int flip = (int)(src.size()*magnitude/10.0);

		// Randomly select bits to flip
		for (int i = 0; i < flip; i++) {
			int bit = rand.nextInt(src.size());
			dst.setBit(bit, !src.isBitTrue(bit));
		}

		return dst;
	}
}
