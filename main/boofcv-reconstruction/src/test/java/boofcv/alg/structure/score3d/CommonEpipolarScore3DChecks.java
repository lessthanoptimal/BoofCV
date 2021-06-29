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

package boofcv.alg.structure.score3d;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.NormOps_DDRM;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class CommonEpipolarScore3DChecks extends BoofStandardJUnit {

	CameraPinholeBrown intrinsic1 = new CameraPinholeBrown(400, 400, 0, 500, 500, 1000, 1000).fsetRadial(0.0, 0.0);
	CameraPinholeBrown intrinsic2 = new CameraPinholeBrown(600, 600, 0, 600, 600, 1000, 1000).fsetRadial(0.0, 0.0);

	CameraPinholeBrown intrinsicBad1 = new CameraPinholeBrown(800, 800, 0, 600, 600, 1000, 1000).fsetRadial(0.0, 0.0);
	CameraPinholeBrown intrinsicBad2 = new CameraPinholeBrown(400, 400, 0, 400, 400, 1000, 1000).fsetRadial(0.0, 0.0);

	DMatrixRMaj fundamental = new DMatrixRMaj(3, 3);
	DogArray_I32 inlierIdx = new DogArray_I32();

	int numPoints = 100;

	public abstract EpipolarScore3D createAlg();

	/**
	 * Perfect case for 3D information. Translation with a rich point cloud.
	 */
	@Test void translation() {
		translation(intrinsic1, intrinsic2);
		translation(intrinsicBad1, intrinsicBad1); // two-views one camera
		translation(intrinsicBad1, intrinsicBad2); // two-views two cameras
	}

	void translation( CameraPinholeBrown prior1, CameraPinholeBrown prior2 ) {
		List<Se3_F64> scenarios = new ArrayList<>();
		scenarios.add(eulerXyz(1, 0, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 1, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 2, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, -0.2, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0, 0.2, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0.2, 0, null));
		scenarios.add(eulerXyz(-0.1, 0, 1, -0.2, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0.2, 0.5, null));

		checkScenarios(prior1, prior2, null, scenarios, true);
	}

	/**
	 * Pure rotation has no 3D information
	 */
	@Test void rotation() {
		rotation(intrinsic1, intrinsic2);
		rotation(intrinsicBad1, intrinsicBad1); // two-views one camera
		rotation(intrinsicBad1, intrinsicBad2); // two-views two cameras
	}

	void rotation( CameraPinholeBrown prior1, CameraPinholeBrown prior2 ) {
		List<Se3_F64> scenarios = new ArrayList<>();
		scenarios.add(eulerXyz(0, 0, 0, 0.05, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 0, 0, 0.2, 0, null));
		scenarios.add(eulerXyz(0, 0, 0, 0, 0, 0.2, null));
		scenarios.add(eulerXyz(0, 0, 0, 0.2, 0, 0.2, null));
		scenarios.add(eulerXyz(0, 0, 0, 0, 0.2, 0.2, null));
		scenarios.add(eulerXyz(0, 0, 0, -0.2, -0.2, 0.2, null));

		checkScenarios(prior1, prior2, null, scenarios, false);
	}

	/**
	 * A perfect plane is viewed. Camera moves along the x and/or y. Sometimes with a little bit of rotation
	 */
	@Test void planar_translate_xy() {
		planar_translate_xy(intrinsic1, intrinsic2);
		planar_translate_xy(intrinsicBad1, intrinsicBad1); // two-views one camera
		planar_translate_xy(intrinsicBad1, intrinsicBad2); // two-views two cameras
	}

	void planar_translate_xy( CameraPinholeBrown prior1, CameraPinholeBrown prior2 ) {
		List<Se3_F64> scenarios = new ArrayList<>();
		scenarios.add(eulerXyz(1, 0, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 1, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(-1, 0, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, -1, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(0.75, -0.4, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(-0.2, 0.6, 0, 0, 0, 0, null));
		scenarios.add(eulerXyz(1, 0, 0, 0, 0, 0.1, null));
		scenarios.add(eulerXyz(1, 0, 0, 0, 0.1, 0, null));
		scenarios.add(eulerXyz(1, 0, 0, 0.1, 0, 0, null));

		// plane is skewed towards the camera
		var generalPlane = new PlaneNormal3D_F64(0, 0, 0.75, -0.05, 0.1, 1);

		checkScenarios(prior1, prior2, generalPlane, scenarios, true);
	}

	/**
	 * This case is problematic. If you're doing self calibration you can simulate the "zoom" by making the focal
	 * length of one of the views smaller.
	 */
	@Test void planar_translate_z_twoCameras() {
		planar_translate_z(intrinsic1, intrinsic2);
		planar_translate_z(intrinsicBad1, intrinsicBad2);
	}

	/**
	 * A single camera. This extra constraint should allow self calibration approaches to work
	 */
	@Test void planar_translate_z_oneCamera() {
		planar_translate_z(intrinsic1, intrinsic1);
		planar_translate_z(intrinsicBad1, intrinsicBad1);
	}

	void planar_translate_z( CameraPinholeBrown prior1, CameraPinholeBrown prior2 ) {
		List<Se3_F64> scenarios = new ArrayList<>();
		scenarios.add(eulerXyz(0, 0, -0.4, 0, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0, 0.2, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0.2, 0, null));
		scenarios.add(eulerXyz(-0.1, 0, 1, -0.2, 0, 0, null));
		scenarios.add(eulerXyz(0, 0, 1, 0, 0.2, 0.5, null));

		// plane is skewed towards the camera
		var generalPlane = new PlaneNormal3D_F64(0, 0, 0.75, -0.05, 0.1, 1);

		checkScenarios(prior1, prior2, generalPlane, scenarios, true);
	}

	void checkScenarios( CameraPinholeBrown prior1, CameraPinholeBrown prior2,
						 @Nullable PlaneNormal3D_F64 plane,
						 List<Se3_F64> scenarios, boolean expect3D ) {
		EpipolarScore3D alg = createAlg();

		boolean oneCamera = prior1 == prior2;
		int scenario = 0;
		for (Se3_F64 view0_to_view1 : scenarios) {
			alg.process(prior1, oneCamera ? null : prior2,
					numPoints, numPoints,
					createAssociations(oneCamera, numPoints, plane, view0_to_view1), fundamental, inlierIdx);
			assertEquals(expect3D, alg.is3D(), "scenario=" + scenario++);
			assertTrue(inlierIdx.size >= numPoints*0.7);
			// just check to see if something is filled in
			assertFalse(Math.abs(NormOps_DDRM.normF(fundamental)) < UtilEjml.TEST_F64);
		}
	}

	private List<AssociatedPair> createAssociations( boolean oneCamera, int N, @Nullable PlaneNormal3D_F64 plane, Se3_F64 view0_to_view1 ) {
		List<Point3D_F64> feats3D;

		if (plane != null) {
			feats3D = UtilPoint3D_F64.random(plane, 0.5, N, rand);
		} else {
			feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, N, rand);
		}

		var associated = new DogArray<>(AssociatedPair::new);

		// If generated from one camera use the first camera for both views
		CameraPinholeBrown intrinsic2 = oneCamera ? intrinsic1 : this.intrinsic2;

		double sigma = 0.5;

		for (int i = 0; i < feats3D.size(); i++) {
			Point3D_F64 X = feats3D.get(i).copy();
			AssociatedPair a = associated.grow();

			if (X.z <= 0)
				throw new RuntimeException("Point is behind camera! Pick a better scenario");
			PerspectiveOps.renderPixel(intrinsic1, X, a.p1);

			SePointOps_F64.transform(view0_to_view1, X, X);

			if (X.z <= 0)
				throw new RuntimeException("Point is behind camera! Pick a better scenario");
			PerspectiveOps.renderPixel(intrinsic2, X, a.p2);

			// add a little bit of noise so that it isn't perfect
			a.p1.x += rand.nextGaussian()*sigma;
			a.p1.y += rand.nextGaussian()*sigma;
			a.p2.x += rand.nextGaussian()*sigma;
			a.p2.y += rand.nextGaussian()*sigma;
		}

		return associated.toList();
	}
}
