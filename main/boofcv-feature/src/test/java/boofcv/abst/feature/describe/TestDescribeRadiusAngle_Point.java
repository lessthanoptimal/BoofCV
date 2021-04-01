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

package boofcv.abst.feature.describe;

import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeRadiusAngle_Point extends BoofStandardJUnit {
	/**
	 * Checks to see if all the expected values are passed through
	 */
	@Test void basic() {
		var describe = new HelperDescribe();
		var orientation = new HelperOrientation();

		var alg = new DescribeRadiusAngle_Point<>(describe, orientation, 7.7);

		TupleDesc_F64 found = alg.createDescription();

		alg.setImage(new GrayU8(1, 1));
		alg.process(1, 2, found);

		assertEquals(1, describe.x);
		assertEquals(2, describe.y);
		assertEquals(-2.3, describe.orientation);
		assertEquals(1, orientation.c_x);
		assertEquals(2, orientation.c_y);
		assertEquals(7.7, orientation.radius);
		assertTrue(describe.setCalled);
		assertEquals(1.0, found.data[0]);
	}

	/**
	 * Tell it use the canonical radius
	 */
	@Test void canonical() {
		var describe = new HelperDescribe();
		var orientation = new HelperOrientation();

		var alg = new DescribeRadiusAngle_Point<>(describe, orientation, 0.0);

		alg.setImage(new GrayU8(1, 1));
		alg.process(1, 2, alg.createDescription());
		assertEquals(5.5/2.0, orientation.radius);
	}

	private static class HelperDescribe implements DescribePointRadiusAngle<GrayU8, TupleDesc_F64> {
		double x;
		double y;
		double orientation;
		double radius;
		boolean setCalled;

		@Override
		public boolean process( double x, double y, double orientation, double radius, TupleDesc_F64 description ) {
			this.x = x;
			this.y = y;
			this.orientation = orientation;
			this.radius = radius;
			description.data[0] = 1;
			return false;
		}

		// @formatter:off
		@Override public void setImage( GrayU8 image ) {setCalled = true;}
		@Override public boolean isScalable() {return true;}
		@Override public boolean isOriented() {return false;}
		@Override public ImageType<GrayU8> getImageType() {return ImageType.SB_U8;}
		@Override public double getCanonicalWidth() {return 5.5;}
		@Override public TupleDesc_F64 createDescription() {return new TupleDesc_F64(2);}
		@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;}
		// @formatter:on
	}

	private static class HelperOrientation implements OrientationImage<GrayU8> {
		double radius;
		double c_x, c_y;

		@Override public double compute( double c_x, double c_y ) {
			this.c_x = c_x;
			this.c_y = c_y;
			return -2.3;
		}

		// @formatter:off
		@Override public RegionOrientation copy() {return this;}
		@Override public void setImage( GrayU8 image ) {}
		@Override public Class<GrayU8> getImageType() {return GrayU8.class;}
		@Override public void setObjectRadius( double radius ) {this.radius = radius;}
		// @formatter:on
	}
}
