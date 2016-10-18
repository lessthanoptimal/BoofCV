/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.alg.geo.RodriguesRotationJacobian;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Analytical Jacobian for optimizing calibration parameters.
 * </p>
 *
 * <p>
 * NOTE: Accuracy is tested in a unit test using a numerical Jacobian.  A very crude tolerance
 * was required to make it pass. I think this implementation is correct, but hand computing the
 * Jacobian is error prone.  In practice it produces virtually the same final results as the numerical
 * Jacobian.
 * </p>
 *
 * @author Peter Abeles
 */
public class Zhang99OptimizationJacobian implements FunctionNtoMxN {

	// used to compute the Jacobian from Rodrigues coordinates
	RodriguesRotationJacobian rodJacobian = new RodriguesRotationJacobian();

	// local variable which stores the predicted location of the feature in the camera frame
	Rodrigues_F64 rodrigues = new Rodrigues_F64();

	// number of functions and parameters being optimized
	private int numParam, numFuncs;

	// description of the calibration grid
	private List<Point3D_F64> grid = new ArrayList<>();

	// List of observation sets.  Required so that it knows the total number of observations in each set
	private List<CalibrationObservation> observationSets;

	// variables for storing intermediate results
	private Se3_F64 se = new Se3_F64();

	// location of point in camera frame
	private Point3D_F64 cameraPt = new Point3D_F64();
	// observed point location in undistorted normalized image coordinates
	private Point2D_F64 normPt = new Point2D_F64();
	// observed point location in distorted normalized image coordinates
	private Point2D_F64 dnormPt = new Point2D_F64();

	// stores the optimization parameters
	private Zhang99ParamCamera param;

	// output index for x and y
	int indexJacX;
	int indexJacY;

	Point3D_F64 Xdot = new Point3D_F64();

	/**
	 * Configurations the optimization function.
	 *
	 * @param grid Location of points on the calibration grid.  z=0
	 */
	public Zhang99OptimizationJacobian(boolean assumeZeroSkew,
									   int numRadial ,
									   boolean includeTangential,
									   List<CalibrationObservation> observationSets,
									   List<Point2D_F64> grid ) {
		this.param = new Zhang99ParamCamera(assumeZeroSkew,numRadial,includeTangential);
		this.observationSets = observationSets;

		for( Point2D_F64 p : grid ) {
			this.grid.add( new Point3D_F64(p.x,p.y,0) );
		}

		numParam = param.numParameters()+(3+3)*observationSets.size();

		numFuncs = CalibrationPlanarGridZhang99.totalPoints(observationSets)*2;
		param.zeroNotUsed();
	}

	@Override
	public int getNumOfInputsN() {
		return numParam;
	}

	@Override
	public int getNumOfOutputsM() {
		return numFuncs;
	}

	@Override
	public void process(double[] input, double[] output) {
		int index = param.setFromParam(input);

		int indexPoint = 0;
		for( int indexView = 0; indexView < observationSets.size(); indexView++ ) {
			CalibrationObservation set = observationSets.get(indexView);

			// extract rotation and translation parameters
			double rodX = input[index++];
			double rodY = input[index++];
			double rodZ = input[index++];
			double tranX = input[index++];
			double tranY = input[index++];
			double tranZ = input[index++];

			rodrigues.setParamVector(rodX,rodY,rodZ);
			rodJacobian.process(rodX,rodY,rodZ);

			ConvertRotation3D_F64.rodriguesToMatrix(rodrigues, se.getR());
			se.T.set(tranX, tranY, tranZ);

			for( int i = 0; i < set.size(); i++ , indexPoint++ ) {
				int gridIndex = set.points.get(i).index;

				// index = (function index)*numParam
				indexJacX = (2*indexPoint     )*numParam;
				indexJacY = (2*indexPoint + 1 )*numParam;

				// Put the point in the camera's reference frame
				SePointOps_F64.transform(se, grid.get(gridIndex), cameraPt);

				// normalized pixel coordinates
				normPt.x = cameraPt.x/ cameraPt.z;
				normPt.y = cameraPt.y/ cameraPt.z;

				// apply distortion to the normalized coordinate
				dnormPt.set(normPt);
				CalibrationPlanarGridZhang99.applyDistortion(dnormPt, param.radial, param.t1, param.t2);

				calibrationGradient(dnormPt,output);
				distortGradient(normPt,output);

				indexJacX += indexView*6;
				indexJacY += indexView*6;

				rodriguesGradient(rodJacobian.Rx,grid.get(gridIndex),cameraPt, normPt,output);
				rodriguesGradient(rodJacobian.Ry,grid.get(gridIndex),cameraPt, normPt,output);
				rodriguesGradient(rodJacobian.Rz,grid.get(gridIndex),cameraPt, normPt,output);

				translateGradient(cameraPt, normPt,output);
			}
		}
	}

