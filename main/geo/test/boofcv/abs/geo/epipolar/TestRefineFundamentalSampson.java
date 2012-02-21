/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abs.geo.epipolar;

import boofcv.abst.geo.epipolar.RefineFundamentalSampson;
import boofcv.alg.geo.d3.epipolar.CommonFundamentalChecks;
import boofcv.alg.geo.d3.epipolar.UtilEpipolar;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineFundamentalSampson extends CommonFundamentalChecks {

	@Test
	public void perfectInput() {
		init(30,false);

		// compute true essential matrix
		DenseMatrix64F E = UtilEpipolar.computeEssential(motion.getR(), motion.getT());

		RefineFundamentalSampson alg = new RefineFundamentalSampson(1e-8,100);
		
		//give it the perfect matrix and see if it screwed it up
		assertTrue(alg.process(E, pairs));

		DenseMatrix64F found = alg.getRefinement();

		// normalize so that they are the same
		CommonOps.divide(E.get(2,2),E);
		CommonOps.divide(found.get(2,2),found);

		assertTrue(MatrixFeatures.isEquals(E, found, 1e-8));
	}

	@Test
	public void incorrectInput() {
		init(30,false);

		// compute true essential matrix
		DenseMatrix64F E = UtilEpipolar.computeEssential(motion.getR(), motion.getT());

		// create an alternative incorrect matrix
		Vector3D_F64 T = motion.getT().copy();
		T.x += 0.5;
		DenseMatrix64F Emod = UtilEpipolar.computeEssential(motion.getR(), T);

		RefineFundamentalSampson alg = new RefineFundamentalSampson(1e-8,100);

		// compute and compare results
		assertTrue(alg.process(Emod, pairs));
		
		DenseMatrix64F found = alg.getRefinement();

		// normalize to allow comparison
		CommonOps.divide(E.get(2,2),E);
		CommonOps.divide(Emod.get(2,2),Emod);
		CommonOps.divide(found.get(2,2),found);
		
		double error0 = 0;
		double error1 = 0;

		// very crude error metric
		for( int i = 0; i < 9; i++ ) {
			error0 += Math.abs(Emod.data[i]-E.data[i]);
			error1 += Math.abs(found.data[i]-E.data[i]);
		}

		assertTrue(error1 < error0);
	}
}
