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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.DetectorInterestPointMulti;
import boofcv.abst.feature.detect.interest.FoundPointSO;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestDetectDescribeMultiFusion {

	/**
	 * If a feature is not in bounds make sure everything is handled correctly
	 */
	@Test
	public void checkFeatureNotInBounds() {

		DetectorInterestPointMulti detector = new DummyDetector(2);
		DescribeRegionPoint describe = new TestDetectDescribeFusion.DummyRegionPoint();
		DetectDescribeMultiFusion alg = new DetectDescribeMultiFusion(detector,null,describe);

		alg.process(new GrayF32(2,2));

		assertEquals(2,alg.getNumberOfSets());

		for( int n = 0; n < alg.getNumberOfSets(); n++ ) {
			PointDescSet set = alg.getFeatureSet(n);

			// one feature should not be inside the image
			if( n == 0 )
				assertEquals(n+8, set.getNumberOfFeatures());
			else
				assertEquals(n+9, set.getNumberOfFeatures());

			for( int i = 0; i < set.getNumberOfFeatures(); i++ ) {
				assertTrue(set.getDescription(i) != null);
				assertTrue(set.getLocation(i) != null);
			}
		}
	}

	@Test
	public void checkWithOrientation() {
		final InterestPointDetector<GrayF32> detector = FactoryInterestPoint.fastHessian(null);
		final OrientationImage ori = FactoryOrientationAlgs.nogradient(5,5, GrayF32.class);
		final DescribeRegionPoint<GrayF32,BrightFeature> desc =
				FactoryDescribeRegionPoint.surfStable(null, GrayF32.class);

		new GenericTestsDetectDescribeMulti(GrayF32.class,BrightFeature.class) {
			@Override
			public DetectDescribeMulti createDetDesc() {
				DetectDescribePoint ddp = new DetectDescribeFusion(detector,ori,desc);
				return new DetectDescribeSingleToMulti(ddp);
			}
		}.allTests();
	}

	@Test
	public void checkWithoutOrientation() {
		final InterestPointDetector<GrayF32> detector = FactoryInterestPoint.fastHessian(null);
		final DescribeRegionPoint<GrayF32,BrightFeature> desc =
				FactoryDescribeRegionPoint.surfStable(null, GrayF32.class);

		new GenericTestsDetectDescribeMulti(GrayF32.class,BrightFeature.class) {

			@Override
			public DetectDescribeMulti createDetDesc() {
				DetectDescribePoint ddp = new DetectDescribeFusion(detector,null,desc);
				return new DetectDescribeSingleToMulti(ddp);
			}
		}.allTests();
	}

	private static class DummyDetector implements DetectorInterestPointMulti {

		List<Found> sets = new ArrayList<>();

		public DummyDetector( int num ) {
			for( int i = 0; i < num; i++ ) {
				sets.add( new Found(i+9));
			}
		}

		@Override
		public void detect(ImageBase input) {
		}

		@Override
		public int getNumberOfSets() {
			return sets.size();
		}

		@Override
		public FoundPointSO getFeatureSet(int set) {
			return sets.get(set);
		}
	}

	private static class Found implements FoundPointSO {

		int num;

		private Found(int num) {
			this.num = num;
		}

		@Override
		public int getNumberOfFeatures() {
			return num;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return new Point2D_F64(1,2);
		}

		@Override
		public double getRadius(int featureIndex) {
			return 2;
		}

		@Override
		public double getOrientation(int featureIndex) {
			return 0.5;
		}
	}
}
