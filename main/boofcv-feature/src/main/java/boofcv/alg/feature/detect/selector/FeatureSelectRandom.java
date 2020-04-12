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
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Random;

/**
 * Randomly selects features up to the limit from the set of detected. This is actually not as bad of an approach
 * as it might seem. Could be viewed as a less effective version of {@link FeatureSelectUniformBest}.
 *
 * @author Peter Abeles
 */
public class FeatureSelectRandom implements FeatureSelectLimit {

	// Random number generator used to select points
	final Random rand;

	// Work space
	private GrowQueue_I32 indexes = new GrowQueue_I32();

	public FeatureSelectRandom(long seed ) {
		rand = new Random(seed);
	}

	@Override
	public void select(GrayF32 intensity, boolean positive,
					   FastAccess<Point2D_I16> prior, FastAccess<Point2D_I16> detected, int limit,
					   FastQueue<Point2D_I16> selected)
	{
		selected.reset();

		// the limit is more than the total number of features. Return them all!
		if( detected.size <= limit ) {
			BoofMiscOps.copyAll_2D_I16(detected,selected);
			return;
		}

		// Create an array with a sequence of numbers
		indexes.resize(detected.size);
		for (int i = 0; i < detected.size; i++) {
			indexes.data[i] = i;
		}

		// randomly select points up to the limit
		for (int i = 0; i < limit; i++) {
			int idx = rand.nextInt(indexes.size-i);
			selected.grow().set( detected.data[ indexes.data[idx] ]);
			// copy an unused value over the used value
			indexes.data[idx] = indexes.data[indexes.size-i-1];
		}
	}
}
