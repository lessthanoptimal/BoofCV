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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.calib.CameraPinhole;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.*;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.equation.Equation;
import org.ejml.interfaces.SolveNullSpace;
import org.ejml.ops.ConvertDMatrixStruct;

import java.util.List;

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
public class SelfCalibrationRefineDualQuadratic extends SelfCalibrationBase {

	SolveNullSpace<DMatrixRMaj> nullspace = new SolveNullSpaceSvd_DDRM();
	DMatrixRMaj _Q = new DMatrixRMaj(4,4);
	DMatrixRMaj p = new DMatrixRMaj(3,1);

	DGrowArray param = new DGrowArray();

	UnconstrainedLeastSquares<DMatrixRMaj> optimizer =  FactoryOptimization.levenbergMarquardt(null,true);
	ResidualK func = new ResidualK();

	/**
	 * Refine calibration matrix K given the dual absolute quadratic Q.
	 *
	 * @param calibration (Input) Initial estimates of K. (Output) Refined estimate.
	 * @param Q (Input) Initial estimate of absolute quadratic (Output) refined estimate.
	 */
	public boolean refine(List<CameraPinhole> calibration , DMatrix4x4 Q ) {
		if( calibration.size()-1 != projectives.size )
			throw new RuntimeException("Calibration and projectives do not match");

		if( projectives.size < 2 )
			throw new IllegalArgumentException("At least 2 projectives are required. You should have more");

		// plane at infinity to the null space of Q
		ConvertDMatrixStruct.convert(Q,_Q);
		nullspace.process(_Q,1,p);
		CommonOps_DDRM.divide(p,p.get(3));
		p.numRows=3;

		System.out.println("Calibration = "+calibration.size());
		System.out.println("projectives = "+projectives.size());

		encode(calibration,p,param);

		optimizer.setVerbose(System.out,0);
		optimizer.setFunction(func,null);
		optimizer.initialize(param.data,1e-8,1e-8);

		if( !UtilOptimize.process(optimizer,100) )
			return false;

		decode(optimizer.getParameters(),calibration,p);
		recomputeQ(p,Q);

		return true;
	}

	void recomputeQ( DMatrixRMaj p , DMatrix4x4 Q ) {
		Equation eq = new Equation();
		DMatrix3x3 K = new DMatrix3x3();
		encodeK(K,3,param.data);
		eq.alias(p,"p",K,"K");
		eq.process("w=K*K'");
		eq.process("Q=[w , -w*p;-p'*w , p'*w*p]");
		DMatrixRMaj _Q = eq.lookupDDRM("Q");
		CommonOps_DDRM.divide(_Q, NormOps_DDRM.normF(_Q));
		ConvertDMatrixStruct.convert(_Q,Q);
	}

	void encode( List<CameraPinhole> calibration , DMatrixRMaj p , DGrowArray params) {
		params.reshape(3+calibration.size()*5);

		int idx = 0;
		params.data[idx++] = p.data[0];
		params.data[idx++] = p.data[1];
		params.data[idx++] = p.data[2];

		for (int i = 0; i < calibration.size(); i++) {
			CameraPinhole K = calibration.get(i);
			params.data[idx++] = K.fx;
			params.data[idx++] = K.skew;
			params.data[idx++] = K.cx;
			params.data[idx++] = K.fy;
			params.data[idx++] = K.cy;
		}
	}

	void decode( double[] params , List<CameraPinhole> calibration , DMatrixRMaj p )
	{
		int idx = 0;
		p.data[0] = params[idx++];
		p.data[1] = params[idx++];
		p.data[2] = params[idx++];

		for (int i = 0; i < calibration.size(); i++) {
			CameraPinhole K = calibration.get(i);
			K.fx = params[idx++];
			K.skew = params[idx++];
			K.cx = params[idx++];
			K.fy = params[idx++];
			K.cy = params[idx++];
		}
	}

	private class ResidualK implements FunctionNtoM {

		Equation eq = new Equation();

		// calibration matrix
		DMatrix3x3 K = new DMatrix3x3();
		// plane at infinity \pi_infty = [p;1]
		DMatrix3 p = new DMatrix3();

		@Override
		public void process(double[] input, double[] output) {
			p.set(  input[0], input[1] , input[2] );
			K.set(  input[3],input[4],input[5],
					0,input[6],input[7],
					0,0,1);

			eq.alias(p,"p",K,"K");
			eq.process("w0=K*K'");

			int indexInput = 8;
			int indexOut = 0;
			for (int i = 0; i < projectives.size; i++) {
				Projective P = projectives.get(i);
				encodeK(K,indexInput,param.data);
				indexInput += 5;

				eq.alias(K,"K",P.A,"A",P.a,"a");
				eq.process("AP = A-a*p'");
				eq.process("kk = K*K'/normF(K*K')");
				eq.process("AW = AP*w0*AP'");
				eq.process("AW = AW / normF(AW)");
				eq.process("R = kk-AW");

				DMatrixRMaj residual = eq.lookupDDRM("R");
				output[indexOut++] = residual.get(0,0);
				output[indexOut++] = residual.get(0,1);
				output[indexOut++] = residual.get(0,2);
				output[indexOut++] = residual.get(1,1);
				output[indexOut++] = residual.get(1,2);
				output[indexOut++] = residual.get(2,2);
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 3+5*(projectives.size+1);
		}

		@Override
		public int getNumOfOutputsM() {
			return 6*projectives.size;
		}
	}
}
