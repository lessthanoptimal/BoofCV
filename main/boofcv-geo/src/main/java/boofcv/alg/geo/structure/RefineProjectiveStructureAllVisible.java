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

package boofcv.alg.geo.structure;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.ConfigConverge;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ejml.data.DMatrixRMaj;

import java.io.PrintStream;
import java.util.List;

/**
 * Refines the projective scene's structure when all points are visible in all views. A user configuration
 * non-linear least squares optimization routine is used.
 *
 * min x'-x = P*X - x
 *
 * @see UnconstrainedLeastSquares
 *
 * @author Peter Abeles
 */
public class RefineProjectiveStructureAllVisible {

	// specifies when the optimization has converged
	ConfigConverge configConverge = new ConfigConverge(1e-8,1e-8,100);
	// the optizer
	UnconstrainedLeastSquares<DMatrixRMaj> optimizer;

	// camera matrix for each view
	FastQueue<View> views = new FastQueue<>(View.class,true);

	// list of points in homogenous coordinates
	List<Point4D_F64> points;

	// total number of views which are not fixed
	int totalNotFixed;
	// total number of parameters being optimized. 12 for each view and 4 for each point
	int numberOfParameters;

	// storage for optimized parameters
	GrowQueue_F64 parameters = new GrowQueue_F64();

	Residuals function = new Residuals();
	Jacobian jacobian = new Jacobian();

	// work space
	Point2D_F64 pixel = new Point2D_F64();

	/**
	 * Constructor which predeclares internal memory
	 *
	 * @param numViews expected number of views
	 * @param numPoints expected number of points
	 */
	public RefineProjectiveStructureAllVisible( UnconstrainedLeastSquares<DMatrixRMaj> optimizer ,
												int numViews , int numPoints ) {
		this.optimizer = optimizer;
		views.growArray(numViews);
	}

	public RefineProjectiveStructureAllVisible() {
		this.optimizer = FactoryOptimization.levenbergMarquardt(null,false);
	}

	/**
	 * Resets it into its initial state.
	 */
	public void reset() {
		views.reset();
	}

	/**
	 * Adds a camera matrix.
	 * @param fixed if true then this parameter will be fixed
	 * @param P the projective camera matrix for a view
	 */
	public void addView( boolean fixed , DMatrixRMaj P , List<Point2D_F64> observations ) {
		View v = views.grow();
		v.fixed = fixed;
		v.P.set(P);
		v.observations = observations;
	}


	/**
	 * Specifies the location of each point in homogenous space
	 */
	public void setPoints( List<Point4D_F64> list  ) {
		points = list;
	}

	/**
	 * Runs the refinement routine
	 *
	 * @return true if optimization was successful
	 */
	public boolean process() {
		// Encode passed in parameters into array for optimization
		numberOfParameters = computeNumberOfParameters();
		parameters.resize(numberOfParameters);
		encode(parameters.data);

		// Configure the optimizer
		optimizer.setFunction(function,jacobian);
		optimizer.initialize(parameters.data, configConverge.ftol, configConverge.gtol);

		// optimizer
		UtilOptimize.process(optimizer, configConverge.maxIterations);

		// recover the results
		decode(parameters.data);

		return true;
	}

	/**
	 * Computes the number of parameters being optimized
	 */
	private int computeNumberOfParameters() {
		totalNotFixed = 0;
		for (int i = 0; i < views.size; i++) {
			if( !views.get(i).fixed)
				totalNotFixed++;
		}
		numberOfParameters = 12*totalNotFixed + 4*points.size();
		return numberOfParameters;
	}

	public int getNumberOfViews() {
		return views.size;
	}

	public DMatrixRMaj getView( int which ) {
		return views.get(which).P;
	}

	public List<Point4D_F64> getPoints() {
		return points;
	}

	/**
	 * Converts data structures into a parameter array
	 */
	protected void encode( double parameters[] ) {
		int n = 0;
		for (int i = 0; i < views.size; i++) {
			if( views.get(i).fixed)
				continue;
			DMatrixRMaj P = views.get(i).P;
			for (int j = 0; j < 12; j++) {
				parameters[n++] = P.data[j];
			}
		}

		for (int i = 0; i < points.size(); i++) {
			Point4D_F64 p = points.get(i);
			parameters[n++] = p.x;
			parameters[n++] = p.y;
			parameters[n++] = p.z;
			parameters[n++] = p.w;
		}
	}

	/**
	 * Converts parameter array into data structures
	 */
	protected void decode( double parameters[] ) {
		int n = 0;
		for (int i = 0; i < views.size; i++) {
			if( views.get(i).fixed)
				continue;
			DMatrixRMaj P = views.get(i).P;
			for (int j = 0; j < 12; j++) {
				P.data[j] = parameters[n++];
			}
		}

		for (int i = 0; i < points.size(); i++) {
			Point4D_F64 p = points.get(i);
			p.x = parameters[n++];
			p.y = parameters[n++];
			p.z = parameters[n++];
			p.w = parameters[n++];
		}
	}

	public void setVerbose(PrintStream out , int level ) {
		optimizer.setVerbose(out, level);
	}

	public ConfigConverge getConfigConverge() {
		return configConverge;
	}

	public void setConfigConverge(ConfigConverge configConverge) {
		this.configConverge = configConverge;
	}

