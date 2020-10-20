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
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.bundle.cameras.BundleCameraProjective;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestCodecSceneStructureProjective extends BoofStandardJUnit {
	final static int width = 300, height = 200;

	@Test
	void encode_decode() {
		SceneStructureProjective original = createScene3D(rand);

		CodecSceneStructureProjective codec = new CodecSceneStructureProjective();

		int N = original.getUnknownViewCount()*12 + original.points.size*3 + original.getUnknownCameraParameterCount();
		double[] param = new double[N];
		codec.encode(original, param);

		SceneStructureProjective found = createScene3D(rand);
		codec.decode(param, found);

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

		for (int i = 0; i < original.views.size; i++) {
			SceneStructureProjective.View o = original.views.data[i];
			SceneStructureProjective.View f = found.views.data[i];

			assertTrue(MatrixFeatures_DDRM.isIdentical(o.worldToView, f.worldToView, UtilEjml.TEST_F64));
		}
	}

	static SceneStructureProjective createScene3D( Random rand ) {
		SceneStructureProjective out = new SceneStructureProjective(false);

		out.initialize(2, 4, 5);

		out.setCamera(0, true, new BundleCameraProjective());
//		out.setCamera(1,true,new BundleCameraProjective());
		out.setCamera(1, false, new CameraPinhole(100, 110,
				0.0001, rand.nextGaussian()/100, rand.nextGaussian()/100, 1, 1));

		for (int i = 0; i < 5; i++) {
			out.setPoint(i, i + 1, i + 2*rand.nextGaussian(), 2*i - 3*rand.nextGaussian());
		}

		for (int i = 0; i < 4; i++) {
			boolean fixed = i%2 == 0;

			DMatrixRMaj P = new DMatrixRMaj(3, 4);
			if (fixed) {
				for (int j = 0; j < 12; j++) {
					P.data[j] = 0.1 + j*0.2;
				}
			} else
				RandomMatrices_DDRM.fillUniform(P, rand);
			out.setView(i, fixed, P, width, height);
			out.views.data[i].camera = i/2;
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < 4; i++) {
			out.points.data[0].views.add(i);
		}
		for (int i = 1; i < out.points.size; i++) {
			out.points.data[i].views.add(i - 1);
		}

		return out;
	}

	static SceneStructureProjective createSceneH( Random rand ) {
		SceneStructureProjective out = new SceneStructureProjective(true);

		out.initialize(2, 4, 5);

		out.setCamera(0, true, new BundleCameraProjective());
//		out.setCamera(1,true,new BundleCameraProjective());
		out.setCamera(1, false, new CameraPinhole(100, 110,
				0.0001, rand.nextGaussian()/100, rand.nextGaussian()/100, 1, 1));

		for (int i = 0; i < 5; i++) {
			double w = rand.nextDouble()*0.1;
			out.setPoint(i, i + 1, i + 2*rand.nextGaussian(), 2*i - 3*rand.nextGaussian(), 0.9 + w);
		}

		for (int i = 0; i < 4; i++) {
			boolean fixed = i%2 == 0;

			DMatrixRMaj P = new DMatrixRMaj(3, 4);
			if (fixed) {
				for (int j = 0; j < 12; j++) {
					P.data[j] = 10.1 + j*0.2;
				}
			} else
				RandomMatrices_DDRM.fillUniform(P, rand);
			out.setView(i, fixed, P, width, height);
			out.views.data[i].camera = i/2;
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < 4; i++) {
			out.points.data[0].views.add(i);
		}
		for (int i = 1; i < out.points.size; i++) {
			out.points.data[i].views.add(i - 1);
		}

		return out;
	}
}
