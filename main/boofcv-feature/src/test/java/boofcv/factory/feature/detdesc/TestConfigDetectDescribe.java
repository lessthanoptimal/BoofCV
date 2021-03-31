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

package boofcv.factory.feature.detdesc;

import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.struct.Configuration;
import boofcv.struct.StandardConfigurationChecks;

import java.util.Random;

/**
 * @author Peter Abeles
 */
class TestConfigDetectDescribe extends StandardConfigurationChecks {
	public TestConfigDetectDescribe() {
		super(ConfigDetectDescribe.class);
	}

	@Override
	public Configuration createNotDefault(Random rand) {
		ConfigDetectDescribe out = new ConfigDetectDescribe();
		out.typeDescribe = ConfigDescribeRegion.Type.SURF_STABLE;
		out.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		return out;
	}
}
