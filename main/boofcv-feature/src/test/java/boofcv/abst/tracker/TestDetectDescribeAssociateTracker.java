/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public class TestDetectDescribeAssociateTracker extends GenericChecksPointTracker<GrayF32> {

	protected TestDetectDescribeAssociateTracker() {
		super(true, false);
	}

	@Override
	public PointTracker<GrayF32> createTracker() {
		ConfigPointTracker config = new ConfigPointTracker();
		config.typeTracker = ConfigPointTracker.TrackerType.DDA;
		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		config.detDesc.detectPoint.shiTomasi.radius = 3;
		config.detDesc.detectPoint.general.radius = 3;
		config.detDesc.typeDescribe = ConfigDescribeRegionPoint.DescriptorType.BRIEF;
		config.detDesc.describeBrief.fixed = true;

		return FactoryPointTracker.tracker(config,GrayF32.class,null);
	}

	@Test
	void pruneExcessiveInactiveTracks() {
		fail("Implement");
	}
}
