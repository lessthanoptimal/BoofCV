/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCodecSceneStructureMetric {
	Random rand = new Random(234);

	@Test
	public void encode_decode() {
		SceneStructureMetric original = createScene(rand);

		CodecSceneStructureMetric codec = new CodecSceneStructureMetric();

		int N = original.getUnknownViewCount()*6 + original.points.length*3 + original.getUnknownCameraParameterCount();
		double param[] = new double[N];
		codec.encode(original,param);

		SceneStructureMetric found = createScene(rand);
		codec.decode(param,found);

		for (int i = 0; i < original.points.length; i++) {
			assertTrue( original.points[i].distance(found.points[i]) < UtilEjml.TEST_F64);
		}

		for (int i = 0; i < original.cameras.length; i++) {
			SceneStructureMetric.Camera o = original.cameras[i];
			SceneStructureMetric.Camera f = found.cameras[i];

			double po[] = new double[o.model.getIntrinsicCount()];
			double pf[] = new double[f.model.getIntrinsicCount()];

			o.model.getIntrinsic(po,0);
			f.model.getIntrinsic(pf,0);

			assertArrayEquals(po,pf, UtilEjml.TEST_F64 );
		}

		for (int i = 0; i < original.views.length; i++) {
			SceneStructureMetric.View o = original.views[i];
			SceneStructureMetric.View f = found.views[i];

			assertTrue(MatrixFeatures_DDRM.isIdentical(o.worldToView.R,
					f.worldToView.R, UtilEjml.TEST_F64));
			assertEquals(o.worldToView.T.x,f.worldToView.T.x, UtilEjml.TEST_F64);
			assertEquals(o.worldToView.T.y,f.worldToView.T.y, UtilEjml.TEST_F64);
			assertEquals(o.worldToView.T.z,f.worldToView.T.z, UtilEjml.TEST_F64);
		}
	}

	public static SceneStructureMetric createScene(Random rand ) {
		SceneStructureMetric out = new SceneStructureMetric(false);

		out.initialize(2,4,5);

		out.setCamera(0,true,new CameraPinhole(200,300,
				0.1,400,500,1,1));
		out.setCamera(1,false,new CameraPinhole(201+rand.nextGaussian(),200,
				0.01,401+rand.nextGaussian(),50+rand.nextGaussian(),1,1));

		for (int i = 0; i < 5; i++) {
			out.setPoint(i,i+1,i+2*rand.nextGaussian(),2*i-3*rand.nextGaussian());
		}

		for (int i = 0; i < 4; i++) {
			boolean fixed = i%2==0;

			Se3_F64 a = new Se3_F64();
			if( fixed ) {
				ConvertRotation3D_F64.eulerToMatrix(EulerType.YXY, i + 0.1, 2, 0, a.R);
				a.T.set(2,3,i*7.3+5);
			} else {
				ConvertRotation3D_F64.eulerToMatrix(EulerType.YXY, i + 0.1, rand.nextGaussian(), 0, a.R);
				a.T.set(rand.nextGaussian(),3*rand.nextGaussian(),i*7.3+5);
			}
			out.setView(i,fixed,a);
			out.views[i].camera = i/2;
		}

		// Assign first point to all views then the other points to just one view
		for (int i = 0; i < 4; i++) {
			out.points[0].views.add(i);
		}
		for (int i = 1; i < out.points.length; i++) {
			out.points[i].views.add( i-1);
		}

		return out;
	}
}