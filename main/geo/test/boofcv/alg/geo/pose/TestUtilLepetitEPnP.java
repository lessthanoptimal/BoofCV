/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilLepetitEPnP {

	Random rand = new Random(234);

	@Test
	public void jacobian4() {

		DenseMatrix64F L_full = RandomMatrices.createRandom(6, 10, rand);
		DenseMatrix64F y = RandomMatrices.createRandom(6,1,rand);

		JacobianEPnP jacobian = new JacobianEPnP();
		ResidualsEPnP residuals = new ResidualsEPnP();

		residuals.setParameters(L_full, y);
		jacobian.setParameters(L_full);

		boolean worked = DerivativeChecker.jacobian(residuals, jacobian, new double[]{1, 2, 3, 4}, 1e-6);
		assertTrue(worked);
	}

	@Test
	public void jacobian3() {

		DenseMatrix64F L_full = RandomMatrices.createRandom(3,6,rand);
		DenseMatrix64F y = RandomMatrices.createRandom(3,1,rand);

		JacobianEPnP jacobian = new JacobianEPnP();
		ResidualsEPnP residuals = new ResidualsEPnP();

		residuals.setParameters(L_full, y);
		jacobian.setParameters(L_full);

		boolean worked = DerivativeChecker.jacobian(residuals,jacobian,new double[]{1,2,3},1e-6);
		assertTrue(worked);
	}

	/**
	 * Used to check jacobian function in UtilLepetitEPnP
	 */
	public class JacobianEPnP implements FunctionNtoMxN {

		// number of control points
		protected int numControl;

		// linear constraint matrix
		protected DenseMatrix64F L_full;

		protected DenseMatrix64F jacobian = new DenseMatrix64F(1,1);

		public void setParameters( DenseMatrix64F L_full ) {
			if( L_full.numRows == 6 )
				numControl = 4;
			else
				numControl = 3;

			this.L_full = L_full;
			jacobian.numRows = L_full.numRows;
			jacobian.numCols = numControl;
		}

		@Override
		public int getNumOfInputsN() {
			return numControl;
		}

		@Override
		public int getNumOfOutputsM() {
			return L_full.numRows;
		}

		@Override
		public void process(double[] input, double[] output) {

			jacobian.data = output;

			if( numControl == 3)
				UtilLepetitEPnP.jacobian_Control3(L_full, input, jacobian);
			else
				UtilLepetitEPnP.jacobian_Control4(L_full, input, jacobian);
		}
	}

	/**
	 * Used to check jacobian function in UtilLepetitEPnP
	 */
	public class ResidualsEPnP implements FunctionNtoM {

		// number of control points
		protected int numControl;

		// linear constraint matrix
		protected DenseMatrix64F L_full;
		// distance between control points
		protected DenseMatrix64F y;

		public void setParameters( DenseMatrix64F L_full , DenseMatrix64F y ) {
			if( L_full.numRows == 6 )
				numControl = 4;
			else
				numControl = 3;

			this.L_full = L_full;
			this.y = y;
		}

		@Override
		public int getNumOfInputsN() {
			return numControl;
		}

		@Override
		public int getNumOfOutputsM() {
			return L_full.numRows;
		}

		@Override
		public void process(double[] input, double[] output) {

			if( numControl == 3)
				UtilLepetitEPnP.residuals_Control3(L_full,y,input,output);
			else
				UtilLepetitEPnP.residuals_Control4(L_full, y, input, output);
		}
	}
}
