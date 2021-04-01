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

import boofcv.abst.feature.orientation.ConfigOrientation2;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.abst.feature.orientation.RegionOrientation;
import boofcv.factory.feature.describe.FactoryDescribePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDescribeSurf_Point extends GenericDescribePointChecks<GrayU8, TupleDesc_F64> {
	protected TestDescribeSurf_Point() {
		super(ImageType.SB_U8);
	}

	@Override protected DescribePoint<GrayU8, TupleDesc_F64> createAlg() {
		var configOrientation = new ConfigOrientation2();
		return FactoryDescribePoint.surfFast(null, configOrientation, 4.5, GrayU8.class);
	}

	/**
	 * Configures it to use the canoncial radius. Verifies that it is passed on to the orientation algorithm.
	 */
	@Test void useCanonicalRadius() {
		HelperOrientation helper = new HelperOrientation();

		var configOrientation = new ConfigOrientation2();
		DescribeSurf_Point<GrayU8,?> alg =
				FactoryDescribePoint.surfFast(null, configOrientation, -1, GrayU8.class);

		alg.orientation = (OrientationIntegral)helper;
		alg.setImage(imageType.createImage(20,20));
		alg.process(1,2, alg.createDescription());
		assertEquals(alg.surf.getCanonicalWidth()/2.0, helper.radius, UtilEjml.TEST_F64);
	}

	private static class HelperOrientation implements OrientationIntegral<GrayS32> {
		double radius;

		@Override public void setObjectRadius( double radius ) {
			this.radius = radius;
		}

		// @formatter:off
		@Override public void setImage( GrayS32 image ) {}
		@Override public Class<GrayS32> getImageType() {return GrayS32.class;}
		@Override public double compute( double c_x, double c_y ) {return 0;}
		@Override public RegionOrientation copy() {return this;}
		// @formatter:on
	}
}
