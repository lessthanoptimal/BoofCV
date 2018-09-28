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

import boofcv.abst.geo.bundle.SceneStructureProjective;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCodecSceneStructureProjective {
	Random rand = new Random(234);

	@Test
	public void encode_decode() {
		SceneStructureProjective original = createScene3D(rand);

		CodecSceneStructureProjective codec = new CodecSceneStructureProjective();

		int N = original.getUnknownViewCount()*12 + original.points.length*3;
		double param[] = new double[N];
		codec.encode(original,param);

		SceneStructureProjective found = createScene3D(rand);
		codec.decode(param,found);

		for (int i = 0; i < original.points.length; i++) {
			assertTrue( original.points[i].distance(found.points[i]) < UtilEjml.TEST_F64);
		}


		for (int i = 0; i < original.views.length; i++) {
			SceneStructureProjective.View o = original.views[i];
			SceneStructureProjective.View f = found.views[i];

			assertTrue(MatrixFeatures_DDRM.isIdentical(o.worldToView,f.worldToView,UtilEjml.TEST_F64));
		}
	}

	public static SceneStructureProjective createScene3D(Random rand ) {
		SceneStructureProjective out = new SceneStructureProjective(false);

		out.initialize(4,5);

		for (int i = 0; i < 5; i++) {
			out.setPoint(i,i+1,i+2*rand.nextGaussian(),2*i-3*rand.nextGaussian());
		}

		for (int i = 0; i < 4; i++) {
			boolean fixed = i%2==0;

			DMatrixRMaj P = new DMatrixRMaj(3,4);
			if( fixed ) {
				for (int j = 0; j < 12; j++) {
					P.data[j] = 0.1+j*0.2;
				}
			} else
				RandomMatrices_DDRM.fillUniform(P,rand);
			out.setView(i,fixed,P);
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

	public static SceneStructureProjective createSceneH(Random rand ) {
		SceneStructureProjective out = new SceneStructureProjective(true);

		out.initialize(4,5);

		for (int i = 0; i < 5; i++) {
			double w = rand.nextDouble()*0.1;
			out.setPoint(i,i+1,i+2*rand.nextGaussian(),2*i-3*rand.nextGaussian(),0.9+w);
		}

		for (int i = 0; i < 4; i++) {
			boolean fixed = i%2==0;

			DMatrixRMaj P = new DMatrixRMaj(3,4);
			if( fixed ) {
				for (int j = 0; j < 12; j++) {
					P.data[j] = 0.1+j*0.2;
				}
			} else
				RandomMatrices_DDRM.fillUniform(P,rand);
			out.setView(i,fixed,P);
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