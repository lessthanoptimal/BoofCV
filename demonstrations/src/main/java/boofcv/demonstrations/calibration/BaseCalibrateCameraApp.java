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

package boofcv.demonstrations.calibration;

import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.CalibrationModelPanel;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.settings.GlobalSettingsControls;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.VariableLockSet;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;
import static boofcv.gui.calibration.DisplayPinholeCalibrationPanel.drawNumbers;

/**
 * Base class for camera calibration applications
 *
 * @author Peter Abeles
 */
public abstract class BaseCalibrateCameraApp extends JPanel {
	// TODO Render preview of target type
	// TODO render undistorted
	// TODO show calibration in GUI
	// TODO statistics summary in GUI

	// TODO add ability to load previously saved results
	// TODO cache most recently viewed images

	protected JMenuBar menuBar;
	protected JMenuItem menuItemFile, menuItemWebcam, menuItemQuit;
	protected JMenu menuRecent;

	// Window the application is shown in
	protected JFrame window;

	boolean calibratorChanged = true;
	boolean targetChanged = true;

	//----------------------- GUI owned objects
	protected ConfigureInfoPanel configurePanel = new ConfigureInfoPanel();
	protected ImageListPanel imageListPanel = new ImageListPanel();
	protected ImageCalibrationPanel imagePanel = new ImageCalibrationPanel();
	//--------------------------------------------------------------------

	// True if a thread is running for calibration
	protected boolean runningCalibration = false;

	protected DetectorLocked detectorSet = new DetectorLocked();
	protected ResultsLocked results = new ResultsLocked();

	{
		BoofSwingUtil.initializeSwing();
	}

	public BaseCalibrateCameraApp() {
		imageListPanel.setPreferredSize(new Dimension(200, 200));
	}

	protected void createMenuBar() {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menuFile);

		this.menuItemFile = new JMenuItem("Open Images");
		BoofSwingUtil.setMenuItemKeys(menuItemFile, KeyEvent.VK_O, KeyEvent.VK_O);
		this.menuItemFile.addActionListener(( e ) -> openImages());
		menuFile.add(this.menuItemFile);

		menuRecent = new JMenu("Open Recent");
		menuFile.add(menuRecent);
		updateRecentItems();

		JMenuItem menuSettings = new JMenuItem("Settings");
		menuSettings.addActionListener(e -> new GlobalSettingsControls().showDialog(window, this));

		menuItemQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItemQuit.addActionListener((e -> System.exit(0)));
		BoofSwingUtil.setMenuItemKeys(menuItemQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);

