/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.calib;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.io.Serializable;

/**
 * <p>
 * Calibration parameters for a stereo camera pair.  Includes intrinsic and extrinsic. The baseline between
 * the two cameras is specified as a rigid body {@link Se3_F64} transform from the right to left camera.
 * </p>
 *
 * <p>
 * NOTE: When generated during camera calibration, the distance units found in 'rightToLeft' will be in the units that
 * the calibration target size was specified in.
 * </p>
 *
 * @author Peter Abeles
 */
public class StereoParameters implements Serializable {

	// serialization version
	public static final long serialVersionUID = 1L;

	/** intrinsic camera parameters of left camera */
	public CameraPinholeBrown left;
	/** intrinsic camera parameters of right camera */
	public CameraPinholeBrown right;
	/** transform from left camera to right camera */
	public Se3_F64 right_to_left;

	public StereoParameters(StereoParameters param ) {
		this(param.left,param.right,param.getRightToLeft());
	}

	public StereoParameters(CameraPinholeBrown left,
							CameraPinholeBrown right,
							Se3_F64 right_to_left ) {
		this.left = new CameraPinholeBrown(left);
		this.right_to_left = right_to_left.copy();
		this.right = new CameraPinholeBrown(right);
	}

	public StereoParameters() {
	}

	public CameraPinholeBrown getLeft() {
		return left;
	}

	public void setLeft(CameraPinholeBrown left) {
		this.left = left;
	}

	public Se3_F64 getRightToLeft() {
		return right_to_left;
	}

	public void setRightToLeft(Se3_F64 right_to_left ) {
		this.right_to_left = right_to_left;
	}

	public CameraPinholeBrown getRight() {
		return right;
	}

	public void setRight(CameraPinholeBrown right) {
		this.right = right;
	}

	/**
	 * Returns the distance between the optical center of each camera
	 */
	public double getBaseline() {
		return right_to_left.getT().norm();
	}

	/**
	 * Checks to see if the parameters define a rectified stereo pair
	 * @param tol Numeric tolerance. Try 1e-7
	 * @return if true then it's rectified
	 */
	public boolean isRectified( double tol ) {
		if( !left.isDistorted() && !right.isDistorted() ) {
			double angle = ConvertRotation3D_F64.matrixToRodrigues(right_to_left.R,null).theta;
			if( Math.abs(angle) < tol ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Makes 'this' identical to 'src'.
	 * @param src The set of parameters that is to be copied.
	 */
	public void set( StereoParameters src ) {
		if( this.left == null )
			this.left = new CameraPinholeBrown(src.left);
		else
			this.left.set(src.left);
		if( this.right == null )
			this.right = new CameraPinholeBrown(src.right);
		else
			this.right.set(src.right);

		if( this.right_to_left == null )
			this.right_to_left = src.right_to_left.copy();
		else
			this.right_to_left.setTo(src.right_to_left);
	}

	public void print() {
		double[] euler = ConvertRotation3D_F64.matrixToEuler(right_to_left.getR(), EulerType.XYZ,(double[])null);
		Vector3D_F64 t = right_to_left.getT();
		System.out.println();
		System.out.println("Left Camera");
		left.print();
		System.out.println();
		System.out.println("Right Camera");
		right.print();
		System.out.println("Right to Left");
		System.out.printf("  Euler XYZ   [ %8.3f , %8.3f , %8.3f ]\n",euler[0],euler[1],euler[2]);
		System.out.printf("  Translation [ %8.3f , %8.3f , %8.3f ]\n",t.x,t.y,t.z);
	}
}
