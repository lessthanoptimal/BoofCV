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

package boofcv.factory.feature.detdesc;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestFactoryDetectDescribe extends BoofStandardJUnit {
	/**
	 * Go through different types of detectors an descriptors and see if anything blows up
	 */
	@Test void checkBlowUp() {
		var config = new ConfigDetectDescribe();
		for (var type : ConfigDetectInterestPoint.Type.values()) {
			config.typeDetector = type;
			if (type == ConfigDetectInterestPoint.Type.POINT) {
				for (var pointType : PointDetectorTypes.values()) {
					config.detectPoint.type = pointType;
					FactoryDetectDescribe.generic(config, GrayU8.class);
					FactoryDetectDescribe.generic(config, GrayF32.class);
				}
			} else {
				FactoryDetectDescribe.generic(config, GrayU8.class);
				FactoryDetectDescribe.generic(config, GrayF32.class);
			}
		}

		for (var type : ConfigDescribeRegion.Type.values()) {
			if (type.name().toLowerCase().contains("color"))
				continue;
			config.typeDescribe = type;
			FactoryDetectDescribe.generic(config, GrayU8.class);
			FactoryDetectDescribe.generic(config, GrayF32.class);
		}
	}
}
