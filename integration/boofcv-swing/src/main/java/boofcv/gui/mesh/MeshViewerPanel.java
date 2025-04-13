/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.SaveImageOnClick;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.InterleavedU8;
import boofcv.struct.mesh.VertexMesh;
import boofcv.visualize.RenderMesh;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.ddogleg.util.VerboseUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Displays a rendered mesh in 3D and provides mouse and keyboard controls for moving the camera. Each shape in the
 * mesh can be assigned a single color. By default this colorization will be based on the normal angle, but the user
 * can provide custom colors. Two styles of control are provided by default {@link OrbitAroundPointControl orbit} and
 * {@link FirstPersonCameraControl first person}, again the user can provide others easily.</p>
 *
 * <p>To ensure smooth updates a double buffer is used and rendering is done in a separate thread. All rendering
 * is done in software and can't use a GPU. The rendering thread will automatically stop and stop depending
 * on the lifecycle of this panel.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MeshViewerPanel extends JPanel implements VerbosePrint, KeyEventDispatcher {
	/** Name given to the default approach that colorizes based on normal angle */
	public static final String COLOR_NORMAL = "Normal";

	/** How big the help button appears to be */
	public int buttonSize = 60;
	public Font helpButtonFont = new Font("Serif", Font.BOLD, 45);
	/** If the help button exists */
	@Getter @Setter public boolean helpButtonActive = true;

	/** Renders the mesh into a projected image */
	@Getter RenderMesh renderer = new RenderMesh();
	VertexMesh mesh = new VertexMesh(); // empty mesh to avoid NPE

	// Lock for swapping the double buffer for rendering. When the active buffer is being drawn this lock is
	// active. When the render thread wants to swap the buffers it will activate the lock just for the swap.
	ReentrantLock lockSwap = new ReentrantLock();

	// Use a double buffer approach. Work is updated by the render thread only and 'buffered' is read by the
	// draw/UI thread. lockSwap is used to deconflict when both threads need to access 'buffered'
	BufferedImage buffered = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	BufferedImage work = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	// If true a request to shutdown the render thread has been made
	boolean shutdownRequested = false;
	// if true a request to render another frame has been made
	boolean renderRequested = false;

	// Has the camera been initialized? This is done lazily because we need to know the camera intrinsics
	boolean cameraInitialized = false;

	// The thread which performs the rendering
	Thread renderThread = new Thread(this::renderLoop, "MeshRender");

	// only read/write when synchronized
	final ImageDimension dimension = new ImageDimension();

	// Horizontal FOV in degrees
	double hfov = 90;

	// If true it will show the depth buffer instead of the regular rendered image
	@Getter @Setter boolean showDepth = false;

	// Variables below are synchronized against 'controls'
	/** Map of all camera controls */
	final Map<String, Swing3dCameraControl> controls = new HashMap<>();
	/** Name of the most recently active control */
	@Getter String lastControlName = "";
	// Most recent world to view. This is passed into a camera control when changed.
	Se3_F64 recentWorldToView = new Se3_F64();
	@Nullable Swing3dCameraControl activeControl;
	//----------------- END synchronized by 'controls'

	// Contains all the possible ways to colorize the mesh.
	// You must synchronize before accessing these two fields since multiple threads can access them
	final Map<String, RenderMesh.SurfaceColor> colorizers = new HashMap<>();
	// List of names in the order they were added for consistency
	final List<String> colorizerNames = new ArrayList<>();
	// Index of the active colorizer. Used to cycle through all the options
	int activeColorizer;

	// Work image for rendering depth
	GrayF32 inverseDepth = new GrayF32(1, 1);

	// Window with help and settings
	@Nullable JFrame helpWindow;

	@Nullable PrintStream verbose = null;

	@Getter @Setter CustomPaint customPaint = ( g2 ) -> {};
	@Getter @Setter CameraChanged cameraChanged = ( cam ) -> {};

	// Intrinsic camera parameters
	CameraPinhole intrinsics = new CameraPinhole();

	/**
	 * Convenience constructor that calls {@link #setMesh(VertexMesh, boolean)} and the default constructor.
	 *
	 * @param mesh That mesh which is to be viewed. A reference is saved internally.
	 */
	public MeshViewerPanel( VertexMesh mesh ) {
		this();
		setMesh(mesh, false);
	}

	/**
	 * Default constructor. Configures the GUI and adds in default controls and colorizations.
	 */
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
					cameraChanged.cameraChanged(getIntrinsicsCopy());
				}
			}
		});

		// Configure camera controls so that they can respond to UI events
		controls.put("Orbit", new OrbitAroundPointControl());
		controls.put("FPS", new FirstPersonCameraControl());
		setActiveControl("Orbit");

		// Add and remove a keyboard listener
		addFocusListener(new FocusListener() {
			@Override public void focusGained( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(MeshViewerPanel.this);
			}

			@Override public void focusLost( FocusEvent e ) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(MeshViewerPanel.this);
			}
		});

		// If the user clicks in the top-left corner open the help screen
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked( MouseEvent e ) {
				requestFocus();
				if (!helpButtonActive || e.getX() >= buttonSize || e.getY() >= buttonSize)
					return;
				if (helpWindow == null)
					showHelpWindow();
			}
		});

		setFocusable(true);
		requestFocus();

		// Give it a reasonable initial size
		setPreferredSize(new Dimension(500, 500));
	}

	public float imagePixelToDepthZ( int pixelX, int pixelY ) {
		// Note: We are ignoring background rendering
		GrayF32 depth = renderer.depthImage;

		if (!depth.isInBounds(pixelX, pixelY))
			return Float.NaN;

		return depth.unsafe_get(pixelX, pixelY);
	}

	/**
	 * Given an image pixel, look up the 3D coordinate of the point. If the pixel doesn't intersect the mesh then
	 * false is returned.
	 *
	 * @param pixelX (Input) x-coordinate of the pixel
	 * @param pixelY (Input) y-coordinate of the pixel
	 * @param norm (Output) normalized image coordinate of pixel
	 * @param p3 (Output) 3D location of point in camera frame
	 */
	public boolean imagePixelToCamera3D( int pixelX, int pixelY, Point2D_F64 norm, Point3D_F64 p3 ) {
		// Note: We are ignoring background rendering
		GrayF32 depth = renderer.depthImage;

		if (!depth.isInBounds(pixelX, pixelY))
			return false;

		float d = depth.unsafe_get(pixelX, pixelY);
		if (Float.isNaN(d) || d <= 0.0f)
			return false;

		PerspectiveOps.convertPixelToNorm(intrinsics, pixelX, pixelY, norm);
		p3.z = d;
		p3.x = norm.x*p3.z;
		p3.y = norm.y*p3.z;

		return true;
	}

	public boolean imagePixelToWorld3D( int pixelX, int pixelY, Point2D_F64 norm, Point3D_F64 p3 ) {
		if (!imagePixelToCamera3D(pixelX, pixelY, norm, p3))
			return false;

		var worldToCamera = new Se3_F64();
		synchronized (controls) {
			worldToCamera.setTo(recentWorldToView);
		}

		worldToCamera.transformReverse(p3, p3);
		return true;
	}

	/// The camera will no longer control the UI
	public void detachCameraFromUI() {
		synchronized (controls) {
			if (activeControl == null)
				return;
			activeControl.detachControls(this);
		}
	}

	/// The camera will now receive UI events
	public void attachCameraToUI() {
		synchronized (controls) {
			if (activeControl == null)
				return;
			activeControl.attachControls(this);
		}
	}

	/**
	 * Changes the active camera control
	 */
	public void setActiveControl( String name ) {
		lastControlName = name;
		synchronized (controls) {
			// true if the current control is attached. If there is no activeControl then attach the new one
//			boolean currentAttached = true;

			// Save the current view so it can select a similar view when changing cameras
			Se3_F64 currentWorldToView = new Se3_F64();
			if (activeControl != null) {
				currentWorldToView.setTo(activeControl.getWorldToCamera());
//				currentAttached = activeControl.isControlAttached();
//				if (currentAttached)
				activeControl.detachControls(this);
			}

			activeControl = Objects.requireNonNull(controls.get(name));
			activeControl.setChangeHandler(this::requestRender);

			// Only attach the new control if the previous one was also attached. To keep behavior consistent
//			if (currentAttached)
			activeControl.attachControls(this);
			if (mesh != null) {
				activeControl.selectInitialParameters(mesh);
				recentWorldToView.setTo(activeControl.getWorldToCamera());
				requestRender();
			}
		}
	}

	/**
	 * Removes the active control
	 */
	public void removeActiveControl() {
		synchronized (controls) {
			if (activeControl != null)
				activeControl.detachControls(this);
			activeControl = null;
		}
	}

	/**
	 * Perform clean up when this component is no longer being used
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		if (helpWindow != null) {
			helpWindow.setVisible(false);
			helpWindow = null;
		}
		shutdownRenderThread();
	}

	/**
	 * Send a request that the rendering thread
	 */
	public void shutdownRenderThread() {
		shutdownRequested = true;
	}

	/**
	 * Sets the mesh which will be rendered.
	 *
	 * @param copy If true then a copy of the mesh will be made and saved. If false then a reference will be saved.
	 * You should create a copy if this mesh is going to be modified.
	 */
	public void setMesh( VertexMesh mesh, boolean copy ) {
		if (copy) {
			mesh = new VertexMesh().setTo(mesh);
		}
		cameraInitialized = false;
		this.mesh = mesh;

		colorizers.clear();
		colorizerNames.clear();

		// Add colorization based on normal angle. This works even if RGB colors are not known
		synchronized (colorizers) {
			setSurfaceColor(COLOR_NORMAL, MeshColorizeOps.colorizeByNormal(mesh));
			setSurfaceColor("Global YawRB-NormG", MeshColorizeOps.colorizeYawRBNormG(mesh, true));
			setSurfaceColor("Local YawRB-NormG", MeshColorizeOps.colorizeYawRBNormG(mesh, false));
			renderer.surfaceColor = Objects.requireNonNull(colorizers.get(COLOR_NORMAL));
			activeColorizer = 0;
		}
	}

	/**
	 * Lets you specify the RGB color for each vertex in the mesh. It's added to the list and made active.
	 *
	 * @param name Name given to this colorization approach
	 */
	public void setSurfaceColor( String name, RenderMesh.SurfaceColor colorizer ) {
		synchronized (colorizers) {
			activeColorizer = colorizers.size();
			colorizers.put(name, colorizer);
			colorizerNames.add(name);
			renderer.surfaceColor = colorizers.get(name);
		}
	}

	/**
	 * It will colorize each surface using the color of the vertexes
	 *
	 * @param name Name given to this colorization approach
	 */
	public void setVertexColors( String name, int[] vertexColors ) {
		if (mesh == null)
			throw new IllegalArgumentException("You must first specify the mesh before calling this function");

		setSurfaceColor(name, MeshColorizeOps.colorizeByVertex(mesh, vertexColors));
	}

	/**
	 * Requests that a new image is rendered. Typically, this is done when a configuration has changed. It will render
	 * when the rendering thread has a chance.
	 */
	public void requestRender() {
		// tell the render thread to stop rendering
		renderRequested = true;
	}

	/**
	 * Performs the main render loop. This will only exit when a request to shutdown has been made.
	 */
	private void renderLoop() {
		if (verbose != null) verbose.println("Starting render loop");
		while (!shutdownRequested) {
			if (renderRequested) {
				renderRequested = false;
				render();
			}

			// Stop it from sucking up all the CPU
			BoofMiscOps.sleep(10);

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

			PerspectiveOps.createIntrinsic(dimension.width, dimension.height, hfov, -1, intrinsics);
			var factory = new LensDistortionPinhole(intrinsics);
			renderer.setCamera(factory, dimension.width, dimension.height);
		}

		synchronized (controls) {
			if (activeControl != null) {
				activeControl.setCamera(intrinsics);

				// Now that we know the intrinsics, initialize the camera
				if (!cameraInitialized) {
					cameraInitialized = true;
					activeControl.selectInitialParameters(mesh);
				}
				renderer.setWorldToView(activeControl.getWorldToCamera());
				renderer.getWorldToView(recentWorldToView);
			}
		}

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

	/**
	 * Each time this is called it will change the colorizer being used, if more than one has been specified
	 */
	public void cycleColorizer() {
		int totalColors = colorizers.size();
		if (mesh.isTextured()) {
			totalColors++;
		}

		synchronized (colorizers) {
			// go to the next one and make sure it's valid
			activeColorizer = (activeColorizer + 1)%totalColors;

			// If it has texture then that is the first possible colorizer
			if (mesh.isTextured()) {
				if (activeColorizer == 0) {
					renderer.forceColorizer = false;
				} else {
					renderer.forceColorizer = true;
					renderer.surfaceColor = Objects.requireNonNull(colorizers.get(colorizerNames.get(activeColorizer - 1)));
				}
			} else {
				renderer.surfaceColor = Objects.requireNonNull(colorizers.get(colorizerNames.get(activeColorizer)));
			}

			// Re-render the image
			requestRender();
		}
	}

	@Override public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		// Start the render thread if it hasn't already started
		try {
			if (!renderThread.isAlive())
				renderThread.start();
		} catch (Exception ignore) {
		}

		var g2 = (Graphics2D)g;
		BoofSwingUtil.antialiasing(g);

		// Lock here to ensure the render thread doesn't swap buffers then update it while we are drawing it here
		lockSwap.lock();
		try {
			g2.drawImage(buffered, 0, 0, null);
		} finally {
			lockSwap.unlock();
		}

		if (helpButtonActive)
			drawHelpButton(g2);

		customPaint.paint(g2);
	}

	private void drawHelpButton( Graphics2D g2 ) {
		String text = "?";

		// Draw a white rectangle to show the button's bounds
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, buttonSize, buttonSize);
		g2.setFont(helpButtonFont);

		// Draw an H to make it easier to understand what it does and so the user investigates
		GlyphVector gv = helpButtonFont.createGlyphVector(g2.getFontRenderContext(), text);
		Rectangle2D bounds = gv.getVisualBounds();
		g2.setColor(Color.RED);
		float offsetX = (float)((buttonSize - bounds.getWidth())/2.0);
		float offsetY = (float)((buttonSize - bounds.getHeight())/2.0 + bounds.getHeight());
		g2.drawString(text, offsetX, offsetY);
	}

	/**
	 * Opens a window which provides help about keys and lets the user modify control settings
	 */
	public void showHelpWindow() {
		// See if the window is already visible
		if (helpWindow != null)
			return;
		helpWindow = new JFrame("Mesh Viewer Help");
		helpWindow.setLocationRelativeTo(this);
		helpWindow.add(new MeshViewerPreferencePanel(this), BorderLayout.CENTER);
		helpWindow.pack();
		helpWindow.setAlwaysOnTop(true);
		helpWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		helpWindow.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed( WindowEvent e ) {
				MeshViewerPanel.this.helpWindow = null;
			}
		});
		helpWindow.setVisible(true);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		verbose = VerboseUtils.addPrefix(this, out);
		VerboseUtils.verboseChildren(out, configuration, renderer);
	}

	/**
	 * Color of background when rendering
	 */
	public void setRenderBackgroundColor( int rgba ) {
		renderer.defaultColorRgba = rgba;
	}

	public int getRenderBackgroundColor() {
		return renderer.defaultColorRgba;
	}

	long coolDownTime = 0L;

	/**
	 * Provides keyboard commands that adjust how and what data is displayed
	 */
	@Override public boolean dispatchKeyEvent( KeyEvent e ) {
		if (System.currentTimeMillis() <= coolDownTime)
			return false;

		// H = Home and resets the view
		if (e.getKeyCode() == KeyEvent.VK_H) {
			if (activeControl != null)
				activeControl.reset();
			cameraInitialized = false;
			requestRender();
		} else if (e.getKeyCode() == KeyEvent.VK_J) {
			cycleColorizer();
		} else if (e.getKeyCode() == KeyEvent.VK_K) {
			showDepth = !showDepth;
			requestRender();
		} else {
			return false;
		}
		coolDownTime = System.currentTimeMillis() + 200L;
		return false;
	}

	public void setTextureImage( InterleavedU8 rgb ) {
		renderer.setTextureImage(rgb);
	}

	/** Returns a copy of the intrinsics */
	public CameraPinhole getIntrinsicsCopy() {
		// We don't copy intrinsics since that is only updated when rendered
		var camera = new CameraPinhole();
		PerspectiveOps.createIntrinsic(dimension.width, dimension.height, hfov, -1, camera);
		return camera;
	}

	public interface CustomPaint {
		void paint( Graphics2D g2 );
	}

	/** Used to listen in when the camera model has changed */
	public interface CameraChanged {
		void cameraChanged( CameraPinhole intrinsics );
	}
}
