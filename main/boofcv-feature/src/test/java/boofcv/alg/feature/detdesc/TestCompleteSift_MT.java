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

import boofcv.abst.feature.detdesc.ConfigCompleteSift;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detdesc.FactoryDetectDescribeAlgs;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCompleteSift_MT extends BoofStandardJUnit {
	ConfigCompleteSift config = new ConfigCompleteSift();

	{
		config.detector.maxFeaturesAll = 500;
	}

	@Test void compareToSingleThread() {
		GrayF32 image = new GrayF32(300, 290);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		BoofConcurrency.USE_CONCURRENT = false;
		CompleteSift single = FactoryDetectDescribeAlgs.sift(config);
		BoofConcurrency.USE_CONCURRENT = true;
		CompleteSift multi = FactoryDetectDescribeAlgs.sift(config);


		single.process(image);
		multi.process(image);

		assertEquals(128, single.getDescriptorLength());
		assertEquals(128, multi.getDescriptorLength());

		assertEquals(single.getLocations().size, multi.getLocations().size);
		int N = single.getLocations().size;

		for (int i = 0; i < N; i++) {
			ScalePoint sp = single.getLocations().get(i);
			ScalePoint mp = multi.getLocations().get(i);

			assertEquals(sp.intensity, mp.intensity);
			assertEquals(sp.scale, mp.scale);
			assertEquals(0.0, sp.pixel.distance(mp.pixel));
			assertEquals(single.getOrientations().get(i), multi.getOrientations().get(i));

			TupleDesc_F64 sd = single.getDescriptions().get(i);
			TupleDesc_F64 md = multi.getDescriptions().get(i);

			assertEquals(0.0, DescriptorDistance.euclidean(sd,md));
		}
	}
}
