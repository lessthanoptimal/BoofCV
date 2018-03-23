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

package boofcv.abst.geo;

import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CheckRefineHomography extends CommonHomographyChecks {

	public abstract RefineEpipolar createAlgorithm();

	DMatrixRMaj H = new DMatrixRMaj(3,3);
	DMatrixRMaj found = new DMatrixRMaj(3,3);

	@Test
	public void perfectInput() {
		createScene(30,false);

		// use the linear algorithm to compute the homography
		HomographyDirectLinearTransform estimator = new HomographyDirectLinearTransform(true);
		estimator.process(pairs,H);

		ModelFitter<DMatrixRMaj,AssociatedPair> alg = createAlgorithm();

		//give it the perfect matrix and see if it screwed it up
		assertTrue(alg.fitModel(pairs, H, found));

		// normalize so that they are the same
		CommonOps_DDRM.divide(H,H.get(2, 2));
		CommonOps_DDRM.divide(found,found.get(2, 2));

		assertTrue(MatrixFeatures_DDRM.isEquals(H, found, 1e-8));
	}

	@Test
	public void incorrectInput() {
		createScene(30,false);

		// use the linear algorithm to compute the homography
		HomographyDirectLinearTransform estimator = new HomographyDirectLinearTransform(true);
		estimator.process(pairs,H);

		ModelFitter<DMatrixRMaj,AssociatedPair> alg = createAlgorithm();

		//give it the perfect matrix and see if it screwed it up
		DMatrixRMaj Hmod = H.copy();
		Hmod.data[0] += 0.1;
		Hmod.data[5] += 0.1;
		assertTrue(alg.fitModel(pairs, Hmod, found));

		// normalize to allow comparison
		CommonOps_DDRM.divide(H,H.get(2,2));
		CommonOps_DDRM.divide(Hmod,Hmod.get(2,2));
		CommonOps_DDRM.divide(found,found.get(2,2));

		double error0 = 0;
		double error1 = 0;

		// very crude error metric
		for( int i = 0; i < 9; i++ ) {
			error0 += Math.abs(Hmod.data[i]-H.data[i]);
			error1 += Math.abs(found.data[i]-H.data[i]);
		}

//		System.out.println("error "+error1);
		assertTrue(error1 < error0);
	}
}
