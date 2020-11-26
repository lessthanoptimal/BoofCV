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

package boofcv.alg.mvs;

import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.CommonOps_FDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.MatrixFeatures_FDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestBundleToRectificationStereoParameters extends BoofStandardJUnit {
	/**
	 * Basic sanity checks to see if it initialized everything it should have.
	 */
	@Test void setView1() {
		var alg = new BundleToRectificationStereoParameters();
		var bundle1 = new BundlePinholeSimplified(200, 0.1, -0.5);

		alg.setView1(bundle1, 100, 150);

		// This conversion is tested more rigorously in a different unittest
		assertEquals(200.0, alg.intrinsic1.fx, UtilEjml.TEST_F64);
		assertEquals(200.0, alg.intrinsic1.fy, UtilEjml.TEST_F64);

		// This part is done internally
		assertEquals(100, alg.intrinsic1.width);
		assertEquals(150, alg.intrinsic1.height);

		assertNotNull(alg.view1_dist_to_undist);
	}

	/**
	 * Basic test to see if variables are initialized but doesn't check correctness.
	 */
	@Test void processView2() {
		var alg = new BundleToRectificationStereoParameters();
		var bundle1 = new BundlePinholeSimplified(200, 0.1, -0.5);
		var bundle2 = new BundlePinholeSimplified(220, 0.1, -0.5);
		Se3_F64 view1_to_view2 = SpecialEuclideanOps_F64.eulerXyz(1.0, 0, -0.1, 0, 0, 0, null);

		// Easiest to initialize view-1 this way
		alg.setView1(bundle1, 100, 150);

		// Invoke the function being tested
		alg.processView2(bundle2, 100, 150, view1_to_view2);

		// See if everything is initialized
		assertTrue(CommonOps_DDRM.elementSum(alg.undist_to_rect1) != 0);
		assertTrue(CommonOps_DDRM.elementSum(alg.undist_to_rect2) != 0);
		assertTrue(CommonOps_FDRM.elementSum(alg.undist_to_rect1_F32) != 0);
		assertTrue(CommonOps_FDRM.elementSum(alg.undist_to_rect2_F32) != 0);
		assertFalse(MatrixFeatures_DDRM.isEquals(alg.undist_to_rect1, alg.undist_to_rect2, UtilEjml.TEST_F64));
		assertFalse(MatrixFeatures_FDRM.isEquals(alg.undist_to_rect1_F32, alg.undist_to_rect2_F32, UtilEjml.TEST_F32));

		assertTrue(CommonOps_DDRM.elementSum(alg.rectifiedK) != 0);
		assertTrue(CommonOps_DDRM.elementSum(alg.rotate_orig_to_rect) != 0);

		assertTrue(alg.rectifiedShape.height*alg.rectifiedShape.width > 0);
	}
}
