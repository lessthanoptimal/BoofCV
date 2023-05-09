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

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.se.Se3_F64;

import javax.swing.*;

/**
 * Interface for controlling a camera which is viewing a 3D scene.
 *
 * @author Peter Abeles
 */
public interface Swing3dCameraControl {
	/**
	 * Attaches controls to the component
	 */
	void attachControls( JComponent parent );

	/**
	 * Detaches controls to the component
	 */
	void detachControls( JComponent parent );

	/**
	 * Selects a reasonable initial location to view the specified mesh
	 */
	void selectInitialParameters( VertexMesh mesh );

	/**
	 * Resets the view back to it's initial state
	 */
	void reset();

	/**
	 * Specifies the camera used to render the scene
	 */
	void setCamera( CameraPinhole camera );

	/**
	 * Transform from world to camera
	 */
	Se3_F64 getWorldToCamera();

	/**
	 * This is called whenever the settings have changed and the rendering should be updated.
	 */
	void setChangeHandler( Runnable handler );
}
