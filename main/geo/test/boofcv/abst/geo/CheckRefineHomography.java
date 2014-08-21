/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CheckRefineHomography extends CommonHomographyChecks {

	public abstract RefineEpipolar createAlgorithm();

	DenseMatrix64F H = new DenseMatrix64F(3,3);
	DenseMatrix64F found = new DenseMatrix64F(3,3);

	@Test
	public void perfectInput() {
		createScene(30,false);

		// use the linear algorithm to compute the homography
		HomographyLinear4 estimator = new HomographyLinear4(true);
		estimator.process(pairs,H);

		ModelFitter<DenseMatrix64F,AssociatedPair> alg = createAlgorithm();

		//give it the perfect matrix and see if it screwed it up
		assertTrue(alg.fitModel(pairs, H, found));

		// normalize so that they are the same
		CommonOps.divide(H,H.get(2, 2));
		CommonOps.divide(found,found.get(2, 2));

		assertTrue(MatrixFeatures.isEquals(H, found, 1e-8));
	}

	@Test
	public void incorrectInput() {
		createScene(30,false);

		// use the linear algorithm to compute the homography
		HomographyLinear4 estimator = new HomographyLinear4(true);
		estimator.process(pairs,H);

		ModelFitter<DenseMatrix64F,AssociatedPair> alg = createAlgorithm();

		//give it the perfect matrix and see if it screwed it up
		DenseMatrix64F Hmod = H.copy();
		Hmod.data[0] += 0.1;
		Hmod.data[5] += 0.1;
		assertTrue(alg.fitModel(pairs, Hmod, found));

		// normalize to allow comparison
		CommonOps.divide(H,H.get(2,2));
		CommonOps.divide(Hmod,Hmod.get(2,2));
		CommonOps.divide(found,found.get(2,2));

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
