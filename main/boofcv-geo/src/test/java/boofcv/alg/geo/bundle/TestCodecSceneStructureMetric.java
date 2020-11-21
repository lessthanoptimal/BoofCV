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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestCodecSceneStructureMetric extends BoofStandardJUnit {
	@Test
	void encode_decode() {
		encode_decode(true, false);
		encode_decode(false, false);
		encode_decode(true, true);
		encode_decode(false, true);
	}

	void encode_decode( boolean homogenous, boolean hasRigid ) {
		SceneStructureMetric original = createScene(rand, homogenous, hasRigid, false);

		CodecSceneStructureMetric codec = new CodecSceneStructureMetric();

		int pointLength = homogenous ? 4 : 3;
		int N = original.getUnknownMotionCount()*6 + original.getUnknownRigidCount()*6 +
				original.points.size*pointLength + original.getUnknownCameraParameterCount();
		assertEquals(N, original.getParameterCount());
		double[] param = new double[N];
		codec.encode(original, param);

		SceneStructureMetric found = createScene(rand, homogenous, hasRigid, false);
		codec.decode(param, found);

		assertEquals(homogenous, found.isHomogenous());
		for (int i = 0; i < original.points.size; i++) {
			assertTrue(original.points.data[i].distance(found.points.data[i]) < UtilEjml.TEST_F64);
		}

		for (int i = 0; i < original.cameras.size; i++) {
			SceneStructureCommon.Camera o = original.cameras.data[i];
			SceneStructureCommon.Camera f = found.cameras.data[i];

			double[] po = new double[o.model.getIntrinsicCount()];
			double[] pf = new double[f.model.getIntrinsicCount()];

			o.model.getIntrinsic(po, 0);
			f.model.getIntrinsic(pf, 0);

			assertArrayEquals(po, pf, UtilEjml.TEST_F64);
		}

		for (int i = 0; i < original.motions.size; i++) {
			SceneStructureMetric.Motion o = original.motions.data[i];
			SceneStructureMetric.Motion f = found.motions.data[i];

			assertTrue(MatrixFeatures_DDRM.isIdentical(o.motion.R,
					f.motion.R, UtilEjml.TEST_F64));
			assertEquals(o.motion.T.x, f.motion.T.x, UtilEjml.TEST_F64);
			assertEquals(o.motion.T.y, f.motion.T.y, UtilEjml.TEST_F64);
			assertEquals(o.motion.T.z, f.motion.T.z, UtilEjml.TEST_F64);
		}

		for (int i = 0; i < original.rigids.size; i++) {
			SceneStructureMetric.Rigid o = original.rigids.data[i];
			SceneStructureMetric.Rigid f = found.rigids.data[i];

			assertTrue(MatrixFeatures_DDRM.isIdentical(o.object_to_world.R,
					f.object_to_world.R, UtilEjml.TEST_F64));
			assertEquals(o.object_to_world.T.x, f.object_to_world.T.x, UtilEjml.TEST_F64);
			assertEquals(o.object_to_world.T.y, f.object_to_world.T.y, UtilEjml.TEST_F64);
			assertEquals(o.object_to_world.T.z, f.object_to_world.T.z, UtilEjml.TEST_F64);
		}
	}

	static SceneStructureMetric createScene( Random rand, boolean homogenous, boolean hasRigid, boolean hasRelative ) {
		SceneStructureMetric out = new SceneStructureMetric(homogenous);

		int numRigid = hasRigid ? 2 : 0;

		out.initialize(2, 4, 4, 5, numRigid);

		out.setCamera(0, true, new CameraPinhole(200, 300,
				0.1, 400, 500, 1, 1));
		out.setCamera(1, false, new CameraPinhole(201 + rand.nextGaussian(), 200,
				0.01, 401 + rand.nextGaussian(), 50 + rand.nextGaussian(), 1, 1));

		if (hasRigid) {
			Se3_F64 worldToRigid0 = SpecialEuclideanOps_F64.eulerXyz(
					rand.nextGaussian()*0.1, 0, 0.15, rand.nextGaussian()*0.01 - 0.1, -0.01, 0.2, null);
			Se3_F64 worldToRigid1 = SpecialEuclideanOps_F64.eulerXyz(-0.1, -0.1, -0.3, -0.1, 0.3, 0, null);

			out.setRigid(0, false, worldToRigid0, 3);
			out.setRigid(1, true, worldToRigid1, 2);

			for (int i = 0; i < out.rigids.size; i++) {
				SceneStructureMetric.Rigid r = out.rigids.data[i];
				if (homogenous) {
					for (int j = 0; j < r.points.length; j++) {
						double w = rand.nextDouble()*3 + 0.5;
						r.setPoint(j, rand.nextGaussian()*0.2, rand.nextGaussian()*0.1, rand.nextGaussian()*0.2, w);
					}
				} else {
					for (int j = 0; j < r.points.length; j++) {
						r.setPoint(j, rand.nextGaussian()*0.1, rand.nextGaussian()*0.2, rand.nextGaussian()*0.1);
					}
				}
			}

			// assign All of first rigid's points to all views
			SceneStructureMetric.Rigid r = out.rigids.data[0];
			for (int idxPoint = 0; idxPoint < r.points.length; idxPoint++) {
				for (int i = 0; i < 4; i++) {
					r.points[idxPoint].views.add(i);
				}
			}
			// just the first point to each view after this
			r = out.rigids.data[1];
			for (int i = 0; i < 4; i++) {
				r.points[0].views.add(i);
			}
		}
		out.assignIDsToRigidPoints();

		if (homogenous) {
			for (int i = 0; i < out.points.size; i++) {
				double w = rand.nextDouble()*0.5 + 0.5;
				out.setPoint(i, w*(i + 1), w*(i + 2*rand.nextGaussian()), w*(2*i - 3*rand.nextGaussian()), w);
			}
		} else {
			for (int i = 0; i < out.points.size; i++) {
				out.setPoint(i, i + 1, i + 2*rand.nextGaussian(), 2*i - 3*rand.nextGaussian());
			}
		}

		for (int i = 0; i < out.views.size; i++) {
			boolean fixed = i%2 == 0;

			Se3_F64 a = new Se3_F64();
			if (fixed) {
				ConvertRotation3D_F64.eulerToMatrix(EulerType.YXY, 0.2*i + 0.1, 0.7, 0, a.R);
				a.T.setTo(2, 3, i*7.3 + 5);
			} else {
				ConvertRotation3D_F64.eulerToMatrix(EulerType.YXY, 0.2*i + 0.1, rand.nextGaussian()*0.1, 0, a.R);
				a.T.setTo(rand.nextGaussian()*0.2, 3*rand.nextGaussian()*0.2, i*7.3 + 5);
			}
			out.setView(i, i/2, fixed, a);

			// Create a chain of relative views
			if (hasRelative)
				out.views.data[i].parent = i > 0 ? out.views.data[i - 1] : null;
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < out.views.size; i++) {
			out.points.data[0].views.add(i);
		}
		for (int i = 1; i < out.points.size; i++) {
			out.points.data[i].views.add(i - 1);
		}

		return out;
	}

	/**
	 * Create a scene where a "stereo" camera is created that moves. The right to left transform is fixed and common
	 * across all views
	 */
	static SceneStructureMetric createSceneStereo( Random rand, boolean homogenous ) {
		SceneStructureMetric out = new SceneStructureMetric(homogenous);

		int numSteps = 2;
		out.initialize(2, 2*numSteps, 10);

		// Left camera
		out.setCamera(0, true, new CameraPinhole(200, 300,
				0.1, 400, 500, 1, 1));

		// Right camera
		out.setCamera(1, false, new CameraPinhole(201 + rand.nextGaussian(), 200,
				0.01, 401 + rand.nextGaussian(), 50 + rand.nextGaussian(), 1, 1));

		// Create a fixed transform for left to right camera
		int leftToRightIdx = out.addMotion(true, SpecialEuclideanOps_F64.eulerXyz(0.25, 0.01, -0.05, 0.01, 0.02, -0.1, null));

		if (homogenous) {
			for (int i = 0; i < out.points.size; i++) {
				double w = rand.nextDouble()*0.5 + 0.5;
				out.setPoint(i, w*(i + 1), w*(i + 2*rand.nextGaussian()), w*(2*i - 3*rand.nextGaussian()), w);
			}
		} else {
			for (int i = 0; i < out.points.size; i++) {
				out.setPoint(i, i + 1, i + 2*rand.nextGaussian(), 2*i - 3*rand.nextGaussian());
			}
		}

		for (int step = 0; step < numSteps; step++) {
			Se3_F64 world_to_left = SpecialEuclideanOps_F64.eulerXyz(0.1, -0.15, -0.05 + step*0.2,
					rand.nextGaussian()*0.05, rand.nextGaussian()*0.05, rand.nextGaussian()*0.05, null);
			out.setView(step*2, 0, false, world_to_left);

			out.setView(step*2 + 1, -1, leftToRightIdx, step*2);
			out.connectViewToCamera(step*2 + 1, 1);
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < out.views.size; i++) {
			out.points.data[0].views.add(i);
		}
		for (int i = 1; i < out.points.size; i++) {
			out.points.data[i].views.add((i - 1)%out.views.size);
		}

		// Sanity check
		assertEquals(numSteps + 1, out.motions.size);
		assertEquals(numSteps*2, out.views.size);
		return out;
	}
}
