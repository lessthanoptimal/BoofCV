/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.interest;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestFactoryDetectPoint extends BoofStandardJUnit {
	/**
	 * Go through every detector type and see if any of them blow up
	 */
	@Test void checkBlowUp() {
		var config = new ConfigPointDetector();
		for (PointDetectorTypes type : PointDetectorTypes.values()) {
			config.type = type;
			FactoryDetectPoint.create(config, GrayU8.class, null);
			FactoryDetectPoint.create(config, GrayF32.class, null);
		}
	}
}
