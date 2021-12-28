/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.d3;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.gui.image.SaveImageOnClick;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.packed.PackedBigArrayPoint3D_F32;
import boofcv.visualize.PointCloudViewer;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.ConvertFloatType;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.transform.se.SePointOps_F32;
import lombok.Getter;
import org.ddogleg.struct.BigDogArray_I32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Renders a 3D point cloud using a perspective pinhole camera model. Points are rendered as sprites which are
 * always the same size. The image is then converted into a BufferedImage for output
 * </p>
 *
 * @author Peter Abeles
 */
public class PointCloudViewerPanelSwing extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener {

	// TODO right click that brings up contextual menu that lets you configure how data is viewed

	// Storage for xyz coordinates of points in the count
	// lock for reading/writing cloud data
	private @Getter final PackedBigArrayPoint3D_F32 cloudXyz = new PackedBigArrayPoint3D_F32();
	// Storage for rgb values of points in the cloud
	private @Getter final BigDogArray_I32 cloudColor = new BigDogArray_I32();

	// Wireframes which will be rendered. Also the LOCK
	private final DogArray<Wireframe> wireframes = new DogArray<>(Wireframe::new);
	private final ReentrantLock lockWireFrame = new ReentrantLock();

	// Maximum render distance
	float maxRenderDistance = Float.MAX_VALUE;
	// If true then fog is rendered. This makes points fade to background color at a distance
	boolean fog;

	@Nullable PointCloudViewer.Colorizer colorizer;

	// intrinsic camera parameters
	float hfov = UtilAngle.radian(50);

	// work space for rendering. Must be locked
	private final RenderingWork rendering = new RenderingWork();

	private int dotRadius = 2; // radius of square dot
	int backgroundColor = 0; // RGB 32-bit format

	// previous mouse location
	int prevX;
	int prevY;

	Keyboard keyboard = new Keyboard();
	@Nullable ScheduledExecutorService pressedTask = null;

	// how far it moves in the world frame for each key press
	volatile float stepSize;

	private static class RenderingWork {
		// Lock which must be enforced when rendering
		private final ReentrantLock lock = new ReentrantLock();

		// transform from world frame to camera frame
		// Only write to it inside the UI thread. If it is read outside of the UI thread then it needs to be locked
		final Se3_F32 worldToCamera = new Se3_F32();

		// used when rendering wireframes
		private final DogArray<Point3D_F32> cameraPts = new DogArray<>(Point3D_F32::new);
		private final DogArray<Point2D_F32> pixels = new DogArray<>(Point2D_F32::new);
		private final DogArray_B visible = new DogArray_B();

		// used when rendering the point cloud
		private final Point3D_F32 worldPt = new Point3D_F32();
		private final Point3D_F32 cameraPt = new Point3D_F32();
		private final Point2D_F32 pixel = new Point2D_F32();

		GrayS32 imageRgb = new GrayS32(1, 1);
		GrayF32 imageDepth = new GrayF32(1, 1);

		BufferedImage imageOutput = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	}

