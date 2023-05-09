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
import boofcv.gui.image.SaveImageOnClick;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import boofcv.visualize.RenderMesh;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Displays a rendered mesh in a JPanel. Has controls to change the view. Rendering is done in a separate thread.
 *
 * @author Peter Abeles
 */
public class MeshViewerPanel extends JPanel implements VerbosePrint, KeyEventDispatcher {
	// TODO add WASD controls
	// TODO add help dialog that let's you change control and view instructions

	/** Renders the mesh into a projected image */
	@Getter RenderMesh renderer = new RenderMesh();
	VertexMesh mesh;

	// Lock for swapping the double buffer for rendering. When the active buffer is being drawn this lock is
	// active. When the render thread wants to swap the buffers it will activate the lock just for the swap.
	ReentrantLock lockSwap = new ReentrantLock();

	// Use a double buffer approach. Work is updated by the render thread only and 'buffered' is read by the
	// draw/UI thread. lockSwap is used to deconflict when both threads need to access 'buffered'
	BufferedImage buffered = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	BufferedImage work = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	// If true a request to shutdown the render thread has been made
	boolean shutdownRequested = false;
	// if true a request to render another frame has been made
	boolean renderRequested = false;

	// The thread which performs the rendering
	Thread renderThread = new Thread(this::renderLoop, "MeshRender");

	// only read/write when synchronized
	final ImageDimension dimension = new ImageDimension();

	// Horizontal FOV in degrees
	double hfov = 90;

	// If true it will show the depth buffer instead of the regular rendered image
	@Getter @Setter boolean showDepth = false;

	Swing3dCameraControl cameraControls = new MouseRotateAroundPoint();

	// Work image for rendering depth
	GrayF32 inverseDepth = new GrayF32(1, 1);

	@Nullable PrintStream verbose = null;

	public MeshViewerPanel() {
		// Add the standard way to save images
		addMouseListener(new SaveImageOnClick(this));

		// Listen to when this component is resized so that it can update the camera model and re-render the scene
		addComponentListener(new ComponentAdapter() {
			@Override public void componentResized( ComponentEvent e ) {
				synchronized (dimension) {
					dimension.width = getWidth();
					dimension.height = getHeight();
					requestRender();
				}
			}
		});

		// Configure camera controls so that they can respond to UI events
		cameraControls.setChangeHandler(this::requestRender);
		cameraControls.attachControls(this);

		// Add and remove a keyboard listener
		addFocusListener(new FocusListener() {
			@Override public void focusGained( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(MeshViewerPanel.this);
			}

			@Override public void focusLost( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(MeshViewerPanel.this);
			}
		});

		setFocusable(true);
		requestFocus();
	}

	/**
	 * Perform clean up when this component is no longer being used
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		shutdownRenderThread();
	}

	public void shutdownRenderThread() {
		shutdownRequested = true;
	}

	public void setMesh( VertexMesh mesh, boolean copy ) {
		if (copy) {
			mesh = new VertexMesh().setTo(mesh);
		}
		cameraControls.selectInitialParameters(mesh);
		this.mesh = mesh;
	}

	/**
	 * Let's ou specify the RGB color for each vertex in the mesh.
	 */
	public void setVertexColor( int[] colorsRgb ) {
		renderer.surfaceColor = ( index ) -> colorsRgb[index] | 0xFF000000;
	}

	/**
	 * Requests that a new image is rendered. Typically this is done when a configuration has changed. It will render
	 * when the rendering thread has a chance.
	 */
	public void requestRender() {
		// tell the render thread to stop rendering
		renderRequested = true;
	}

	private void renderLoop() {
		if (verbose != null) verbose.println("Starting render loop");
		while (!shutdownRequested) {
			if (renderRequested) {
				renderRequested = false;
				render();
			}

			// Stop it from sucking up all the CPU
			Thread.yield();

			// Can't sleep or wait here. That requires interrupting the thread to wake it up, unfortunately
			// that conflicts with the concurrency code and interrupts that too
		}
		if (verbose != null) verbose.println("Finished render loop");
	}

	/**
	 * Renders the scene onto a buffered image. Every time this is called the intrinsic parameters are recomputed
	 * based on the current panel dimensions. Rendering is done using a double buffer to avoid artifacts when
	 * drawn.
	 */
	private void render() {
		// Update render parameters from GUI
		synchronized (dimension) {
			// Skip if not visible
			if (dimension.width <= 0 || dimension.height <= 0) {
				if (verbose != null) verbose.println("invalid size");
				return;
			}

			PerspectiveOps.createIntrinsic(dimension.width, dimension.height, hfov, -1, renderer.getIntrinsics());
		}

		cameraControls.setCamera(renderer.getIntrinsics());
		renderer.worldToView.setTo(cameraControls.getWorldToCamera());

		// Render the mesh
		long time0 = System.currentTimeMillis();
		renderer.render(mesh);
		long time1 = System.currentTimeMillis();

		// Copy the rendered image into the work buffer
		try {
			if (showDepth) {
				GrayF32 depth = renderer.getDepthImage();

				// easier to scale up close and very far away colors when using inverse depth
				inverseDepth.reshapeTo(depth);
				int N = depth.totalPixels();
				for (int i = 0; i < N; i++) {
					float d = depth.data[i];
					if (Float.isNaN(d) || d <= 0.0f)
						inverseDepth.data[i] = -1;
					else
						inverseDepth.data[i] = 1.0f/d;
				}
				work = VisualizeImageData.inverseDepth(inverseDepth, work, 0.0f, -1, 0x000000);
			} else {
				InterleavedU8 rgb = renderer.getRgbImage();
				work = ConvertBufferedImage.checkDeclare(rgb.width, rgb.height, work, work.getType());
				ConvertBufferedImage.convertTo(rgb, work, false);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		long time2 = System.currentTimeMillis();
		if (verbose != null)
			verbose.printf("render(): %dx%d  mesh: %d (ms) convert: %d (ms)\n",
					dimension.width, dimension.height, time1 - time0, time2 - time1);


		// After rendering in the work buffer swap it with the active buffer that will be displayed in the UI
		lockSwap.lock();
		try {
			BufferedImage tmp = buffered;
			buffered = work;
			work = tmp;
		} finally {
			lockSwap.unlock();
		}

		// Tell Swing to redraw this panel so that we can see what has been rendered
		super.repaint();
	}

	/**
	 * Changes the camera's horizontal field-of-view.
	 *
	 * @param degrees FOV in degrees
	 */
	public void setHorizontalFov( double degrees ) {
		this.hfov = degrees;
		requestRender();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);

		// Start the render thread if it hasn't already started
		try {
			if (!renderThread.isAlive())
				renderThread.start();
		} catch (Exception ignore) {
		}

		var g2 = (Graphics2D)g;

		// Lock here to ensure the render thread doesn't swap buffers then update it while we are drawing it here
		lockSwap.lock();
		try {
			g2.drawImage(buffered, 0, 0, null);
		} finally {
			lockSwap.unlock();
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, renderer);
	}

	long coolDownTIme = 0L;

	@Override public boolean dispatchKeyEvent( KeyEvent e ) {
		if (System.currentTimeMillis() <= coolDownTIme)
			return false;

		// H = Home and resets the view
		if (e.getKeyCode() == KeyEvent.VK_H) {
			cameraControls.reset();
			requestRender();
		} else if (e.getKeyCode() == KeyEvent.VK_K) {
			showDepth = !showDepth;
			requestRender();
		} else {
			return false;
		}
		coolDownTIme = System.currentTimeMillis() + 200L;
		return false;
	}
}
