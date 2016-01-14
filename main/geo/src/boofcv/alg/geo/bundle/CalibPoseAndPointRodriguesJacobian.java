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

package boofcv.alg.geo.bundle;

import boofcv.alg.geo.RodriguesRotationJacobian;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;

import java.util.Arrays;
import java.util.List;

/**
 * Computes the Jacobian for {@link CalibPoseAndPointResiduals}.
 *
 * @author Peter Abeles
 */
public class CalibPoseAndPointRodriguesJacobian implements FunctionNtoMxN {

	// if the extrinsic parameters are known, specify them here
	Se3_F64 extrinsic[];
	// observed location of features in each view
	List<ViewPointObservations> observations;

	// number of camera views
	int numViews;
	// number of points in world coordinates
	int numPoints;
	// number of views with unknown extrinsic parameters
	int numViewsUnknown;
	// number of observations across all views
	int numObservations;
	// total number of parameters being optimized
	int numParameters;

	// used to compute the Jacobian from Rodrigues coordinates
	RodriguesRotationJacobian rodJacobian = new RodriguesRotationJacobian();

	// local variable which stores the predicted location of the feature in the camera frame
	Rodrigues_F64 rodrigues = new Rodrigues_F64();
	// rotation matrix
	DenseMatrix64F R = new DenseMatrix64F(3,3);
	// translation vector
	Vector3D_F64 T = new Vector3D_F64();
	// feature location in world coordinates
	Point3D_F64 worldPt = new Point3D_F64();
	// feature location in camera coordinates
	Point3D_F64 cameraPt = new Point3D_F64();

	// index in parameters of the first point
	int indexFirstPoint;
	// how many point observations have been processed
	int countPointObs;

	// Jacobian matrix index of x and y partial
	int indexX;
	int indexY;

	// reference to output Jacobian matrix
	double[] output;
	
	public void configure( List<ViewPointObservations> observations , int numPoints , Se3_F64 ...extrinsic) {
		if( extrinsic.length < observations.size() )
			throw new RuntimeException("knownExtrinsic length is less than the number of views in 'observations'");
		
		this.observations = observations;
		this.extrinsic = extrinsic;
		this.numViews = observations.size();
		this.numPoints = numPoints;
		
		numViewsUnknown = 0;
		numObservations = 0;
		for( int i = 0; i < numViews; i++ ) {
			if( extrinsic[i] == null )
				numViewsUnknown++;
			numObservations += observations.get(i).points.size;
		}
		
		indexFirstPoint = numViewsUnknown*6;
		numParameters = numViewsUnknown*6 + numPoints*3;
	}
	
	@Override
	public int getNumOfInputsN() {
		return numParameters;
	}

	@Override
	public int getNumOfOutputsM() {
		return numObservations*2;
	}

	@Override
	public void process(double[] input, double[] output) {
		this.output = output;
		int paramIndex = 0;
		countPointObs = 0;
		
		Arrays.fill(output,0);
		
		// first decode the transformation
		for( int i = 0; i < numViews; i++ ) {
			if( extrinsic[i] == null ) {
				double rodX = input[paramIndex++];
				double rodY = input[paramIndex++];
				double rodZ = input[paramIndex++];

				T.x = input[paramIndex++];
				T.y = input[paramIndex++];
				T.z = input[paramIndex++];

				rodrigues.setParamVector(rodX,rodY,rodZ);
				rodJacobian.process(rodX,rodY,rodZ);
			
				ConvertRotation3D_F64.rodriguesToMatrix(rodrigues,R);
				ViewPointObservations obs = observations.get(i);
				gradientViewMotionAndPoint(input, paramIndex-6, obs);
			} else {
				T.set( extrinsic[i].getT());
				R.set( extrinsic[i].getR());
				
				ViewPointObservations obs = observations.get(i);
				gradientViewPoint(input, obs);
			}
		}
	}

	/**
	 * Computes the partials for the pose and observed points at this view
	 */
	private void gradientViewMotionAndPoint(double[] input,
											int extrinsicParamStart,
											ViewPointObservations obs) 
	{
		for( int j = 0; j < obs.points.size; j++ , countPointObs++ ) {
			PointIndexObservation o = obs.points.get(j);
			int indexParamWorld = indexFirstPoint+o.pointIndex*3;
			
			// extract location of world point
			worldPt.x = input[indexParamWorld++];
			worldPt.y = input[indexParamWorld++];
			worldPt.z = input[indexParamWorld];

			// compute the index in output matrix for derivatives
			indexX = numParameters*countPointObs*2 + extrinsicParamStart;
			indexY = indexX + numParameters;

			// location of point in camera view
			GeometryMath_F64.mult(R, worldPt, cameraPt);
			cameraPt.x += T.x;
			cameraPt.y += T.y;
			cameraPt.z += T.z;

			// add gradient from rotation
			addRodriguesJacobian(rodJacobian.Rx,worldPt);
			addRodriguesJacobian(rodJacobian.Ry,worldPt);
			addRodriguesJacobian(rodJacobian.Rz,worldPt);

			// add gradient from translation
			addTranslationJacobian();
			
			// add gradient for the point in this view
			indexX = numParameters*countPointObs*2 + indexFirstPoint+o.pointIndex*3;
			indexY = indexX + numParameters;
			addWorldPointGradient(R);
		}
	}

