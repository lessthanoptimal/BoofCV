/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.visualize;

import boofcv.struct.Point3dRgbI;
import georegression.struct.se.Se3_F64;

import javax.swing.*;
import java.util.List;

public interface PointCloudViewer {
	void setCloudXyzRgbI32( List<Point3dRgbI> cloud );

	/**
	 * Specifies the camera's FOV in radians
	 * @param radians FOV size
	 */
	void setCameraFov( double radians );

	/**
	 * Changes the camera location
	 * @param cameraToWorld transform from camera to world coordinates
	 */
	void setCameraToWorld(Se3_F64 cameraToWorld );

	JComponent getComponent();
}
