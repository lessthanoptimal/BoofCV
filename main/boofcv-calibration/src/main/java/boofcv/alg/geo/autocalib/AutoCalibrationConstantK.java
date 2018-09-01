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

package boofcv.alg.geo.autocalib;

import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.NormOps_DDF3;

/**
 * Performs auto-calibration when the camera matrix is assumed to be constant. All 6-calibration
 * parameters are solved for along with the plane at infinity.
 *
 * <p>
 * w<sup>*</sup> = P Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup>
 * </p>
 *
 * TODO Describe
 *
 * TODO constant internal parameters
 * TODO aspect ratio and known skew
 * TODO
 *
 * @author Peter Abeles
 */
public class AutoCalibrationConstantK extends AutoCalibrationBase {


	public void solve() {
		if( projectives.size < 2 )
			throw new IllegalArgumentException("At least 2 projectives are required");

		solveForAbsoluteDualQuadratic();

	}


	private void solveForAbsoluteDualQuadratic() {

	}

	private class ResidualK implements FunctionNtoM {

		// calibration matrix
		DMatrix3x3 K = new DMatrix3x3();
		DMatrix3x3 KK = new DMatrix3x3();
		// plane at infinity [p,1]
		DMatrix3 p = new DMatrix3();
		// workspace
		DMatrix3x3 outer = new DMatrix3x3();
		DMatrix3x3 tmp1 = new DMatrix3x3();
		DMatrix3x3 PQP = new DMatrix3x3();

		@Override
		public void process(double[] input, double[] output) {
			K.set(  input[0],input[1],input[2],
					0,input[3],input[4],
					0,0,input[5]);
			p.set(6,input);

			CommonOps_DDF3.multTransB(K,K,KK);

			// pick a common scale
			double norm = NormOps_DDF3.fastNormF(KK);
			CommonOps_DDF3.divide(KK,norm);

			int indexOut = 0;
			for (int i = 0; i < projectives.size; i++) {
				Projective P = projectives.get(i);

				CommonOps_DDF3.multAddOuter(1.0,P.A,-1,P.a,p, outer);
				CommonOps_DDF3.mult(outer,KK,tmp1);
				CommonOps_DDF3.multTransB(tmp1,outer, PQP);

				// Common Scale
				norm = NormOps_DDF3.fastNormF(PQP);
				CommonOps_DDF3.divide(PQP,norm);

				// output is the difference between the two
				output[indexOut++] = KK.a11 - PQP.a11;
				output[indexOut++] = KK.a12 - PQP.a12;
				output[indexOut++] = KK.a13 - PQP.a13;
				output[indexOut++] = KK.a22 - PQP.a22;
				output[indexOut++] = KK.a23 - PQP.a23;
				output[indexOut++] = KK.a33 - PQP.a33;
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 9;
		}

		@Override
		public int getNumOfOutputsM() {
			return 6* projectives.size;
		}
	}
}
