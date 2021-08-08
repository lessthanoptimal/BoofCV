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
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.CalibrationModelPanel;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.settings.GlobalSettingsControls;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Base class for camera calibration applications
 *
 * @author Peter Abeles
 */
public abstract class BaseCalibrateCameraApp extends JPanel {
	// TODO Add visualization flags
	// TODO colorize images in file list if targets are not detected
	// TODO Render preview of target type
	// TODO render all stuff from before

	// TODO add ability to load previously saved results
	// TODO cache most recently viewed images

	protected JMenuBar menuBar;
	protected JMenuItem menuItemFile, menuItemWebcam, menuItemQuit;
	protected JMenu menuRecent;

	// Window the application is shown in
	protected JFrame window;

	boolean calibratorChanged = true;
	boolean targetChanged = true;

	/** Calibration target detector */
	protected DetectSingleFiducialCalibration detector;
	protected CalibrateMonoPlanar calibrator;
	protected boolean runningCalibration = false;
	protected ReentrantLock detectorLock = new ReentrantLock();

	//----------------------- GUI owned objects
	protected ConfigureInfoPanel configurePanel = new ConfigureInfoPanel();
	protected ImageListPanel imageListPanel = new ImageListPanel();
	protected ImageCalibrationPanel imagePanel = new ImageCalibrationPanel();

	// Path to all input images
	protected final List<String> imagePaths = new ArrayList<>();
	// List of found observations
	protected final Map<String, CalibrationObservation> imageObservations = new HashMap<>();
	//--------------------------------------------------------------------

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
		List<String> images = UtilIO.listImages(UtilIO.pathExample("calibration/fisheye/chessboard"), true);

		// Disable the menu bar so the user can't try to open more images
		setMenuBarEnabled(false);

		targetChanged = true;

		// Process the images in a non-gui thread
		new Thread(() -> handleProcessCalled()).start();
	}

	protected void createAlgorithms() {
		if (targetChanged)
			detector = configurePanel.targetPanel.createSingleTargetDetector();
		calibrator = new CalibrateMonoPlanar(detector.getLayout());
		configurePanel.modelPanel.configureCalibrator(calibrator);

		targetChanged = false;
		calibratorChanged = false;
	}

	/**
	 * Detects image features from the set
	 */
	public void processFiles( List<String> foundImages ) {
		// reset all data structures
		imagePaths.clear();
		imageObservations.clear();
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

			detectorLock.lock();
			boolean detected;
			try {
				detected = detector.process(gray);
			} catch (RuntimeException e) {
				e.printStackTrace(System.err);
				continue;
			} finally {
				detectorLock.unlock();
			}

			if (!detected) {
				System.out.println("Failed to find target in " + path);
			}

			// Record that it could process this image and display it in the GUI
			CalibrationObservation observation = detector.getDetectedPoints();
			SwingUtilities.invokeLater(() -> {
				imagePaths.add(path);
				imageObservations.put(path, observation);
				imageListPanel.addImage(new File(path).getName(), detected);
				imagePanel.setImage(buffered);
			});
		}
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
		}).start();
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

		String path = imagePaths.get(index);
		BufferedImage image = UtilImageIO.loadImage(path);
		if (image == null) {
			System.err.println("Could not laod image: "+path);
			return;
		}
		configurePanel.setImageSize(image.getWidth(), image.getHeight());
		imagePanel.setImage(image);
		imagePanel.repaint();
	}

	protected void handleProcessCalled() {
		BoofSwingUtil.checkNotGuiThread();
		boolean detectTargets = targetChanged;
		createAlgorithms();

		if (detectTargets) {
			System.out.println("Detecting targets again");
			// Disable the menu bar so the user can't try to open more images
			SwingUtilities.invokeLater(() -> setMenuBarEnabled(false));

			// pass in a copy since the original will be modified
			processFiles(new ArrayList<>(imagePaths));
		}

		System.out.println("Computing calibration");
		calibrateFromCorners();
	}

	protected void calibrateFromCorners() {
		BoofSwingUtil.checkNotGuiThread();
		if (runningCalibration)
			return;
		runningCalibration = true;
		detectorLock.lock();
		try {
			calibrator.reset();
			for (String image : imagePaths) {
				calibrator.addImage(imageObservations.get(image));
			}
			calibrator.process();
			calibrator.printStatistics(System.out);
			SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(false));
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
		} finally {
			runningCalibration = false;
			detectorLock.unlock();
		}
	}

	protected void settingsChanged( boolean target, boolean calibrator ) {
		BoofSwingUtil.checkGuiThread();
		targetChanged |= target;
		calibratorChanged |= calibrator;
		SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(true));
	}

	protected class ImageCalibrationPanel extends ImageZoomPanel {
		public ImageCalibrationPanel() {
			setWheelScrollingEnabled(false);
			panel.addMouseWheelListener(e -> {
				setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e));
			});
		}
	}

	protected class ImageListPanel extends StandardAlgConfigPanel implements ListSelectionListener {
		JList<String> imageList;
		List<String> imageNames = new ArrayList<>();
		int selectedImage = -1;

		public ImageListPanel() {
			imageList = new JList<>();
			imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			imageList.addListSelectionListener(this);
			JScrollPane scroll = new JScrollPane(imageList);

			add(scroll);
		}

		public void addImage( String imageName, boolean success ) {
			imageNames.add(imageName);
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
			add(tabbedPane);
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
					new Thread(BaseCalibrateCameraApp.this::handleProcessCalled).start();
				}
			} else if (source == zoom.spinner) {
				zoom.updateValue();
			}
		}
	}
}