	/**
	 * Gradient for calibration matrix
	 */
	private void calibrationGradient( Point2D_F64 distNorm , double[] output ) {
		output[indexJacX++] = distNorm.x;
		output[indexJacX++] = 0;
		if( !param.assumeZeroSkew )
			output[indexJacX++] = distNorm.y;
		output[indexJacX++] = 1;
		output[indexJacX++] = 0;

		output[indexJacY++] = 0;
		output[indexJacY++] = distNorm.y;
		if( !param.assumeZeroSkew )
			output[indexJacY++] = 0;
		output[indexJacY++] = 0;
		output[indexJacY++] = 1;
	}

	/**
	 * Gradient for radial and tangential distortion
	 *
	 * @param norm undistorted normalized image coordinate
	 */
	private void distortGradient( Point2D_F64 norm , double[] output ) {

		double r2 = norm.x*norm.x + norm.y*norm.y;
		double r2i = r2;
		for( int i = 0; i < param.radial.length; i++ ) {
			double xdot = norm.x*r2i;
			double ydot = norm.y*r2i;

			output[indexJacX++] = param.a*xdot + param.c*ydot;
			output[indexJacY++] = param.b*ydot;
			r2i *= r2;
		}

		if( param.includeTangential ) {
			double xy2 = 2.0*norm.x*norm.y;
			double r2yy = r2 + 2*norm.y*norm.y;
			double r2xx = r2 + 2*norm.x*norm.x;

			output[indexJacX++] = param.a*xy2 + param.c*r2yy;
			output[indexJacY++] = param.b*r2yy;

			output[indexJacX++] = param.a*r2xx + param.c*xy2;
			output[indexJacY++] = param.b*xy2;
		}
	}

