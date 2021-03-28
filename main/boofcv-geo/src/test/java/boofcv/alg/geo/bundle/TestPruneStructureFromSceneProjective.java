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

import boofcv.abst.geo.bundle.PruneStructureFromSceneProjective;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPoint4D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestPruneStructureFromSceneProjective extends BoofStandardJUnit {
	SceneStructureProjective structure;
	SceneObservations observations;

	Point3D_F64 center = new Point3D_F64(0, 0, 4);

	@Test
	void pruneObservationsByErrorRank() {
		createPerfectScene();

		// add noise to some of the observations
		// assume that the odds of one being selected twice is low
		int N = structure.getObservationCount();
		int noisyCount = (int)(N*0.02 + 0.5);
		for (int i = 0; i < noisyCount; i++) {
			int viewIdx = rand.nextInt(structure.views.size);
			SceneObservations.View vo = observations.views.data[viewIdx];

			int idx = rand.nextInt(vo.point.size);
			vo.observations.data[idx*2] += 5;
			vo.observations.data[idx*2 + 1] += 5;
		}

		PruneStructureFromSceneProjective alg = new PruneStructureFromSceneProjective(structure, observations);

		alg.pruneObservationsByErrorRank(0.98);

		assertEquals(N - noisyCount, structure.getObservationCount());
		checkAllObservationsArePerfect();
	}

	@Test
	void prunePoints() {

		createPerfectScene();
		int obsCount = structure.getObservationCount();

		PruneStructureFromSceneProjective alg = new PruneStructureFromSceneProjective(structure, observations);

		// there should be no change
		assertFalse(alg.prunePoints(1));
		assertEquals(500, structure.points.size);
		assertEquals(obsCount, structure.getObservationCount());
		checkAllObservationsArePerfect();

		// count the number with 8 or more views
		int points8 = 0;
		for (int i = 0; i < structure.points.size; i++) {
			if (structure.points.data[i].views.size >= 8) {
				points8++;
			}
		}
		assertTrue(alg.prunePoints(8));
		assertEquals(points8, structure.points.size);
		assertTrue(obsCount > structure.getObservationCount());
		checkAllObservationsArePerfect();
	}

	@Test
	void pruneViews() {
		createPerfectScene();

		int initialObs = structure.getObservationCount();
		int initialViews = structure.views.size;
		int initialPoints = structure.points.size;

		PruneStructureFromSceneProjective alg = new PruneStructureFromSceneProjective(structure, observations);

		// no change expected
		alg.pruneViews(100);
		assertEquals(initialViews, structure.views.size);
		assertEquals(initialPoints, structure.points.size);
		assertEquals(initialObs, structure.getObservationCount());
		checkAllObservationsArePerfect();

		// prune all but perfect coverage views
		int expectedViews = 0;
		for (int i = 0; i < initialViews; i++) {
			if (observations.views.data[i].point.size >= 500)
				expectedViews++;
		}
		alg.pruneViews(499);
		assertEquals(expectedViews, structure.views.size);
		assertEquals(initialPoints, structure.points.size);
		assertTrue(structure.getObservationCount() < initialObs);
		checkAllObservationsArePerfect();
	}

	private void createPerfectScene() {
		structure = new SceneStructureProjective(true);
		structure.initialize(10, 500);

		CameraPinhole intrinsic = new CameraPinhole(400, 410, 0.1, 500, 501, 1000, 1000);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			Se3_F64 worldToView = new Se3_F64();

			if (viewIdx > 0) {
				worldToView.T.x = rand.nextGaussian()*0.5;
				worldToView.T.y = rand.nextGaussian()*0.5;
				worldToView.T.z = rand.nextGaussian()*0.05;

				// increased rotation variance until a few of the points weren't always visible
				ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
						rand.nextGaussian()*0.5, rand.nextGaussian()*0.1, rand.nextGaussian()*0.1,
						worldToView.R);
			}

			DMatrixRMaj cameraMatrix = PerspectiveOps.createCameraMatrix(worldToView.R, worldToView.T, K, null);
			structure.setView(viewIdx, viewIdx == 0, cameraMatrix, intrinsic.width, intrinsic.height);
		}

		List<Point4D_F64> points = UtilPoint4D_F64.randomN(center, 1.0, 0.5, structure.points.size, rand);
		for (int i = 0; i < points.size(); i++) {
			Point4D_F64 p = points.get(i);
			double s = rand.nextGaussian();
			if (Math.abs(s) < 1e-5) // make sure it isn't scaled by zero
				s = rand.nextDouble() + 0.01;
			p.scale(s);
			structure.points.data[i].set(p.x, p.y, p.z, p.w);
		}

		createRestOfScene();
	}

	private void createRestOfScene() {
		observations = new SceneObservations();
		observations.initialize(structure.views.size);

		Point4D_F64 X = new Point4D_F64();
		Point3D_F64 xx = new Point3D_F64();
		Point2D_F64 x = new Point2D_F64();

		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureProjective.View vs = structure.views.data[viewIdx];

			DMatrixRMaj P = vs.worldToView;
			int width = vs.width;
			int height = vs.height;

			SceneObservations.View vo = observations.views.data[viewIdx];

			for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
				SceneStructureCommon.Point ps = structure.points.data[pointIdx];
				ps.get(X);
				GeometryMath_F64.mult(P, X, xx);

				// behind the camera. I think...
				if (Math.signum(X.w)*xx.z < 0)
					continue;

				x.x = xx.x/xx.z;
				x.y = xx.y/xx.z;

				if (x.x >= 0 && x.x <= width && x.y >= 0 && x.y <= height) {
					ps.views.add(viewIdx);
					vo.add(pointIdx, (float)x.x, (float)x.y);
				}
			}
		}
	}

	/**
	 * See if all the observations are perfect. This acts as a sanity check on the scenes structure after modification
	 */
	private void checkAllObservationsArePerfect() {
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 x = new Point2D_F64();
		Point2D_F64 y = new Point2D_F64();

		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureProjective.View vs = structure.views.data[viewIdx];
			DMatrixRMaj P = vs.worldToView;

			SceneObservations.View vo = observations.views.data[viewIdx];

			for (int i = 0; i < vo.point.size; i++) {
				int pointIdx = vo.point.get(i);
				structure.points.data[pointIdx].get(X);
				GeometryMath_F64.mult(P, X, x);
				vo.getPixel(i, y);
				assertEquals(0, x.x - y.x, UtilEjml.TEST_F64_SQ);
				assertEquals(0, x.y - y.y, UtilEjml.TEST_F64_SQ);
			}
		}
	}
}
