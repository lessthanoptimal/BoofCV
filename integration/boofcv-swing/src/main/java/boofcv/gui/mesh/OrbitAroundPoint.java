/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;

/**
 * Contains the mathematics for controlling a camera by orbiting around a point in 3D space
 *
 * @author Peter Abeles
 */
public class OrbitAroundPoint {
	/** Intrinsics camera model. Used to interpret mouse motions */
	@Getter CameraPinhole camera = new CameraPinhole();

	/** Transform from world to camera view reference frames */
	Se3_F64 worldToView = new Se3_F64();

	// Point it's orbiting around in world coordinates
	private Point3D_F64 targetWorld = new Point3D_F64();

	// Storage for normalized image coordinates
	Point2D_F64 norm1 = new Point2D_F64();
	Point2D_F64 norm2 = new Point2D_F64();

	Point3D_F64 targetInView = new Point3D_F64();
	Point3D_F64 targetInOld = new Point3D_F64();
	Se3_F64 viewToPoint = new Se3_F64();
	Se3_F64 viewToPoint2 = new Se3_F64();
	Se3_F64 worldToPoint = new Se3_F64();
	Se3_F64 pointToRot = new Se3_F64();
	Se3_F64 oldToNew = new Se3_F64();

	public OrbitAroundPoint() {
		resetView();
	}

	public void setTarget( double x, double y, double z ) {
		targetWorld.setTo(x, y, z);

		updateAfterExternalChange();
	}

	public void resetView() {
		worldToView.reset();

		// See if the principle point and the target are on top of each other
		updateAfterExternalChange();
	}

	/**
	 * After an external change to the transform or target, make sure it's pointed at the target and a reasonable
	 * distance.
	 */
	private void updateAfterExternalChange() {
		// See if the principle point and the target are on top of each other
		if (Math.abs(worldToView.T.distance(targetWorld)) < 1e-16) {
			// back the camera off a little bit
			worldToView.T.z += 0.1;
		}

		pointAtTarget();
	}

	/**
	 * Points the camera at the target point
	 */
	private void pointAtTarget() {
		oldToNew.reset();

		worldToView.transform(targetWorld, targetInView);
		PerspectiveOps.pointAt(targetInView.x, targetInView.y, targetInView.z, oldToNew.R);

		Se3_F64 tmp = pointToRot;
		worldToView.concatInvert(oldToNew, tmp);
		worldToView.setTo(tmp);
	}

	public void mouseWheel( double ticks, double scale ) {
		worldToView.transform(targetWorld, targetInView);

		// How much it will move towards or away from the target
		worldToView.T.z += targetInView.norm()*0.02*ticks*scale;

		// Because it's moving towards the point it won't need to adjust its angle
	}

	public void mouseDragRotate( double x0, double y0, double x1, double y1 ) {
		// do nothing if the camera isn't configured yet
		if (camera.fx == 0.0 || camera.fy == 0.0)
			return;

		// convert into normalize image coordinates
		PerspectiveOps.convertPixelToNorm(camera, x0, y0, norm1);
		PerspectiveOps.convertPixelToNorm(camera, x1, y1, norm2);

		// Figure out the actual angle the user rotated
		double rotX = UtilAngle.minus(Math.atan(norm1.x), Math.atan(norm2.x));
		double rotY = UtilAngle.minus(Math.atan(norm1.y), Math.atan(norm2.y));

		applyLocalEuler(rotX, -rotY, 0.0);
	}

	/**
	 * Applies a X Y Z Euler rotation in the point's reference frame so that it will appear to be rotating
	 * around that point
	 */
	private void applyLocalEuler( double rotX, double rotY, double rotZ ) {
		pointToRot.reset();

		worldToView.transform(targetWorld, targetInOld);

		viewToPoint.T.setTo(-targetInOld.x, -targetInOld.y, -targetInOld.z);

		worldToView.concat(viewToPoint, worldToPoint);

		// Set the local rotation
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotY, rotX, rotZ, pointToRot.R);

		viewToPoint.concat(pointToRot, viewToPoint2);
		worldToPoint.concatInvert(viewToPoint2, worldToView);

		pointAtTarget();
	}

	/**
	 * Drag will move in the +z direction and roll the camera
	 */
	public void mouseDragZoomRoll( double x0, double y0, double x1, double y1 ) {
		// do nothing if the camera isn't configured yet
		if (camera.fx == 0.0 || camera.fy == 0.0)
			return;

		// convert into normalize image coordinates
		PerspectiveOps.convertPixelToNorm(camera, x0, y0, norm1);
		PerspectiveOps.convertPixelToNorm(camera, x1, y1, norm2);

		// Zoom in and out using the mouse
		worldToView.transform(targetWorld, targetInView);
		worldToView.T.z += (norm2.y - norm1.y)*targetInView.norm();

		// Roll the camera
		double rotX = UtilAngle.minus(Math.atan(norm1.x), Math.atan(norm2.x));
		applyLocalEuler(0, 0, rotX);
	}
}
