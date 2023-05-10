/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.mesh;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;

/**
 * Contains the math for adjusting a camera using first person shooting inspired keyboard and mouse controls.
 *
 * @author Peter Abeles
 */
public class FirstPersonShooterCamera {
	/** Intrinsics camera model. Used to interpret mouse motions */
	@Getter CameraPinhole camera = new CameraPinhole();

	// Storage for the output transform
	final Se3_F64 worldToView = new Se3_F64();

	// Workspace when applying rotation from mouse motion
	final Se3_F64 mouseMotion = new Se3_F64();

	/** How much it will move in a single translation step */
	public double motionUnit;

	// Storage for normalized image coordinates
	Point2D_F64 norm1 = new Point2D_F64();
	Point2D_F64 norm2 = new Point2D_F64();

	// Workspace
	final Se3_F64 tmp = new Se3_F64();

	public void resetView() {
		worldToView.reset();
	}

	/**
	 * Translation from a keyboard press. Directions are assumed to be -1,0,1. Scale is used to increase
	 * the amount of motion or decrease it.
	 *
	 * @param dx Motion along the x-axis. Expected value of -1, 0, 1
	 * @param dy Motion along the y-axis. Expected value of -1, 0, 1
	 * @param dz Motion along the z-axis. Expected value of -1, 0, 1
	 * @param scale Scale factor applied to nominal motion
	 */
	public void translateKey( int dx, int dy, int dz, double scale ) {
		worldToView.T.x += dx*motionUnit*scale;
		worldToView.T.y += dy*motionUnit*scale;
		worldToView.T.z += dz*motionUnit*scale;
	}

	/**
	 * Performs a pan tilt motion given a mouse that has been dragged between the two provided points
	 */
	public void mouseDragPanTilt( double x0, double y0, double x1, double y1 ) {
		if (!isCameraConfigured())
			return;

		// convert into normalize image coordinates
		PerspectiveOps.convertPixelToNorm(camera, x0, y0, norm1);
		PerspectiveOps.convertPixelToNorm(camera, x1, y1, norm2);

		// Figure out the actual angle the user rotated
		double rotX = UtilAngle.minus(Math.atan(norm1.x), Math.atan(norm2.x));
		double rotY = UtilAngle.minus(Math.atan(norm1.y), Math.atan(norm2.y));

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotY, -rotX, 0.0, mouseMotion.R);
		worldToView.concat(mouseMotion, tmp);
		worldToView.setTo(tmp);
	}

	/**
	 * Performs a roll motion given a mouse that has been dragged between the two provided points.
	 */
	public void mouseDragRoll( double x0, double y0, double x1, double y1 ) {
		if (!isCameraConfigured())
			return;

		// convert into normalize image coordinates
		PerspectiveOps.convertPixelToNorm(camera, x0, y0, norm1);
		PerspectiveOps.convertPixelToNorm(camera, x1, y1, norm2);

		double angle0 = Math.atan2(norm1.y, norm1.x);
		double angle1 = Math.atan2(norm2.y, norm2.x);

		double rotZ = angle1 - angle0;

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.0, 0.0, rotZ, mouseMotion.R);
		worldToView.concat(mouseMotion, tmp);
		worldToView.setTo(tmp);
	}

	private boolean isCameraConfigured() {
		return camera.fx != 0.0 && camera.fy != 0.0;
	}
}
