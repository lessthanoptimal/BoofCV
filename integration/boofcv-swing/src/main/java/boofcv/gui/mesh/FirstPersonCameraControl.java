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
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controls the camera using similar commands as a first person shooting. WASD keys will translate the camera and
 * dragging the mouse will cause a rotation.
 *
 * @author Peter Abeles
 */
public class FirstPersonCameraControl extends MouseAdapter implements Swing3dCameraControl {
	// Used when initializing.
	public int maxSamplePoints = 1000;

	// Math for performing the first person camera
	@Getter final FirstPersonShooterCamera fps = new FirstPersonShooterCamera();

	/** Called whenever the camera has been modified */
	@Setter Runnable handleCameraChanged = () -> {};

	// Previous mouse location
	int prevX, prevY;

	// Adjusts how much it translate
	double translateScale = 1.0;

	private final Se3_F64 worldToCamera = new Se3_F64();

	// Processes key presses and keeps tracks of which keys are being held down
	private final KeyPressHandler keyHandler = new KeyPressHandler();

	// Periodic task the causes a key being held down to cause continuous motions
	@Nullable ScheduledExecutorService pressedTask = null;
	final Object lockPressed = new Object();

	/**
	 * Enable and disable the key pressed task. This is to prevent a key being held down from triggering
	 * actions in the display when it's not the active window.
	 */
	private final FocusListener focusListener = new FocusListener() {
		@Override public void focusGained( FocusEvent e ) {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyHandler);
			synchronized (lockPressed) {
				pressedTask = Executors.newScheduledThreadPool(1);
				pressedTask.scheduleAtFixedRate(() -> keyHandler.applyKeyAction(), 100, 30, TimeUnit.MILLISECONDS);
			}
		}

		@Override public void focusLost( FocusEvent e ) {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyHandler);
			stopPressedTask();
		}
	};

	@Override public void attachControls( JComponent parent ) {
		keyHandler.reset();
		parent.addMouseListener(this);
		parent.addMouseMotionListener(this);
		parent.addMouseWheelListener(this);
		parent.addFocusListener(focusListener);
	}

	@Override public void detachControls( JComponent parent ) {
		parent.removeMouseListener(this);
		parent.removeMouseMotionListener(this);
		parent.removeMouseWheelListener(this);
		parent.removeFocusListener(focusListener);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyHandler);
		stopPressedTask();
	}

	private void stopPressedTask() {
		synchronized (lockPressed) {
			if (pressedTask != null) {
				pressedTask.shutdown();
				pressedTask = null;
			}
		}
	}

	/**
	 * There is no real good universal way to select how much it should translate. In this case it will select
	 * a step such that it will take 150 ticks to get to the median point
	 */
	@Override public void selectInitialParameters( VertexMesh mesh ) {
		// sub-sample points to keep compute at a reasonable speed.
		final int N = Math.min(maxSamplePoints, mesh.vertexes.size());

		// Just set it to one. There is no cloud to view anyways
		if (N == 0) {
			fps.motionUnit = 1.0;
			return;
		}

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

		var median = new Point3D_F64();
		for (int axis = 0; axis < 3; axis++) {
			int _axis = axis;
			sampled.forIdx(( idx, v ) -> values.set(idx, v.getIdx(_axis)));
			values.sort();
			median.setIdx(axis, values.getFraction(0.5));
		}

		// just an arbitrary unit
		fps.motionUnit = median.norm()/150.0;
	}

	@Override public void reset() {
		fps.resetView();
	}

	@Override public void setCamera( CameraPinhole intrinsics ) {
		fps.getCamera().setTo(intrinsics);
	}

	@Override public Se3_F64 getWorldToCamera() {
		synchronized (fps) {
			worldToCamera.setTo(fps.worldToView);
		}
		return worldToCamera;
	}

	@Override public void setChangeHandler( Runnable handler ) {
		this.handleCameraChanged = handler;
	}

	@Override public String getHelpText() {
		return """
				First Person Shooter Controls
				a   left
				d   right
				w   forward
				s   backwards
				q   Own
				e   Up
				Mouse for pan and tilt
				Mouse right for roll
				""";
	}

	@Override public void mousePressed( MouseEvent e ) {
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override public void mouseDragged( MouseEvent e ) {
		synchronized (fps) {
			if (e.isShiftDown() || SwingUtilities.isRightMouseButton(e))
				fps.mouseDragRoll(prevX, prevY, e.getX(), e.getY());
			else
				fps.mouseDragPanTilt(prevX, prevY, e.getX(), e.getY());
		}
		prevX = e.getX();
		prevY = e.getY();
		handleCameraChanged.run();
	}

	private class KeyPressHandler implements KeyEventDispatcher {
		boolean downW, downA, downS, downD, downQ, downE;

		public void reset() {
			downW = downA = downS = downD = downQ = downE = false;
		}

		/**
		 * Marks WASD keys as up or down.
		 */
		@Override public boolean dispatchKeyEvent( KeyEvent e ) {
			// Determine if a key was pressed or released
			boolean pressed;
			switch (e.getID()) {
				case KeyEvent.KEY_PRESSED -> pressed = true;
				case KeyEvent.KEY_RELEASED -> pressed = false;
				default -> {
					return false;
				}
			}

			// Apply the change to the correct key
			switch (e.getKeyCode()) {
				case KeyEvent.VK_W -> downW = pressed;
				case KeyEvent.VK_A -> downA = pressed;
				case KeyEvent.VK_S -> downS = pressed;
				case KeyEvent.VK_D -> downD = pressed;
				case KeyEvent.VK_Q -> downQ = pressed;
				case KeyEvent.VK_E -> downE = pressed;
				default -> {
					return false;
				}
			}

			// Immediately perform the requested action
			applyKeyAction();

			return false;
		}

		/**
		 * See which keys are held down and apply the action that they stimulate.
		 */
		public void applyKeyAction() {
			int dx = 0, dy = 0, dz = 0;

			if (downW) dz = -1;
			if (downS) dz = 1;
			if (downA) dx = 1;
			if (downD) dx = -1;
			if (downQ) dy = -1;
			if (downE) dy = 1;

			// See if no translation should occur. In that case, do nothing and don't render again.
			if (dx == 0 && dy == 0 && dz == 0)
				return;

			// Update the camera location then tell the listener that the camera has changed
			fps.translateKey(dx, dy, dz, translateScale);
			handleCameraChanged.run();
		}
	}
}
