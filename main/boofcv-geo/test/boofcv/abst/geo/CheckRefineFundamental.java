/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.f.EpipolarTestSimulation;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CheckRefineFundamental extends EpipolarTestSimulation {


	public abstract RefineEpipolar createAlgorithm();

	DMatrixRMaj found = new DMatrixRMaj(3,3);

	@Test
	public void checkMarkerInterface() {
		ModelFitter<DMatrixRMaj,AssociatedPair> alg = createAlgorithm();
	}

	@Test
	public void perfectInput() {
		init(30,false);

		// compute true essential matrix
		DMatrixRMaj E = MultiViewOps.createEssential(worldToCamera.getR(), worldToCamera.getT());

		ModelFitter<DMatrixRMaj,AssociatedPair> alg = createAlgorithm();

		//give it the perfect matrix and see if it screwed it up
		assertTrue(alg.fitModel(pairs, E, found));

		// normalize so that they are the same
		CommonOps_DDRM.divide(E,E.get(2, 2));
		CommonOps_DDRM.divide(found,found.get(2,2));

		assertTrue(MatrixFeatures_DDRM.isEquals(E, found, 1e-8));
	}

	@Test
	public void incorrectInput() {
		init(30,false);

		// compute true essential matrix
		DMatrixRMaj E = MultiViewOps.createEssential(worldToCamera.getR(), worldToCamera.getT());

		// create an alternative incorrect matrix
		Vector3D_F64 T = worldToCamera.getT().copy();
		T.x += 0.1;
		DMatrixRMaj Emod = MultiViewOps.createEssential(worldToCamera.getR(), T);

		ModelFitter<DMatrixRMaj,AssociatedPair> alg = createAlgorithm();

		// compute and compare results
		assertTrue(alg.fitModel(pairs, Emod, found));

		// normalize to allow comparison
		CommonOps_DDRM.divide(E,E.get(2,2));
		CommonOps_DDRM.divide(Emod,Emod.get(2,2));
		CommonOps_DDRM.divide(found,found.get(2,2));

		double error0 = 0;
		double error1 = 0;

		// very crude error metric
		for( int i = 0; i < 9; i++ ) {
			error0 += Math.abs(Emod.data[i]-E.data[i]);
			error1 += Math.abs(found.data[i]-E.data[i]);
		}

//		System.out.println("error "+error1+"   other "+error0);
		assertTrue(error1 < error0);
	}
}
