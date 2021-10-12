/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import georegression.struct.so.Quaternion_F64;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.ejml.FancyPrint;

import java.io.Serializable;

/**
 * <p>
 * Calibration parameters for a stereo camera pair. Includes intrinsic and extrinsic. The baseline between
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
@Data
public class StereoParameters implements Serializable {

	// serialization version
	public static final long serialVersionUID = 1L;

	/** intrinsic camera parameters of left camera */
	public CameraPinholeBrown left = new CameraPinholeBrown();
	/** intrinsic camera parameters of right camera */
	public CameraPinholeBrown right = new CameraPinholeBrown();
	/** transform from left camera to right camera */
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	public Se3_F64 right_to_left = new Se3_F64();

	public StereoParameters( StereoParameters param ) {
		this(param.left, param.right, param.getRightToLeft());
	}

	public StereoParameters( CameraPinholeBrown left,
							 CameraPinholeBrown right,
							 Se3_F64 right_to_left ) {
		this.left.setTo(left);
		this.right.setTo(right);
		this.right_to_left.setTo(right_to_left);
	}

	public StereoParameters() {}

	public Se3_F64 getRightToLeft() {
		return right_to_left;
	}

	public void setRightToLeft( Se3_F64 right_to_left ) {
		this.right_to_left = right_to_left;
	}

	/**
	 * Returns the distance between the optical center of each camera
	 */
	public double getBaseline() {
		return right_to_left.getT().norm();
	}

	/**
	 * Checks to see if the parameters define a rectified stereo pair
	 *
	 * @param tol Numeric tolerance. Try 1e-7
	 * @return if true then it's rectified
	 */
	public boolean isRectified( double tol ) {
		if (!left.isDistorted() && !right.isDistorted()) {
			double angle = ConvertRotation3D_F64.matrixToRodrigues(right_to_left.R, null).theta;
			if (Math.abs(angle) < tol) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Makes 'this' identical to 'src'.
	 *
	 * @param src The set of parameters that is to be copied.
	 */
	public void setTo( StereoParameters src ) {
		this.left.setTo(src.left);
		this.right.setTo(src.right);
		this.right_to_left.setTo(src.right_to_left);
	}

	@Override public String toString() {
		return "StereoParameters{" +
				"left=" + left +
				", right=" + right +
				", right_to_left=" + right_to_left +
				'}';
	}

	public String toStringQuaternion() {
		FancyPrint fp = new FancyPrint();
		Quaternion_F64 quat = ConvertRotation3D_F64.matrixToQuaternion(right_to_left.R, null);
		return "StereoParameters{" +
				"left=" + left +
				", right=" + right +
				", right_to_left={ p={" + fp.p(right_to_left.T.x) + " " + fp.p(right_to_left.T.y) +
				" " + fp.p(right_to_left.T.z) + "}, q={ " +
				fp.p(quat.x) + " " + fp.p(quat.y) + " " + fp.p(quat.z) + " " + fp.p(quat.w) + "} }";
	}

	public void print() {
		double[] euler = ConvertRotation3D_F64.matrixToEuler(right_to_left.getR(), EulerType.XYZ, (double[])null);
		Vector3D_F64 t = right_to_left.getT();
		System.out.println();
		System.out.println("Left Camera");
		left.print();
		System.out.println();
		System.out.println("Right Camera");
		right.print();
		System.out.println("Right to Left");
		System.out.printf("  Euler XYZ   [ %8.3f , %8.3f , %8.3f ]\n", euler[0], euler[1], euler[2]);
		System.out.printf("  Translation [ %8.3f , %8.3f , %8.3f ]\n", t.x, t.y, t.z);
	}
}
