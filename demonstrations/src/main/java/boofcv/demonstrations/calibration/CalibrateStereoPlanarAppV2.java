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
import boofcv.gui.calibration.StereoImageSet;
import boofcv.gui.calibration.StereoImageSetList;
import boofcv.gui.calibration.UtilCalibrationGui;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.controls.ControlPanelPinhole;
import boofcv.gui.controls.JCheckBoxValue;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.gui.dialogs.OpenStereoSequencesChooser;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.gui.settings.GlobalSettingsControls;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.VariableLockSet;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static boofcv.demonstrations.calibration.CalibrateMonocularPlanarApp.saveCalibrationTarget;
import static boofcv.demonstrations.calibration.CalibrateMonocularPlanarApp.saveIntrinsics;
import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Application that lets you change calibration and target settings and recalibrate stereo image sets.
 *
 * @author Peter Abeles
 */
public class CalibrateStereoPlanarAppV2 extends JPanel {
	// TODO remove corners
	// TODO remove images
	// TODO Dialog for reading in split image sequence

	protected @Nullable StereoImageSet inputImages;
	protected final Object lockInput = new Object();

	AlgorithmsLocked algorithms = new AlgorithmsLocked();
	ResultsLocked resultsLeft = new ResultsLocked();
	ResultsLocked resultsRight = new ResultsLocked();

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