	/**
	 * Computes the partials for observed points at this view
	 */
	private void gradientViewPoint(double[] input,
								   ViewPointObservations obs) 
	{
		for( int j = 0; j < obs.points.size; j++, countPointObs++ ) {
			PointIndexObservation o = obs.points.get(j);
			int indexParamWorld = indexFirstPoint+o.pointIndex*3;

			// extract location of world point
			worldPt.x = input[indexParamWorld];
			worldPt.y = input[indexParamWorld+1];
			worldPt.z = input[indexParamWorld+2];

			// location of point in camera view
			GeometryMath_F64.mult(R, worldPt, cameraPt);
			cameraPt.x += T.x;
			cameraPt.y += T.y;
			cameraPt.z += T.z;

			// add gradient for the point in this view
			indexX = numParameters*countPointObs*2 + indexFirstPoint+o.pointIndex*3;
			indexY = indexX + numParameters;
			addWorldPointGradient(R);
		}
	}

	/**
	 * Adds to the Jacobian matrix using the derivative from a Rodrigues parameter.
	 *
	 * deriv [x,y] = -dot(z)/(z^2)*(R*X+T) + (1/z)*dot(R)*X
	 *
	 * where R is rotation matrix, T is translation, z = z-coordinate of point in camera frame
	 *
	 * @param Rj Jacobian for Rodrigues
	 * @param worldPt Location of point in world coordinates
	 */
	private void addRodriguesJacobian( DenseMatrix64F Rj , Point3D_F64 worldPt )
	{
		// (1/z)*dot(R)*X
		double Rx = (Rj.data[0]*worldPt.x + Rj.data[1]*worldPt.y + Rj.data[2]*worldPt.z)/cameraPt.z;
		double Ry = (Rj.data[3]*worldPt.x + Rj.data[4]*worldPt.y + Rj.data[5]*worldPt.z)/cameraPt.z;

		// dot(z)/(z^2)
		double zDot_div_z2 = (Rj.data[6]*worldPt.x + Rj.data[7]*worldPt.y + Rj.data[8]*worldPt.z)/
				(cameraPt.z*cameraPt.z);

		output[indexX++] = -zDot_div_z2*cameraPt.x + Rx;
		output[indexY++] = -zDot_div_z2*cameraPt.y + Ry;
	}

	/**
	 * Derivative for translation element
	 *
	 * deriv [x,y] = -dot(z)*T/(z^2) + dot(T)/z
	 *
	 * where T is translation, z = z-coordinate of point in camera frame
	 */
	private void addTranslationJacobian()
	{
		double divZ = 1.0/cameraPt.z;
		double divZ2 = 1.0/(cameraPt.z*cameraPt.z);

		// partial T.x
		output[indexX++] = divZ;
		output[indexY++] = 0;
		// partial T.y
		output[indexX++] = 0;
		output[indexY++] = divZ;
		// partial T.z
		output[indexX++] = -cameraPt.x*divZ2;
		output[indexY++] = -cameraPt.y*divZ2;
	}

	/**
	 * Gradient of the feature's 3D location
	 *
	 * deriv [x,y] = -dot(z)*(R*X+T)/(z^2) + R*dot(X)/z
	 *
	 * @param R rotation matrix
	 */
	private void addWorldPointGradient( DenseMatrix64F R ) {
		double divZ2 = 1.0/(cameraPt.z*cameraPt.z);

		// partial P.x
		output[indexX++] = -R.data[6]*divZ2*cameraPt.x + R.data[0]/cameraPt.z;
		output[indexY++] = -R.data[6]*divZ2*cameraPt.y + R.data[3]/cameraPt.z;
		// partial P.y
		output[indexX++] = -R.data[7]*divZ2*cameraPt.x + R.data[1]/cameraPt.z;
		output[indexY++] = -R.data[7]*divZ2*cameraPt.y + R.data[4]/cameraPt.z;
		// partial P.z
		output[indexX++] = -R.data[8]*divZ2*cameraPt.x + R.data[2]/cameraPt.z;
		output[indexY++] = -R.data[8]*divZ2*cameraPt.y + R.data[5]/cameraPt.z;
	}
}
