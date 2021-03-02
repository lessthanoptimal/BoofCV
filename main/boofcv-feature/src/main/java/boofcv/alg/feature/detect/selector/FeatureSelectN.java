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

import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * Selects features periodically in the order they were detected until it hits the limit. This is better than just
 * selecting the first N since features tend to ordered in a very specific way, e.g. top to bottom. you're more likely
 * to get a spread out less biased set this way
 *
 * @author Peter Abeles
 */
public class FeatureSelectN<Point> implements FeatureSelectLimit<Point> {
	@Override
	public void select( int imageWidth, int imageHeight,
						@Nullable FastAccess<Point> prior,
						FastAccess<Point> detected, int limit, FastArray<Point> selected ) {
		final int N = Math.min(detected.size, limit);
		assert (N > 0);
		selected.resize(N);
		for (int i = 0; i < N; i++) {
			int selectedIdx = i*detected.size/N;
			selected.set(i, detected.get(selectedIdx));
		}
	}
}