	public CalibrateStereoPlanarAppV2() {
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

	protected void updateRecentItems() {
		BoofSwingUtil.updateRecentItems(this, menuRecent, ( info ) -> {
			List<String> left = UtilIO.listSmartImages(info.files.get(0), false);
			List<String> right = UtilIO.listSmartImages(info.files.get(1), false);

			checkDefaultTarget(new File(info.files.get(0)));

			// disable menu bar while in the UI thread
			setMenuBarEnabled(false);
			new Thread(() -> process(left, right), "Recent Item").start();
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
			saveLandmarks(resultsLeft, destination);
			saveLandmarks(resultsRight, destination);
		} catch (RuntimeException e) {
			e.printStackTrace();
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	protected void saveLandmarks(ResultsLocked results, File destination) {
		results.safe(() -> {
			String detectorName = algorithms.select(() -> algorithms.detector.getClass().getSimpleName());
			for (int imageIdx = 0; imageIdx < results.imageNames.size(); imageIdx++) {
				String imageName = results.imageNames.get(imageIdx);
				String outputName = FilenameUtils.getBaseName(imageName) + ".csv";
				CalibrationIO.saveLandmarksCsv(imageName, detectorName, results.observations.get(imageIdx),
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

		// Remember where it opened these files and add it to the recent file list
		BoofSwingUtil.addToRecentFiles(this, selected.left.getParent(),
				BoofMiscOps.asList(selected.left.getPath(), selected.right.getPath()));

		// Load the files and process
		List<String> left = UtilIO.listSmartImages(selected.left.getPath(), false);
		List<String> right = UtilIO.listSmartImages(selected.right.getPath(), false);

		if (left.isEmpty() || right.isEmpty())
			return;

		checkDefaultTarget(new File(left.get(0)).getParentFile());

		// disable menu bar while in the UI thread
		setMenuBarEnabled(false);
		new Thread(() -> process(left, right), "Open Dialog").start();
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
	 * @param horizontal true then left and right images are side by side and split in the middle
	 */
	public void process( List<String> listFused, boolean horizontal ) {

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
		SwingUtilities.invokeLater(() -> changeSelectedGUI(inputImages.size() - 1));
	}

	/**
	 * Resets then adds all used observations to the calibrator
	 */
	private void addObservationsToCalibrator() {
		algorithms.lock();
		resultsLeft.lock();
		resultsRight.lock();
		try {
			algorithms.calibrator.reset();
			for (int i = 0; i < resultsLeft.usedImages.size(); i++) {
				int imageIndex = resultsLeft.usedImages.get(i);
				CalibrationObservation left = resultsLeft.observations.get(imageIndex);
				CalibrationObservation right = resultsRight.observations.get(imageIndex);
				algorithms.calibrator.addPair(left, right);
			}
		} finally {
			algorithms.unlock();
			resultsLeft.unlock();
			resultsRight.unlock();
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
		resultsLeft.reset();
		resultsRight.reset();

		int numUsed = 0;
		GrayF32 image = new GrayF32(1, 1);
		for (int imageIdx = 0; imageIdx < numStereoPairs; imageIdx++) {
			CalibrationObservation calibLeft, calibRight;
			BufferedImage buffLeft, buffRight;
			String leftName, rightName;

			// Load the image
			synchronized (lockInput) {
				inputImages.setSelected(imageIdx);
				buffLeft = inputImages.loadLeft();
				buffRight = inputImages.loadRight();
				leftName = inputImages.getLeftName();
				rightName = inputImages.getRightName();
			}
			// Detect calibration landmarks
			ConvertBufferedImage.convertFrom(buffLeft, image);
			calibLeft = algorithms.select(() -> {
				algorithms.detector.process(image);
				return algorithms.detector.getDetectedPoints();
			});
			// see if at least one view was able to use this target
			boolean used = resultsLeft.add(leftName, imageIdx, calibLeft);

			ConvertBufferedImage.convertFrom(buffRight, image);
			calibRight = algorithms.select(() -> {
				algorithms.detector.process(image);
				return algorithms.detector.getDetectedPoints();
			});
			used |= resultsRight.add(rightName, imageIdx, calibRight);

			// Pass in the results to the calibrator for future use
			if (used) {
				numUsed++;
			}

			resultsLeft.lock();
			resultsLeft.inputToUsed.add(used ? numUsed - 1 : -1);
			resultsLeft.unlock();
			resultsRight.lock();
			resultsRight.inputToUsed.add(used ? numUsed - 1 : -1);
			resultsRight.unlock();

			// Update the GUI by showing the latest images
			boolean _used = used;
			SwingUtilities.invokeLater(() -> {
				// Show images as they are being loaded
				stereoPanel.panelLeft.setImage(buffLeft);
				stereoPanel.panelRight.setImage(buffRight);
				stereoPanel.repaint();

				imageListPanel.addImage(leftName, _used);
			});
		}
	}

	/** Format statistics on results and add to a text panel */
	private void showStatsToUser() {
		BoofSwingUtil.checkGuiThread();

		algorithms.lock();
		resultsLeft.lock();
		resultsRight.lock();

		try {
			double averageError = 0.0;
			double maxError = 0.0;
			List<ImageResults> results = algorithms.calibrator.computeErrors();
			if (results.isEmpty())
				return;

			for (int i = 0; i < results.size(); i++) {
				ImageResults r = results.get(i);
				averageError += r.meanError;
				maxError = Math.max(maxError, r.maxError);
			}
			averageError /= results.size();
			String text = String.format("Reprojection Errors (px):\n\nmean=%.3f max=%.3f\n\n", averageError, maxError);
			text += String.format("%-10s | %8s\n", "image", "max (px)");
			for (int i = 0; i < imageListPanel.imageNames.size(); i++) {
				int resultsIndex = resultsLeft.inputToUsed.get(i)*2;
				if (resultsIndex < 0)
					continue;
				String image = imageListPanel.imageNames.get(i);
				ImageResults r = results.get(resultsIndex);
				text += String.format("%-12s %8.3f\n", image, r.maxError);
				// print right image now
				r = results.get(resultsIndex + 1);
				text += String.format("%-12s %8.3f\n", "", r.maxError);
			}

			String _text = text;
			SwingUtilities.invokeLater(() -> configurePanel.textAreaStats.setText(_text));
		} finally {
			algorithms.unlock();
			resultsLeft.unlock();
			resultsRight.unlock();
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

		resultsLeft.safe(() -> {
			List<CalibrationObservation> all = algorithms.select(() ->
					algorithms.calibrator.getCalibLeft().getObservations());
			CalibrationObservation o = resultsLeft.observations.get(inputIndex);
			int errorIndex = resultsLeft.inputToUsed.get(inputIndex);
			List<ImageResults> errors = algorithms.calibrator.getCalibLeft().getErrors();
			ImageResults results = errorIndex == -1 || errors == null ? null : errors.get(errorIndex);
			stereoPanel.panelLeft.setResults(o, results, all);
		});
		resultsLeft.safe(() -> {
			List<CalibrationObservation> all = algorithms.select(() ->
					algorithms.calibrator.getCalibRight().getObservations());
			CalibrationObservation o = resultsRight.observations.get(inputIndex);
			int errorIndex = resultsRight.inputToUsed.get(inputIndex);
			List<ImageResults> errors = algorithms.calibrator.getCalibRight().getErrors();
			ImageResults results = errorIndex == -1 || errors == null ? null : errors.get(errorIndex);
			stereoPanel.panelRight.setResults(o, results, all);
		});

		stereoPanel.repaint();
	}

	/**
	 * Creates and configures a panel for displaying images names and control buttons for removing points/images
	 */
	protected CalibrationListPanel createImageListPanel() {
		var panel = new CalibrationListPanel();
		// TODO implement these
//		panel.bRemovePoint.addActionListener(( e ) -> removePoint());
//		panel.bRemoveImage.addActionListener(( e ) -> removeImage());
//		panel.bReset.addActionListener(( e ) -> undoAllRemove());
		panel.selectionChanged = this::changeSelectedGUI;
		return panel;
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
		JComboBox<String> comboRect = combo(rectType.ordinal(), RectifyFillType.values());

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
					new Thread(() -> handleProcessCalled(), "bCompute").start();
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
		// Index of images used when calibrating
		protected final DogArray_I32 usedImages = new DogArray_I32();
		protected final DogArray_I32 inputToUsed = new DogArray_I32();
		protected final List<String> imageNames = new ArrayList<>();
		// Active list of observations
		protected final List<CalibrationObservation> observations = new ArrayList<>();
		// Copy of original observation before any edits
		protected final DogArray<CalibrationObservation> original = new DogArray<>(CalibrationObservation::new);

		public boolean add( String path, int imageIndex, CalibrationObservation o ) {
			boolean used = o.points.size() >= 4;
			safe(() -> {
				if (used)
					usedImages.add(imageIndex);
				observations.add(o);
				imageNames.add(path);
				original.grow().setTo(o);
			});
			return used;
		}

		public void reset() {
			safe(() -> {
				usedImages.reset();
				observations.clear();
				original.reset();
				inputToUsed.reset();
				imageNames.clear();
			});
		}
	}

	public static void main( String[] args ) {
		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		List<String> leftImages = UtilIO.listByPrefix(directory, "left", null);
		List<String> rightImages = UtilIO.listByPrefix(directory, "right", null);

		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateStereoPlanarAppV2();

			app.window = ShowImages.showWindow(app, "Planar Stereo Calibration", true);
			app.window.setJMenuBar(app.menuBar);

			app.checkDefaultTarget(new File(directory));
			app.setMenuBarEnabled(false);
			new Thread(() -> app.process(leftImages, rightImages)).start();
		});
	}
}
