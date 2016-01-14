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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.RodriguesRotationJacobian;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.List;

/**
 * Computes the Jacobian of the error function in {@link boofcv.alg.geo.pose.PnPResidualReprojection}.  For a calibrated
 * camera given observations in normalized image coordinates.  The rotation matrix is assumed to be
 * parameterized using {@link georegression.struct.so.Rodrigues_F64} coordinates.
 *
 * @author Peter Abeles
 */
// TODO Make PnPJacobianRodrigues and this class share a common parent for common functions?
public class PnPStereoJacobianRodrigues implements FunctionNtoMxN {

	// transformation from world to left camera frame
	private Se3_F64 worldToLeft = new Se3_F64();
	// known transform from left to right camera frame
	private Se3_F64 leftToRight;

	private List<Stereo2D3D> observations;

	// used to compute the Jacobian from Rodrigues coordinates
	private RodriguesRotationJacobian rodJacobian = new RodriguesRotationJacobian();

	// local variable which stores the predicted location of the feature in the camera frame
	private Rodrigues_F64 rodrigues = new Rodrigues_F64();

	// 3D location of point in camera frame
	private Point3D_F64 cameraPt = new Point3D_F64();

	// output array
	private double[] output;
	// index for x component
	private int indexX;
	private int indexY;

	// storage for intermediate results
	private DenseMatrix64F rotR = new DenseMatrix64F(3,3);

	public void setObservations(List<Stereo2D3D> observations) {
		this.observations = observations;
	}

	public void setLeftToRight(Se3_F64 leftToRight) {
		this.leftToRight = leftToRight;
	}

	@Override
	public int getNumOfInputsN() {
		return 6;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.size()*4;
	}

	@Override
	public void process(double[] input, double[] output) {

		this.output = output;

		// initialize data structures
		rodrigues.setParamVector(input[0],input[1],input[2]);
		rodJacobian.process(input[0], input[1], input[2]);

		worldToLeft.T.x = input[3];
		worldToLeft.T.y = input[4];
		worldToLeft.T.z = input[5];

		ConvertRotation3D_F64.rodriguesToMatrix(rodrigues, worldToLeft.getR());

		// compute the gradient for each observation
		for( int i = 0; i < observations.size(); i++ ) {
			Stereo2D3D o = observations.get(i);


			// --------- Left camera observations
			SePointOps_F64.transform(worldToLeft,o.location, cameraPt);

			indexX = 4*6*i;
			indexY = indexX + 6;

			// add gradient from rotation
			addRodriguesJacobian(rodJacobian.Rx,o.location,cameraPt);
			addRodriguesJacobian(rodJacobian.Ry,o.location,cameraPt);
			addRodriguesJacobian(rodJacobian.Rz,o.location,cameraPt);

			// add gradient from translation
			addTranslationJacobian(cameraPt);

			// --------- Right camera observations
			SePointOps_F64.transform(leftToRight,cameraPt, cameraPt);

			indexX = indexY;
			indexY = indexY + 6;

			CommonOps.mult(leftToRight.getR(), rodJacobian.Rx, rotR);
			addRodriguesJacobian(rotR,o.location,cameraPt);
			CommonOps.mult(leftToRight.getR(), rodJacobian.Ry, rotR);
			addRodriguesJacobian(rotR,o.location,cameraPt);
			CommonOps.mult(leftToRight.getR(), rodJacobian.Rz, rotR);
			addRodriguesJacobian(rotR,o.location,cameraPt);

			addTranslationJacobian(leftToRight.getR(),cameraPt);
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
	 * @param cameraPt Location of point in camera coordinates
	 */
	private void addRodriguesJacobian( DenseMatrix64F Rj , Point3D_F64 worldPt , Point3D_F64 cameraPt )
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
	private void addTranslationJacobian( Point3D_F64 cameraPt )
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
	 * The translation vector is now multiplied by 3x3 matrix R.  The components of T are no longer decoupled.
	 *
	 * deriv [x,y] = R*dot(T)/z - (dot(z)/(z^2))(R*T)
	 *
	 * @param R
	 * @param cameraPt
	 */
	private void addTranslationJacobian( DenseMatrix64F R ,
										 Point3D_F64 cameraPt )
	{
		double z = cameraPt.z;
		double z2 = z*z;

		// partial T.x
		output[indexX++] = R.get(0,0)/cameraPt.z - R.get(2,0)/z2*cameraPt.x;
		output[indexY++] = R.get(1,0)/cameraPt.z - R.get(2,0)/z2*cameraPt.y;
		// partial T.y
		output[indexX++] = R.get(0,1)/cameraPt.z - R.get(2,1)/z2*cameraPt.x;
		output[indexY++] = R.get(1,1)/cameraPt.z - R.get(2,1)/z2*cameraPt.y;
		// partial T.z
		output[indexX++] = R.get(0,2)/cameraPt.z - R.get(2,2)/z2*cameraPt.x;
		output[indexY++] = R.get(1,2)/cameraPt.z - R.get(2,2)/z2*cameraPt.y;
	}
}
