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

package boofcv.alg.sfm.structure.score3d;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.structure.EpipolarScore3D;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonEpipolarScore3DChecks extends BoofStandardJUnit {

	DMatrixRMaj fundamental = new DMatrixRMaj(3, 3);
	DogArray_I32 inlierIdx = new DogArray_I32();

	int numPoints = 100;

	public abstract EpipolarScore3D createAlg();

	/**
	 * Perfect case for 3D information. Translation with a rich point cloud.
	 */
	@Test void translation() {
		EpipolarScore3D alg = createAlg();

		assertTrue(alg.process(createAssociations(numPoints, false, false), fundamental, inlierIdx));

		assertTrue(alg.is3D());
		assertTrue(inlierIdx.size >= numPoints*0.7);
		// just check to see if something is filled in
		assertFalse(Math.abs(NormOps_DDRM.normF(fundamental)) < UtilEjml.TEST_F64);
	}

	/**
	 * Pure rotation has no 3D information
	 */
	@Test void rotation() {
		EpipolarScore3D alg = createAlg();

		assertTrue(alg.process(createAssociations(numPoints, false, true), fundamental, inlierIdx));

		assertFalse(alg.is3D());
		assertTrue(inlierIdx.size >= numPoints*0.7);
		// just check to see if something is filled in
		assertFalse(Math.abs(NormOps_DDRM.normF(fundamental)) < UtilEjml.TEST_F64);
	}

	/**
	 * Planar contains 3D information but can be degenerate case
	 */
	@Test void planar() {
		EpipolarScore3D alg = createAlg();

		assertTrue(alg.process(createAssociations(numPoints, false, true), fundamental, inlierIdx));

		assertTrue(alg.is3D());
		assertTrue(inlierIdx.size >= numPoints*0.7);
		// just check to see if something is filled in
		assertFalse(Math.abs(NormOps_DDRM.normF(fundamental)) < UtilEjml.TEST_F64);
	}

	private List<AssociatedPair> createAssociations( int N, boolean planar, boolean pureRotation ) {
		var intrinsic = new CameraPinhole(400, 410, 0, 500, 500, 1000, 1000);
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(0.3, 0, 0.01, -0.04, -2e-3, 0.4, null);

		if (pureRotation)
			view0_to_view1.T.setTo(0, 0, 0);

		List<Point3D_F64> feats3D;

		if (planar) {
			var plane = new PlaneNormal3D_F64(0, 0, 1, 0.001, 0.02, 1);
			feats3D = UtilPoint3D_F64.random(plane, 0.5, N, rand);
		} else {
			feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, N, rand);
		}

		var associated = new DogArray<>(AssociatedPair::new);

		double sigma = 0.5;

		for (int i = 0; i < feats3D.size(); i++) {
			Point3D_F64 X = feats3D.get(i);
			AssociatedPair a = associated.grow();

			a.p1.setTo(PerspectiveOps.renderPixel(intrinsic, X, null));
			a.p2.setTo(PerspectiveOps.renderPixel(view0_to_view1, intrinsic, X, null));

			// add a little bit of noise so that it isn't perfect
			a.p1.x += rand.nextGaussian()*sigma;
			a.p1.y += rand.nextGaussian()*sigma;
			a.p2.x += rand.nextGaussian()*sigma;
			a.p2.y += rand.nextGaussian()*sigma;
		}

		return associated.toList();
	}
}
