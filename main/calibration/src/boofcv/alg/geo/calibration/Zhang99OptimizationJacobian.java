/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
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
	private List<Point3D_F64> grid = new ArrayList<Point3D_F64>();

	// number of calibration targets that were observed
	private int numObservedTargets;

	// should it assume the skew parameter is zero?
	private boolean assumeZeroSkew;

	// variables for storing intermediate results
	private Se3_F64 se = new Se3_F64();

	// location of point in camera frame
	private Point3D_F64 cameraPt = new Point3D_F64();
	// observed point location in normalized image coordinates
	private Point2D_F64 normPt = new Point2D_F64();

	// intrinsic camera parameters
	public double a,b,c,x0,y0;
	// radial distortion
	public double radial[];

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
									   int numObservedTargets ,
									   List<Point2D_F64> grid ) {
		this.assumeZeroSkew = assumeZeroSkew;
		this.numObservedTargets = numObservedTargets;

		for( Point2D_F64 p : grid ) {
			this.grid.add( new Point3D_F64(p.x,p.y,0) );
		}

		numParam = numRadial+(3+3)*numObservedTargets;
		if( assumeZeroSkew )
			numParam += 4;
		else
			numParam += 5;

		numFuncs = numObservedTargets*grid.size()*2;

		radial = new double[numRadial];

		if( assumeZeroSkew )
			c = 0;
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
		int index = 0;

		// extract calibration matrix parameters
		a = input[index++];
		b = input[index++];
		if( !assumeZeroSkew )
			c = input[index++];
		x0 = input[index++];
		y0 = input[index++];

		// extract radial distortion parameters
		for( int i = 0; i < radial.length; i++ ) {
			radial[i] = input[index++];
		}

		for( int indexView = 0; indexView < numObservedTargets; indexView++ ) {

			// extract rotation and translation parameters
			double rodX = input[index++];
			double rodY = input[index++];
			double rodZ = input[index++];
			double tranX = input[index++];
			double tranY = input[index++];
			double tranZ = input[index++];

			rodrigues.setParamVector(rodX,rodY,rodZ);
			rodJacobian.process(rodX,rodY,rodZ);

			RotationMatrixGenerator.rodriguesToMatrix(rodrigues, se.getR());
			se.T.set(tranX, tranY, tranZ);

			for( int i = 0; i < grid.size(); i++ ) {
				// index = (function index)*numParam
				indexJacX = (2*indexView*grid.size() + i*2     )*numParam;
				indexJacY = (2*indexView*grid.size() + i*2 + 1 )*numParam;

				// Put the point in the camera's reference frame
				SePointOps_F64.transform(se, grid.get(i), cameraPt);

				// normalized pixel coordinates
				normPt.x = cameraPt.x/ cameraPt.z;
				normPt.y = cameraPt.y/ cameraPt.z;

				calibrationGradient(output);
				distortGradient(normPt,output);

				indexJacX += indexView*6;
				indexJacY += indexView*6;

				rodriguesGradient(rodJacobian.Rx,grid.get(i),cameraPt, normPt,output);
				rodriguesGradient(rodJacobian.Ry,grid.get(i),cameraPt, normPt,output);
				rodriguesGradient(rodJacobian.Rz,grid.get(i),cameraPt, normPt,output);

				translateGradient(cameraPt, normPt,output);
			}
		}
	}

	/**
	 * Gradient for calibration matrix
	 */
	private void calibrationGradient( double[] output ) {
		output[indexJacX++] = normPt.x;
		output[indexJacX++] = 0;
		if( !assumeZeroSkew )
			output[indexJacX++] = normPt.y;
		output[indexJacX++] = 1;
		output[indexJacX++] = 0;

		output[indexJacY++] = 0;
		output[indexJacY++] = normPt.y;
		if( !assumeZeroSkew )
			output[indexJacY++] = 0;
		output[indexJacY++] = 0;
		output[indexJacY++] = 1;
	}

	/**
	 * Gradient for radial distortion
	 *
	 * deriv [x,y] =  [x,y]*r
	 *       [x,y] =  [x,y]*r*r
	 */
	private void distortGradient( Point2D_F64 pt , double[] output ) {

		double r2 = pt.x*pt.x + pt.y*pt.y;
		double r = r2;
		for( int i = 0; i < radial.length; i++ ) {
			double xdot = pt.x*r;
			double ydot = pt.y*r;

			output[indexJacX++] = a*xdot + c*ydot;
			output[indexJacY++] = b*ydot;
			r *= r2;
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

		double r2 = normPt.x*normPt.x + normPt.y*normPt.y;
		double r = r2;
		double rdev = 1;

		double sum = 0;
		double sumdot = 0;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r;
			sumdot += radial[i]*2*(i+1)*rdev;

			r *= r2;
			rdev *= r2;
		}

		GeometryMath_F64.mult(Rdot,X,Xdot);

		// part of radial distortion derivative
		double r_dot = (normPt.x*Xdot.x + normPt.y*Xdot.y)/cameraPt.z - r2*Xdot.z/cameraPt.z;

		// derivative of normPt
		double n_dot_x = (-normPt.x*Xdot.z+Xdot.x)/cameraPt.z;
		double n_dot_y = (-normPt.y*Xdot.z+Xdot.y)/cameraPt.z;
//		double n_dot_z = 0;

		// total partial derivative
		double xdot = sumdot*r_dot*normPt.x + (1 + sum)*n_dot_x;
		double ydot = sumdot*r_dot*normPt.y + (1 + sum)*n_dot_y;
//		double zdot = 0;

		output[indexJacX++] = a*xdot + c*ydot;
		output[indexJacY++] = b*ydot;
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

		double r2 = normPt.x*normPt.x + normPt.y*normPt.y;
		double r = r2;
		double rdev = 1;

		double sum = 0;
		double sumdot = 0;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r;
			sumdot += radial[i]*2*(i+1)*rdev;

			r *= r2;
			rdev *= r2;
		}
		// Partial T.x
		double xdot = sumdot*normPt.x*normPt.x/cameraPt.z + (1+sum)/cameraPt.z;
		double ydot = sumdot*normPt.x*normPt.y/cameraPt.z;
		//double zdot = 0;

		output[indexJacX++] = a*xdot + c*ydot;
		output[indexJacY++] = b*ydot;

		// Partial T.y
		xdot = sumdot*normPt.y*normPt.x/cameraPt.z;
		ydot = sumdot*normPt.y*normPt.y/cameraPt.z + (1 + sum)/cameraPt.z;

		output[indexJacX++] = a*xdot + c*ydot;
		output[indexJacY++] = b*ydot;

		// Partial T.z
		xdot = -sumdot*r2*normPt.x/cameraPt.z;
		ydot = -sumdot*r2*normPt.y/cameraPt.z;

		xdot += -(1 + sum)*normPt.x/cameraPt.z;
		ydot += -(1 + sum)*normPt.y/cameraPt.z;

		output[indexJacX++] = a*xdot + c*ydot;
		output[indexJacY++] = b*ydot;
	}
}
