/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
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
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

	// TODO right-click or shift-click  perform a roll

	// TODO Mouse rotation rotates so that the point clicked is moved to where the mouse is
	// use vectors to figure out that rotation

	// Storage for xyz coordinates of points in the count
	private @Getter final GrowQueue_F32 cloudXyz = new GrowQueue_F32(); // lock for reading/writing cloud data
	// Storage for rgb values of points in the cloud
	private @Getter final GrowQueue_I32 cloudColor = new GrowQueue_I32();

	// Wireframes which will be rendered. Also the LOCK
	private final FastQueue<Wireframe> wireframes = new FastQueue<>(Wireframe::new);

	// Maximum render distance
	float maxRenderDistance = Float.MAX_VALUE;
	// If true then fog is rendered. This makes points fade to background color at a distance
	boolean fog;

	PointCloudViewer.Colorizer colorizer;

	// intrinsic camera parameters
	float hfov = UtilAngle.radian(50);

	//--------- Work Variables for Rendering
	// transform from world frame to camera frame
	// Only write to it inside the UI thread. If it is read outside of the UI thread then it needs to be locked
	final Se3_F32 worldToCamera = new Se3_F32();

	// used when rendering wireframes
	private final FastQueue<Point3D_F32> cameraPts = new FastQueue<>(Point3D_F32::new);
	private final FastQueue<Point2D_F32> pixels = new FastQueue<>(Point2D_F32::new);
	private final GrowQueue_B visible = new GrowQueue_B();

	// used when rendering the point cloud
	private final Point3D_F32 worldPt = new Point3D_F32();
	private final Point3D_F32 cameraPt = new Point3D_F32();
	private final Point3D_F32 pixel = new Point3D_F32();

	// how far it moves in the world frame for each key press
	float stepSize;

	GrayS32 imageRgb = new GrayS32(1,1);
	GrayF32 imageDepth = new GrayF32(1,1);

	BufferedImage imageOutput = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
	//--------- END work variables

	private int dotRadius = 2; // radius of square dot
	int backgroundColor = 0; // RGB 32-bit format

	// previous mouse location
	int prevX;
	int prevY;

	Keyboard keyboard = new Keyboard();
	ScheduledExecutorService pressedTask;

	public PointCloudViewerPanelSwing(float keyStepSize ) {

		addMouseListener(new SaveImageOnClick(this));

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		setFocusable(true);
		requestFocus();
		this.stepSize = keyStepSize;

		addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
//				System.out.println("focus gained");
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboard);

				// start a timed task which checks current key presses. Less OS dependent this way
				pressedTask = Executors.newScheduledThreadPool(1);
				pressedTask.scheduleAtFixedRate(new KeypressedTask(),100,30, TimeUnit.MILLISECONDS);
			}

			@Override
			public void focusLost(FocusEvent e) {
//				System.out.println("focus lost");
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboard);
				pressedTask.shutdown();
				pressedTask = null;
				resetKey();
			}
		});
	}

	public PointCloudViewerPanelSwing( float hfov, float keyStepSize) {
		this(keyStepSize);
		setHorizontalFieldOfView(hfov);
	}

	public void resetKey() {
		pressed.clear();
		shiftPressed = false;
	}

	public void addWireFrame(List<Point3D_F64> vertexes, boolean closed, int rgb, int radiusPixels) {
		synchronized (wireframes) {
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
		}
	}

	public void setWorldToCamera( Se3_F32 worldToCamera ) {
		this.worldToCamera.set(worldToCamera);
	}

	public void setHorizontalFieldOfView( float radians ) {
		this.hfov = radians;
	}

	public void clearCloud() {
		synchronized (cloudXyz) {
			cloudXyz.reset();
			cloudColor.reset();
		}
		synchronized (wireframes) {
			wireframes.reset();
		}
	}

	public void addPoint( float x , float y , float z , int rgb ) {
		synchronized (cloudXyz) {
			cloudXyz.add(x);
			cloudXyz.add(y);
			cloudXyz.add(z);
			cloudColor.add(rgb);
		}
	}
	public void addPoints( float pointsXYZ[] , int pointsRGB[] , int length ) {
		synchronized (cloudXyz) {
			int idxSrc = cloudXyz.size * 3;

			cloudXyz.extend(cloudXyz.size + length * 3);
			cloudColor.extend(cloudColor.size + length);

			for (int i = 0, idx = 0; i < length; i++) {
				cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
				cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
				cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
				cloudColor.data[i] = pointsRGB[i];
			}
		}
	}

	/**
	 * Returns a copy of the worldToCamera transform
	 */
	public Se3_F32 getWorldToCamera(@Nullable Se3_F32 worldToCamera ) {
		if( worldToCamera == null )
			worldToCamera = new Se3_F32();
		synchronized (this.worldToCamera ) {
			worldToCamera.set(this.worldToCamera);
		}
		return worldToCamera;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		projectScene();
		imageOutput = ConvertBufferedImage.checkDeclare(imageRgb.width,imageRgb.height,imageOutput,BufferedImage.TYPE_INT_RGB);
		DataBufferInt buffer = (DataBufferInt)imageOutput.getRaster().getDataBuffer();
		System.arraycopy(imageRgb.data,0,buffer.getData(),0,imageRgb.width*imageRgb.height);
		g.drawImage(imageOutput,0,0,null);
	}

	private synchronized void projectScene() {
		int w = getWidth();
		int h = getHeight();

		imageDepth.reshape(w,h);
		imageRgb.reshape(w,h);

		CameraPinhole intrinsic = PerspectiveOps.createIntrinsic(w,h,UtilAngle.degree(hfov));

		ImageMiscOps.fill(imageDepth,Float.MAX_VALUE);
		ImageMiscOps.fill(imageRgb,backgroundColor);

		float maxDistanceSq = maxRenderDistance*maxRenderDistance;
		if( Float.isInfinite(maxDistanceSq))
			maxDistanceSq = Float.MAX_VALUE;

		synchronized (cloudXyz) {
			renderCloud(intrinsic, colorizer, maxDistanceSq);
		}
		synchronized (wireframes) {
			renderWireframes(intrinsic, maxDistanceSq);
		}
	}

	private void renderCloud(CameraPinhole intrinsic, final PointCloudViewer.Colorizer colorizer, float maxDistanceSq) {
		final float fx = (float)intrinsic.fx;
		final float fy = (float)intrinsic.fy;
		final float cx = (float)intrinsic.cx;
		final float cy = (float)intrinsic.cy;
		// NOTE: To make this concurrent there needs to be a way to write the points and not run into race conditions
		//       Each thread writing to its own image seems too expensive for large images and combining the results
		int totalPoints = cloudXyz.size/3;
		for( int i = 0,pointIdx=0; i < totalPoints; i++ ) {
			worldPt.x = cloudXyz.data[pointIdx++];
			worldPt.y = cloudXyz.data[pointIdx++];
			worldPt.z = cloudXyz.data[pointIdx++];

			SePointOps_F32.transform(worldToCamera,worldPt,cameraPt);

			// can't render if it's behind the camera
			if( cameraPt.z < 0 )
				continue;

			float r2 = cameraPt.normSq();
			if( r2 > maxDistanceSq )
				continue;

			pixel.x = fx * cameraPt.x/cameraPt.z + cx;
			pixel.y = fy * cameraPt.y/cameraPt.z + cy;

			int x = (int)(pixel.x+0.5f);
			int y = (int)(pixel.y+0.5f);

			if( !imageDepth.isInBounds(x,y) )
				continue;

			int rgb;
			if( colorizer == null ) {
				rgb = cloudColor.data[i];
			} else {
				rgb = colorizer.color(i,worldPt.x,worldPt.y,worldPt.z);
			}

			if( fog ) {
				rgb = applyFog(rgb, 1.0f-(float)Math.sqrt(r2)/maxRenderDistance );
			}
			renderDot(x,y,cameraPt.z,rgb,dotRadius);
		}
	}

	/**
	 * Draws wire frame sprites
	 */
	private void renderWireframes(CameraPinhole intrinsic, float maxDistanceSq ) {
		final float fx = (float)intrinsic.fx;
		final float fy = (float)intrinsic.fy;
		final float cx = (float)intrinsic.cx;
		final float cy = (float)intrinsic.cy;

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
				pixel.x = fx * cameraPt.x / cameraPt.z + cx;
				pixel.y = fy * cameraPt.y / cameraPt.z + cy;
				boolean v = cameraPt.z > 0;//&& imageDepth.isInBounds((int)(pixel.x+0.5f),(int)(pixel.x+0.5f));
				visible.add(v);
			}

			// if two consecutive pixels are visible draw that line
			// A line is drawn by rending square dots every pixel. This was done because it was easy to program
			// rendering a smooth line is a way someone could improve this code
			for (int i = 0, j = 1; j < wf.vertexes.size; i = j, j++) {
				if (!visible.get(i) || !visible.get(j))
					continue;
				Point2D_F32 a = pixels.get(i);
				Point2D_F32 b = pixels.get(j);
				Point3D_F32 A = cameraPts.get(i);
				Point3D_F32 B = cameraPts.get(j);

				float distance = a.distance(b);
				// Draw the dots less often when the radius is larger make it render faster
				int N = (int) Math.ceil(distance / (1.0 + 1.5 * wf.radiusPixels));

				// skip the rest if it's too small
				if (N < 1)
					continue;

				for (int locidx = 0; locidx < N; locidx++) {
					// interpolate to find the coordinate along the line in 2D and 3D
					float lineLoc = locidx / (float) N;
					cameraPt.x = (B.x - A.x) * lineLoc + A.x;
					cameraPt.y = (B.y - A.y) * lineLoc + A.y;
					cameraPt.z = (B.z - A.z) * lineLoc + A.z;

					float r2 = cameraPt.normSq();
					if (r2 > maxDistanceSq)
						continue;

					// Since the two vertexes are inside the image the inner points must also be
					float x = (b.x - a.x) * lineLoc + a.x;
					float y = (b.y - a.y) * lineLoc + a.y;
					int px = (int) (x + 0.5f);
					int py = (int) (y + 0.5f);

					if( !imageDepth.isInBounds(px,py) )
						continue;

					int rgb = wf.rgb;
					if (fog) {
						rgb = applyFog(rgb, 1.0f - (float) Math.sqrt(r2) / maxRenderDistance);
					}
					renderDot(px, py, cameraPt.z, rgb, wf.radiusPixels);
				}
			}
		}
	}

	/**
	 * Fades color into background as a function of distance
	 */
	private int applyFog( int rgb , float fraction ) {
		// avoid floating point math
		int adjustment = (int)(1000*fraction);

		int r = (rgb >> 16)&0xFF;
		int g = (rgb >> 8)&0xFF;
		int b = rgb & 0xFF;

		r = (r * adjustment + ((backgroundColor>>16)&0xFF)*(1000-adjustment)) / 1000;
		g = (g * adjustment + ((backgroundColor>>8)&0xFF)*(1000-adjustment)) / 1000;
		b = (b * adjustment + (backgroundColor&0xFF)*(1000-adjustment)) / 1000;

		return (r << 16) | (g << 8) | b;
	}

	/**
	 * Renders a dot as a square sprite with the specified color
	 */
	private void renderDot( int cx , int cy , float Z , int rgb, final int dotRadius ) {
		for (int i = -dotRadius; i <= dotRadius; i++) {
			int y = cy+i;
			if( y < 0 || y >= imageRgb.height )
				continue;
			for (int j = -dotRadius; j <= dotRadius; j++) {
				int x = cx+j;
				if( x < 0 || x >= imageRgb.width )
					continue;

				int pixelIndex = imageDepth.getIndex(x,y);
				float depth = imageDepth.data[pixelIndex];
				if( depth > Z ) {
					imageDepth.data[pixelIndex] = Z;
					imageRgb.data[pixelIndex] = rgb;
				}
			}
		}
	}

	boolean shiftPressed = false;
	private final Set<Integer> pressed = new HashSet<>();

	private class Keyboard implements KeyEventDispatcher {

		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			switch (e.getID()) {

				case KeyEvent.KEY_PRESSED:
//					System.out.println("Key pressed "+e.getKeyChar());
					switch( e.getKeyCode() ) {
						case KeyEvent.VK_SHIFT: shiftPressed=true; break;
						default:pressed.add(e.getKeyCode());break;

					}
					break;

				case KeyEvent.KEY_RELEASED:
//					System.out.println("Key released "+e.getKeyChar());
					switch( e.getKeyCode() ) {
						case KeyEvent.VK_SHIFT: shiftPressed=false; break;
						default:
							if( !pressed.remove(e.getKeyCode()) ) {
								System.err.println("Possible Java / Mac OS X bug related to 'character accent menu'" +
										" if using Java 1.8 try upgrading to 11 or later");
							}
							break;
					}
					break;
			}
			return false;
		}
	}

	private void handleKeyPress() {
		Vector3D_F32 T = worldToCamera.getT();

		synchronized (pressed) {
			double multiplier = shiftPressed?5:1;
			Integer[] keys = pressed.toArray(new Integer[0]);

			for( int k : keys ) {
				switch( k ) {
					case KeyEvent.VK_W:T.z -= stepSize*multiplier; break;
					case KeyEvent.VK_S:T.z += stepSize*multiplier; break;
					case KeyEvent.VK_A:T.x += stepSize*multiplier; break;
					case KeyEvent.VK_D:T.x -= stepSize*multiplier; break;
					case KeyEvent.VK_Q:T.y -= stepSize*multiplier; break;
					case KeyEvent.VK_E:T.y += stepSize*multiplier; break;
					case KeyEvent.VK_H:worldToCamera.reset(); break;
				}
			}
		}
		repaint();
	}

	private class KeypressedTask implements Runnable {

		@Override
		public void run() {
			synchronized (pressed) {
				handleKeyPress();
			}
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
//		offsetZ -= e.getWheelRotation()*pixelToDistance;

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		requestFocus();
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public synchronized void mouseDragged(MouseEvent e) {
		float rotX = 0;
		float rotY = 0;
		float rotZ = 0;

		rotY += (e.getX() - prevX)*0.002;
		rotX += (prevY - e.getY())*0.002;

		Se3_F32 rotTran = new Se3_F32();
		ConvertRotation3D_F32.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,rotTran.getR());
		synchronized (worldToCamera) {
			Se3_F32 temp = worldToCamera.concat(rotTran, null);
			worldToCamera.set(temp);
		}

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public int getDotRadius() {
		return dotRadius;
	}

	public void setDotRadius(int dotRadius) {
		this.dotRadius = dotRadius;
	}

	private static class Wireframe {
		public final FastQueue<Point3D_F32> vertexes = new FastQueue<>(Point3D_F32::new);
		public int radiusPixels = 1;
		public int rgb;
	}
}