		menuFile.addSeparator();
		menuFile.add(menuSettings);
		menuFile.add(menuItemQuit);
	}

	/**
	 * Disables or enables the GUI. This can be done to prevent people from changing settings while computing results
	 */
	public void setGuiEnabled( boolean enabled ) {
		// TODO needs to work inside and outside of the UI thread
	}

	public void setMenuBarEnabled( boolean enabled ) {
		menuBar.setEnabled(enabled);
	}

	protected void openImages() {
		File selected = BoofSwingUtil.openFileChooser(this, BoofSwingUtil.FileTypes.DIRECTORIES);
		if (selected == null)
			return;
		List<String> selectedImages = UtilIO.listImages(selected.getPath(), true);
		// Disable the menu bar so the user can't try to open more images
		setMenuBarEnabled(false);

		targetChanged = true;

		// Process the images in a non-gui thread
		new Thread(() -> handleProcessCalled(selectedImages), "OpenImages()").start();
	}

	protected void createAlgorithms() {
		detectorSet.safe(() -> {
			if (targetChanged)
				detectorSet.detector = configurePanel.targetPanel.createSingleTargetDetector();
			detectorSet.calibrator = new CalibrateMonoPlanar(detectorSet.detector.getLayout());
			configurePanel.modelPanel.configureCalibrator(detectorSet.calibrator);
		});

		targetChanged = false;
		calibratorChanged = false;
	}

	/**
	 * Detects image features from the set
	 */
	public void processFiles( List<String> foundImages ) {
		// reset all data structures
		results.reset();
		SwingUtilities.invokeLater(() -> imageListPanel.clearImages());

		// Load and detect calibration targets
		GrayF32 gray = new GrayF32(1, 1);
		for (String path : foundImages) {
			BufferedImage buffered = UtilImageIO.loadImage(path);
			if (buffered == null) {
				System.err.println("Failed to load image: " + path);
				continue;
			}

			// Convert to gray and detect the marker inside of it
			ConvertBufferedImage.convertFrom(buffered, gray);

			detectorSet.lock();
			boolean detected;
			CalibrationObservation observation;
			try {
				detected = detectorSet.detector.process(gray);
				observation = detectorSet.detector.getDetectedPoints();
			} catch (RuntimeException e) {
				e.printStackTrace(System.err);
				continue;
			} finally {
				detectorSet.unlock();
			}

			if (!detected) {
				System.out.println("Failed to find target in " + path);
			}

			// Record that it could process this image and display it in the GUI
			results.safe(() -> {
				results.imagePaths.add(path);
				results.imageObservations.put(path, observation);
				// only images with 4 points can be used in calibration
				if (observation.points.size() >= 4)
					results.usedImages.add(results.imagePaths.size() - 1);
			});
			SwingUtilities.invokeLater(() -> {
				imageListPanel.addImage(new File(path).getName(), detected);
				// This will show the viewed image, but it won't be "selected". Selecting it will cause the image to
				// be loaded again
				imagePanel.setBufferedImageNoChange(buffered);
			});
		}

		// Officially change the selected image
		SwingUtilities.invokeLater(() -> imageListPanel.setSelected(imageListPanel.imageNames.size() - 1));
	}

	protected void openImages( String directoryPath ) {
		setGuiEnabled(false);

		// Start a new thread to process everything in
		new Thread(() -> {
			imageListPanel.clearImages();
			List<String> foundImages = UtilIO.listSmartImages(directoryPath, true);
			if (foundImages.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No images found!");
			} else {
				processFiles(foundImages);
			}
			setGuiEnabled(true);
		}, "openImages(dir)").start();
	}

	/**
	 * Updates the list in recent menu
	 */
	protected void updateRecentItems() {
		if (menuRecent == null)
			return;
		menuRecent.removeAll();
		List<BoofSwingUtil.RecentFiles> recentFiles = BoofSwingUtil.getListOfRecentFiles(this);

		for (BoofSwingUtil.RecentFiles info : recentFiles) {
			JMenuItem recentItem = new JMenuItem(info.name);
			recentItem.addActionListener(e -> openImages(info.files.get(0)));
			menuRecent.add(recentItem);
		}

		// don't add clear option if there is nothing to clear
		if (recentFiles.size() == 0)
			return;

		// Add the option to clear the list of recent files
		JMenuItem clearItem = new JMenuItem("Clear Recent");
		clearItem.addActionListener(e -> {
			menuRecent.removeAll();
			BoofSwingUtil.saveRecentFiles(getClass().getSimpleName(), new ArrayList<>());
		});
		menuRecent.addSeparator();
		menuRecent.add(clearItem);
	}

	/**
	 * Change which image is being displayed. Request from GUI
	 */
	private void changeSelectedGUI( int index ) {
		BoofSwingUtil.checkGuiThread();

		// Change the item selected in the list
		imageListPanel.setSelected(index);

		String path = results.select(() -> results.imagePaths.get(index));
		BufferedImage image = UtilImageIO.loadImage(path);
		if (image == null) {
			System.err.println("Could not load image: " + path);
			return;
		}
		configurePanel.setImageSize(image.getWidth(), image.getHeight());
		imagePanel.setBufferedImageNoChange(image);
		imagePanel.repaint();
	}

	protected void handleProcessCalled( @Nullable List<String> imagePaths ) {
		BoofSwingUtil.checkNotGuiThread();
		boolean detectTargets = targetChanged;
		createAlgorithms();

		if (detectTargets) {
			// If null then it must want to reprocess the current set of images
			if (imagePaths == null) {
				List<String> l = new ArrayList<>();
				results.safe(() -> l.addAll(results.imagePaths));
				imagePaths = l;
			}

			System.out.println("Detecting targets again");
			// Disable the menu bar so the user can't try to open more images
			SwingUtilities.invokeLater(() -> setMenuBarEnabled(false));
			processFiles(imagePaths);
		}

		System.out.println("Computing calibration");
		calibrateFromCorners();
		SwingUtilities.invokeLater(() -> imagePanel.repaint());
	}

	protected void calibrateFromCorners() {
		BoofSwingUtil.checkNotGuiThread();
		if (runningCalibration)
			return;
		runningCalibration = true;
		detectorSet.lock();
		try {
			detectorSet.calibrator.reset();
			results.safe(() -> {
				for (int usedIdx = 0; usedIdx < results.usedImages.size(); usedIdx++) {
					String image = results.imagePaths.get(results.usedImages.get(usedIdx));
					detectorSet.calibrator.addImage(results.imageObservations.get(image));
				}
			});

			// Calibrate
			detectorSet.calibrator.process();

			// Save results for visualization
			results.safe(() -> {
				results.imageResults.clear();
				List<ImageResults> listResults = detectorSet.calibrator.getErrors();
				for (int i = 0; i < listResults.size(); i++) {
					String image = results.imagePaths.get(results.usedImages.get(i));
					results.imageResults.put(image, listResults.get(i));
				}
			});

			detectorSet.calibrator.printStatistics(System.out);
			SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(false));
		} catch (RuntimeException e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> BoofSwingUtil.warningDialog(this, e));
		} finally {
			runningCalibration = false;
			detectorSet.unlock();
		}
	}

	protected void settingsChanged( boolean target, boolean calibrator ) {
		BoofSwingUtil.checkGuiThread();
		targetChanged |= target;
		calibratorChanged |= calibrator;
		SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(true));
	}

	/** If an image is selected, it returns the observed calibration landmarks */
	protected @Nullable CalibrationObservation getObservationsForSelected() {
		BoofSwingUtil.checkGuiThread();

		int selected = imageListPanel.selectedImage;
		return results.select(() -> {
			if (selected < 0 || selected >= results.imagePaths.size())
				return null;
			return results.imageObservations.get(results.imagePaths.get(selected));
		});
	}

	protected @Nullable ImageResults getResultsForSelected() {
		BoofSwingUtil.checkGuiThread();

		int selected = imageListPanel.selectedImage;
		return results.select(() -> {
			if (selected < 0 || selected >= results.imagePaths.size())
				return null;
			return results.imageResults.get(results.imagePaths.get(selected));
		});
	}

	public static void renderOrder( Graphics2D g2, double scale, List<PointIndex2D_F64> points ) {
		g2.setStroke(new BasicStroke(5));

		Line2D.Double l = new Line2D.Double();

		for (int i = 0, j = 1; j < points.size(); i = j, j++) {
			Point2D_F64 p0 = points.get(i).p;
			Point2D_F64 p1 = points.get(j).p;

			double fraction = i/((double)points.size() - 2);
//			fraction = fraction * 0.8 + 0.1;

			int red = (int)(0xFF*fraction) + (int)(0x00*(1 - fraction));
			int green = 0x00;
			int blue = (int)(0x00*fraction) + (int)(0xff*(1 - fraction));

			int lineRGB = red << 16 | green << 8 | blue;

			l.setLine(scale*p0.x, scale*p0.y, scale*p1.x, scale*p1.y);

			g2.setColor(new Color(lineRGB));
			g2.draw(l);
		}
	}

	/**
	 * Displays and visualizes calibration information for a sepcific image
	 */
	protected class ImageCalibrationPanel extends ImageZoomPanel {
		Ellipse2D.Double ellipse = new Ellipse2D.Double();
		Point2D_F32 adj = new Point2D_F32();

		public ImageCalibrationPanel() {
			setWheelScrollingEnabled(false);
			panel.addMouseWheelListener(e -> setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e)));
		}

		@Override public synchronized void setScale( double scale ) {
			boolean changed = this.scale != scale;
			super.setScale(scale);
			if (changed)
				configurePanel.setZoom(scale);
		}

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			CalibrationObservation set = getObservationsForSelected();
			if (set == null)
				return;

			if (configurePanel.checkOrder.value) {
				renderOrder(g2, scale, set.points);
			}

			if (configurePanel.checkPoints.value) {
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(3));
				for (PointIndex2D_F64 p : set.points) {
					adj.setTo((float)p.p.x, (float)p.p.y);
					VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
				}
				g2.setStroke(new BasicStroke(1));
				g2.setColor(Color.RED);
				for (PointIndex2D_F64 p : set.points) {
					adj.setTo((float)p.p.x, (float)p.p.y);
					VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
				}
			}

			if (configurePanel.checkNumbers.value) {
				drawNumbers(g2, set.points, null, scale);
			}

			if (configurePanel.checkAll.value) {
				results.safe(() -> {
					for (String imageName : results.imagePaths) {
						CalibrationObservation l = results.imageObservations.get(imageName);
						for (PointIndex2D_F64 p : l.points) {
							adj.setTo((float)p.p.x, (float)p.p.y);
							VisualizeFeatures.drawPoint(g2, adj.x*scale, adj.y*scale, 3, Color.BLUE, Color.WHITE, ellipse);
						}
					}
				});
			}

			ImageResults results = getResultsForSelected();
			if (results == null)
				return;

			if (configurePanel.checkErrors.value) {
				double errorScale = configurePanel.selectErrorScale.value.doubleValue();
				g2.setStroke(new BasicStroke(4));
				g2.setColor(Color.BLACK);
				for (int i = 0; i < set.size(); i++) {
					PointIndex2D_F64 p = set.get(i);
					adj.setTo((float)p.p.x, (float)p.p.y);

					double r = errorScale*results.pointError[i];
					if (r < 1)
						continue;

					VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);
				}

				g2.setStroke(new BasicStroke(2.5f));
				g2.setColor(Color.ORANGE);
				for (int i = 0; i < set.size(); i++) {
					PointIndex2D_F64 p = set.get(i);
					adj.setTo((float)p.p.x, (float)p.p.y);

					double r = errorScale*results.pointError[i];
					if (r < 1)
						continue;


					VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r);
				}
			}
		}
	}

	/**
	 * List images used to calibrate the camera.
	 */
	protected class ImageListPanel extends StandardAlgConfigPanel implements ListSelectionListener {
		JList<String> imageList;
		List<String> imageNames = new ArrayList<>();
		DogArray_B imageSuccess = new DogArray_B();
		int selectedImage = -1;

		public ImageListPanel() {
			imageList = new JList<>();
			imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			imageList.addListSelectionListener(this);

			// Highlight images where it failed
			imageList.setCellRenderer(new DefaultListCellRenderer() {
				@Override public Component getListCellRendererComponent( JList list, Object value, int index,
																		 boolean isSelected, boolean cellHasFocus ) {
					Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if (!imageSuccess.get(index))
						setBackground(Color.RED);
					return c;
				}
			});

			JScrollPane scroll = new JScrollPane(imageList);

			add(scroll);
		}

		public void addImage( String imageName, boolean success ) {
			imageNames.add(imageName);
			this.imageSuccess.add(success);
			String[] names = imageNames.toArray(new String[0]);
			imageList.removeListSelectionListener(ImageListPanel.this);
			imageList.setListData(names);
			if (names.length == 1) {
				selectedImage = 0;
				imageList.addListSelectionListener(ImageListPanel.this);
				imageList.setSelectedIndex(selectedImage);
				validate();
			} else {
				// each time an image is added it resets the selected value
				imageList.setSelectedIndex(selectedImage);
				imageList.addListSelectionListener(ImageListPanel.this);
			}
		}

		public void clearImages() {
			imageNames.clear();
			imageSuccess.reset();
			imageList.removeListSelectionListener(ImageListPanel.this);
			imageList.setListData(new String[0]);
			selectedImage = -1;
		}

		public void setSelected( int index ) {
			if (imageList.getSelectedIndex() == index)
				return;
			imageList.setSelectedIndex(index);
		}

		@Override public void valueChanged( ListSelectionEvent e ) {
			if (e.getValueIsAdjusting() || e.getFirstIndex() == -1)
				return;

			int selected = imageList.getSelectedIndex();

			// See if there's no change
			if (selected == selectedImage)
				return;

			selectedImage = selected;
			changeSelectedGUI(selected);
		}
	}

	/**
	 * Provides controls to configure detection and calibration while also listing all the files
	 */
	protected class ConfigureInfoPanel extends StandardAlgConfigPanel {
		protected JSpinnerNumber zoom = spinnerWrap(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);
		protected JLabel imageSizeLabel = new JLabel();

		JButton bCompute = button("Compute", false);

		JCheckBoxValue checkPoints = checkboxWrap("Points", true).tt("Show calibration landmarks");
		JCheckBoxValue checkErrors = checkboxWrap("Errors", true).tt("Visualize residual errors");
		JCheckBoxValue checkUndistorted = checkboxWrap("Undistort", false).tt("Visualize undistorted image");
		JCheckBoxValue checkAll = checkboxWrap("All", false).tt("Show location of all landmarks in all images");
		JCheckBoxValue checkNumbers = checkboxWrap("Numbers", false).tt("Draw feature numbers");
		JCheckBoxValue checkOrder = checkboxWrap("Order", true).tt("Visualize landmark order");
		JSpinnerNumber selectErrorScale = spinnerWrap(10.0, 0.1, 1000.0, 2.0);

		CalibrationModelPanel modelPanel = new CalibrationModelPanel();
		CalibrationTargetPanel targetPanel = new CalibrationTargetPanel(( a, b ) -> settingsChanged(true, false));

		public ConfigureInfoPanel() {

			modelPanel.listener = () -> settingsChanged(false, true);

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Model", modelPanel);
			tabbedPane.addTab("Target", targetPanel);

			addLabeled(imageSizeLabel, "Image Size", "Size of image being viewed");
			addLabeled(zoom.spinner, "Zoom", "Zoom of image being viewed");
			addAlignCenter(bCompute, "Press to compute calibration with current settings.");
			add(createVisualFlagPanel());
			addLabeled(selectErrorScale.spinner, "Error Scale", "Increases the error visualization");
			add(tabbedPane);
		}

		private JPanel createVisualFlagPanel() {
			var panel = new JPanel(new GridLayout(0, 3));
			panel.setBorder(BorderFactory.createTitledBorder("Visual Flags"));

			panel.add(checkPoints.check);
			panel.add(checkErrors.check);
			panel.add(checkUndistorted.check);
			panel.add(checkAll.check);
			panel.add(checkNumbers.check);
			panel.add(checkOrder.check);

			panel.setMaximumSize(panel.getPreferredSize());

			return panel;
		}

		public void setZoom( double _zoom ) {
			_zoom = Math.max(MIN_ZOOM, _zoom);
			_zoom = Math.min(MAX_ZOOM, _zoom);
			if (_zoom == zoom.value.doubleValue())
				return;
			zoom.value = _zoom;

			BoofSwingUtil.invokeNowOrLater(() -> zoom.spinner.setValue(zoom.value));
		}

		public void setImageSize( final int width, final int height ) {
			BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width + " x " + height));
		}

		@Override public void controlChanged( final Object source ) {
			if (source == bCompute) {
				if (!runningCalibration) {
					new Thread(() -> handleProcessCalled(null), "bCompute").start();
				}
			} else if (source == zoom.spinner) {
				zoom.updateValue();
				imagePanel.setScale(zoom.vdouble());
			} else {
				if (source == checkPoints.check) {
					checkPoints.updateValue();
				} else if (source == checkErrors.check) {
					checkErrors.updateValue();
				} else if (source == checkUndistorted.check) {
					checkUndistorted.updateValue();
				} else if (source == checkAll.check) {
					checkAll.updateValue();
				} else if (source == checkNumbers.check) {
					checkNumbers.updateValue();
				} else if (source == checkOrder.check) {
					checkOrder.updateValue();
				} else if (source == selectErrorScale.spinner) {
					selectErrorScale.updateValue();
				}
				imagePanel.repaint();
			}
		}
	}

	private static class DetectorLocked extends VariableLockSet {
		protected DetectSingleFiducialCalibration detector;
		protected CalibrateMonoPlanar calibrator;
	}

	private static class ResultsLocked extends VariableLockSet {
		// Path to all input images
		protected final List<String> imagePaths = new ArrayList<>();
		// List of found observations and results
		protected final Map<String, CalibrationObservation> imageObservations = new HashMap<>();
		protected final Map<String, ImageResults> imageResults = new HashMap<>();
		// Index of images used when calibrating
		protected final DogArray_I32 usedImages = new DogArray_I32();

		public void reset() {
			safe(() -> {
				imagePaths.clear();
				imageObservations.clear();
				imageResults.clear();
				usedImages.reset();
			});
		}
	}
}
