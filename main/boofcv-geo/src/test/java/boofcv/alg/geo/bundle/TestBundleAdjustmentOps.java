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

package boofcv.alg.geo.bundle;

import boofcv.alg.geo.bundle.cameras.BundleKannalaBrandt;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestBundleAdjustmentOps extends BoofStandardJUnit {

	int width = 50;
	int height = 60;

	@Test void convert_bundlePinhole_pinhole() {
		var src = new BundlePinhole().setK(2, 3, 0, 7, 3);
		var dst = new CameraPinhole(10, 12, 1, 1, 2, 10, 32);

		assertSame(dst, BundleAdjustmentOps.convert(src, width, height, dst));

		assertEquals(src.fx, dst.fx);
		assertEquals(src.fy, dst.fy);
		assertEquals(src.cx, dst.cx);
		assertEquals(src.cy, dst.cy);
		assertEquals(src.skew, dst.skew);
		assertEquals(width, dst.width);
		assertEquals(height, dst.height);
	}

	@Test void convert_bundleBrown_brown() {
		var src = new BundlePinholeBrown().setK(2, 3, 0, 7, 3).setRadial(1, 2).setTangential(-1, -9);
		var dst = new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(-1, -2).fsetTangental(0.1, 0.2);

		assertSame(dst, BundleAdjustmentOps.convert(src, width, height, dst));

		assertArrayEquals(src.radial, dst.radial);
		assertEquals(src.t1, dst.t1);
		assertEquals(src.t2, dst.t2);
		assertEquals(src.fx, dst.fx);
		assertEquals(src.fy, dst.fy);
		assertEquals(src.cx, dst.cx);
		assertEquals(src.cy, dst.cy);
		assertEquals(src.skew, dst.skew);
		assertEquals(width, dst.width);
		assertEquals(height, dst.height);
	}

	@Test void convert_bundleSimple_brown() {
		var src = new BundlePinholeSimplified(10, 1, 2);
		var dst = new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(-1, -2).fsetTangental(0.1, 0.2);

		assertSame(dst, BundleAdjustmentOps.convert(src, width, height, dst));

		assertArrayEquals(new double[]{src.k1, src.k2}, dst.radial);
		assertEquals(0.0, dst.t1);
		assertEquals(0.0, dst.t2);
		assertEquals(src.f, dst.fx);
		assertEquals(src.f, dst.fy);
		assertEquals(width/2, dst.cx);
		assertEquals(height/2, dst.cy);
		assertEquals(0.0, dst.skew);
		assertEquals(width, dst.width);
		assertEquals(height, dst.height);
	}

	@Test void convert_brown_to_bundlePinhole() {
		var src = new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(-1, -2).fsetTangental(0.1, 0.2);
		var dst = new BundlePinhole(true);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		assertFalse(dst.zeroSkew);
		assertEquals(src.fx, dst.fx);
		assertEquals(src.fy, dst.fy);
		assertEquals(src.cx, dst.cx);
		assertEquals(src.cy, dst.cy);
		assertEquals(src.skew, dst.skew);
	}

	@Test void convert_brown_to_bundleBrown() {
		var src = new CameraPinholeBrown().fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(-1, -2).fsetTangental(0.1, 0.2);
		var dst = new BundlePinholeBrown(true, false);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		assertFalse(dst.zeroSkew);
		assertTrue(dst.tangential);

		assertArrayEquals(src.radial, dst.radial);
		assertEquals(src.t1, dst.t1);
		assertEquals(src.t2, dst.t2);
		assertEquals(src.fx, dst.fx);
		assertEquals(src.fy, dst.fy);
		assertEquals(src.cx, dst.cx);
		assertEquals(src.cy, dst.cy);
		assertEquals(src.skew, dst.skew);
	}

	@Test void convert_bundleKB_cameraKB() {
		var src = new BundleKannalaBrandt();
		var dst = new CameraKannalaBrandt();

		src.model.fsetK(500, 550, 0.0, 600, 650);
		src.model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06, 0.12).
				fsetRadialTrig(0.01, 0.03, -0.03, 0.04).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		assertSame(dst, BundleAdjustmentOps.convert(src, 100, 120, dst));

		assertEquals(100, dst.width);
		assertEquals(120, dst.height);

		assertEquals(src.model.fx, dst.fx, UtilEjml.TEST_F64);
		assertEquals(src.model.fy, dst.fy, UtilEjml.TEST_F64);
		assertEquals(src.model.cx, dst.cx, UtilEjml.TEST_F64);
		assertEquals(src.model.cy, dst.cy, UtilEjml.TEST_F64);
		assertEquals(src.model.skew, dst.skew, UtilEjml.TEST_F64);
		assertArrayEquals(src.model.symmetric, dst.symmetric, UtilEjml.TEST_F64);
		assertArrayEquals(src.model.radial, dst.radial, UtilEjml.TEST_F64);
		assertArrayEquals(src.model.tangent, dst.tangent, UtilEjml.TEST_F64);
		assertArrayEquals(src.model.radialTrig, dst.radialTrig, UtilEjml.TEST_F64);
		assertArrayEquals(src.model.tangentTrig, dst.tangentTrig, UtilEjml.TEST_F64);
	}

	@Test void convert_pinhole_to_bundleSimple() {
		var src = new CameraPinhole(10, 12, 1, 1, 2, 10, 32);
		var dst = new BundlePinholeSimplified(9, 2, 3);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		assertEquals(11, dst.f, UtilEjml.TEST_F64);
		assertEquals(0, dst.k1);
		assertEquals(0, dst.k2);
	}

	@Test void convert_pinhole_to_bundlePinhole() {
		var src = new CameraPinhole(10, 12, 1, 1, 2, 10, 32);
		var dst = new BundlePinhole().setK(9, 3, 0, 0, 2);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		assertEquals(src.fx, dst.fx);
		assertEquals(src.fy, dst.fy);
		assertEquals(src.cx, dst.cx);
		assertEquals(src.cy, dst.cy);
		assertEquals(src.skew, dst.skew);
		assertFalse(dst.zeroSkew);
	}

	@Test void convert_matrix_to_bundleSimple() {
		var src = CommonOps_DDRM.diag(10, 12, 1);
		var dst = new BundlePinholeSimplified(9, 2, 3);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		assertEquals(11, dst.f, UtilEjml.TEST_F64);
		assertEquals(0.0, dst.k1);
		assertEquals(0.0, dst.k2);
	}

	@Test void convert_bundleSimple_to_matrix() {
		var src = new BundlePinholeSimplified(10, 1, 2);
		var dst = RandomMatrices_DDRM.rectangle(3, 3, -1, 1, rand);

		assertSame(dst, BundleAdjustmentOps.convert(src, dst));

		var expected = CommonOps_DDRM.diag(10, 10, 1);
		assertTrue(MatrixFeatures_DDRM.isEquals(expected, dst, UtilEjml.TEST_F64));
	}
}
