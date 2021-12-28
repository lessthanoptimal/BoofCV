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

package boofcv.demonstrations.feature.disparity;

import boofcv.abst.disparity.DisparitySmoother;
import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.cloud.DisparityToColorPointCloud;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.concurrency.BoofConcurrency;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.controls.ControlPanelDisparityDisplay;
import boofcv.gui.d3.UtilDisparitySwing;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertMatrixData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.geo.RectifyImageOps.transformRectToPixel;
import static boofcv.gui.BoofSwingUtil.KEY_PREVIOUS_DIRECTORY;
import static boofcv.gui.BoofSwingUtil.saveDisparityDialog;

/**
 * Computes and displays disparity from still disparity images. The disparity can be viewed
 * as a color surface plot or as a 3D point cloud. Different tuning parameters can be adjusted
 * use a side control panel.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeStereoDisparity<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase
		implements ControlPanelDisparityDisplay.Listener {
	// original input before rescaling
	BufferedImage origLeft;
	BufferedImage origRight;
	StereoParameters origCalib;

	// rectified color image from left and right camera for display
	private BufferedImage colorLeft;
	private BufferedImage colorRight;
	// Output disparity color surface plot
	private BufferedImage disparityOut;

	// gray scale input image before rectification
	private T inputLeft;
	private T inputRight;
	// gray scale input images after rectification
	private T rectLeft;
	private T rectRight;

	// Stored disparity results. if a new instance of the detector is created we will
	// still have a reference to the old image and things won't blow up
	private D disparityImage;
	private int disparityMin, disparityRange;

	// calibration parameters
	private StereoParameters calib;
	// rectification algorithm
	private RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

	// GUI components
	private final ControlPanelDisparityDisplay control = new ControlPanelDisparityDisplay(0, 150, GrayU8.class);
	private final JPanel panel = new JPanel();
	private final DisparityImageView imagePanel = new DisparityImageView();
	private final PointCloudViewer pcv = VisualizeData.createPointCloudViewer();

	// if true the point cloud has already been computed and does not need to be recomputed
	private boolean computedCloud;

	// instance of the selected algorithm
	private StereoDisparity<T, D> activeAlg;
	private DisparitySmoother<T, D> smoother;

	// camera calibration matrix of rectified images
	private DMatrixRMaj rectK;
	private DMatrixRMaj rectR;

	// makes sure process has been called before render disparity is done
	// There was a threading issue where disparitySettingChange() created a new alg() but render was called before
	// it could process an image.
	private volatile boolean processCalled = false;
	private boolean rectifiedImages = false;

	// coordinate transform from left rectified image to its original pixels
	Point2Transform2_F64 leftRectToPixel;

	public VisualizeStereoDisparity( List<PathLabel> examples ) {
		super(true, false, examples, ImageType.single(GrayU8.class));

		// Tell the demo code to not extract inputs from inputs
		super.inputAsFile = true;

		control.setListener(this);

		panel.setLayout(new BorderLayout());
		panel.add(control, BorderLayout.WEST);
		panel.add(imagePanel, BorderLayout.CENTER);

		add(BorderLayout.WEST, control);
		add(BorderLayout.CENTER, panel);

		// When a new image is opened it will be automatically re-scaled, Turn off that feature so that it doesn't
		// keep on happening when views change
		imagePanel.setListener(scale -> {
			imagePanel.autoScaleCenterOnSetImage = false;
			control.setZoom(imagePanel.getScale());
		});
	}

	@Override
	protected void customAddToFileMenu( JMenu menuFile ) {
		menuFile.addSeparator();

		JMenuItem itemSaveDisparity = new JMenuItem("Save Disparity");
		itemSaveDisparity.addActionListener(e -> saveDisparity());
		menuFile.add(itemSaveDisparity);

		JMenuItem itemSaveCloud = new JMenuItem("Save Point Cloud");
		itemSaveCloud.addActionListener(e -> savePointCloud());
		menuFile.add(itemSaveCloud);
	}

	private void saveDisparity() {
		StereoDisparity<T, D> activeAlg = this.activeAlg;
		if (activeAlg == null)
			return;
		saveDisparityDialog(this, KEY_PREVIOUS_DIRECTORY, activeAlg.getDisparity());
	}

	private void savePointCloud() {
		if (!computedCloud) {
			JOptionPane.showMessageDialog(this, "Need to generate point cloud first");
		} else {
			BoofSwingUtil.savePointCloudDialog(this, KEY_PREVIOUS_DIRECTORY, pcv);
		}
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY, 2);
		if (files == null)
			return;
		BoofSwingUtil.invokeNowOrLater(() -> openImageSet(false, files));
	}

	@Override
	public void reprocessInput() {
		// this really should use the demonstration thread pool
		processDisparityInThread();
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {}

	@Override
	public void processFiles( String[] files ) {
		if (files.length == 3) {
			origCalib = CalibrationIO.load(media.openFileNotNull(files[0]));
			origLeft = media.openImageNotNull(files[1]);
			origRight = media.openImageNotNull(files[2]);
		} else if (files.length == 2) {
			origLeft = media.openImageNotNull(files[0]);
			origRight = media.openImageNotNull(files[1]);

			// Guess something "reasonable"
			// baseline of 1 and distortion free cameras with a 90 HFOV
			origCalib = new StereoParameters();
			origCalib.right_to_left = new Se3_F64();
			origCalib.right_to_left.T.x = 1;
			origCalib.left = PerspectiveOps.createIntrinsic(origLeft.getWidth(), origLeft.getHeight(), 90, null);
			origCalib.right = PerspectiveOps.createIntrinsic(origRight.getWidth(), origRight.getHeight(), 90, null);
		} else {
			throw new IllegalArgumentException("Unexpected number of files. " + files.length);
		}
		if (activeAlg == null)
			createAlgConcurrent();
		changeInputScale();
	}

	public void processDisparityInThread() {
		// make sure the user can't modify settings while the disparity is being computed
		control.enableAlgControls(false);
		new Thread(this::processDisparity).start();
	}

	public synchronized void processDisparity() {
		if (!rectifiedImages) {
			SwingUtilities.invokeLater(() -> control.enableAlgControls(true));
			return;
		}

		// if the data types have changed the images need to be rectified again
		if (!activeAlg.getInputType().isSameType(rectLeft.getImageType())) {
			changeInputScale();
		}

		ProcessThread progress = new ProcessThread(this);
		progress.start();

		computedCloud = false;
		long elapsedTime = 0;
		try {
			BoofConcurrency.USE_CONCURRENT = control.concurrent;
			long time0 = System.nanoTime();
			activeAlg.process(rectLeft, rectRight);
			disparityImage = activeAlg.getDisparity();
			disparityMin = activeAlg.getDisparityMin();
			disparityRange = activeAlg.getDisparityRange();
			smoother.process(rectLeft, disparityImage, disparityRange);
			long time1 = System.nanoTime();
			elapsedTime = time1 - time0;
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
		} finally {
			BoofConcurrency.USE_CONCURRENT = true;
			processCalled = true;
		}

		progress.stopThread();

		final long _elapsedTime = elapsedTime;
		SwingUtilities.invokeLater(() -> {
			control.enableAlgControls(true);
			control.setProcessingTimeMS(_elapsedTime/1e6);
			disparityRender();
		});
	}

	/**
	 * Changes which image is being displayed depending on GUI selection
	 */
	private synchronized void changeImageView() {
		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Must be in UI thread");

		if (control.selectedView < 3) {
			BufferedImage img;

			switch (control.selectedView) {
				case 0:
					img = disparityOut;
					break;
				case 1:
					img = colorLeft;
					break;
				case 2:
					img = colorRight;
					break;
				default:
					throw new RuntimeException("Unknown option");
			}
			if (img == null)
				return;

			imagePanel.setImage(img);
			imagePanel.setPreferredSize(new Dimension(origLeft.getWidth(), origLeft.getHeight()));
			swapVisualizationPanel(imagePanel, pcv.getComponent());
		} else {
			// TODO spawn this into it's own thread?
			// TODO is thread safety a concern here? What happens if these settings are changed at the same time?
			if (!computedCloud) {
				computedCloud = true;
				DisparityToColorPointCloud d2c = new DisparityToColorPointCloud();
				PointCloudWriter.CloudArraysF32 cloud = new PointCloudWriter.CloudArraysF32();

				double baseline = calib.getRightToLeft().getT().norm();
				d2c.configure(baseline, rectK, rectR, leftRectToPixel, disparityMin, disparityRange);
				if (imagePanel.state == 2) // has the user defined the ROI?
					d2c.setRegionOfInterest(imagePanel.x0, imagePanel.y0, imagePanel.x1, imagePanel.y1);
				d2c.process(disparityImage, UtilDisparitySwing.wrap(colorLeft), cloud);

				CameraPinhole rectifiedPinhole = PerspectiveOps.matrixToPinhole(
						rectK, colorLeft.getWidth(), colorLeft.getHeight(), null);

				pcv.clearPoints();
				pcv.setCameraHFov(PerspectiveOps.computeHFov(rectifiedPinhole));
				pcv.addCloud(cloud.cloudXyz, cloud.cloudRgb);
				changeView3D();
			}

			swapVisualizationPanel(pcv.getComponent(), imagePanel);
		}
	}

	private void swapVisualizationPanel( Component target, Component other ) {
		if (panel != target.getParent()) {
			BoofMiscOps.checkTrue(other.getParent() == panel);
			panel.remove(other);
			panel.add(target, BorderLayout.CENTER);
			panel.revalidate();
		}
		panel.repaint();
		target.requestFocus();
	}

	/**
	 * Removes distortion and rectifies images.
	 */
	private void rectifyInputImages() {
		// Check to see if the input images have already been recitified
		rectifiedImages = calib.isRectified(1e-3);

		// get intrinsic camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(calib.left, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(calib.right, (DMatrixRMaj)null);

		// if rectified just copy the image and return, computing rectification again will not help improve it
		// and could add a little bit of noise
		if (rectifiedImages) {
			rectLeft.setTo(inputLeft);
			rectRight.setTo(inputRight);
			leftRectToPixel = new DoNothing2Transform2_F64();
			rectK = K1;
			rectR = CommonOps_DDRM.identity(3);
			return;
		}

		// compute rectification matrices
		rectifyAlg.process(K1, new Se3_F64(), K2, calib.getRightToLeft().invert(null));

		DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
		DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
		rectK = rectifyAlg.getCalibrationMatrix();
		rectR = rectifyAlg.getRectifiedRotation();

		// adjust view to maximize viewing area while not including black regions
		RectifyImageOps.allInsideLeft(calib.left, rect1, rect2, rectK, null);

		// compute transforms to apply rectify the images
		leftRectToPixel = transformRectToPixel(calib.left, rect1);

		ImageType<T> imageType = activeAlg.getInputType();

		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3); // TODO simplify code some how
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<T, T> distortRect1 = RectifyDistortImageOps.rectifyImage(
				calib.left, rect1_F32, BorderType.EXTENDED, imageType);
		ImageDistort<T, T> distortRect2 = RectifyDistortImageOps.rectifyImage(
				calib.right, rect2_F32, BorderType.EXTENDED, imageType);

		// rectify and undo distortion
		distortRect1.apply(inputLeft, rectLeft);
		distortRect2.apply(inputRight, rectRight);

		rectifiedImages = true;
	}

	@Override
	public synchronized void algorithmChanged() {
		createAlgConcurrent();
		// Only disparity of the user wants to on every setting change
		if (control.recompute) {
			processDisparityInThread();
		}
	}

	@Override
	public synchronized void recompute() {
		if (control.recompute) {
			processDisparityInThread();
		}
	}

	@Override
	public synchronized void disparityGuiChange() {
		disparityRender();
	}

	@Override
	public synchronized void disparityRender() {
		if (!processCalled)
			return;

		D disparity = activeAlg.getDisparity();

		// TODO Got a NPE inside of this function once when rapidly changing settings. Make sure everything is locked down
		disparityOut = VisualizeImageData.disparity(disparity, null, activeAlg.getDisparityRange(), control.backgroundColorDisparity);

		changeImageView();
	}

	@Override
	public synchronized void changeInputScale() {
		calib = new StereoParameters(origCalib);

		double scale = control.inputScale/100.0;

		PerspectiveOps.scaleIntrinsic(calib.left, scale);
		PerspectiveOps.scaleIntrinsic(calib.right, scale);

		int w = (int)(origLeft.getWidth()*scale);
		int h = (int)(origLeft.getHeight()*scale);

		colorLeft = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
		colorRight = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);

		colorLeft.createGraphics().drawImage(origLeft, AffineTransform.getScaleInstance(scale, scale), null);
		colorRight.createGraphics().drawImage(origRight, AffineTransform.getScaleInstance(scale, scale), null);

		inputLeft = activeAlg.getInputType().createImage(w, h);
		inputRight = activeAlg.getInputType().createImage(w, h);
		rectLeft = activeAlg.getInputType().createImage(w, h);
		rectRight = activeAlg.getInputType().createImage(w, h);

		ConvertBufferedImage.convertFrom(colorLeft, inputLeft, true);
		ConvertBufferedImage.convertFrom(colorRight, inputRight, true);

		// When the input image has changed automatically scale and center
		imagePanel.autoScaleCenterOnSetImage = true;
		imagePanel.setPreferredSize(new Dimension(inputLeft.width, inputLeft.height));
		imagePanel.resetRoi();

		rectifyInputImages();

		BoofSwingUtil.invokeNowOrLater(() -> control.setImageSize(rectLeft.width, rectLeft.height));

		processDisparity();
	}

	private void createAlgConcurrent() {
		BoofConcurrency.USE_CONCURRENT = control.concurrent;
		activeAlg = control.controlDisparity.createAlgorithm();
		smoother = control.controlDisparity.createSmoother();
		BoofConcurrency.USE_CONCURRENT = true;
	}

	@Override
	public void changeView3D() {
		double baseline = calib.getRightToLeft().getT().norm();
		control.controlCloud.configure(pcv, baseline*10, baseline/30.0);
		pcv.getComponent().repaint();
	}

	@Override
	public void changeZoom() {
		imagePanel.setScale(control.zoom);
	}

	@Override
	public void changeBackgroundColor() {
		if (control.selectedView == 0) {
			disparityRender();
		} else if (control.selectedView == 3) {
			pcv.setBackgroundColor(control.controlCloud.getActiveBackgroundColor());
			pcv.getComponent().repaint();
		}
	}

	/**
	 * Displays images and sets the user select a region of interest
	 */
	public class DisparityImageView extends ImageZoomPanel implements MouseListener, MouseMotionListener {
		int x0, y0, x1, y1;

		int state = 0;

		public DisparityImageView() {
			resetRoi();

			panel.addMouseListener(this);
			panel.addMouseMotionListener(this);

			panel.addMouseWheelListener(e -> {
				setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e));
			});
		}

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			if (state == 0)
				return;
			BoofSwingUtil.antialiasing(g2);

			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.RED);

			int x0 = (int)Math.round(this.x0*scale);
			int y0 = (int)Math.round(this.y0*scale);
			int x1 = (int)Math.round(this.x1*scale);
			int y1 = (int)Math.round(this.y1*scale);

			g2.drawLine(x0, y0, x1, y0);
			g2.drawLine(x1, y0, x1, y1);
			g2.drawLine(x0, y1, x1, y1);
			g2.drawLine(x0, y0, x0, y1);
		}

		void resetRoi() {
			x0 = y0 = -1;
			x1 = y1 = Integer.MAX_VALUE;
		}

		@Override
		public void mouseClicked( MouseEvent e ) {
			// clear region if one has been selected
			if (state != 0) {
				boolean change = state == 2;
				state = 0;
				if (change) {
					computedCloud = false;
					changeImageView();
				}
				repaint();
			}
		}

		@Override
		public void mousePressed( MouseEvent e ) {
			panel.requestFocus();
			if (SwingUtilities.isLeftMouseButton(e)) {
				Point2D_F64 p = pixelToPoint(e.getX(), e.getY());

				// if shift it down the user is defining the ROI
				if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
					state = 1;
					x0 = x1 = (int)Math.round(p.x);
					y0 = y1 = (int)Math.round(p.y);
				} else {
					centerView(p.x, p.y);
				}
			}
		}

		@Override
		public void mouseReleased( MouseEvent e ) {
			// Finish defining the ROI, save it, and update the cloud
			if (state == 1) {
				Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
				x1 = (int)Math.round(p.x);
				y1 = (int)Math.round(p.y);

				// make sure the top left is the lower extent
				int x0 = Math.min(this.x0, this.x1);
				int x1 = Math.max(this.x0, this.x1);
				int y0 = Math.min(this.y0, this.y1);
				int y1 = Math.max(this.y0, this.y1);
				this.x0 = x0;
				this.x1 = x1;
				this.y0 = y0;
				this.y1 = y1;

				// tell it to update the view with the new region
				state = 2;
				computedCloud = false;
				changeImageView();
			}
		}

		@Override
		public void mouseEntered( MouseEvent e ) {}

		@Override
		public void mouseExited( MouseEvent e ) {
			// user exited, clear the ROI that's in progress
			if (state == 1) {
				resetRoi();
				state = 0;
				computedCloud = false;
				changeImageView();
			}
		}

		@Override
		public void mouseDragged( MouseEvent e ) {
			if (state == 1) {
				Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
				x1 = (int)Math.round(p.x);
				y1 = (int)Math.round(p.y);
				// just repaint what's needed. in theory
//				panel.repaint(x0-10,y0-10,x1-x0+10,y1-y0+10);
				panel.repaint();
			}
		}

		@Override
		public void mouseMoved( MouseEvent e ) {}
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public static class ProcessThread extends ProgressMonitorThread {
		int state = 0;

		public ProcessThread( JComponent owner ) {
			super(new ProgressMonitor(owner, "Computing Disparity", "", 0, 100));
		}

		@Override
		public void doRun() {
			SwingUtilities.invokeLater(() -> {
				monitor.setProgress(state);
				state = (++state%100);
			});
		}
	}

	public static void main( String[] args ) {
		String stereoCalib = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/stereo.yaml");

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Chair 1", stereoCalib
				, UtilIO.pathExample("stereo/chair01_left.jpg"), UtilIO.pathExample("stereo/chair01_right.jpg")));
