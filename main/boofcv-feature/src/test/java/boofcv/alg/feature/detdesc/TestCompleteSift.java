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

package boofcv.alg.feature.detdesc;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only very basic tests are done here
 *
 * @author Peter Abeles
 */
public class TestCompleteSift extends BoofStandardJUnit {

	/**
	 * Doesn't do much more than see if it blows up and the expected size of objects is returned
	 */
	@Test void basic() {
		GrayF32 image = new GrayF32(300, 290);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		CompleteSift alg = createAlg();

		alg.process(image);

		assertEquals(128, alg.getDescriptorLength());

		DogArray_F64 orientations = alg.getOrientations();
		FastAccess<ScalePoint> locations = alg.getLocations();
		FastAccess<TupleDesc_F64> descriptions = alg.getDescriptions();

		assertTrue(orientations.size > 10);
		assertEquals(orientations.size, locations.size);
		assertEquals(orientations.size, descriptions.size);
	}

	/**
	 * If a maximum number of features returned is specified make sure this setting is obeyed.
	 *
	 * There was a bug where this was ignored
	 */
	@Test void respectMaxAll() {
		int desiredMax = 10;

		GrayF32 image = new GrayF32(300, 290);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		CompleteSift alg = createAlg();
		alg.detector.maxFeaturesAll = desiredMax;
		alg.process(image);

		// Testing max feature is a bit problematic. While the detector respects it, each detection can have more
		// than one angle resulting in multiple features
		assertTrue(desiredMax*2 >= alg.getLocations().size);
		assertEquals( alg.getLocations().size, alg.getOrientations().size);
		assertEquals( alg.getLocations().size, alg.getDescriptions().size);
	}

	private CompleteSift createAlg() {
		SiftScaleSpace ss = new SiftScaleSpace(-1, 4, 3, 1.6);

		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0, 1, true, true, true));
		NonMaxLimiter limiter = new NonMaxLimiter(
				nonmax, FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN()), 300);
		OrientationHistogramSift<GrayF32> ori =
				new OrientationHistogramSift<>(36, 1.5, GrayF32.class);
		DescribePointSift<GrayF32> describe =
				new DescribePointSift<>(4, 4, 8, 1.5, 0.5, 0.2, GrayF32.class);
		SiftDetector detector = new SiftDetector(FactorySelectLimit.
				intensity(ConfigSelectLimit.selectBestN()), 10, limiter);

		return new CompleteSift(ss, detector, ori, describe);
	}
}