	public PointCloudViewerPanelSwing( float keyStepSize ) {

		addMouseListener(new SaveImageOnClick(this));

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		setFocusable(true);
		requestFocus();

		stepSize = keyStepSize;

		addFocusListener(new FocusListener() {
			@Nullable ScheduledFuture<?> future;
			@Override
			public void focusGained( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboard);

				// start a timed task which checks current key presses. Less OS dependent this way
				if (pressedTask != null)
					throw new RuntimeException("BUG! pressedTask is not null");
				pressedTask = Executors.newScheduledThreadPool(1);
				future = pressedTask.scheduleAtFixedRate(new KeyPressedTask(), 100, 30, TimeUnit.MILLISECONDS);
			}

			@Override
			public void focusLost( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboard);
				Objects.requireNonNull(pressedTask).shutdown();
				pressedTask = null;
				resetKey();
			}
		});
	}

	public PointCloudViewerPanelSwing( float hfov, float keyStepSize ) {
		this(keyStepSize);
		setHorizontalFieldOfView(hfov);
	}

	public void resetKey() {
		lockPressed.lock();
		try {
			pressed.clear();
		} finally {
			lockPressed.unlock();
		}
		shiftPressed = false;
	}

	public void addWireFrame( List<Point3D_F64> vertexes, boolean closed, int rgb, int radiusPixels ) {
		lockWireFrame.lock();
		try {
			if (vertexes.size() <= 1)
				return;
			Wireframe wf = wireframes.grow();
			wf.vertexes.reset();
			wf.rgb = rgb;
			wf.radiusPixels = radiusPixels;
			for (int i = 0; i < vertexes.size(); i++) {
				ConvertFloatType.convert(vertexes.get(i), wf.vertexes.grow());
			}
			// add an extra point instead of special logic to close the loop
			if (closed) {
				ConvertFloatType.convert(vertexes.get(0), wf.vertexes.grow());
			}
		} finally {
			lockWireFrame.unlock();
		}
	}

	public void setWorldToCamera( Se3_F32 worldToCamera ) {
		rendering.lock.lock();
		try {
			rendering.worldToCamera.setTo(worldToCamera);
		} finally {
			rendering.lock.unlock();
		}
	}

	public void setHorizontalFieldOfView( float radians ) {
		this.hfov = radians;
	}

	public void clearCloud() {
		synchronized (cloudXyz) {
			cloudXyz.reset();
			cloudColor.reset();
		}
		lockWireFrame.lock();
		try {
			wireframes.reset();
		} finally {
			lockWireFrame.unlock();
		}
	}

	public void addPoint( float x, float y, float z, int rgb ) {
		synchronized (cloudXyz) {
			cloudXyz.append(x, y, z);
			cloudColor.add(rgb);
		}
	}

	/**
	 * Returns a copy of the worldToCamera transform
	 */
	public Se3_F32 getWorldToCamera( @Nullable Se3_F32 worldToCamera ) {
		if (worldToCamera == null)
			worldToCamera = new Se3_F32();
		rendering.lock.lock();
		try {
			worldToCamera.setTo(rendering.worldToCamera);
		} finally {
			rendering.lock.unlock();
		}
		return worldToCamera;
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		rendering.lock.lock();
		try {
			projectScene();
			rendering.imageOutput = ConvertBufferedImage.checkDeclare(
					rendering.imageRgb.width, rendering.imageRgb.height, rendering.imageOutput, BufferedImage.TYPE_INT_RGB);
			DataBufferInt buffer = (DataBufferInt)rendering.imageOutput.getRaster().getDataBuffer();
			System.arraycopy(rendering.imageRgb.data, 0, buffer.getData(), 0, rendering.imageRgb.width*rendering.imageRgb.height);
			g.drawImage(rendering.imageOutput, 0, 0, null);
		} finally {
			rendering.lock.unlock();
		}
	}

	private void projectScene() {
		if (!rendering.lock.isLocked())
			throw new RuntimeException("Must be locked already");

		int w = getWidth();
		int h = getHeight();

		rendering.imageDepth.reshape(w, h);
		rendering.imageRgb.reshape(w, h);

		CameraPinhole intrinsic = PerspectiveOps.createIntrinsic(w, h, UtilAngle.degree(hfov), null);

		ImageMiscOps.fill(rendering.imageDepth, Float.MAX_VALUE);
		ImageMiscOps.fill(rendering.imageRgb, backgroundColor);

		float maxDistanceSq = maxRenderDistance*maxRenderDistance;
		if (Float.isInfinite(maxDistanceSq))
			maxDistanceSq = Float.MAX_VALUE;

		synchronized (cloudXyz) {
			renderCloud(intrinsic, colorizer, maxDistanceSq);
		}

		lockWireFrame.lock();
		try {
			renderWireframes(intrinsic, maxDistanceSq);
		} finally {
			lockWireFrame.unlock();
		}
	}

	private void renderCloud( CameraPinhole intrinsic,
							  final @Nullable PointCloudViewer.Colorizer colorizer,
							  float maxDistanceSq ) {
		if (!rendering.lock.isLocked())
			throw new RuntimeException("Must be locked already");

		final Point2D_F32 pixel = rendering.pixel;
		final Point3D_F32 cameraPt = rendering.cameraPt;
		final Se3_F32 worldToCamera = rendering.worldToCamera;
		final GrayF32 imageDepth = rendering.imageDepth;

		final float fx = (float)intrinsic.fx;
		final float fy = (float)intrinsic.fy;
		final float cx = (float)intrinsic.cx;
		final float cy = (float)intrinsic.cy;
		// NOTE: To make this concurrent there needs to be a way to write the points and not run into race conditions
		//       Each thread writing to its own image seems too expensive for large images and combining the results
		cloudXyz.forIdx(0, cloudXyz.size(), (BoofLambdas.ProcessIndex<Point3D_F32>)( idx, worldPt ) -> {
			SePointOps_F32.transform(worldToCamera, worldPt, cameraPt);

			// can't render if it's behind the camera
			if (cameraPt.z < 0)
				return;

			float r2 = cameraPt.normSq();
			if (r2 > maxDistanceSq)
				return;

			pixel.x = fx*cameraPt.x/cameraPt.z + cx;
			pixel.y = fy*cameraPt.y/cameraPt.z + cy;

			int x = (int)(pixel.x + 0.5f);
			int y = (int)(pixel.y + 0.5f);

			if (!imageDepth.isInBounds(x, y))
				return;

			int rgb;
			if (colorizer == null) {
				rgb = cloudColor.get(idx);
			} else {
				rgb = colorizer.color(idx, worldPt.x, worldPt.y, worldPt.z);
			}

			if (fog) {
				rgb = applyFog(rgb, 1.0f - (float)Math.sqrt(r2)/maxRenderDistance);
			}
			renderDot(x, y, cameraPt.z, rgb, dotRadius);
		});
	}

	/**
	 * Draws wire frame sprites
	 */
	private void renderWireframes( CameraPinhole intrinsic, float maxDistanceSq ) {
		if (!rendering.lock.isLocked())
			throw new RuntimeException("Must be locked already");
		if (!lockWireFrame.isLocked())
			throw new RuntimeException("Wireframe must already be locked");

		final DogArray<Point3D_F32> cameraPts = rendering.cameraPts;
		final DogArray<Point2D_F32> pixels = rendering.pixels;
		final DogArray_B visible = rendering.visible;
		final Se3_F32 worldToCamera = rendering.worldToCamera;
		final GrayF32 imageDepth = rendering.imageDepth;

		final float fx = (float)intrinsic.fx;
		final float fy = (float)intrinsic.fy;
		final float cx = (float)intrinsic.cx;
		final float cy = (float)intrinsic.cy;

		// The number of points can blow up. This will pick a reasonable max to abort
		final int maxDotsPerLine = rendering.imageDepth.width;

		for (int wireIdx = 0; wireIdx < wireframes.size; wireIdx++) {
			Wireframe wf = wireframes.get(wireIdx);
			pixels.reset();
			visible.reset();
			cameraPts.reset();

			// Compute the pixel coordinates of each vertex and if it's visible or not
			for (int i = 0; i < wf.vertexes.size; i++) {
				Point3D_F32 cameraPt = cameraPts.grow();
				Point2D_F32 pixel = pixels.grow();
				SePointOps_F32.transform(worldToCamera, wf.vertexes.get(i), cameraPt);
				pixel.x = fx*cameraPt.x/cameraPt.z + cx;
				pixel.y = fy*cameraPt.y/cameraPt.z + cy;
				boolean v = cameraPt.z > 0;//&& imageDepth.isInBounds((int)(pixel.x+0.5f),(int)(pixel.x+0.5f));
				visible.add(v);
			}

			// if two consecutive pixels are visible draw that line
			// A line is drawn by rending square dots every pixel. This was done because it was easy to program
			// rendering a smooth line is a way someone could improve this code
			for (int i = 0, j = 1; j < wf.vertexes.size; i = j, j++) {
				if (!visible.data[i] || !visible.data[j])
					continue;
				Point2D_F32 a = pixels.get(i);
				Point2D_F32 b = pixels.get(j);
				Point3D_F32 A = cameraPts.get(i);
				Point3D_F32 B = cameraPts.get(j);

				// If both pixels are outside the image skip and move on
				if (!BoofMiscOps.isInside(imageDepth, a.x, a.y) && !BoofMiscOps.isInside(imageDepth, b.x, b.y))
					continue;
				// NOTE: if one was outside then a good idea would be to find the intersection

				float distance = a.distance(b);
				// Draw the dots less often when the radius is larger make it render faster
				int N = (int)Math.ceil(distance/(1.0 + 1.5*wf.radiusPixels));

				// skip the rest if it's too small or too big
				// When it's too bug all the known cases are when one corner is outside the image
				if (N < 1 || N > maxDotsPerLine)
					continue;

				final Point3D_F32 cameraPt = rendering.worldPt;
				for (int locidx = 0; locidx < N; locidx++) {
					// interpolate to find the coordinate along the line in 2D and 3D
					float lineLoc = locidx/(float)N;
					cameraPt.x = (B.x - A.x)*lineLoc + A.x;
					cameraPt.y = (B.y - A.y)*lineLoc + A.y;
					cameraPt.z = (B.z - A.z)*lineLoc + A.z;

					float r2 = cameraPt.normSq();
					if (r2 > maxDistanceSq)
						continue;

					// Since the two vertexes are inside the image the inner points must also be
					float x = (b.x - a.x)*lineLoc + a.x;
					float y = (b.y - a.y)*lineLoc + a.y;
					int px = (int)(x + 0.5f);
					int py = (int)(y + 0.5f);

					if (!imageDepth.isInBounds(px, py))
						continue;

					int rgb = wf.rgb;
					if (fog) {
						rgb = applyFog(rgb, 1.0f - (float)Math.sqrt(r2)/maxRenderDistance);
					}
					renderDot(px, py, cameraPt.z, rgb, wf.radiusPixels);
				}
			}
		}
	}

	/**
	 * Fades color into background as a function of distance
	 */
	private int applyFog( int rgb, float fraction ) {
		// avoid floating point math
		int adjustment = (int)(1000*fraction);

		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;

		r = (r*adjustment + ((backgroundColor >> 16) & 0xFF)*(1000 - adjustment))/1000;
		g = (g*adjustment + ((backgroundColor >> 8) & 0xFF)*(1000 - adjustment))/1000;
		b = (b*adjustment + (backgroundColor & 0xFF)*(1000 - adjustment))/1000;

		return (r << 16) | (g << 8) | b;
	}

	/**
	 * Renders a dot as a square sprite with the specified color
	 */
	private void renderDot( int cx, int cy, float Z, int rgb, final int dotRadius ) {
		final GrayF32 imageDepth = rendering.imageDepth;
		final GrayS32 imageRgb = rendering.imageRgb;

		int x0 = cx - dotRadius;
		int x1 = cx + dotRadius + 1;
		int y0 = cy - dotRadius;
		int y1 = cy + dotRadius + 1;

		if (x0 < 0) x0 = 0;
		if (x1 > imageRgb.width) x1 = imageRgb.width;
		if (y0 < 0) y0 = 0;
		if (y1 > imageRgb.height) y1 = imageRgb.height;

		for (int y = y0; y < y1; y++) {
			int pixelIndex = y*imageDepth.width + x0;
			for (int x = x0; x < x1; x++, pixelIndex++) {
				float depth = imageDepth.data[pixelIndex];
				if (depth > Z) {
					imageDepth.data[pixelIndex] = Z;
					imageRgb.data[pixelIndex] = rgb;
				}
			}
		}
	}

	boolean shiftPressed = false;
	boolean controlPressed = false;

	private final ReentrantLock lockPressed = new ReentrantLock();
	private final Set<Integer> pressed = new HashSet<>();

	private class Keyboard implements KeyEventDispatcher {

		@Override
		public boolean dispatchKeyEvent( KeyEvent e ) {
			lockPressed.lock();
			try {
				switch (e.getID()) {
					case KeyEvent.KEY_PRESSED -> {
						switch (e.getKeyCode()) {
							case KeyEvent.VK_SHIFT -> shiftPressed = true;
							case KeyEvent.VK_CONTROL -> controlPressed = true;
							default -> pressed.add(e.getKeyCode());
						}
					}
					case KeyEvent.KEY_RELEASED -> {
						switch (e.getKeyCode()) {
							case KeyEvent.VK_SHIFT -> shiftPressed = false;
							case KeyEvent.VK_CONTROL -> controlPressed = false;
							default -> {
								if (!pressed.remove(e.getKeyCode())) {
									System.err.println("Possible Java / Mac OS X bug related to 'character accent menu'" +
											" if using Java 1.8 try upgrading to 11 or later");
								}
							}
						}
					}
				}
			} finally {
				lockPressed.unlock();
			}
			return false;
		}
	}

	private void handleKeyPress() {
		float x = 0, y = 0, z = 0;
		boolean reset = false;
		boolean change = true;

		lockPressed.lock();
		try {
			float multiplier = shiftPressed ? 5 : 1;
			multiplier *= controlPressed ? 1.0f/5.0f : 1.0f;

			Integer[] keys = pressed.toArray(new Integer[0]);
			for (int k : keys) {
				switch (k) {
					case KeyEvent.VK_W -> z -= multiplier;
					case KeyEvent.VK_S -> z += multiplier;
					case KeyEvent.VK_A -> x += multiplier;
					case KeyEvent.VK_D -> x -= multiplier;
					case KeyEvent.VK_Q -> y -= multiplier;
					case KeyEvent.VK_E -> y += multiplier;
					case KeyEvent.VK_H -> reset = true;
					default -> change = false;
				}
			}
		} finally {
			lockPressed.unlock();
		}

		if (change) {
			rendering.lock.lock();
			try {
				final float stepSize = this.stepSize;
				final Vector3D_F32 T = rendering.worldToCamera.getT();
				T.x += stepSize*x;
				T.y += stepSize*y;
				T.z += stepSize*z;
				if (reset) {
					rendering.worldToCamera.reset();
				}
			} finally {
				rendering.lock.unlock();
			}
		}

		repaint();
	}

	private class KeyPressedTask implements Runnable {
		@Override
		public void run() {
			handleKeyPress();
		}
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e ) {
//		offsetZ -= e.getWheelRotation()*pixelToDistance;
		repaint();
	}

	@Override
	public void mouseClicked( MouseEvent e ) {}

	@Override
	public void mousePressed( MouseEvent e ) {
		requestFocus();
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased( MouseEvent e ) {}

	@Override
	public void mouseEntered( MouseEvent e ) {}

	@Override
	public void mouseExited( MouseEvent e ) {}

	@Override
	public synchronized void mouseDragged( MouseEvent e ) {
		float rotX = 0;
		float rotY = 0;
		float rotZ = 0;

		if (controlPressed) {
			// Roll if control is held down
			int centerX = getWidth()/2;
			int centerY = getHeight()/2;
			double angle0 = Math.atan2(prevX - centerX, prevY - centerY);
			double angle1 = Math.atan2(e.getX() - centerX, e.getY() - centerY);
			rotZ += (float)(angle0 - angle1);
		} else {
			// otherwise pan and tilt
			rotY += (e.getX() - prevX)*0.002f;
			rotX += (prevY - e.getY())*0.002f;
		}

		Se3_F32 rotTran = new Se3_F32();
		ConvertRotation3D_F32.eulerToMatrix(EulerType.XYZ, rotX, rotY, rotZ, rotTran.getR());

		rendering.lock.lock();
		try {
			Se3_F32 temp = rendering.worldToCamera.concat(rotTran, null);
			rendering.worldToCamera.setTo(temp);
		} finally {
			rendering.lock.unlock();
		}

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved( MouseEvent e ) {}

	public float getStepSize() {
		return stepSize;
	}

	public void setStepSize( float size ) {
		stepSize = size;
	}

	public int getDotRadius() {
		return dotRadius;
	}

	public void setDotRadius( int dotRadius ) {
		this.dotRadius = dotRadius;
	}

	private static class Wireframe {
		public final DogArray<Point3D_F32> vertexes = new DogArray<>(Point3D_F32::new);
		public int radiusPixels = 1;
		public int rgb;
	}
}