//		inputs.add(new PathLabel("Chair 2",  new File(dirCalib,"stereo.yaml"),dirImgs+"chair02_left.jpg",dirImgs+"chair02_right.jpg"));
		examples.add(new PathLabel("Stones 1", stereoCalib
				, UtilIO.pathExample("stereo/stones01_left.jpg"), UtilIO.pathExample("stereo/stones01_right.jpg")));
		examples.add(new PathLabel("Lantern 1", stereoCalib
				, UtilIO.pathExample("stereo/lantern01_left.jpg"), UtilIO.pathExample("stereo/lantern01_right.jpg")));
		examples.add(new PathLabel("Wall 1", stereoCalib
				, UtilIO.pathExample("stereo/wall01_left.jpg"), UtilIO.pathExample("stereo/wall01_right.jpg")));
//		inputs.add(new PathLabel("Garden 1", new File(dirCalib,"stereo.yaml",dirImgs+"garden01_left.jpg",dirImgs+"garden01_right.jpg"));
		examples.add(new PathLabel("Garden 2", stereoCalib
				, UtilIO.pathExample("stereo/garden02_left.jpg"), UtilIO.pathExample("stereo/garden02_right.jpg")));
		examples.add(new PathLabel("Sundial 1", stereoCalib
				, UtilIO.pathExample("stereo/sundial01_left.jpg"), UtilIO.pathExample("stereo/sundial01_right.jpg")));

		SwingUtilities.invokeLater(() -> {
			VisualizeStereoDisparity app = new VisualizeStereoDisparity(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Stereo Disparity");
		});
	}
}
