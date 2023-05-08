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
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Camera controls for {@link MeshViewerPanel} where it rotates around a central control point.
 * Dragging the mouse will rotate the camera around this point. Using the mouse wheel will zoom in and out.
 * This has been designed so that multiple threads can access it at the same time
 *
 * @author Peter Abeles
 */
public class MouseRotateAroundPoint extends MouseAdapter {
	public int maxSamplePoints = 1000;

	/**
	 * Called every time the camera has changed
	 */
	public Runnable handleCameraChanged = () -> {};

	// Math for computing the orbit. All read/write must be synchronized for thread safety
	final OrbitAroundPoint orbit = new OrbitAroundPoint();

	int prevX, prevY;

	public void attachControls( JComponent parent ) {
		parent.addMouseListener(this);
		parent.addMouseMotionListener(this);
		parent.addMouseWheelListener(this);
	}

	public void detachControls( JComponent parent ) {
		parent.removeMouseListener(this);
		parent.removeMouseMotionListener(this);
		parent.removeMouseWheelListener(this);
	}

	public void reset() {
		orbit.resetView();
	}

	/**
	 * By default sets the focal point that it rotates around to the medium point
	 */
	public void selectTargetPoint( VertexMesh mesh ) {
		// sub-sample points to keep compute at a reasonable speed.
		final int N = Math.min(maxSamplePoints, mesh.vertexes.size());

		var sampled = new DogArray<>(Point3D_F64::new);
		sampled.reserve(N);

		// evenly sub sample points in the mesh
		for (int i = 0; i < N; i++) {
			int vertIndex = i*mesh.vertexes.size()/N;
			mesh.vertexes.getCopy(vertIndex, sampled.grow());
		}

		// select the medium point as the focal point
		var values = new DogArray_F64();
		values.resize(sampled.size);

		synchronized (orbit) {
			for (int axis = 0; axis < 3; axis++) {
				int _axis = axis;
				sampled.forIdx(( idx, v ) -> values.set(idx, v.getIdx(_axis)));
				values.sort();
				orbit.targetPoint.setIdx(axis, values.getFraction(0.5));
			}
		}
	}

	public void setCamera( CameraPinhole camera ) {
		synchronized (orbit) {
			orbit.getCamera().setTo(camera);
		}
	}

	public Se3_F64 getWorldToCamera() {
		synchronized (orbit) {
			orbit.updateTransform();
			return orbit.worldToView.copy();
		}
	}

	@Override public void mousePressed( MouseEvent e ) {
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override public void mouseWheelMoved( MouseWheelEvent e ) {
		synchronized (orbit) {
			orbit.mouseWheel(e.getPreciseWheelRotation(), 1.0);
		}
		handleCameraChanged.run();
	}

	@Override public void mouseDragged( MouseEvent e ) {
		synchronized (orbit) {
			if (e.isShiftDown() || SwingUtilities.isMiddleMouseButton(e))
				orbit.mouseDragTranslate(prevX, prevY, e.getX(), e.getY(), true);
			else if (e.isControlDown() || SwingUtilities.isRightMouseButton(e))
				orbit.mouseDragTranslate(prevX, prevY, e.getX(), e.getY(), false);
			else
				orbit.mouseDragRotate(prevX, prevY, e.getX(), e.getY());
		}
		prevX = e.getX();
		prevY = e.getY();
		handleCameraChanged.run();
	}
}
