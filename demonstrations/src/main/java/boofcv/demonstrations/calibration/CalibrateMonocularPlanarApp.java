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
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.calibration.DisplayCalibrationPanel;
import boofcv.gui.calibration.DisplayFisheyeCalibrationPanel;
import boofcv.gui.calibration.DisplayPinholeCalibrationPanel;
import boofcv.gui.calibration.UtilCalibrationGui;
import boofcv.gui.controls.CalibrationModelPanel;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.gui.settings.GlobalSettingsControls;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.VariableLockSet;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraModelType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application for calibrating single cameras from planar targets. User can change the camera model and
 * target type from the GUI.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrateMonocularPlanarApp extends JPanel {
	public static final String CALIBRATION_TARGET = "calibration_target.yaml";
	public static final String INTRINSICS = "intrinsics.yaml";

	public JMenuBar menuBar;
	protected JMenu menuRecent;

	// Window the application is shown in
	public JFrame window;

	boolean calibratorChanged = true;
	boolean targetChanged = true;
	// if true the landmarks have been modified and it should not display results
	boolean resultsInvalid;

	//----------------------- GUI owned objects
	protected @Getter ConfigureInfoPanel configurePanel = new ConfigureInfoPanel();
	protected CalibrationListPanel imageListPanel = createImageListPanel();
	//	protected ImageCalibrationPanel imagePanel = new ImageCalibrationPanel();
	protected DisplayFisheyeCalibrationPanel fisheyePanel = new DisplayFisheyeCalibrationPanel();
	protected DisplayPinholeCalibrationPanel pinholePanel = new DisplayPinholeCalibrationPanel();
	protected boolean cameraIsPinhole = true;

	//--------------------------------------------------------------------

	// Directory where images were loaded from
	File imageDirectory = new File(".");

	// True if a thread is running for calibration
	protected boolean runningCalibration = false;

	protected DetectorLocked detectorSet = new DetectorLocked();
	protected ResultsLocked results = new ResultsLocked();

	{
		BoofSwingUtil.initializeSwing();
	}

	public CalibrateMonocularPlanarApp() {
		setLayout(new BorderLayout());
		imageListPanel.setPreferredSize(new Dimension(200, 200));

		// When these images change value pass on the scale
		fisheyePanel.setScale = ( scale ) -> configurePanel.setZoom(scale);
		pinholePanel.setScale = ( scale ) -> configurePanel.setZoom(scale);

		updateVisualizationSettings();
		getCalibrationPanel().setPreferredSize(new Dimension(600, 600));

		createMenuBar();
		add(configurePanel, BorderLayout.WEST);
		add(imageListPanel, BorderLayout.EAST);
		add(getCalibrationPanel(), BorderLayout.CENTER);

		createAlgorithms();
	}

	protected void createMenuBar() {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menuFile);

		var menuItemFile = new JMenuItem("Open Images");
		BoofSwingUtil.setMenuItemKeys(menuItemFile, KeyEvent.VK_O, KeyEvent.VK_O);
		menuItemFile.addActionListener(( e ) -> openImages());
		menuFile.add(menuItemFile);

		menuRecent = new JMenu("Open Recent");
		menuFile.add(menuRecent);
		updateRecentItems();

		var menuItemSaveCalibration = new JMenuItem("Save Intrinsics");
		BoofSwingUtil.setMenuItemKeys(menuItemSaveCalibration, KeyEvent.VK_S, KeyEvent.VK_S);
		menuItemSaveCalibration.addActionListener(( e ) ->
				saveIntrinsics(this, imageDirectory, detectorSet.calibrator.getIntrinsic()));
		menuFile.add(menuItemSaveCalibration);

		var menuItemSaveLandmarks = new JMenuItem("Save Landmarks");
		menuItemSaveLandmarks.addActionListener(( e ) -> saveLandmarks());
		menuFile.add(menuItemSaveLandmarks);

		var menuItemSaveTarget = new JMenuItem("Save Target");
		menuItemSaveTarget.addActionListener(( e ) -> saveCalibrationTarget(this, imageDirectory,
				configurePanel.targetPanel.createConfigCalibrationTarget()));
		menuFile.add(menuItemSaveTarget);

		JMenuItem menuSettings = new JMenuItem("Settings");
		menuSettings.addActionListener(e -> new GlobalSettingsControls().showDialog(window, this));

		var menuItemQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuItemQuit.addActionListener((e -> System.exit(0)));
		BoofSwingUtil.setMenuItemKeys(menuItemQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);

		menuFile.addSeparator();
		menuFile.add(menuSettings);
		menuFile.add(menuItemQuit);
	}

	/**
	 * Adds a new menu for examples
	 */
	public void addExamples( List<PathLabel> examples ) {
		JMenu menuExamples = new JMenu("Examples");

		for (PathLabel p : examples) {
			var menuItem = new JMenuItem(p.label);
			menuItem.addActionListener(( e ) -> processDirectory(new File(p.path[0])));
			menuExamples.add(menuItem);
		}

		menuBar.add(menuExamples);
	}

	/**
	 * Let the user select a directory to save detected landmarks
	 */
	protected void saveLandmarks() {
		// Open a dialog which will save using the default name in the place images were recently loaded from
		var chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setSelectedFile(imageDirectory);
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		// Make sure the directory exists
		File destination = chooser.getSelectedFile();
		if (!destination.exists())
			BoofMiscOps.checkTrue(destination.mkdirs());

		BoofMiscOps.checkTrue(!destination.isFile(), "Can't select a file as output");

		try {
			results.safe(() -> {
				String detectorName = detectorSet.select(() -> detectorSet.detector.getClass().getSimpleName());
				for (String imageName : results.imagePaths) {
					String outputName = FilenameUtils.getBaseName(imageName) + ".csv";
					CalibrationIO.saveLandmarksCsv(imageName, detectorName, results.getObservation(imageName),
							new File(destination, outputName));
				}
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	/**
	 * Saves found intrinsic parameters
	 */
	protected static void saveIntrinsics( JComponent owner, File directory, Object calibration ) {
		boolean mono = calibration instanceof CameraModel;
		var chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("yaml", "yaml", "yml"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setSelectedFile(new File(directory, mono ? INTRINSICS : "stereo.yaml"));
		int returnVal = chooser.showSaveDialog(owner);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		try {
			if (mono) {
				CalibrationIO.save((CameraModel)calibration, chooser.getSelectedFile());
			} else {
				CalibrationIO.save((StereoParameters)calibration, chooser.getSelectedFile());
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(owner, e);
		}
	}

	/**
	 * Saves a calibration target description to disk so that it can be loaded again later on.
	 */
	protected static void saveCalibrationTarget( Component owner, File imageDirectory, ConfigCalibrationTarget target ) {
		// Open a dialog which will save using the default name in the place images were recently loaded from
		var chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("yaml", "yaml", "yml"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setCurrentDirectory(imageDirectory);
		chooser.setSelectedFile(new File(imageDirectory, CALIBRATION_TARGET));
		int returnVal = chooser.showSaveDialog(owner);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}

		try {
			File file = chooser.getSelectedFile();
			UtilIO.saveConfig(target, new ConfigCalibrationTarget(), file);
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(owner, e);
		}
	}

	public void setMenuBarEnabled( boolean enabled ) {
		menuBar.setEnabled(enabled);
	}

	protected void openImages() {
		File selected = BoofSwingUtil.openFileChooser(this, BoofSwingUtil.FileTypes.DIRECTORIES);
		if (selected == null)
			return;
		processDirectory(selected);
	}

	/**
	 * Change camera model
	 */
	protected void createAlgorithms() {
		detectorSet.safe(() -> {
			if (targetChanged)
				detectorSet.detector = configurePanel.targetPanel.createSingleTargetDetector();
			detectorSet.calibrator = new CalibrateMonoPlanar(detectorSet.detector.getLayout());
			configurePanel.modelPanel.configureCalibrator(detectorSet.calibrator);
		});

		// See which type of camera model is active
		boolean isPinhole = configurePanel.modelPanel.selected == CameraModelType.BROWN;

		// Switch image visualization if it's a pinhole or fisheye model
		SwingUtilities.invokeLater(() -> {
			if (isPinhole != cameraIsPinhole) {
				// Pass in the image to the now active panel
				BufferedImage image = getCalibrationPanel().getImage();
				remove(getCalibrationPanel());
				cameraIsPinhole = isPinhole;
				add(getCalibrationPanel(), BorderLayout.CENTER);

				// Synchronize the image panel's state with the latest settings
				getCalibrationPanel().setBufferedImageNoChange(image);
				getCalibrationPanel().setScale(configurePanel.zoom.vdouble());
				updateVisualizationSettings();
				validate();
			}
		});


		targetChanged = false;
		calibratorChanged = false;
		resultsInvalid = true;
	}

	/**
	 * Detects image features from the set
	 */
	public void processDirectory( File directory ) {
		List<String> selectedImages = UtilIO.listImages(directory.getPath(), true);
		if (selectedImages.isEmpty())
			return;

		BoofSwingUtil.invokeNowOrLater(() -> {
			// Disable the menu bar so the user can't try to open more images
			setMenuBarEnabled(false);
			// Add to list of recently opened directories
			BoofSwingUtil.addToRecentFiles(this, directory.getName(), BoofMiscOps.asList(directory.getPath()));
			updateRecentItems();
		});

		processImages(directory, selectedImages);
	}

	/**
	 * Detects image features from the set
	 */
	public void processImages( File directory, List<String> selectedImages ) {
		loadDefaultTarget(directory, configurePanel.targetPanel);

		imageDirectory = directory;
		targetChanged = true;

		// We need to launch the processing thread from the UI thread since it might have loaded the calibration
		// target and that won't take effect until the UI thread runs
		SwingUtilities.invokeLater(() ->
				// Process the images in a non-gui thread
				new Thread(() -> handleProcessCalled(selectedImages), "OpenImages()").start());
	}

	protected static boolean loadDefaultTarget( File directory, CalibrationTargetPanel panel ) {
		// If the calibration target type is specified load that
		var fileTarget = new File(directory, CALIBRATION_TARGET);
		try {
			// Just try to load it. Checking to see if it exists will fail inside a jar where it will always return
			// false.
			ConfigCalibrationTarget config = UtilIO.loadConfig(fileTarget);
			BoofSwingUtil.invokeNowOrLater(() -> panel.setConfigurationTo(config));
			return true;
		} catch (RuntimeException ignore) {}
		return false;
	}

	protected DisplayCalibrationPanel getCalibrationPanel() {
		return cameraIsPinhole ? pinholePanel : fisheyePanel;
	}

	/**
	 * Updates the list in recent menu
	 */
	protected void updateRecentItems() {
		BoofSwingUtil.updateRecentItems(this, menuRecent, ( info ) -> processDirectory(new File(info.files.get(0))));
	}

	/**
	 * Change which image is being displayed. Request from GUI
	 */
	private void changeSelectedGUI( int index ) {
		if (index < 0 || index >= results.imagePaths.size())
			return;

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
		getCalibrationPanel().setBufferedImageNoChange(image);

		CalibrationObservation imageObservations = getObservationsForSelected();
		ImageResults imageResults = getResultsForSelected();
		if (imageObservations == null || imageResults == null)
			return;
		getCalibrationPanel().setResults(imageObservations, imageResults, results.allUsedObservations);
		getCalibrationPanel().repaint();
	}

	/**
	 * Handle the user clicking on the process button. This will either detect landmarks AND calibrate or just
	 * calibrate using existing features.
	 *
	 * @param imagePaths List of images or null if it should use existing landmarks.
	 */
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

			// Disable the menu bar so the user can't try to open more images
			SwingUtilities.invokeLater(() -> setMenuBarEnabled(false));
			detectLandmarksInImages(imagePaths);
		}

		calibrateFromCorners();
		SwingUtilities.invokeLater(() -> getCalibrationPanel().repaint());
	}

	/**
	 * Detects image features from the set
	 */
	protected void detectLandmarksInImages( List<String> foundImages ) {
		// reset all data structures
		results.reset();
		SwingUtilities.invokeLater(() -> {
			imageListPanel.clearImages();
			getCalibrationPanel().clearCalibration();
			getCalibrationPanel().clearResults();
		});

		// Let the user configure verbose output to stdout
		detectorSet.safe(() -> BoofSwingUtil.setVerboseWithDemoSettings(detectorSet.calibrator));

		// Load and detect calibration targets
		GrayF32 gray = new GrayF32(1, 1);
		for (String path : foundImages) {
			BufferedImage buffered = UtilImageIO.loadImage(path);
			if (buffered == null) {
				System.err.println("Failed to load image: " + path);
				continue;
			}

			// Convert to gray and detect the marker inside it
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

			// Order matters for visualization later on
			Collections.sort(observation.points, Comparator.comparingInt(a -> a.index));

			// Record that it could process this image and display it in the GUI
			results.safe(() -> {
				results.imagePaths.add(path);
				results.imageObservations.put(path, observation);
				// only images with 4 points can be used in calibration
				if (observation.points.size() >= 4) {
					results.usedImages.add(results.imagePaths.size() - 1);
					results.allUsedObservations.add(observation);
				}
				// need to create a copy since the copy being passed in to the other structures might be modified
				// later on
				results.originalObservations.grow().setTo(observation);
			});
			SwingUtilities.invokeLater(() -> {
				imageListPanel.addImage(new File(path).getName(), detected);
				// This will show the viewed image, but it won't be "selected". Selecting it will cause the image to
				// be loaded again
				getCalibrationPanel().setBufferedImageNoChange(buffered);
				getCalibrationPanel().repaint();
			});
		}

		// Officially change the selected image
		SwingUtilities.invokeLater(() -> imageListPanel.setSelected(imageListPanel.imageNames.size() - 1));
	}

	protected void calibrateFromCorners() {
		BoofSwingUtil.checkNotGuiThread();
		if (runningCalibration)
			return;

		SwingUtilities.invokeLater(() -> getCalibrationPanel().clearCalibration());

		runningCalibration = true;
		detectorSet.lock();
		// by default assume the calibration will be unsuccessful
		detectorSet.calibrationSuccess = false;
		try {
			detectorSet.calibrator.reset();
			results.safe(() -> {
				for (int usedIdx = 0; usedIdx < results.usedImages.size(); usedIdx++) {
					String image = results.imagePaths.get(results.usedImages.get(usedIdx));
					detectorSet.calibrator.addImage(results.getObservation(image));
				}
			});

			// Calibrate
			detectorSet.calibrator.process();
			detectorSet.calibrationSuccess = true;
			resultsInvalid = false;

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
		} catch (RuntimeException e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> BoofSwingUtil.warningDialog(this, e));
			return;
		} finally {
			runningCalibration = false;
			detectorSet.unlock();
		}

		displayCalibrationResults();
		showStatsToUser();
	}

	private void displayCalibrationResults() {
		SwingUtilities.invokeLater(() -> detectorSet.safe(() -> {
			// pass in the new calibrated camera
			CameraModel foundIntrinsics = detectorSet.calibrator.getIntrinsic();
			if (cameraIsPinhole) {
				pinholePanel.setCalibration((CameraPinholeBrown)foundIntrinsics);
			} else {
				LensDistortionWideFOV model = LensDistortionFactory.wide(foundIntrinsics);
				fisheyePanel.setCalibration(model, foundIntrinsics.width, foundIntrinsics.height);
			}
			configurePanel.bCompute.setEnabled(false);

			// Force it to redraw with new image features
			int selected = imageListPanel.imageList.getSelectedIndex();
			imageListPanel.imageList.clearSelection();
			imageListPanel.setSelected(selected);

			// Show the user the found calibration parameters. Format a bit to make it look nicer
			String text = foundIntrinsics.toString().replace(',', '\n').replace("{", "\n ");
			text = text.replace('}', '\n');
			configurePanel.textAreaCalib.setText(text);
		}));
	}

	/** Format statistics on results and add to a text panel */
	private void showStatsToUser() {
		results.safe(() -> {
			double averageError = 0.0;
			double maxError = 0.0;
			for (int i = 0; i < results.usedImages.size(); i++) {
				String image = results.imagePaths.get(results.usedImages.get(i));
				ImageResults r = results.getResults(image);
				averageError += r.meanError;
				maxError = Math.max(maxError, r.maxError);
			}
			averageError /= results.usedImages.size();
			String text = String.format("Reprojection Errors (px):\n\nmean=%.3f max=%.3f\n\n", averageError, maxError);
			text += String.format("%-10s | %8s\n", "image", "max (px)");
			for (int i = 0; i < results.usedImages.size(); i++) {
				String image = results.imagePaths.get(results.usedImages.get(i));
				ImageResults r = results.getResults(image);
				text += String.format("%-12s %8.3f\n", new File(image).getName(), r.maxError);
			}

			String _text = text;
			SwingUtilities.invokeLater(() -> {
				configurePanel.textAreaStats.setText(_text);
				configurePanel.textAreaStats.setCaretPosition(0); // show the top where summary stats are
			});
		});
	}

	protected void settingsChanged( boolean target, boolean calibrator ) {
		BoofSwingUtil.checkGuiThread();
		targetChanged |= target;
		calibratorChanged |= calibrator;
		SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(true));
	}

	/** Removes the selected point or does nothing if nothing is selected */
	protected void removePoint() {
		int whichPoint = getCalibrationPanel().getSelectedObservation();
		if (whichPoint < 0)
			return;

		CalibrationObservation observation = getCalibrationPanel().getObservation();
		if (observation == null)
			return;

		resultsInvalid = true;

		results.safe(() -> {
			if (whichPoint >= observation.points.size())
				return;
			observation.points.remove(whichPoint);
			if (observation.points.size() < 4) {
				removeImage();
			}
		});

		SwingUtilities.invokeLater(() -> {
			// Remove the results since they are no longer valid
			getCalibrationPanel().results = null;
			getCalibrationPanel().deselectPoint();
			configurePanel.bCompute.setEnabled(true);
			getCalibrationPanel().repaint();
		});
	}

	/** Removes an image */
	protected void removeImage() {
		BoofSwingUtil.invokeNowOrLater(() -> {
			int selected = imageListPanel.imageList.getSelectedIndex();
			if (selected < 0)
				return;

			// If the image isn't "used" don't remove it
			if (!imageListPanel.imageSuccess.get(selected))
				return;

			resultsInvalid = true;

			// Mark it as not used in the UI
			imageListPanel.imageSuccess.set(selected, false);

			results.safe(() -> {
				// Remove all points from this image, which will remove it from the active list
				String image = results.imagePaths.get(selected);
				results.getObservation(image).points.clear();

				// This image is no longer used for calibration
				int usedIdx = results.usedImages.indexOf(selected);
				if (usedIdx >= 0)
					results.usedImages.remove(usedIdx);
			});

			// Visually show the changes
			getCalibrationPanel().results = null;
			configurePanel.bCompute.setEnabled(true);
			getCalibrationPanel().repaint();
		});
	}

	/** Adds all images and points back in */
	protected void undoAllRemove() {
		resultsInvalid = true;
		results.safe(() -> {
			// we will re-generate the used image list
			results.usedImages.reset();
			for (int i = 0; i < results.originalObservations.size; i++) {
				// Revert by mindlessly copying
				CalibrationObservation o = results.originalObservations.get(i);
				String image = results.imagePaths.get(i);
				results.getObservation(image).setTo(o);

				// If the image has enough points, use it
				if (o.size() >= 4)
					results.usedImages.add(i);
			}
		});

		// Update the list of which images can be used in the UI
		BoofSwingUtil.invokeNowOrLater(() -> {
			results.safe(() -> {
				for (int i = 0; i < results.usedImages.size; i++) {
					int which = results.usedImages.get(i);
					imageListPanel.imageSuccess.set(which, true);
				}
			});

			// Visually show the changes
			configurePanel.bCompute.setEnabled(true);
			getCalibrationPanel().repaint();
			imageListPanel.repaint();
		});
	}

	/** If an image is selected, it returns the observed calibration landmarks */
	protected @Nullable CalibrationObservation getObservationsForSelected() {
		BoofSwingUtil.checkGuiThread();

		int selected = imageListPanel.selectedImage;
		return results.selectNull(() -> {
			if (selected < 0 || selected >= results.imagePaths.size())
				return null;
			return results.imageObservations.get(results.imagePaths.get(selected));
		});
	}


	protected @Nullable ImageResults getResultsForSelected() {
		BoofSwingUtil.checkGuiThread();

		if (resultsInvalid)
			return null;

		int selected = imageListPanel.selectedImage;
		return results.selectNull(() -> {
			if (selected < 0 || selected >= results.imagePaths.size())
				return null;
			return results.getResults(results.imagePaths.get(selected));
		});
	}

	protected void updateVisualizationSettings() {
		DisplayCalibrationPanel panel = getCalibrationPanel();
		panel.setDisplay(configurePanel.checkPoints.value, configurePanel.checkErrors.value,
				configurePanel.checkUndistorted.value, configurePanel.checkAll.value,
				configurePanel.checkNumbers.value, configurePanel.checkOrder.value, configurePanel.selectErrorScale.vdouble());
		panel.showResiduals = configurePanel.checkResidual.value;
		panel.repaint();
	}

	/**
	 * Creates and configures a panel for displaying images names and control buttons for removing points/images
	 */
	protected CalibrationListPanel createImageListPanel() {
		var panel = new CalibrationListPanel();
		panel.bRemovePoint.addActionListener(( e ) -> removePoint());
		panel.bRemoveImage.addActionListener(( e ) -> removeImage());
		panel.bReset.addActionListener(( e ) -> undoAllRemove());
		panel.selectionChanged = this::changeSelectedGUI;
		return panel;
	}

	/**
	 * Provides controls to configure detection and calibration while also listing all the files
	 */
	public class ConfigureInfoPanel extends StandardAlgConfigPanel {
		protected JSpinnerNumber zoom = spinnerWrap(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);
		protected JLabel imageSizeLabel = new JLabel();

		JButton bCompute = button("Compute", false);

		JCheckBoxValue checkPoints = checkboxWrap("Points", true).tt("Show calibration landmarks");
		JCheckBoxValue checkResidual = checkboxWrap("Residual", false).tt("Line showing residual exactly");
		JCheckBoxValue checkErrors = checkboxWrap("Errors", true).tt("Exaggerated residual errors");
		JCheckBoxValue checkUndistorted = checkboxWrap("Undistort", false).tt("Visualize undistorted image");
		JCheckBoxValue checkAll = checkboxWrap("All", false).tt("Show location of all landmarks in all images");
		JCheckBoxValue checkNumbers = checkboxWrap("Numbers", false).tt("Draw feature numbers");
		JCheckBoxValue checkOrder = checkboxWrap("Order", true).tt("Visualize landmark order");
		JSpinnerNumber selectErrorScale = spinnerWrap(10.0, 0.1, 1000.0, 2.0);

		@Getter CalibrationModelPanel modelPanel = new CalibrationModelPanel();
		@Getter CalibrationTargetPanel targetPanel = new CalibrationTargetPanel(( a, b ) -> handleUpdatedTarget());
		// Displays a preview of the calibration target
		ImagePanel targetPreviewPanel = new ImagePanel();
		// Displays calibration information
		JTextArea textAreaCalib = new JTextArea();
		JTextArea textAreaStats = new JTextArea();

		public ConfigureInfoPanel() {
			configureTextArea(textAreaCalib);
			configureTextArea(textAreaStats);

			modelPanel.listener = () -> settingsChanged(false, true);

			targetPreviewPanel.setScaling(ScaleOptions.DOWN);
			targetPreviewPanel.setCentering(true);
			targetPreviewPanel.setPreferredSize(new Dimension(200, 300));
			var targetVerticalPanel = new JPanel(new BorderLayout());
			targetVerticalPanel.add(targetPanel, BorderLayout.NORTH);
			targetVerticalPanel.add(targetPreviewPanel, BorderLayout.CENTER);
			handleUpdatedTarget();

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Model", modelPanel);
			tabbedPane.addTab("Target", targetVerticalPanel);
			tabbedPane.addTab("Calib", new JScrollPane(textAreaCalib));
			tabbedPane.addTab("Stats", new JScrollPane(textAreaStats));

			addLabeled(imageSizeLabel, "Image Size", "Size of image being viewed");
			addLabeled(zoom.spinner, "Zoom", "Zoom of image being viewed");
			addAlignCenter(bCompute, "Press to compute calibration with current settings.");
			add(createVisualFlagPanel());
			addLabeled(selectErrorScale.spinner, "Error Scale", "Increases the error visualization");
			add(tabbedPane);
		}

		private void configureTextArea( JTextArea textAreaCalib ) {
			textAreaCalib.setEditable(false);
			textAreaCalib.setWrapStyleWord(true);
			textAreaCalib.setLineWrap(true);
			textAreaCalib.setFont(new Font("monospaced", Font.PLAIN, 12));
		}

		private void handleUpdatedTarget() {
			BufferedImage preview = UtilCalibrationGui.renderTargetBuffered(
					targetPanel.selected, targetPanel.getActiveConfig(), 40);
			targetPreviewPanel.setImageUI(preview);
			settingsChanged(true, false);
		}

		private JPanel createVisualFlagPanel() {
			var panel = new JPanel(new GridLayout(0, 3));
			panel.setBorder(BorderFactory.createTitledBorder("Visual Flags"));

			panel.add(checkPoints.check);
			panel.add(checkErrors.check);
			panel.add(checkUndistorted.check);
			panel.add(checkResidual.check);
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
				getCalibrationPanel().setScale(zoom.vdouble());
			} else {
				updateVisualizationSettings();
			}
		}
	}

	private static class DetectorLocked extends VariableLockSet {
		protected DetectSingleFiducialCalibration detector;
		protected CalibrateMonoPlanar calibrator;
		protected boolean calibrationSuccess;
	}

	private static class ResultsLocked extends VariableLockSet {
		// Path to all input images
		protected final List<String> imagePaths = new ArrayList<>();
		// List of found observations and results
		protected final Map<String, CalibrationObservation> imageObservations = new HashMap<>();
		protected final Map<String, ImageResults> imageResults = new HashMap<>();
		// All observations with at least 4 points
		protected final List<CalibrationObservation> allUsedObservations = new ArrayList<>();
		// Index of images used when calibrating
		protected final DogArray_I32 usedImages = new DogArray_I32();
		// Copy of original observation before any edits
		protected final DogArray<CalibrationObservation> originalObservations = new DogArray<>(CalibrationObservation::new);

		public CalibrationObservation getObservation( String key ) {
			return Objects.requireNonNull(imageObservations.get(key));
		}

		public ImageResults getResults( String key ) {
			return Objects.requireNonNull(imageResults.get(key));
		}

		public void reset() {
			safe(() -> {
				imagePaths.clear();
				imageObservations.clear();
				imageResults.clear();
				usedImages.reset();
				allUsedObservations.clear();
				originalObservations.reset();
			});
		}
	}

	public static void main( @Nullable String[] args ) {
		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateMonocularPlanarApp();

			app.window = ShowImages.showWindow(app, "Monocular Planar Calibration", true);
			app.window.setJMenuBar(app.menuBar);
		});
	}
}