	/**
	 * Adds to the Jacobian matrix using the derivative from a Rodrigues parameter.
	 *
	 * deriv [x,y] = [distort deriv] - [dist]*dot(z)/(z^2)*(R*X+T) + [dist]*(1/z)*dot(R)*X
	 *
	 * where R is rotation matrix, T is translation, z = z-coordinate of point in camera frame
	 *
	 * @param Rdot Jacobian for Rodrigues
	 * @param X Location of point in world coordinates
	 */
	private void rodriguesGradient( DenseMatrix64F Rdot ,
									Point3D_F64 X ,
									Point3D_F64 cameraPt ,
									Point2D_F64 normPt ,
									double[] output ) {
		// create short hand for normalized image coordinate
		final double x = normPt.x;
		final double y = normPt.y;

		final double r2 = x*x + y*y;
		double r2i = r2;
		double rdev = 1;

		double sum = 0;
		double sumdot = 0;

		for( int i = 0; i < param.radial.length; i++ ) {
			sum += param.radial[i]*r2i;
			sumdot += param.radial[i]*2*(i+1)*rdev;

			r2i *= r2;
			rdev *= r2;
		}

		GeometryMath_F64.mult(Rdot,X,Xdot);

		// part of radial distortion derivative
		double r_dot = (x*Xdot.x + y*Xdot.y)/cameraPt.z - r2*Xdot.z/cameraPt.z;

		// derivative of normPt
		double n_dot_x = (-x*Xdot.z+Xdot.x)/cameraPt.z;
		double n_dot_y = (-y*Xdot.z+Xdot.y)/cameraPt.z;
//		double n_dot_z = 0;

		// total partial derivative
		double xdot = sumdot*r_dot*x + (1 + sum)*n_dot_x;
		double ydot = sumdot*r_dot*y + (1 + sum)*n_dot_y;
//		double zdot = 0;

		if( param.includeTangential ) {
			xdot += 2*param.t1*(n_dot_x*y + x*n_dot_y) + 6*param.t2*x*n_dot_x + 2*param.t2*y*n_dot_y;
			ydot += 2*param.t1*x*n_dot_x + 6*param.t1*y*n_dot_y + 2*param.t2*(n_dot_x*y + x*n_dot_y);
		}

		output[indexJacX++] = param.a*xdot + param.c*ydot;
		output[indexJacY++] = param.b*ydot;
	}

	/**
	 * Gradient for translational motion component
	 *
	 * deriv [x,y] = [distort deriv] - [dist]*dot(z)*T/(z^2) + [dist]*dot(T)/z
	 *
	 * where T is translation, z = z-coordinate of point in camera frame
	 */
	private void translateGradient( Point3D_F64 cameraPt ,
									Point2D_F64 normPt ,
									double[] output ) {

		// create short hand for normalized image coordinate
		final double x = normPt.x;
		final double y = normPt.y;

		final double r2 = x*x + y*y;
		double r2i = r2;
		double rdev = 1;

		double sum = 0;
		double sumdot = 0;

		for( int i = 0; i < param.radial.length; i++ ) {
			sum += param.radial[i]*r2i;
			sumdot += param.radial[i]*(i+1)*rdev;

			r2i *= r2;
			rdev *= r2;
		}
		// Partial T.x
		double xdot = sumdot*2*x*x/cameraPt.z + (1+sum)/cameraPt.z;
		double ydot = sumdot*2*x*y/cameraPt.z;
		// double zdot = 0
		if( param.includeTangential ) {
			xdot += (2*param.t1*y + param.t2*6*x)/cameraPt.z;
			ydot += (2*param.t1*x + 2*y*param.t2)/cameraPt.z;
		}

		output[indexJacX++] = param.a*xdot + param.c*ydot;
		output[indexJacY++] = param.b*ydot;

		// Partial T.y
		xdot = sumdot*2*y*x/cameraPt.z;
		ydot = sumdot*2*y*y/cameraPt.z + (1 + sum)/cameraPt.z;
		if( param.includeTangential ) {
			xdot += (2*param.t1*x + param.t2*2*y)/cameraPt.z;
			ydot += (6*param.t1*y + 2*x*param.t2)/cameraPt.z;
		}

		output[indexJacX++] = param.a*xdot + param.c*ydot;
		output[indexJacY++] = param.b*ydot;

		// Partial T.z
		xdot = -sumdot*2*r2*x/cameraPt.z;
		ydot = -sumdot*2*r2*y/cameraPt.z;

		xdot += -(1 + sum)*x/cameraPt.z;
		ydot += -(1 + sum)*y/cameraPt.z;

		if( param.includeTangential ) {
			xdot += -(4*param.t1*x*y + 6*param.t2*x*x + 2*param.t2*y*y)/cameraPt.z;
			ydot += -(2*param.t1*x*x + 6*param.t1*y*y + 4*x*y*param.t2)/cameraPt.z;
		}

		output[indexJacX++] = param.a*xdot + param.c*ydot;
		output[indexJacY++] = param.b*ydot;
	}
}
