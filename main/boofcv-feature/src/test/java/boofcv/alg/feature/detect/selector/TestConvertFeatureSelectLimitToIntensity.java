/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestConvertFeatureSelectLimitToIntensity extends BoofStandardJUnit {
	@Test
	void callTheFunction() {
		Dummy<Point2D_I16> dummy = new Dummy<>();

		var intensity = new GrayF32(30, 40);

		FeatureSelectLimitIntensity<Point2D_I16> wrapped = new ConvertLimitToIntensity<>(dummy);

		var detected = new DogArray<>(Point2D_I16::new);
		var selected = new FastArray<>(Point2D_I16.class);

		wrapped.select(intensity, -1, -1, true, null, detected, 100, selected);

		assertEquals(intensity.width, dummy.width);
		assertEquals(intensity.height, dummy.height);
		assertEquals(100, dummy.limit);
	}

	@Nested
	public class CheckNoImage extends ChecksFeatureSelectLimitIntensity.NoImage {
		@Override public FeatureSelectLimitIntensity<IntensityPoint> createAlgorithm() {
			var selector = new FeatureSelectN<IntensityPoint>();
			var alg = new ConvertLimitToIntensity<>(selector);
			alg.setSampler(new SampleIntensityPoint());
			return alg;
		}
	}

	private static class Dummy<Point> implements FeatureSelectLimit<Point> {

		int width, height, limit;

		@Override
		public void select( int imageWidth, int imageHeight, @Nullable FastAccess<Point> prior,
							FastAccess<Point> detected, int limit, FastArray<Point> selected ) {
			this.width = imageWidth;
			this.height = imageHeight;
			this.limit = limit;
		}
	}
}
