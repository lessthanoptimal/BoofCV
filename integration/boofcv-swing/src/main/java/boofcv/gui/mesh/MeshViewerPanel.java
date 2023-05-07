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
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import boofcv.visualize.RenderMesh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Peter Abeles
 */
public class MeshViewerPanel extends JPanel {
	RenderMesh renderer = new RenderMesh();
	VertexMesh mesh;

	// use a double buffer approach. Work is updated by the render thread only
	// The UI thread checks to see if the work thread has been update, if so swaps
	// This is done safely using thread locks
	BufferedImage buffered = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	BufferedImage work = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	// Lock for swapping BufferedImages and setting pending update
	ReentrantLock lockUpdate = new ReentrantLock();
	boolean pendingUpdate;

	boolean shutdownRequested = false;
	boolean renderRequested = false;
	Thread renderThread = new Thread(() -> renderLoop(), "MeshRender");

	// only read/write when synchornized
	final ImageDimension dimension = new ImageDimension();

	// Horizontal FOV in degrees
	double hfov = 90;

	public MeshViewerPanel() {
		addComponentListener(new ComponentAdapter() {
			@Override public void componentResized( ComponentEvent e ) {
				System.out.println("Updating camera shape");
				synchronized (dimension) {
					dimension.width = getWidth();
					dimension.height = getHeight();
					requestRender();
				}
			}
		});
	}

	/**
	 * Perform clean up when this component is no longer being used
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		shutdownRenderThread();
	}

	public void startRenderThread() {
		renderThread.start();
	}

	public void shutdownRenderThread() {
		shutdownRequested = true;
		renderThread.interrupt();
	}

	public void clearMesh() {
		mesh = new VertexMesh();
		requestRender();
	}

	public void setMesh( VertexMesh mesh, boolean copy ) {
		if (copy) {
			mesh = new VertexMesh().setTo(mesh);
		}
		this.mesh = mesh;
		requestRender();
	}

	public void requestRender() {
		// tell the render thread to stop rendering
		renderRequested = true;
		renderThread.interrupt();
	}

	private void renderLoop() {
		System.out.println("Starting render loop");
		while (!shutdownRequested) {
			if (renderRequested) {
				renderRequested = false;
				render();
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException ignore) {
			}
		}
		System.out.println("Finished render loop");
	}

	private void render() {
		// Update render parameters from GUI
		synchronized (dimension) {
			// Skip if not visible
			if (dimension.width <= 0 || dimension.height <= 0) {
				System.out.println("invalid size");
				return;
			}

			PerspectiveOps.createIntrinsic(dimension.width, dimension.height, hfov, -1, renderer.getIntrinsics());
		}

		System.out.println("Rendering. size: " + dimension.width + "x" + dimension.height);

		// Render the mesh
		long time0 = System.currentTimeMillis();
		renderer.render(mesh);
		long time1 = System.currentTimeMillis();

		System.out.println("After render. " + (time1 - time0) + " (ms)");
		// TODO fix case where there is already a pending update, the work buffer has another update requested
		//      and when the buffers are swapped the work buffer is being drawn and converted at the same time

		// Copy the rendered image into the work buffer
		lockUpdate.lock();
		try {
			InterleavedU8 rgb = renderer.getRgbImage();
			work = ConvertBufferedImage.checkDeclare(rgb.width, rgb.height, work, work.getType());
			ConvertBufferedImage.convertTo(renderer.rgbImage, work, true);
			pendingUpdate = true;
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			lockUpdate.unlock();
		}

		// Tell Swing to redraw this panel so that we can see what has been rendered
		super.repaint();
		System.out.println("Exit render");
	}

	public void setHorizontalFov( double degrees ) {
		this.hfov = degrees;
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);

		// Check to see if there's a new rendered image, if so swap it with the work buffer
		if (lockUpdate.tryLock()) {
			if (pendingUpdate) {
				pendingUpdate = false;
				System.out.println("swapped buffers");
				BufferedImage tmp = buffered;
				buffered = work;
				work = tmp;
			}
			lockUpdate.unlock();
		}

		var g2 = (Graphics2D)g;
		g2.drawImage(buffered, 0, 0, null);
	}
}
