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
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectDescribeFusion {

	/**
	 * If a feature is not in bounds make sure everything is handled correctly
	 */
	@Test
	public void checkFeatureNotInBounds() {

		InterestPointDetector detector = new DummyDetector();
		DescribeRegionPoint describe = new DummyRegionPoint();

		DetectDescribeFusion alg = new DetectDescribeFusion(detector,null,describe);

		alg.detect(new GrayF32(2,2));

		// one feature should not be inside the image
		assertEquals(9, alg.getNumberOfFeatures());

		for( int i = 0; i < 9; i++ ) {
			assertEquals(2,alg.getRadius(i),1e-8);
			assertEquals(1,alg.getOrientation(i),1e-8);
			assertTrue(alg.getDescription(i) != null);
			assertTrue(alg.getLocation(i) != null);
		}
	}

	@Test
	public void checkWithOrientation() {
		final InterestPointDetector<GrayF32> detector = FactoryInterestPoint.fastHessian(null);
		final OrientationImage ori = FactoryOrientationAlgs.nogradient(1.0/2.0,5,GrayF32.class);
		final DescribeRegionPoint<GrayF32,BrightFeature> desc =
				FactoryDescribeRegionPoint.surfStable(null, GrayF32.class);

		new GenericTestsDetectDescribePoint(true,true, ImageType.single(GrayF32.class),BrightFeature.class) {

			@Override
			public DetectDescribePoint createDetDesc() {
				return new DetectDescribeFusion(detector,ori,desc);
			}
		}.allTests();
	}

	@Test
	public void checkWithoutOrientation() {
		final InterestPointDetector<GrayF32> detector = FactoryInterestPoint.fastHessian(null);
		final DescribeRegionPoint<GrayF32,BrightFeature> desc =
				FactoryDescribeRegionPoint.surfStable(null, GrayF32.class);

		new GenericTestsDetectDescribePoint(true,false, ImageType.single(GrayF32.class),BrightFeature.class) {

			@Override
			public DetectDescribePoint createDetDesc() {
				return new DetectDescribeFusion(detector,null,desc);
			}
		}.allTests();
	}

	public static class DummyDetector implements InterestPointDetector {

		@Override
		public void detect(ImageBase input) {}

		@Override
		public int getNumberOfFeatures() {
			return 10;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return new Point2D_F64();
		}

		@Override
		public double getRadius(int featureIndex) {
			return 2;
		}

		@Override
		public double getOrientation(int featureIndex) {
			return 1;
		}

		@Override
		public boolean hasScale() {
			return true;
		}

		@Override
		public boolean hasOrientation() {
			return true;
		}
	}

	public static class DummyRegionPoint implements DescribeRegionPoint {

		int calls = 0;

		@Override
		public void setImage(ImageBase image) {}

		@Override
		public TupleDesc createDescription() {
			return new BrightFeature(10);
		}

		@Override
		public boolean process(double x, double y, double orientation, double radius, TupleDesc ret) {
			return calls++ != 5;
		}

		@Override
		public boolean requiresRadius() {
			return false;
		}

		@Override
		public boolean requiresOrientation() {
			return false;
		}

		@Override
		public Class getDescriptionType() {
			return BrightFeature.class;
		}

		@Override
		public ImageType getImageType() {return null;}

		@Override
		public double getCanonicalWidth() {
			throw new RuntimeException("Foo");
		}
	}
}
