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

package boofcv.abst.feature.detdesc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSurf_DetectDescribe_MT extends GenericTestsDetectDescribePoint<GrayF32, TupleDesc_F64>
{
	static {
		BoofConcurrency.USE_CONCURRENT = true;
	}

	TestSurf_DetectDescribe_MT() {
		super(true, true, ImageType.single(GrayF32.class), TupleDesc_F64.class);
	}

	@Override
	public DetectDescribePoint<GrayF32, TupleDesc_F64> createDetDesc() {
		return FactoryDetectDescribe.surfStable(null,null,null, GrayF32.class);
	}

	@Test
	void compare() {
		BoofConcurrency.USE_CONCURRENT = false;
		DetectDescribePoint<GrayF32, TupleDesc_F64> surfA = createDetDesc();
		BoofConcurrency.USE_CONCURRENT = true;
		DetectDescribePoint<GrayF32, TupleDesc_F64> surfB = createDetDesc();

		GrayF32 image = new GrayF32(400,300);
		GImageMiscOps.fillUniform(image, rand, 0, 100);

		surfA.detect(image);
		surfB.detect(image);

		// results should be equivalent
		// Order of features is likely to be shuffled
		assertEquals(surfA.getNumberOfFeatures(),surfB.getNumberOfFeatures());
		assertTrue(surfA.getNumberOfFeatures()>200);

		int N = surfA.getNumberOfFeatures();
		for (int i = 0; i < N; i++) {
			boolean matched = false;
			for (int j = 0; j < N; j++) {
				if(UtilAngle.dist(surfA.getOrientation(i) , surfB.getOrientation(j)) > UtilEjml.TEST_F64)
					continue;
				if( surfA.getRadius(i) != surfB.getRadius(j))
					continue;
				if( surfA.getSet(i) != surfB.getSet(j))
					continue;
				Point2D_F64 pa = surfA.getLocation(i);
				Point2D_F64 pb = surfB.getLocation(j);
				if( pa.distance(pb) > UtilEjml.TEST_F64 )
					continue;

				TupleDesc_F64 a = surfA.getDescription(i);
				TupleDesc_F64 b = surfB.getDescription(j);

				matched = true;
				for (int k = 0; k < a.size(); k++) {
					if( Math.abs(a.data[k]-b.data[k]) > UtilEjml.TEST_F64) {
						matched = false;
						break;
					}
				}
			}
			assertTrue(matched);
		}

	}
}