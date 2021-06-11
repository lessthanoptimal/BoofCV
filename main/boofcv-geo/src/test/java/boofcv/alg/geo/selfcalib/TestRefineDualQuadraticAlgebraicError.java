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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.selfcalib.RefineDualQuadraticAlgebraicError.CameraState;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineDualQuadraticAlgebraicError extends CommonAutoCalibrationChecks {

	@Test void encode_decode() {
		var alg = new RefineDualQuadraticAlgebraicError();

		for (int i = 0; i < 10; i++) {
			alg.priorCameras.grow().setTo(400 + i, 1.0 + i/20.0, 340, 400);
		}
		alg.cameras.resize(alg.priorCameras.size);

		double[] parameters = new double[(1 + 1 + 2)*10 + 3];

		Point3D_F64 p = new Point3D_F64(planeAtInfinity.get(0), planeAtInfinity.get(1), planeAtInfinity.get(2));
		alg.encodeParameters(p, alg.priorCameras, parameters);

		p.setTo(0, 0, 0);
		alg.decodeParameters(parameters, alg.cameras, p);

		for (int cameraIdx = 0; cameraIdx < alg.cameras.size; cameraIdx++) {
			CameraState found = alg.cameras.get(cameraIdx);
			CameraState expected = alg.cameras.get(cameraIdx);

			assertEquals(expected.fx, found.fx);
			assertEquals(expected.aspectRatio, found.aspectRatio);
			assertEquals(expected.cx, found.cx);
			assertEquals(expected.cy, found.cy);
		}

		assertEquals(planeAtInfinity.get(0), p.x);
		assertEquals(planeAtInfinity.get(1), p.y);
		assertEquals(planeAtInfinity.get(2), p.z);
	}

	@Test void solvePerfect() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.0, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));
		}

		var alg = new RefineDualQuadraticAlgebraicError();
		checkRefine(alg, expected, found, 1e-6);
	}

	@Test void solveNoise() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.0, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 1.5*rand.nextGaussian();
			found.get(i).fy += 1.5*rand.nextGaussian();
			found.get(i).cx += 1.5*rand.nextGaussian();
			found.get(i).cy += 1.5*rand.nextGaussian();
		}

		var alg = new RefineDualQuadraticAlgebraicError();
		checkRefine(alg, expected, found, 1.5);
	}

	@Test void solveNoise_solveSingleCamera() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();

		// Just one camera as a constraint
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400, 420, 0.0, 410, 420, 0, 0));
		}

		found.add(new CameraPinhole(expected.get(0)));

		found.get(0).fx += 1.5*rand.nextGaussian();
		found.get(0).fy += 1.5*rand.nextGaussian();
		found.get(0).cx += 1.5*rand.nextGaussian();
		found.get(0).cy += 1.5*rand.nextGaussian();

		var alg = new RefineDualQuadraticAlgebraicError();
		checkRefine(alg, expected, found, 0.5);
	}

	@Test void solveNoise_KnownPrinciplePoint() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.0, 5, 6, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			found.get(i).fx += 1*rand.nextGaussian();
			found.get(i).fy += 1*rand.nextGaussian();
		}

		var alg = new RefineDualQuadraticAlgebraicError();
		alg.setKnownPrinciplePoint(true);
		checkRefine(alg, expected, found, 5);
	}

	@Test void solveFixedAspect() {
		List<CameraPinhole> expected = new ArrayList<>();
		List<CameraPinhole> found = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			expected.add(new CameraPinhole(400 + i*5, 420, 0.0, 410, 420, 0, 0));

			found.add(new CameraPinhole(expected.get(i)));

			double scaleF = 1.0 + (rand.nextDouble()-0.5)/10.0;
			found.get(i).fx *= scaleF;
			found.get(i).fy *= scaleF;
			found.get(i).cx += 2*rand.nextGaussian();
			found.get(i).cy += 2*rand.nextGaussian();
		}

		var alg = new RefineDualQuadraticAlgebraicError();
		alg.setKnownAspect(true);
		checkRefine(alg, expected, found, 5);
	}

	private void checkRefine( RefineDualQuadraticAlgebraicError alg,
							  List<CameraPinhole> expected, List<CameraPinhole> noisy, double tol ) {
//		alg.setVerbose(System.out, null);
		renderGood(expected);

		setState(alg, noisy, planeAtInfinity.get(0), planeAtInfinity.get(1), planeAtInfinity.get(2));

		assertTrue(alg.refine());

		List<CameraState> found = alg.getCameras().toList();
		assertEquals(noisy.size(), found.size()); // checking against size of noisy, since noisy is number of cameras
		// if intrinsics are the same than there will only be one camera

		// estimate gets worse
		for (int i = 0; i < noisy.size(); i++) {
			CameraPinhole e = expected.get(i);
			CameraState f = found.get(i);

			if (alg.isKnownAspect()) {
				assertEquals(e.fx, f.fx, tol);
				assertEquals(e.fy/e.fx, f.aspectRatio, 0.01);
			} else {
				assertEquals(e.fx, f.fx, tol);
				assertEquals(e.fy/e.fx, f.aspectRatio, 0.05);
			}
			if (alg.isKnownPrinciplePoint()) {
				assertEquals(e.cx, f.cx, UtilEjml.TEST_F64_SQ);
				assertEquals(e.cy, f.cy, UtilEjml.TEST_F64_SQ);
			} else {
				assertEquals(e.cx, f.cx, tol);
				assertEquals(e.cy, f.cy, tol);
			}
		}
	}

	private void setState( RefineDualQuadraticAlgebraicError alg,
						   List<CameraPinhole> noisy, double px, double py, double pz ) {
		alg.initialize(noisy.size(), listP.size());
		for (int i = 0; i < listP.size(); i++) {
			alg.setProjective(i, listP.get(i));
		}
		for (int i = 0; i < noisy.size(); i++) {
			CameraPinhole c = noisy.get(i);
			alg.setCamera(i, c.fx, c.cx, c.cy, c.fy/c.fx);
		}
		alg.setPlaneAtInfinity(px, py, pz);

		if (listP.size() == noisy.size()) {
			for (int i = 0; i < listP.size(); i++) {
				alg.setViewToCamera(i, i);
			}
		} else {
			for (int i = 0; i < listP.size(); i++) {
				alg.setViewToCamera(i, 0);
			}
		}
	}
}
