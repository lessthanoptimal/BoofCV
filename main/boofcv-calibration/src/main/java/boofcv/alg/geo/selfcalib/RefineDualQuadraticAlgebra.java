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

import boofcv.misc.ConfigConverge;
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
 * <p>
 * Non-linear optimization on camera parameters for each view and for the plane at infinity. The plane at infinity
 * is used to compute a predicted camera parameter from cameras and that is then compared against the estimated
 * camera parameter for each view. The error which is minimized is an algebraic error and this should be followed up
 * with geometric error from bundle adjustment. See [1] for a complete description of the mathematics.
 * </p>
 *
 * <p>
 *     w<sup>*</sup><sub>i</sub> = K<sub>i</sub> *K<sup>T</sup><sub>i</sub> <br>
 *     w<sup>*</sup><sub>i</sub> = P<sub>i</sub>Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup><sub>i</sub>
 * </p>
 * where K<sub>i</sub>  is the 3x3 camera calibration matrix for view i. Q is a 4x4 symmetric matrix and is the absolute dual
 * quadratic. P<sub>i</sub> is a projective transform from view i+1 to i.
 *
 * <p>
 *     A[i] = P<sub>i</sub>Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup><sub>i</sub> <br>
 *     residual[i] = w<sup>*</sup><sub>i</sub>/||w<sup>*</sup><sub>i</sub>|| - A[i]/||A[i]||
 * </p>
 * Residuals are computed for each projective. The F-norm of the residuals are what is minimized.
 * w and A are normalized to ensure that they have the same scale.
 *
 * <p>Different restrictions on camera parameters can be turned on and off. Zero principle-point, zero-skew, and
 * fixed aspect ratio. If fixed aspect ratio is true then the aspect ratio of the input
 * parameters is used and kept constant.</p>
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class RefineDualQuadraticAlgebra extends SelfCalibrationBase
{
	// used to find null space of Q, which is plane at infinity
	SolveNullSpace<DMatrixRMaj> nullspace = new SolveNullSpaceSvd_DDRM();
	DMatrixRMaj _Q = new DMatrixRMaj(4,4);
	DMatrixRMaj p = new DMatrixRMaj(3,1); // plane at infinity

	// non-linear optimization
	UnconstrainedLeastSquares<DMatrixRMaj> optimizer =  FactoryOptimization.levenbergMarquardt(null,false);
	ResidualK func;
	DGrowArray param = new DGrowArray();
	ConfigConverge converge = new ConfigConverge(1e-6,1e-5,100);

	// toggles for assumptions about calibration matrix
	private boolean zeroPrinciplePoint=false;
	private boolean zeroSkew=false;
	private boolean fixedAspectRatio=false;

	// storage of aspect ratio of each camera, if configured to save
	DGrowArray aspect = new DGrowArray();

	// number of calibration parameters being optimized
	int calibParameters;

	/**
	 * Refine calibration matrix K given the dual absolute quadratic Q.
	 *
	 * @param calibration (Input) Initial estimates of K. (Output) Refined estimate.
	 * @param Q (Input) Initial estimate of absolute quadratic (Output) refined estimate.
	 */
	public boolean refine(List<CameraPinhole> calibration , DMatrix4x4 Q ) {
		if( calibration.size() != cameras.size )
			throw new RuntimeException("Calibration and cameras do not match");

		computeNumberOfCalibrationParameters();
		func = new ResidualK();

		if( func.getNumOfInputsN() > 3*calibration.size() )
			throw new IllegalArgumentException("Need more views to refine. eqs="+(3*calibration.size()+" unknowns="+func.getNumOfInputsN()));

		// Declared new each time to ensure all variables are properly zeroed


		// plane at infinity to the null space of Q
		ConvertDMatrixStruct.convert(Q,_Q);
		nullspace.process(_Q,1,p);
		CommonOps_DDRM.divide(p,p.get(3));
		p.numRows=3;

		// Convert input objects into an array which can be understood by optimization
		encode(calibration,p,param);

		// Configure optimization
//		optimizer.setVerbose(System.out,0);
		optimizer.setFunction(func,null); // Compute using a numerical Jacobian
		optimizer.initialize(param.data,converge.ftol,converge.gtol);

		// Tell it to run for at most 100 iterations
		if( !UtilOptimize.process(optimizer,converge.maxIterations) )
			return false;

		// extract solution
		decode(optimizer.getParameters(),calibration,p);
		recomputeQ(p,Q);

		return true;
	}

	private void computeNumberOfCalibrationParameters() {
		calibParameters = 0;
		if( !zeroPrinciplePoint )
			calibParameters += 2;
		if( !zeroSkew )
			calibParameters++;
		if( fixedAspectRatio )
			calibParameters++;
		else
			calibParameters += 2;
	}

	/**
	 * Compuets the absolute dual quadratic from the first camera parameters and
	 * plane at infinity
	 * @param p plane at infinity
	 * @param Q (Output) ABQ
	 */
	void recomputeQ( DMatrixRMaj p , DMatrix4x4 Q ) {
		Equation eq = new Equation();
		DMatrix3x3 K = new DMatrix3x3();
		encodeK(K,0,3,param.data);
		eq.alias(p,"p",K,"K");
		eq.process("w=K*K'");
		eq.process("Q=[w , -w*p;-p'*w , p'*w*p]");
		DMatrixRMaj _Q = eq.lookupDDRM("Q");
		CommonOps_DDRM.divide(_Q, NormOps_DDRM.normF(_Q));
		ConvertDMatrixStruct.convert(_Q,Q);
	}

	void encode( List<CameraPinhole> calibration , DMatrixRMaj p , DGrowArray params) {
		aspect.reshape(calibration.size());
		params.reshape(3+calibration.size()*calibParameters);

		int idx = 0;
		params.data[idx++] = p.data[0];
		params.data[idx++] = p.data[1];
		params.data[idx++] = p.data[2];

		for (int i = 0; i < calibration.size(); i++) {
			CameraPinhole K = calibration.get(i);
			if( fixedAspectRatio ) {
				aspect.data[i] = K.fy/K.fx;
				params.data[idx++] = K.fx;
			} else {
				params.data[idx++] = K.fx;
				params.data[idx++] = K.fy;
			}
			if( !zeroSkew ) {
				params.data[idx++] = K.skew;
			}
			if( !zeroPrinciplePoint ) {
				params.data[idx++] = K.cx;
				params.data[idx++] = K.cy;
			}

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
			if( fixedAspectRatio ) {
				K.fx = params[idx++];
				K.fy = aspect.data[i]*K.fx;
			} else {
				K.fx = params[idx++];
				K.fy = params[idx++];
			}
			if( !zeroSkew ){
				K.skew = params[idx++];
			} else {
				K.skew = 0;
			}
			if( !zeroPrinciplePoint ) {
				K.cx = params[idx++];
				K.cy = params[idx++];
			} else {
				K.cx = K.cy = 0;
			}
		}
	}

	/**
	 * Encode the calibration as a 3x3 matrix. K is assumed to zero initially or at
	 * least all non-zero elements will align with values that are written to.
	 */
	public int encodeK( DMatrix3x3 K , int which, int offset, double params[] ) {

		if( fixedAspectRatio ) {
			K.a11 = params[offset++];
			K.a22 = aspect.data[which]*K.a11;
		} else {
			K.a11 = params[offset++];
			K.a22 = params[offset++];
		}

		if( !zeroSkew ) {
			K.a12 = params[offset++];
		}

		if( !zeroPrinciplePoint ) {
			K.a13 = params[offset++];
			K.a23 = params[offset++];
		}
		K.a33 = 1;
		return  offset;
	}

	/**
	 * Computes difference between K and predicted K at each iteration.
	 */
	private class ResidualK implements FunctionNtoM {

		Equation eq = new Equation();

		// calibration matrix
		DMatrix3x3 K = new DMatrix3x3();
		// plane at infinity \pi_infty = [p;1]
		DMatrix3 p = new DMatrix3();

		@Override
		public void process(double[] input, double[] output) {
			p.set(  input[0], input[1] , input[2] );
			int indexInput = encodeK(K,0,3,input);

			eq.alias(p,"p",K,"K");
			eq.process("w0=K*K'");

			int indexOut = 0;
			for (int i = 1; i < cameras.size; i++) {
				Projective P = cameras.get(i);
				indexInput = encodeK(K,i,indexInput,param.data);

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
			return 3+calibParameters*cameras.size;
		}

		@Override
		public int getNumOfOutputsM() {
			return 6* cameras.size;
		}
	}

	public ConfigConverge getConverge() {
		return converge;
	}

	public boolean isZeroPrinciplePoint() {
		return zeroPrinciplePoint;
	}

	public void setZeroPrinciplePoint(boolean zeroPrinciplePoint) {
		this.zeroPrinciplePoint = zeroPrinciplePoint;
	}

	public boolean isZeroSkew() {
		return zeroSkew;
	}

	public void setZeroSkew(boolean zeroSkew) {
		this.zeroSkew = zeroSkew;
	}

	public boolean isFixedAspectRatio() {
		return fixedAspectRatio;
	}

	public void setFixedAspectRatio(boolean fixedAspectRatio) {
		this.fixedAspectRatio = fixedAspectRatio;
	}
}
