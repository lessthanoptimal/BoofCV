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

package boofcv.abst.feature.detect.interest;

import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestInterestPointDetectorOverride {

	@Test
	public void checkNoOverride_flags() {
		DummyPoint p = new DummyPoint(true,true);

		InterestPointDetectorOverride alg = new InterestPointDetectorOverride(p,null);

		assertTrue(alg.hasOrientation());
		assertTrue(alg.hasScale());

		p = new DummyPoint(true,false);
		alg = new InterestPointDetectorOverride(p,null);

		assertTrue(alg.hasOrientation());
		assertFalse(alg.hasScale());

		p = new DummyPoint(false,true);
		alg = new InterestPointDetectorOverride(p,null);

		assertFalse(alg.hasOrientation());
		assertTrue(alg.hasScale());
	}

	@Test
	public void checkOrientation_flags() {
		DummyPoint p = new DummyPoint(true,true);
		DummyOrientation o = new DummyOrientation();

		InterestPointDetectorOverride alg = new InterestPointDetectorOverride(p,o);

		assertTrue(alg.hasOrientation());
		assertTrue(alg.hasScale());

		p = new DummyPoint(true,false);
		alg = new InterestPointDetectorOverride(p,o);

		assertTrue(alg.hasOrientation());
		assertFalse(alg.hasScale());

		p = new DummyPoint(false,true);
		alg = new InterestPointDetectorOverride(p,o);

		assertTrue(alg.hasOrientation());
		assertTrue(alg.hasScale());
	}

	@Test
	public void checkOrientation_process() {
		DummyPoint p = new DummyPoint(true,true);
		DummyOrientation o = new DummyOrientation();

		InterestPointDetectorOverride alg = new InterestPointDetectorOverride(p,o);

		alg.detect(null);
		assertTrue(o.setImage);

		alg.getOrientation(0);
		assertTrue(o.setRadius);
	}

	private static class DummyPoint implements InterestPointDetector {

		public boolean scale;
		public boolean orientation;

		private DummyPoint(boolean orientation, boolean scale) {
			this.orientation = orientation;
			this.scale = scale;
		}

		@Override
		public void detect(ImageBase input) {}

		@Override
		public int getNumberOfFeatures() {return 2;}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {return new Point2D_F64();}

		@Override
		public double getRadius(int featureIndex) {return 1;}

		@Override
		public double getOrientation(int featureIndex) {return 2;}

		@Override
		public boolean hasScale() {return scale;}

		@Override
		public boolean hasOrientation() {return orientation;}
	}

	private static class DummyOrientation implements OrientationImage {

		boolean setImage = false;
		boolean setRadius = false;

		@Override
		public void setImage(ImageGray image) { setImage =true;}

		@Override
		public Class getImageType() {return null;}

		@Override
		public void setObjectRadius(double radius) {setRadius =true;}

		@Override
		public double compute(double c_x, double c_y) {return 3;}
	}
}
