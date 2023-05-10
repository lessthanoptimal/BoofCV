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
import georegression.geometry.GeometryMath_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

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

	DMatrixRMaj localRotation = new DMatrixRMaj(3, 3);
	DMatrixRMaj rotationAroundTarget = new DMatrixRMaj(3, 3);
	DMatrixRMaj tmp = new DMatrixRMaj(3, 3);

	// Translation applied after the orbit has been done
	Vector3D_F64 translateWorld = new Vector3D_F64();

	// Point it's orbiting around
	Point3D_F64 targetPoint = new Point3D_F64();

	// Adjustment applied to distance from target point in final transform
	double radiusScale = 1.0;

	Point3D_F64 cameraLoc = new Point3D_F64();

	// Storage for normalized image coordinates
	Point2D_F64 norm1 = new Point2D_F64();
	Point2D_F64 norm2 = new Point2D_F64();

	public OrbitAroundPoint() {
		resetView();
	}

	public void resetView() {
		radiusScale = 1.0;
		translateWorld.zero();
		CommonOps_DDRM.setIdentity(rotationAroundTarget);
	}

	public void updateTransform() {
		// Compute location of camera principle point with no rotation in target point reference frame
		cameraLoc.x = -targetPoint.x*radiusScale;
		cameraLoc.y = -targetPoint.y*radiusScale;
		cameraLoc.z = -targetPoint.z*radiusScale;

		// Apply rotation
		GeometryMath_F64.mult(rotationAroundTarget, cameraLoc, cameraLoc);

		// Compute the full transform
		worldToView.T.setTo(
				cameraLoc.x + targetPoint.x + translateWorld.x,
				cameraLoc.y + targetPoint.y + translateWorld.y,
				cameraLoc.z + targetPoint.z + translateWorld.z);
		worldToView.R.setTo(rotationAroundTarget);
	}

	public void mouseWheel( double ticks, double scale ) {
		radiusScale = Math.max(0.005, radiusScale*(1.0 + 0.02*ticks*scale));
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

		// Set the local rotation
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, -rotY, rotX, 0, localRotation);

		// Update the global rotation
		CommonOps_DDRM.mult(localRotation, rotationAroundTarget, tmp);
		rotationAroundTarget.setTo(tmp);
	}

	/**
	 * Uses mouse drag motion to translate the view
	 */
	public void mouseDragTranslate( double x0, double y0, double x1, double y1 ) {
		// do nothing if the camera isn't configured yet
		if (camera.fx == 0.0 || camera.fy == 0.0)
			return;

		// convert into normalize image coordinates
		PerspectiveOps.convertPixelToNorm(camera, x0, y0, norm1);
		PerspectiveOps.convertPixelToNorm(camera, x1, y1, norm2);

		// Figure out the distance along the projection at the plane at the distance of the target point
		double z = targetPoint.plus(translateWorld).norm()*radiusScale;

		translateWorld.x += (norm2.x - norm1.x)*z;
		translateWorld.y += (norm2.y - norm1.y)*z;
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
		double z = targetPoint.plus(translateWorld).norm()*radiusScale;
		translateWorld.z += (norm2.y - norm1.y)*z;

		// Perform roll around the z-axis
		double rotX = UtilAngle.minus(Math.atan(norm1.x), Math.atan(norm2.x));
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, 0, -rotX, localRotation);
		CommonOps_DDRM.mult(localRotation, rotationAroundTarget, tmp);
		rotationAroundTarget.setTo(tmp);

	}
}
