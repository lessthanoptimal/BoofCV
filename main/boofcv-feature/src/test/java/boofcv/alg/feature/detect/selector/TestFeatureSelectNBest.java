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

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectNBest extends ChecksFeatureSelectLimitIntensity.I16 {

	@Override
	public FeatureSelectNBest<Point2D_I16> createAlgorithm() {
		return new FeatureSelectNBest<>(new SampleIntensityImage.I16());
	}

	/**
	 * The size of N is less than the number of points
	 */
	@Test void tooFewFeatures() {
		GrayF32 intensity = new GrayF32(10, 20);
		intensity.set(5, 10, -3);
		intensity.set(4, 10, -3.5f);
		intensity.set(5, 11, 0);
		intensity.set(8, 8, 10);

		QueueCorner detected = new QueueCorner();
		detected.append(5, 10);
		detected.append(4, 10);
		detected.append(5, 11);
		detected.append(8, 8);

		var found = new FastArray<>(Point2D_I16.class);
		FeatureSelectNBest<Point2D_I16> alg = new FeatureSelectNBest<>(new SampleIntensityImage.I16());
		alg.select(intensity, -1, -1, true, null, detected, 20, found);

		assertEquals(4, found.size);
	}

	@Nested
	public class CheckNoImage extends NoImage {
		@Override public FeatureSelectLimitIntensity<IntensityPoint> createAlgorithm() {
			return new FeatureSelectNBest<>(new SampleIntensityPoint());
		}
	}
}
