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

import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * Converts {@link FeatureSelectLimit} into {@link FeatureSelectLimitIntensity}.
 *
 * @author Peter Abeles
 */
public class ConvertLimitToIntensity<Point> implements FeatureSelectLimitIntensity<Point> {
	FeatureSelectLimit<Point> alg;

	public ConvertLimitToIntensity( FeatureSelectLimit<Point> alg ) {
		this.alg = alg;
	}

	@Override
	public void select( @Nullable GrayF32 intensity, int width, int height, boolean positive,
						@Nullable FastAccess<Point> prior, FastAccess<Point> detected, int limit,
						FastArray<Point> selected ) {
		width = intensity == null ? width : intensity.width;
		height = intensity == null ? height : intensity.height;

		alg.select(width, height, prior, detected, limit, selected);
	}

	@Override public void setSampler( SampleIntensity<Point> sampler ) {}
}
