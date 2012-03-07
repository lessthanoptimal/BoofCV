/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.geo.simulation.impl;

import boofcv.geo.simulation.CameraControl;
import boofcv.geo.simulation.CameraModel;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;

/**
 * Moves the camera forward at a constant speed.
 *
 * @author Peter Abeles
 */
public class ForwardCameraMotion implements CameraControl {

	CameraModel camera;
	double speed;
	
	Point3D_F64 direction = new Point3D_F64();

	public ForwardCameraMotion(double speed) {
		this.speed = speed;
	}

	@Override
	public void setCamera(CameraModel camera) {
		this.camera = camera;
	}

	@Override
	public void update() {
		// compute the motion's direction
		direction.set(0,0,1);
		GeometryMath_F64.mult(camera.getWorldToCamera().getR(),direction,direction);
		
		// move the camera
		Vector3D_F64 T = camera.getWorldToCamera().getT();
		T.x += direction.x*speed;
		T.y += direction.y*speed;
		T.z += direction.z*speed;
	}
}
