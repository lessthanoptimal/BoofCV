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

import boofcv.abst.geo.calibration.CalibrateStereoPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyFillType;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.calibration.*;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.controls.ControlPanelPinhole;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.dialogs.OpenStereoSequencesChooser;
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
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;

import static boofcv.demonstrations.calibration.CalibrateMonocularPlanarApp.saveCalibrationTarget;
import static boofcv.demonstrations.calibration.CalibrateMonocularPlanarApp.saveIntrinsics;
import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application that lets you change calibration and target settings and recalibrate stereo image sets.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CalibrateStereoPlanarApp extends JPanel {
	protected @Nullable StereoImageSet inputImages;
	protected final Object lockInput = new Object();

	AlgorithmsLocked algorithms = new AlgorithmsLocked();
	ResultsLocked results = new ResultsLocked();

	//--------------------- GUI owned thread
	public JMenuBar menuBar;
	protected JMenu menuRecent;
	public JFrame window;
	StereoCalibrationPanel stereoPanel = new StereoCalibrationPanel();
	protected @Getter ConfigureInfoPanel configurePanel = new ConfigureInfoPanel();
	protected CalibrationListPanel imageListPanel = createImageListPanel();
	//--------------------------------------------------------------------

	// Directory where input images came from. Used as a default output in some situations
	File sourceDirectory = new File(".");

	// True if a thread is running for calibration
	protected boolean runningCalibration = false;

	// Specifies if the user changed calibration settings
	boolean calibratorChanged = true;
	boolean targetChanged = true;
	// if true the landmarks have been modified and it should not display results
	boolean resultsInvalid;

	{
		BoofSwingUtil.initializeSwing();
	}

	public CalibrateStereoPlanarApp() {
		setLayout(new BorderLayout());

		stereoPanel.panelLeft.setScale = ( scale ) -> configurePanel.setZoom(scale);
		stereoPanel.panelRight.setScale = ( scale ) -> configurePanel.setZoom(scale);

		stereoPanel.setPreferredSize(new Dimension(1000, 720));
		updateVisualizationSettings();

		createAlgorithms();
		add(imageListPanel, BorderLayout.EAST);
		add(configurePanel, BorderLayout.WEST);
		add(stereoPanel, BorderLayout.CENTER);

		createMenuBar();
	}

	protected void createMenuBar() {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menuFile);

		var menuItemFile = new JMenuItem("Open Images");
		BoofSwingUtil.setMenuItemKeys(menuItemFile, KeyEvent.VK_O, KeyEvent.VK_O);
		menuItemFile.addActionListener(( e ) -> openDialog());
		menuFile.add(menuItemFile);

		menuRecent = new JMenu("Open Recent");
		menuFile.add(menuRecent);
		updateRecentItems();

		var menuItemSaveCalibration = new JMenuItem("Save Calibration");
		BoofSwingUtil.setMenuItemKeys(menuItemSaveCalibration, KeyEvent.VK_S, KeyEvent.VK_S);
		menuItemSaveCalibration.addActionListener(( e ) -> algorithms.safe(() -> {
			if (algorithms.calibrationSuccess)
				saveIntrinsics(this, sourceDirectory, algorithms.parameters);
		}));
		menuFile.add(menuItemSaveCalibration);

		var menuItemSaveLandmarks = new JMenuItem("Save Landmarks");
		menuItemSaveLandmarks.addActionListener(( e ) -> saveLandmarks());
		menuFile.add(menuItemSaveLandmarks);

		var menuItemSaveTarget = new JMenuItem("Save Target");
		menuItemSaveTarget.addActionListener(( e ) -> saveCalibrationTarget
				(this, sourceDirectory, configurePanel.targetPanel.createConfigCalibrationTarget()));
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
			menuItem.addActionListener(( e ) -> {
				checkDefaultTarget(new File(p.getPath()));
				new Thread(() -> processExample(p.getPath())).start();
			});
			menuExamples.add(menuItem);
		}

		menuBar.add(menuExamples);
	}

	protected void updateRecentItems() {
		BoofSwingUtil.updateRecentItems(this, menuRecent, ( info ) -> {
			if (info.files.size() == 1) {
				List<String> images = UtilIO.listSmartImages(info.files.get(0), false);
				if (images.isEmpty()) {
					return;
				}

				checkDefaultTarget(new File(info.files.get(0)));
				Integer splitX = determineSplit(images);
				if (splitX == null) return;

				// disable menu bar while in the UI thread
				setMenuBarEnabled(false);
				new Thread(() -> process(images, splitX), "Recent Item").start();
			} else {
				List<String> left = UtilIO.listSmartImages(info.files.get(0), false);
				List<String> right = UtilIO.listSmartImages(info.files.get(1), false);

				if (left.isEmpty() || left.size() != right.size()) {
					System.err.println("Bad image sets. left=" + left.size() + " right=" + right.size());
					return;
				}

				checkDefaultTarget(new File(info.files.get(0)));

				// disable menu bar while in the UI thread
				setMenuBarEnabled(false);
				new Thread(() -> process(left, right), "Recent Item").start();
			}
		});
	}

	protected void saveLandmarks() {
		// Open a dialog which will save using the default name in the place images were recently loaded from
		var chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setSelectedFile(sourceDirectory);
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
			saveLandmarks(results.left, true, destination);
			saveLandmarks(results.right, false, destination);
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	protected void saveLandmarks( ViewResults view, boolean left, File destination ) {
		results.safe(() -> {
			String detectorName = algorithms.select(() -> algorithms.detector.getClass().getSimpleName());
			for (int imageIdx = 0; imageIdx < results.names.size(); imageIdx++) {
				String imageName = results.names.get(imageIdx);
				if (!results.used.get(imageIdx))
					continue;
				String fileName = left ? imageName : results.namesRight.get(imageIdx);
				String outputName = FilenameUtils.getBaseName(fileName) + ".csv";
				CalibrationIO.saveLandmarksCsv(imageName, detectorName, view.getObservation(imageName),
						new File(destination, outputName));
			}
		});
	}

	/**
	 * Opens a dialog and lets the user select stereo sequences
	 */
	public void openDialog() {
		BoofSwingUtil.checkGuiThread();

		OpenStereoSequencesChooser.Selected selected =
				BoofSwingUtil.openStereoChooser(window, getClass(), true, true);
		if (selected == null)
			return;

		// Load the files and process
		if (selected.isSplit()) {
			// Remember where it opened these files and add it to the recent file list
			BoofSwingUtil.addToRecentFiles(this, selected.left.getParent(),
					BoofMiscOps.asList(selected.left.getPath()));

			List<String> images = UtilIO.listSmartImages(selected.left.getPath(), false);

			if (images.isEmpty())
				return;

			// Load the first image and assume it's split halfway through
			Integer splitX = determineSplit(images);
			if (splitX == null) return;

			checkDefaultTarget(new File(images.get(0)).getParentFile());

			// disable menu bar while in the UI thread
			setMenuBarEnabled(false);
			new Thread(() -> process(images, splitX), "Open Dialog").start();
		} else {
			// Remember where it opened these files and add it to the recent file list
			BoofSwingUtil.addToRecentFiles(this, selected.left.getParent(),
					BoofMiscOps.asList(selected.left.getPath(), Objects.requireNonNull(selected.right).getPath()));

			List<String> left = UtilIO.listSmartImages(selected.left.getPath(), false);
			List<String> right = UtilIO.listSmartImages(selected.right.getPath(), false);

			if (left.isEmpty() || right.isEmpty())
				return;

			checkDefaultTarget(new File(left.get(0)).getParentFile());

			// disable menu bar while in the UI thread
			setMenuBarEnabled(false);
			new Thread(() -> process(left, right), "Open Dialog").start();
		}
	}

	@Nullable private Integer determineSplit( List<String> images ) {
		BufferedImage tmp = UtilImageIO.loadImage(images.get(0));
		if (tmp == null) {
			System.err.println("Failed to load " + images.get(0));
			return null;
		}
		int splitX = tmp.getWidth()/2;
		return splitX;
	}

	/**
	 * If there's a default target for this data, update the target.
	 *
	 * @param directory directory potentially containing target description
	 */
	public void checkDefaultTarget( File directory ) {
		BoofSwingUtil.checkGuiThread();
		sourceDirectory = directory;
		if (!CalibrateMonocularPlanarApp.loadDefaultTarget(sourceDirectory, configurePanel.targetPanel)) {
			targetChanged = true;
		}
	}

	/**
	 * Change camera model
	 */
	protected void createAlgorithms() {
		algorithms.safe(() -> {
			if (targetChanged)
				algorithms.detector = configurePanel.targetPanel.createSingleTargetDetector();

			if (targetChanged || calibratorChanged) {
				algorithms.calibrator = new CalibrateStereoPlanar(algorithms.detector.getLayout());
				ControlPanelPinhole controls = configurePanel.pinhole;
				algorithms.calibrator.configure(controls.skew.value, controls.numRadial.vint(), controls.tangential.value);
			}
		});

		targetChanged = false;
		calibratorChanged = false;
		resultsInvalid = true;
	}

	public void setMenuBarEnabled( boolean enabled ) {
		BoofSwingUtil.checkGuiThread();
		BoofSwingUtil.recursiveEnable(menuBar, enabled);
	}

	/**
	 * Process images in example directory
	 */
	public void processExample( String pathDirectory ) {
		List<String> leftImages = UtilIO.listByPrefix(pathDirectory, "left", null);
		List<String> rightImages = UtilIO.listByPrefix(pathDirectory, "right", null);

		process(leftImages, rightImages);
	}

	/**
	 * Process two sets of images for left and right cameras
	 */
	public void process( List<String> listLeft, List<String> listRight ) {
		try {
			if (listLeft.isEmpty())
				return;
			BoofMiscOps.checkEq(listLeft.size(), listRight.size(), "The two image sets must have matching pairs");

			Collections.sort(listLeft);
			Collections.sort(listRight);

			synchronized (lockInput) {
				inputImages = new StereoImageSetList(listLeft, listRight);
			}

			targetChanged = true;
			handleProcessCalled();
		} finally {
			SwingUtilities.invokeLater(() -> setMenuBarEnabled(true));
		}
	}

	/**
	 * Process a single set of images that will be automatically split
	 *
	 * @param listFused List of input images
	 * @param splitX Coordinate where the stereo image is slit in half
	 */
	public void process( List<String> listFused, int splitX ) {
		try {
			if (listFused.isEmpty())
				return;

			Collections.sort(listFused);

			synchronized (lockInput) {
				inputImages = new StereoImageSetListSplit(listFused, splitX);
			}

			targetChanged = true;
			handleProcessCalled();
		} finally {
			SwingUtilities.invokeLater(() -> setMenuBarEnabled(true));
		}
	}

	protected void handleProcessCalled() {
		BoofSwingUtil.checkNotGuiThread();
		if (inputImages == null)
			return;

		// Compute has been invoked and can be disabled
		SwingUtilities.invokeLater(() -> {
			configurePanel.bCompute.setEnabled(false);
			stereoPanel.clearVisuals();
		});

		// Update algorithm based on the latest user requests
		boolean detectTargets = targetChanged;
		createAlgorithms();

		if (detectTargets)
			detectLandmarksInImages();

		// User specifies if stdout should be verbose or not
		algorithms.safe(() -> BoofSwingUtil.setVerboseWithDemoSettings(algorithms.calibrator));
		addObservationsToCalibrator();

		// Perform calibration
		try {
			StereoParameters param = algorithms.select(() -> algorithms.calibrator.process());

			saveErrorsInResults();

			// Visualize the results
			setRectification(param);
			algorithms.calibrationSuccess = true;
			algorithms.parameters = param;

			// Show the user the found calibration parameters. Format a bit to make it look nicer
			String text = param.toStringQuaternion().replace(',', '\n').replace("{", "\n ");
			text = text.replace('}', '\n');
			String _text = text;
			SwingUtilities.invokeLater(() -> {
				configurePanel.textAreaCalib.setText(_text);
				showStatsToUser();
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> {
				BoofSwingUtil.warningDialog(this, e);
				configurePanel.textAreaCalib.setText("");
				configurePanel.textAreaStats.setText("");
			});
			algorithms.calibrationSuccess = false;
		}
		// Tell it to select the last image since that's what's being previewed already
		SwingUtilities.invokeLater(() -> changeSelectedGUI(Objects.requireNonNull(inputImages).size() - 1));
	}

	private void saveErrorsInResults() {
		results.lock();
		algorithms.lock();
		for (int imageIdx = 0, usedIdx = 0; imageIdx < results.names.size(); imageIdx++) {
			if (!results.used.get(imageIdx))
				continue;
			String imageName = results.names.get(imageIdx);
			results.left.errors.put(imageName, algorithms.calibrator.getCalibLeft().getErrors().get(usedIdx));
			results.right.errors.put(imageName, algorithms.calibrator.getCalibRight().getErrors().get(usedIdx));
			usedIdx += 1;
		}
		results.unlock();
		algorithms.unlock();
	}

	/**
	 * Resets then adds all used observations to the calibrator
	 */
	private void addObservationsToCalibrator() {
		algorithms.lock();
		results.lock();
		try {
			algorithms.calibrator.reset();
			for (int imageIdx = 0; imageIdx < results.names.size(); imageIdx++) {
				String imageName = results.names.get(imageIdx);
				if (!results.used.get(imageIdx))
					continue;
				CalibrationObservation left = results.left.observations.get(imageName);
				CalibrationObservation right = results.right.observations.get(imageName);
				if (left == null || right == null)
					throw new RuntimeException("Egads");
				algorithms.calibrator.addPair(left, right);
			}
		} finally {
			algorithms.unlock();
			results.unlock();
		}
	}

	/**
	 * The user has changed how reftification should fill the stereo pair
	 */
	private void handleRectificationChange() {
		if (!algorithms.calibrationSuccess)
			return;
		setRectification(algorithms.parameters);
		SwingUtilities.invokeLater(() -> stereoPanel.recomputeRectification());
	}

	/**
	 * Computes stereo rectification and then passes the distortion along to the gui.
	 */
	private void setRectification( final StereoParameters param ) {
		// calibration matrix for left and right camera
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(param.getLeft(), (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(param.getRight(), (DMatrixRMaj)null);

		RectifyCalibrated rectify = RectifyImageOps.createCalibrated();
		rectify.process(K1, new Se3_F64(), K2, param.getRightToLeft().invert(null));

		final DMatrixRMaj rect1 = rectify.getUndistToRectPixels1();
		final DMatrixRMaj rect2 = rectify.getUndistToRectPixels2();
		final DMatrixRMaj rectK = rectify.getCalibrationMatrix();

		RectifyImageOps.adjustView(configurePanel.rectType, param.getLeft(), rect1, rect2, rectK, null);

		SwingUtilities.invokeLater(() -> stereoPanel.setRectification(param.getLeft(), rect1, param.getRight(), rect2));
	}

	private void detectLandmarksInImages() {
		// Remove the previously displayed images
		SwingUtilities.invokeLater(() -> imageListPanel.clearImages());

		int numStereoPairs;
		synchronized (lockInput) {
			if (inputImages == null)
				return;
			numStereoPairs = inputImages.size();
		}

		algorithms.calibrationSuccess = false;
		results.reset();

		GrayF32 image = new GrayF32(1, 1);
		for (int imageIdx = 0; imageIdx < numStereoPairs; imageIdx++) {
			CalibrationObservation calibLeft, calibRight;
			BufferedImage buffLeft, buffRight;
			String imageName, imageRight;

			// Load the image
			synchronized (lockInput) {
				inputImages.setSelected(imageIdx);
				buffLeft = inputImages.loadLeft();
				buffRight = inputImages.loadRight();
				imageName = inputImages.getLeftName();
				imageRight = inputImages.getRightName();
				// we use the left image to identify the stereo pair
			}
			// Detect calibration landmarks
			ConvertBufferedImage.convertFrom(buffLeft, image);
			calibLeft = algorithms.select(() -> {
				algorithms.detector.process(image);
				return algorithms.detector.getDetectedPoints();
			});
			// Order matters for visualization later on
			Collections.sort(calibLeft.points, Comparator.comparingInt(a -> a.index));
			// see if at least one view was able to use this target
			boolean used = results.select(() -> results.left.add(imageName, calibLeft));

			ConvertBufferedImage.convertFrom(buffRight, image);
			calibRight = algorithms.select(() -> {
				algorithms.detector.process(image);
				return algorithms.detector.getDetectedPoints();
			});
			Collections.sort(calibRight.points, Comparator.comparingInt(a -> a.index));
			used |= results.select(() -> results.right.add(imageName, calibRight));

			boolean _used = used;
			results.safe(() -> {
				results.used.add(_used);
				results.names.add(imageName);
				results.namesRight.add(imageRight);
			});

			// Update the GUI by showing the latest images
			SwingUtilities.invokeLater(() -> {
				// Show images as they are being loaded
				stereoPanel.panelLeft.setImage(buffLeft);
				stereoPanel.panelRight.setImage(buffRight);
				stereoPanel.repaint();

				imageListPanel.addImage(imageName, _used);
			});
		}
	}

	/** Format statistics on results and add to a text panel */
	private void showStatsToUser() {
		BoofSwingUtil.checkGuiThread();

		algorithms.lock();
		results.lock();

		try {
			double averageError = 0.0;
			double maxError = 0.0;
			List<ImageResults> errors = algorithms.calibrator.computeErrors();
			if (errors.isEmpty())
				return;

			for (int i = 0; i < errors.size(); i++) {
				ImageResults r = errors.get(i);
				averageError += r.meanError;
				maxError = Math.max(maxError, r.maxError);
			}
			averageError /= errors.size();
			String text = String.format("Reprojection Errors (px):\n\nmean=%.3f max=%.3f\n\n", averageError, maxError);
			text += String.format("%-10s | %8s\n", "image", "max (px)");
			for (int imageIdx = 0, i = 0; imageIdx < results.names.size(); imageIdx++) {
				if (!results.used.get(imageIdx))
					continue;
				String imageName = results.names.get(imageIdx);
				ImageResults r = errors.get(i);
				text += String.format("%-12s %8.3f\n", imageName, r.maxError);
				// print right image now
				r = errors.get(i + 1);
				text += String.format("%-12s %8.3f\n", "", r.maxError);
				i += 2;
			}

			String _text = text;
			SwingUtilities.invokeLater(() -> {
				configurePanel.textAreaStats.setText(_text);
				configurePanel.textAreaStats.setCaretPosition(0); // show the top where summary stats are
			});
		} finally {
			algorithms.unlock();
			results.unlock();
		}
	}

	protected void settingsChanged( boolean target, boolean calibrator ) {
		BoofSwingUtil.checkGuiThread();
		targetChanged |= target;
		calibratorChanged |= calibrator;
		SwingUtilities.invokeLater(() -> configurePanel.bCompute.setEnabled(true));
	}

	protected void updateVisualizationSettings() {
		BoofSwingUtil.checkGuiThread();
		stereoPanel.setShowPoints(configurePanel.checkPoints.value);
		stereoPanel.setShowErrors(configurePanel.checkErrors.value);
		stereoPanel.setRectify(configurePanel.checkRectified.value);
		stereoPanel.setShowAll(configurePanel.checkAll.value);
		stereoPanel.setShowNumbers(configurePanel.checkNumbers.value);
		stereoPanel.setShowOrder(configurePanel.checkOrder.value);
		stereoPanel.setErrorScale(configurePanel.selectErrorScale.value.doubleValue());
		stereoPanel.setShowResiduals(configurePanel.checkResidual.value);
		stereoPanel.repaint();
	}

	/**
	 * Change which image is being displayed. Request from GUI
	 */
	private void changeSelectedGUI( int index ) {
		BoofSwingUtil.checkGuiThread();
		if (inputImages == null || index < 0 || index >= inputImages.size())
			return;

		// Change the item selected in the list
		imageListPanel.setSelected(index);

		BufferedImage buffLeft;
		BufferedImage buffRight;
		synchronized (lockInput) {
			inputImages.setSelected(index);
			buffLeft = inputImages.loadLeft();
			buffRight = inputImages.loadRight();
		}
		configurePanel.setImageSize(buffLeft.getWidth(), buffLeft.getHeight());
		stereoPanel.panelLeft.setBufferedImageNoChange(buffLeft);
		stereoPanel.panelRight.setBufferedImageNoChange(buffRight);

		updateResultsVisuals(index);
	}

	private void updateResultsVisuals( int inputIndex ) {
		BoofSwingUtil.checkGuiThread();

		results.safe(() -> {
			String imageName = results.names.get(inputIndex);

			{
				List<CalibrationObservation> all = algorithms.select(() ->
						algorithms.calibrator.getCalibLeft().getObservations());
				CalibrationObservation o = results.left.getObservation(imageName);
				ImageResults errors = results.left.errors.get(imageName);
				stereoPanel.panelLeft.setResults(o, errors, all);
			}

			{
				List<CalibrationObservation> all = algorithms.select(() ->
						algorithms.calibrator.getCalibRight().getObservations());
				CalibrationObservation o = results.right.getObservation(imageName);
				ImageResults errors = results.right.errors.get(imageName);
				stereoPanel.panelRight.setResults(o, errors, all);
			}
		});

		stereoPanel.repaint();
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

	protected void removePoint() {
		removePoint(stereoPanel.panelLeft);
		removePoint(stereoPanel.panelRight);
	}

	protected void removePoint( DisplayPinholeCalibrationPanel panel ) {
		int whichPoint = panel.getSelectedObservation();
		if (whichPoint < 0)
			return;

		CalibrationObservation observation = panel.getObservation();
		if (observation == null)
			return;

		resultsInvalid = true;

		results.safe(() -> {
			if (whichPoint >= observation.points.size())
				return;
			observation.points.remove(whichPoint);
			if (observation.points.size() < 4) {
				removeImage(true);
			}
		});

		SwingUtilities.invokeLater(() -> {
			// Remove the results since they are no longer valid
			panel.results = null;
			panel.deselectPoint();
			configurePanel.bCompute.setEnabled(true);
			panel.repaint();
		});
	}

	protected void removeImage() {
		removeImage(false);
	}

	/**
	 * Removes the selected image. If soft is specified then it's only removed if there are too few points
	 * in both images.
	 */
	protected void removeImage( boolean soft ) {
		BoofSwingUtil.invokeNowOrLater(() -> {
			int selected = imageListPanel.imageList.getSelectedIndex();
			if (selected < 0)
				return;

			// If the image isn't "used" don't remove it
			if (!imageListPanel.imageSuccess.get(selected))
				return;

			String imageName = results.select(() -> results.names.get(selected));

			if (soft) {
				// if a soft rule is applied then only remove if there are not enough observations in left and right
				// images
				boolean leftTooFew = results.select(() -> results.left.getObservation(imageName).size() < 4);
				boolean rightTooFew = results.select(() -> results.right.getObservation(imageName).size() < 4);
				if (!leftTooFew || !rightTooFew)
					return;
			}
			resultsInvalid = true;

			// Mark it as not used in the UI
			imageListPanel.imageSuccess.set(selected, false);

			results.safe(() -> {
				results.used.set(selected, false);
				results.left.getObservation(imageName).points.clear();
				results.right.getObservation(imageName).points.clear();
			});

			// Visually show the changes
			stereoPanel.panelLeft.results = null;
			stereoPanel.panelRight.results = null;
			configurePanel.bCompute.setEnabled(true);
			stereoPanel.repaint();
		});
	}

	/**
	 * Reverts any changes to observations
	 */
	protected void undoAllRemove() {
		resultsInvalid = true;

		// Rebuild list of used images and copy over original observations into the active observations
		results.safe(() -> {
			results.used.reset();
			for (int i = 0; i < results.names.size(); i++) {
				String imageName = results.names.get(i);
				CalibrationObservation ol = results.left.getObservation(imageName);
				CalibrationObservation or = results.right.getObservation(imageName);
				ol.setTo(results.left.original.get(i));
				or.setTo(results.right.original.get(i));

				results.used.add(ol.size() >= 4 || or.size() >= 4);
			}
		});

		// Visually show the changes
		BoofSwingUtil.invokeNowOrLater(() -> {
			results.safe(() -> {
				for (int i = 0; i < results.names.size(); i++) {
					imageListPanel.imageSuccess.set(i, results.used.get(i));
				}
			});

			configurePanel.bCompute.setEnabled(true);
			stereoPanel.repaint();
			imageListPanel.repaint();
		});
	}

	private void handleComputeButtonPressed() {
		BoofSwingUtil.checkGuiThread();
		setMenuBarEnabled(false);
		new Thread(() -> {
			try {
				handleProcessCalled();
			} finally {
				SwingUtilities.invokeLater(() -> setMenuBarEnabled(true));
			}
		}, "bCompute").start();
	}

	/**
	 * Provides controls to configure detection and calibration while also listing all the files
	 */
	public class ConfigureInfoPanel extends StandardAlgConfigPanel {
		protected JSpinnerNumber zoom = spinnerWrap(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);
		protected JLabel imageSizeLabel = new JLabel();
		protected RectifyFillType rectType = RectifyFillType.FULL_VIEW_LEFT;

		JButton bCompute = button("Compute", false);

		JCheckBoxValue checkPoints = checkboxWrap("Points", true).tt("Show calibration landmarks");
		JCheckBoxValue checkResidual = checkboxWrap("Residual", false).tt("Line showing residual exactly");
		JCheckBoxValue checkErrors = checkboxWrap("Errors", false).tt("Exaggerated residual errors");
		JCheckBoxValue checkRectified = checkboxWrap("Rectify", false).tt("Visualize rectified images");
		JCheckBoxValue checkAll = checkboxWrap("All", false).tt("Show location of all landmarks in all images");
		JCheckBoxValue checkNumbers = checkboxWrap("Numbers", false).tt("Draw feature numbers");
		JCheckBoxValue checkOrder = checkboxWrap("Order", true).tt("Visualize landmark order");
		JSpinnerNumber selectErrorScale = spinnerWrap(10.0, 0.1, 1000.0, 2.0);
		JComboBox<String> comboRect = combo(rectType.ordinal(), (Object[])RectifyFillType.values());

		@Getter ControlPanelPinhole pinhole = new ControlPanelPinhole(() -> settingsChanged(false, true));
		@Getter CalibrationTargetPanel targetPanel = new CalibrationTargetPanel(( a, b ) -> handleUpdatedTarget());
		// Displays a preview of the calibration target
		ImagePanel targetPreviewPanel = new ImagePanel();
		// Displays calibration information
		JTextArea textAreaCalib = new JTextArea();
		JTextArea textAreaStats = new JTextArea();

		public ConfigureInfoPanel() {
			configureTextArea(textAreaCalib);
			configureTextArea(textAreaStats);

			targetPreviewPanel.setScaling(ScaleOptions.DOWN);
			targetPreviewPanel.setCentering(true);
			targetPreviewPanel.setPreferredSize(new Dimension(200, 400));
			var targetVerticalPanel = new JPanel(new BorderLayout());
			targetVerticalPanel.add(targetPanel, BorderLayout.NORTH);
			targetVerticalPanel.add(targetPreviewPanel, BorderLayout.CENTER);
			handleUpdatedTarget();

			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Model", pinhole);
			tabbedPane.addTab("Target", targetVerticalPanel);
			tabbedPane.addTab("Calib", new JScrollPane(textAreaCalib));
			tabbedPane.addTab("Stats", new JScrollPane(textAreaStats));

			addLabeled(imageSizeLabel, "Image Size", "Size of image being viewed");
			addLabeled(zoom.spinner, "Zoom", "Zoom of image being viewed");
			addLabeled(comboRect, "Rectify");
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
			panel.add(checkRectified.check);
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
					handleComputeButtonPressed();
				}
			} else if (source == zoom.spinner) {
				stereoPanel.setScale(zoom.vdouble());
			} else if (source == comboRect) {
				rectType = RectifyFillType.values()[comboRect.getSelectedIndex()];
				handleRectificationChange();
			} else {
				updateVisualizationSettings();
			}
		}
	}

	private static class AlgorithmsLocked extends VariableLockSet {
		protected DetectSingleFiducialCalibration detector;
		protected CalibrateStereoPlanar calibrator;
		protected boolean calibrationSuccess;
		protected StereoParameters parameters;
	}

	private static class ResultsLocked extends VariableLockSet {
		// List of all image names. Left view, but this is these are the names used to identify the pairs
		protected final List<String> names = new ArrayList<>();
		// Names of right images
		protected final List<String> namesRight = new ArrayList<>();
		// List of images in use
		protected final DogArray_B used = new DogArray_B();

		protected final ViewResults left = new ViewResults();
		protected final ViewResults right = new ViewResults();

		public void reset() {
			safe(() -> {
				used.reset();
				left.reset();
				right.reset();
				names.clear();
			});
		}
	}

	private static class ViewResults {
		protected final Map<String, ImageResults> errors = new HashMap<>();
		// Active list of observations
		protected final Map<String, CalibrationObservation> observations = new HashMap<>();
		// Copy of original observation before any edits
		protected final DogArray<CalibrationObservation> original = new DogArray<>(CalibrationObservation::new);

		public ImageResults getError(String key) {
			return Objects.requireNonNull(errors.get(key));
		}

		public CalibrationObservation getObservation(String key) {
			return Objects.requireNonNull(observations.get(key));
		}

		public boolean add( String path, CalibrationObservation o ) {
			observations.put(path, o);
			original.grow().setTo(o);
			return o.size() >= 4;
		}

		public void reset() {
			errors.clear();
			observations.clear();
			original.reset();
		}
	}

	public static void main( @Nullable String[] args ) {
		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateStereoPlanarApp();

			app.window = ShowImages.showWindow(app, "Planar Stereo Calibration", true);
			app.window.setJMenuBar(app.menuBar);
		});
	}
}
