/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.abst.feature.describe.WrapDescribeBrief;
import boofcv.abst.feature.detdesc.DetectDescribeFusion;
import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointBrief;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDetectAssociateTracker extends StandardImagePointTracker<ImageFloat32> {

	DetectAssociateTracker<ImageFloat32,TupleDesc_B> dat;

	public TestDetectAssociateTracker() {
		super(true, false);
	}

	/**
	 * Make sure drop track is correctly recycling the data
	 */
	@Test
	public void dropTrack_Recycle() {
		createTracker();

		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());

		PointTrack a = dat.tracksAll.get(1);

		assertEquals(0,dat.unused.size());
		dat.dropTrack(a);
		assertEquals(1,dat.unused.size());

		assertEquals(2,dat.tracksAll.size());
	}

	/**
	 * Make sure drop all tracks is correctly recycling the data
	 */
	@Test
	public void dropAllTracks_Recycle() {
		createTracker();

		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());
		dat.tracksAll.add(dat.getUnused());

		dat.dropAllTracks();
		assertEquals(3,dat.unused.size());
		assertEquals(0, dat.tracksAll.size());
	}

	@Override
	public ImagePointTracker<ImageFloat32> createTracker() {
		DescribePointBrief<ImageFloat32> brief =
				FactoryDescribePointAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(ImageFloat32.class, 0, 4));

		GeneralFeatureDetector<ImageFloat32,ImageFloat32> corner =
				FactoryDetectPoint.createShiTomasi(2, false, 0, 100, ImageFloat32.class);

		InterestPointDetector<ImageFloat32> detector =
				FactoryInterestPoint.wrapPoint(corner, 1,ImageFloat32.class, ImageFloat32.class);
		ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

		AssociateDescription2D<TupleDesc_B> association =
				new AssociateDescTo2D<TupleDesc_B>(FactoryAssociation.greedy(score, 400, 300, true));

		DetectDescribeFusion<ImageFloat32,TupleDesc_B> fused =
				new DetectDescribeFusion<ImageFloat32,TupleDesc_B>(
						detector,null,new WrapDescribeBrief<ImageFloat32>(brief));

		dat = new DetectAssociateTracker<ImageFloat32,TupleDesc_B>(fused, association,false);


		return dat;
	}
}
