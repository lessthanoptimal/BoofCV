/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.junit.Test;

import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Only very basic tests are done here
 *
 * @author Peter Abeles
 */
public class TestCompleteSift {

	Random rand = new Random(234);

	/**
	 * Doesn't do much more than see if it blows up and the expected size of objects is returned
	 */
	@Test
	public void basic() {
		GrayF32 image = new GrayF32(300,290);
		GImageMiscOps.fillUniform(image,rand,0,200);

		CompleteSift alg = createAlg();

		alg.process(image);

		assertEquals(128,alg.getDescriptorLength());

		GrowQueue_F64 orientations = alg.getOrientations();
		FastQueue<ScalePoint> locations = alg.getLocations();
		FastQueue<BrightFeature> descriptions = alg.getDescriptions();

		assertTrue(orientations.size>10);
		assertEquals(orientations.size,locations.size);
		assertEquals(orientations.size,descriptions.size);
	}

	private CompleteSift createAlg() {

		SiftScaleSpace ss = new SiftScaleSpace(-1,4,3,1.6);

		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(1,0,1,true,true,true));
		NonMaxLimiter limiter = new NonMaxLimiter(nonmax,300);
		OrientationHistogramSift<GrayF32> ori =
				new OrientationHistogramSift<>(36,1.5,GrayF32.class);
		DescribePointSift<GrayF32> describe =
				new DescribePointSift<>(4,4,8,1.5,0.5,0.2,GrayF32.class);

		return new CompleteSift(ss,10,limiter,ori,describe);
	}
}