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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNonMaxLimiter extends BoofStandardJUnit {
	@Test void checkNoLimit() {
		GrayF32 intensity = new GrayF32(30,25);
		intensity.set(10,15,20);
		intensity.set(12,20,25);

		intensity.set(1,15,-20);
		intensity.set(1,20,-25);

		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1,0,0,true,true,true));
		FeatureSelectLimitIntensity<NonMaxLimiter.LocalExtreme> selector =
				FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
		NonMaxLimiter limiter = new NonMaxLimiter(extractor,selector,20);

		limiter.process(intensity);

		FastAccess<NonMaxLimiter.LocalExtreme> found = limiter.getFeatures();
		assertEquals(4,found.size());

		for (int i = 0; i < found.size(); i++) {
			NonMaxLimiter.LocalExtreme a = found.get(i);
			assertEquals(a.getIntensitySigned(),intensity.get(a.location.x,a.location.y));
			assertEquals( a.getIntensitySigned() > 0 , a.max);
		}
	}

	/**
	 * 4 features, but a max of 2 is requested.
	 */
	@Test void checkLimit() {
		GrayF32 intensity = new GrayF32(30,25);
		intensity.set(10,15,20);
		intensity.set(12,20,25);

		intensity.set(1,15,-20);
		intensity.set(1,20,-25);

		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1,0,0,true,true,true));
		FeatureSelectLimitIntensity<NonMaxLimiter.LocalExtreme> selector =
				FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
		NonMaxLimiter limiter = new NonMaxLimiter(extractor,selector,2);

		limiter.process(intensity);

		FastAccess<NonMaxLimiter.LocalExtreme> found = limiter.getFeatures();
		assertEquals(2,found.size());

		for (int i = 0; i < found.size(); i++) {
			NonMaxLimiter.LocalExtreme a = found.get(i);
			assertEquals(a.getIntensitySigned(),intensity.get(a.location.x,a.location.y));
			assertEquals( a.getIntensitySigned() > 0 , a.max);
			assertEquals(25,a.intensity,1e-8);

		}
	}
}