	static class View {
		// if true then the camera matrix is optimized
		boolean fixed;
		// camera matrix
		DMatrixRMaj P = new DMatrixRMaj(3,4);
		// observation of points in this value. in pixels
		List<Point2D_F64> observations;
	}

	/**
	 * Error/residual function
	 *
	 * residuals = x'-x = x' - P*X
	 */
	class Residuals implements FunctionNtoM {
		@Override
		public void process(double[] input, double[] output) {
			decode(input);

			int outIdx = 0;
			for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
				DMatrixRMaj P = views.get(viewIdx).P;
				List<Point2D_F64> observations = views.get(viewIdx).observations;

				for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
					Point4D_F64 X = points.get(pointIdx);
					Point2D_F64 x = observations.get(pointIdx);

					PerspectiveOps.renderPixel(P,X,pixel);
					output[outIdx++] = x.x - pixel.x;
					output[outIdx++] = x.y - pixel.y;
				}
			}
		}

		@Override
		public int getNumOfInputsN() {
			return numberOfParameters;
		}

		@Override
		public int getNumOfOutputsM() {
			return views.size*points.size()*2;
		}
	}

	/**
	 * Jacobian of residual function
	 */
	class Jacobian implements FunctionNtoMxN<DMatrixRMaj> {

		@Override
		public void process(double[] input, DMatrixRMaj output) {
			decode(input);
			computeViewJacobian(output);
			computePointJacobian(output);
		}

		private void computePointJacobian(DMatrixRMaj output) {
			double out[] = output.data;
			for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
				DMatrixRMaj P = views.get(viewIdx).P;

				double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2],  P14 = P.data[3];
				double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6],  P24 = P.data[7];
				double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10], P34 = P.data[11];

				for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
					int row = viewIdx*points.size()*2 + pointIdx*2;
					int outIdxX = row*numberOfParameters + totalNotFixed*12 + pointIdx*4;
					int outIdxY = outIdxX + numberOfParameters;

					Point4D_F64 X = points.get(pointIdx);

					double pX = P11*X.x + P12*X.y + P13*X.z + P14*X.w;
					double pY = P21*X.x + P22*X.y + P23*X.z + P24*X.w;
					double pZ = P31*X.x + P32*X.y + P33*X.z + P34*X.w;

//					double x = pX / pZ;
//					double y = pY / pZ;

					out[outIdxX++] = P11/pZ - pX/(pZ*pZ)*P31;
					out[outIdxX++] = P12/pZ - pX/(pZ*pZ)*P32;
					out[outIdxX++] = P13/pZ - pX/(pZ*pZ)*P33;
					out[outIdxX++] = P14/pZ - pX/(pZ*pZ)*P34;

					out[outIdxY++] = P21/pZ - pY/(pZ*pZ)*P31;
					out[outIdxY++] = P23/pZ - pY/(pZ*pZ)*P33;
					out[outIdxY++] = P22/pZ - pY/(pZ*pZ)*P32;
					out[outIdxY++] = P14/pZ - pY/(pZ*pZ)*P34;
				}
			}
		}

		private void computeViewJacobian(DMatrixRMaj output) {
			int notFixedCount = 0;
			double out[] = output.data;
			for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
				if( views.get(viewIdx).fixed )
					continue;

				DMatrixRMaj P = views.get(viewIdx).P;

				double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2],  P14 = P.data[3];
				double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6],  P24 = P.data[7];
				double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10], P34 = P.data[11];

				for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
					int row = viewIdx*points.size()*2 + pointIdx*2;
					int outIdxX = row*numberOfParameters + notFixedCount*12;
					int outIdxY = outIdxX + numberOfParameters;

					Point4D_F64 X = points.get(pointIdx);

					double pX = P11*X.x + P12*X.y + P13*X.z + P14*X.w;
					double pY = P21*X.x + P22*X.y + P23*X.z + P24*X.w;
					double pZ = P31*X.x + P32*X.y + P33*X.z + P34*X.w;

//					double x = pX / pZ;
//					double y = pY / pZ;

					// x component in output
					// row 0 in P
					out[outIdxX++] = X.x/pZ; out[outIdxX++] = X.y/pZ; out[outIdxX++] = X.z/pZ; out[outIdxX++] = X.w/pZ;
					// row 1 in P
					outIdxX += 4;
					// row 2 in P
					double c = -pX/(pZ*pZ);
					out[outIdxX++] = c*X.x; out[outIdxX++] = c*X.y; out[outIdxX++] = c*X.z; out[outIdxX++] = c*X.w;

					// y component in output
					// row 0 in P
					outIdxY += 4;
					// row 1 in P
					out[outIdxY++] = X.x/pZ; out[outIdxY++] = X.y/pZ; out[outIdxY++] = X.z/pZ; out[outIdxY++] = X.w/pZ;
					// row 2 in P
					c = -pY/(pZ*pZ);
					out[outIdxY++] = c*X.x; out[outIdxY++] = c*X.y; out[outIdxY++] = c*X.z; out[outIdxY++] = c*X.w;
				}

				notFixedCount++;
			}
		}

		@Override
		public DMatrixRMaj declareMatrixMxN() {
			return new DMatrixRMaj(getNumOfOutputsM(),getNumOfInputsN());
		}

		@Override
		public int getNumOfInputsN() {
			return numberOfParameters;
		}

		@Override
		public int getNumOfOutputsM() {
			return views.size*points.size()*2;
		}
	}
}
