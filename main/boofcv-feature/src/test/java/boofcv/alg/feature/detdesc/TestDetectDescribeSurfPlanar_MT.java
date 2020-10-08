/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detdesc;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDetectDescribeSurfPlanar_MT extends BoofStandardJUnit {
	int width = 200;
	int height = 250;

	@Test
	void compare_Single_to_MT() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,width,height,3);

		GImageMiscOps.addUniform(input, rand, 0, 200);

		DetectDescribeSurfPlanar<GrayF32> desc_ST;
		DetectDescribeSurfPlanar_MT<GrayF32> desc_MT;

		{
			DescribePointSurf<GrayF32> desc = new DescribePointSurf<>(GrayF32.class);
			DescribePointSurfPlanar<GrayF32> descMulti = new DescribePointSurfPlanar<>(desc, 3);

			FastHessianFeatureDetector<GrayF32> detector = FactoryInterestPointAlgs.fastHessian(null);
			OrientationIntegral<GrayF32> orientation = FactoryOrientationAlgs.sliding_ii(null, GrayF32.class);
			desc_ST = new DetectDescribeSurfPlanar<>(detector, orientation, descMulti);
		}

		{
			DescribePointSurf<GrayF32> desc = new DescribePointSurf<>(GrayF32.class);
			DescribePointSurfPlanar<GrayF32> descMulti = new DescribePointSurfPlanar<>(desc, 3);

			FastHessianFeatureDetector<GrayF32> detector = FactoryInterestPointAlgs.fastHessian(null);
			OrientationIntegral<GrayF32> orientation = FactoryOrientationAlgs.sliding_ii(null, GrayF32.class);
			desc_MT = new DetectDescribeSurfPlanar_MT<>(detector, orientation, descMulti);
		}

		GrayF32 gray = ConvertImage.average(input, null);

		desc_ST.detect(gray,input);
		desc_MT.detect(gray,input);

		assertEquals(desc_ST.getNumberOfFeatures(),desc_MT.getNumberOfFeatures());

		int N = desc_ST.getNumberOfFeatures();
		for (int idx_st = 0; idx_st < N; idx_st++) {
			Point2D_F64 loc_st = desc_ST.getLocation(idx_st);

			// order isn't guaranteed. Do an exhaustive search
			boolean matched = false;
			for (int idx_mt = 0; idx_mt < N; idx_mt++) {
				Point2D_F64 loc_mt = desc_MT.getLocation(idx_mt);

				if( loc_st.x != loc_mt.x || loc_st.y != loc_mt.y ) {
					continue;
				}

				if( desc_ST.getRadius(idx_st) != desc_MT.getRadius(idx_mt) ) {
					continue;
				}

				if( desc_ST.getOrientation(idx_st) != desc_MT.getOrientation(idx_mt) ) {
					continue;
				}

				if( desc_ST.isWhite(idx_st) != desc_MT.isWhite(idx_mt) ) {
					continue;
				}

				TupleDesc_F64 fd_st = desc_ST.getDescription(idx_st);
				TupleDesc_F64 fd_mt = desc_MT.getDescription(idx_mt);

				assertEquals(0, DescriptorDistance.sad(fd_st,fd_mt));
				matched = true;
				break;
			}
			assertTrue(matched,"No match "+idx_st);
		}
	}
}