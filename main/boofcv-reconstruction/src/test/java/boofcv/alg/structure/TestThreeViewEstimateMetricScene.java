/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestThreeViewEstimateMetricScene extends BoofStandardJUnit {
	CameraPinhole intrinsic = new CameraPinhole(400, 400, 0, 0, 0, 900, 900);

	List<Point3D_F64> features;
	Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.02, -0.2, -0.05, 0, 0.1, null);
	Se3_F64 view0_to_view2 = SpecialEuclideanOps_F64.eulerXyz(-0.2, 0.1, 0.01, 0.1, 0.05, 0.01, null);

	List<AssociatedTriple> views = new ArrayList<>();

	@BeforeEach void before() {
		features = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, 100, rand);
		for (int i = 0; i < features.size(); i++) {
			Point3D_F64 X = features.get(i);

			Point2D_F64 x0 = PerspectiveOps.renderPixel(intrinsic, X, null);
			Point2D_F64 x1 = PerspectiveOps.renderPixel(view0_to_view1, intrinsic, X, null);
			Point2D_F64 x2 = PerspectiveOps.renderPixel(view0_to_view2, intrinsic, X, null);

			views.add(new AssociatedTriple(x0, x1, x2));
		}
	}

	@Test void perfectData() {
		var alg = new ThreeViewEstimateMetricScene();

		for (boolean singleCamera : new boolean[]{false, true}) {
			alg.singleCamera = singleCamera;
			alg.initialize(900, 900);
			assertTrue(alg.process(views));

			// See if the reconstructed seen matches the original to within a high level of precision
			SceneStructureMetric structure = alg.getStructure();
			for (SceneStructureCommon.Camera cam : structure.getCameras().toList()) {
				BundlePinholeSimplified c = cam.getModel();
				assertEquals(intrinsic.fx, c.f, 1e-4);
				assertEquals(0, c.k1, 1e-5);
				assertEquals(0, c.k2, 1e-5);
			}

			Se3_F64 found1 = structure.getParentToView(1);
			Se3_F64 found2 = structure.getParentToView(2);

			view0_to_view1.T.normalize();
			found1.T.normalize();
			assertEquals(0, found1.T.distance(view0_to_view1.T), 1e-4);
			view0_to_view2.T.normalize();
			found2.T.normalize();
			assertEquals(0, found2.T.distance(view0_to_view2.T), 1e-4);

			double[] found_xyz = ConvertRotation3D_F64.matrixToEuler(found1.R, EulerType.XYZ, null);
			double[] expec_xyz = ConvertRotation3D_F64.matrixToEuler(view0_to_view1.R, EulerType.XYZ, null);

			for (int i = 0; i < 3; i++) {
				assertEquals(expec_xyz[i], found_xyz[i], 1e-4);
			}
			found_xyz = ConvertRotation3D_F64.matrixToEuler(found2.R, EulerType.XYZ, null);
			expec_xyz = ConvertRotation3D_F64.matrixToEuler(view0_to_view2.R, EulerType.XYZ, null);

			for (int i = 0; i < 3; i++) {
				assertEquals(expec_xyz[i], found_xyz[i], 1e-4);
			}
		}
	}
}
