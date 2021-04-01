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

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDescribeSift_RadiusAngle extends GenericDescribePointRadiusAngleChecks<GrayF32, TupleDesc_F64> {

	TestDescribeSift_RadiusAngle() {
		super(ImageType.single(GrayF32.class));
	}

	@Test
	void flags() {
		DescribeSift_RadiusAngle<GrayF32> alg = declare();

		assertTrue(alg.isOriented());
		assertTrue(alg.isScalable());

		TupleDesc_F64 desc = alg.createDescription();
		assertEquals(128, desc.size());

		assertEquals(2*alg.describe.getCanonicalRadius(), alg.getCanonicalWidth(), 1e-8);
	}

	@Test
	void process() {
		GrayF32 image = new GrayF32(640, 480);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		DescribeSift_RadiusAngle<GrayF32> alg = declare();
		alg.setImage(image);

		TupleDesc_F64 desc0 = alg.createDescription();
		TupleDesc_F64 desc1 = alg.createDescription();
		TupleDesc_F64 desc2 = alg.createDescription();

		// same location, but different orientations and scales
		assertTrue(alg.process(100, 120, 0.5, 10, desc0));
		assertTrue(alg.process(100, 50, -1.1, 10, desc1));
		assertTrue(alg.process(100, 50, 0.5, 7, desc2));

		// should be 3 different descriptions
		assertNotEquals(desc0.getDouble(0), desc1.getDouble(0), 1e-6);
		assertNotEquals(desc0.getDouble(0), desc2.getDouble(0), 1e-6);
		assertNotEquals(desc1.getDouble(0), desc2.getDouble(0), 1e-6);

		// see if it blows up along the image border
		assertTrue(alg.process(0, 120, 0.5, 10, desc0));
		assertTrue(alg.process(100, 0, 0.5, 10, desc0));
		assertTrue(alg.process(639, 120, 0.5, 10, desc0));
		assertTrue(alg.process(100, 479, 0.5, 10, desc0));
	}

	static void assertNotEquals( double a, double b, double tol ) {
		assertTrue(Math.abs(a - b) > tol);
	}

	private DescribeSift_RadiusAngle<GrayF32> declare() {
		SiftScaleSpace ss = new SiftScaleSpace(0, 4, 3, 1.6);
		DescribePointSift<GrayF32> desc = FactoryDescribeAlgs.sift(null, GrayF32.class);

		return new DescribeSift_RadiusAngle<>(ss, desc, GrayF32.class);
	}

	@Override
	protected DescribeSift_RadiusAngle<GrayF32> createAlg() {
		return declare();
	}
}
