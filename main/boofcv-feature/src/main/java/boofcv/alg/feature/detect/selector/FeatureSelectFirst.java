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

import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;

/**
 * This selects features in the order they were detected. This is probably the worst built in strategy, primarily
 * included as a way to verify that the others are doing something.
 *
 * @author Peter Abeles
 */
public class FeatureSelectFirst implements FeatureSelectLimit {
	@Override
	public void select(GrayF32 intensity, boolean positive, FastAccess<Point2D_I16> prior,
					   FastAccess<Point2D_I16> detected, int limit, FastQueue<Point2D_I16> selected)
	{
		selected.reset();
		int N = Math.min(detected.size,limit);
		for (int i = 0; i < N; i++) {
			selected.grow().set(detected.get(i));
		}
	}
}
