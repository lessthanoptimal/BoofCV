/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.tracker;

import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;

class TestPointTrackerHybrid extends GenericChecksPointTracker<GrayF32> {

	public TestPointTrackerHybrid() {
		super(true, false);
	}

	@Override
	public PointTracker<GrayF32> createTracker() {
		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.HYBRID;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
		config.detDesc.detectPoint.shiTomasi.radius = 2;
		config.detDesc.detectPoint.general.radius = 2;
		config.detDesc.detectPoint.general.maxFeatures = 100;
		config.detDesc.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
		config.detDesc.describeBrief.fixed = true;
		config.associate.greedy.maxErrorThreshold = 400;

		return FactoryPointTracker.tracker(config, GrayF32.class, null);
	}
}
