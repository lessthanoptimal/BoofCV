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

package boofcv.abst.feature.tracker;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.abst.feature.describe.WrapDescribeBrief;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.GrayF32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestPointTrackerCombined extends StandardPointTracker<GrayF32> {

	public TestPointTrackerCombined() {
		super(true, false);
	}

	@Override
	public PointTracker<GrayF32> createTracker() {
		DescribePointBrief<GrayF32> brief = FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(GrayF32.class, 0, 4));

		GeneralFeatureDetector<GrayF32,GrayF32> corner =
				FactoryDetectPoint.createShiTomasi(new ConfigGeneralDetector(100,2,0), false, GrayF32.class);

		InterestPointDetector<GrayF32> detector =
				FactoryInterestPoint.wrapPoint(corner, 1,GrayF32.class, GrayF32.class);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription<TupleDesc_B> association =
				FactoryAssociation.greedy(score, 400, true);

		PointTracker<GrayF32> pointTracker = FactoryPointTracker.combined(
				detector, null,
				new WrapDescribeBrief<>(brief,GrayF32.class),
				association, null, 20, GrayF32.class);

		return pointTracker;
	}
}
