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

package boofcv.app;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.MultiToSingleFiducialCalibration;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.calib.hammingchess.HammingChessboardGenerator;
import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.FiducialRenderEngineGraphics2D;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.gui.controls.CalibrationModelPanel;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.OpenWebcamDialog;
import boofcv.struct.calib.CameraModelType;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.prefs.Preferences;

import static boofcv.gui.BoofSwingUtil.KEY_PREVIOUS_SELECTION;
import static boofcv.gui.StandardAlgConfigPanel.addLabeled;

/**
 * GUI which let's you configure the camera calibration process
 *
 * @author Peter Abeles
 */
// TODO specify output file name
// TODO output format
public class CameraCalibrationGui extends JPanel
		implements CalibrationTargetPanel.Listener, ActionListener {

	CameraCalibration app;

	JComboBox<CameraCalibration.FormatType> comboOutputFormat;
	CameraCalibration.FormatType outputFormat = CameraCalibration.FormatType.BOOFCV;
	JTextField textOutput;

	CalibrationTargetPanel controlsTarget = new CalibrationTargetPanel(this);
	CalibrationModelPanel controlsModel = new CalibrationModelPanel();
	ImagePanel renderingPanel = new ImagePanel();

	JFrame frame;

	public CameraCalibrationGui() {
		setLayout(new BorderLayout());

		this.app = new CameraCalibration();
		this.app.visualize = true;

		final File outputFile = new File(FileSystemView.getFileSystemView().getHomeDirectory(), "camera_calibration.yaml");

		textOutput = new JTextField();
		textOutput.setColumns(20);
		textOutput.setText(outputFile.getAbsolutePath());
		textOutput.setMaximumSize(new Dimension(textOutput.getPreferredSize().width, 30));
		JButton bOutput = new JButton(UIManager.getIcon("FileView.fileIcon"));
		bOutput.setPreferredSize(new Dimension(30, 30));
		bOutput.setMaximumSize(bOutput.getPreferredSize());
		bOutput.addActionListener(a -> {
			File f = BoofSwingUtil.fileChooser(null, this, false, textOutput.getText(), null);
			if (f != null) {
				textOutput.setText(f.getAbsolutePath());
			}
		});

		JPanel panelOutput = new JPanel();
		panelOutput.setLayout(new BoxLayout(panelOutput, BoxLayout.X_AXIS));
		panelOutput.add(textOutput);
		panelOutput.add(bOutput);

		comboOutputFormat = new JComboBox<>(CameraCalibration.FormatType.values());
		comboOutputFormat.setSelectedIndex(outputFormat.ordinal());
		comboOutputFormat.addActionListener(this);
		comboOutputFormat.setMaximumSize(comboOutputFormat.getPreferredSize());

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		addLabeled(panelOutput, "Output", null, controlsPanel);
		addLabeled(comboOutputFormat, "Format", null, controlsPanel);
		controlsPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		controlsPanel.add(controlsTarget);
		controlsPanel.add(controlsModel);

		renderingPanel.setScaling(ScaleOptions.DOWN);
		renderingPanel.setCentering(true);
		renderingPanel.setPreferredSize(new Dimension(400, 400));

		add(BorderLayout.WEST, controlsPanel);
		add(BorderLayout.CENTER, renderingPanel);
		createMenuBar();

		// trigger an event which will cause the target to be rendered
		controlsTarget.updateParameters();

		frame = ShowImages.showWindow(this, "BoofCV Camera Calibration", true);
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		JMenuItem menuOpenDirectory = new JMenuItem("Input Directory");
		BoofSwingUtil.setMenuItemKeys(menuOpenDirectory, KeyEvent.VK_D, KeyEvent.VK_D);
		menuOpenDirectory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				processDirectory();
			}
		});

		JMenuItem menuWebcam = new JMenuItem("Input Webcam");
		BoofSwingUtil.setMenuItemKeys(menuWebcam, KeyEvent.VK_W, KeyEvent.VK_W);
		menuWebcam.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				openWebcam();
			}
		});

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				showHelp();
			}
		});

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);
		menuQuit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				System.exit(0);
			}
		});

		menu.addSeparator();
		menu.add(menuOpenDirectory);
		menu.add(menuWebcam);
		menu.add(menuHelp);
		menu.add(menuQuit);

		add(BorderLayout.NORTH, menuBar);
	}

	private void showHelp() {
		JOptionPane.showMessageDialog(this, "Many more options and better documentation available through commandline");
	}

	private void openWebcam() {
		OpenWebcamDialog.Selection s = OpenWebcamDialog.showDialog(null);

		if (s != null) {
			app.inputType = BaseStandardInputApp.InputType.WEBCAM;
			app.cameraName = s.camera.getName();
			app.desiredWidth = s.width;
			app.desiredHeight = s.height;
			createDetector();
			frame.setVisible(false);

			new Thread(() -> app.process()).start();
		}
	}

	private void processDirectory() {
		Preferences prefs;
		prefs = Preferences.userRoot().node(this.getClass().getSimpleName());
		String previousPath = prefs.get(KEY_PREVIOUS_SELECTION, ".");

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(previousPath));
		chooser.setDialogTitle("Directory Input");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			prefs.put(KEY_PREVIOUS_SELECTION, chooser.getSelectedFile().getAbsolutePath());
			app.inputType = BaseStandardInputApp.InputType.IMAGE;
			app.inputPattern = chooser.getSelectedFile().getAbsolutePath();
			createDetector();
			frame.setVisible(false);

			new Thread(() -> {
				try {
					app.process();
				} catch (RuntimeException e) {
					BoofSwingUtil.warningDialog(frame, e);
				}
			}).start();
		}
	}

	private void createDetector() {
		switch (controlsTarget.selected) {
			case CHESSBOARD -> app.detector = FactoryFiducialCalibration.chessboardX(null, controlsTarget.configChessboard);
			case ECOCHECK -> app.detector = new MultiToSingleFiducialCalibration(
					FactoryFiducialCalibration.ecocheck(null, controlsTarget.configECoCheck));
			case SQUARE_GRID -> app.detector = FactoryFiducialCalibration.squareGrid(null, controlsTarget.configSquare);
			case CIRCLE_GRID -> app.detector = FactoryFiducialCalibration.circleRegularGrid(null, controlsTarget.configCircle);
			case CIRCLE_HEXAGONAL -> app.detector = FactoryFiducialCalibration.circleHexagonalGrid(null, controlsTarget.configCircleHex);
			default -> {
				JOptionPane.showMessageDialog(this, "Target type not yet support: " + controlsTarget.selected);
				throw new RuntimeException("Not supported yet. " + controlsTarget.selected);
			}
		}

		app.modeType = controlsModel.selected;
		if (app.modeType == CameraModelType.BROWN) {
			app.numRadial = controlsModel.pinholeRadial;
			app.tangential = controlsModel.pinholeTangential;
			app.zeroSkew = controlsModel.pinholeSkew;
		} else if (app.modeType == CameraModelType.UNIVERSAL) {
			app.numRadial = controlsModel.universalRadial;
			app.tangential = controlsModel.universalTangential;
			app.zeroSkew = controlsModel.universalSkew;
		} else if (app.modeType == CameraModelType.KANNALA_BRANDT) {
			app.kbNumSymmetric = controlsModel.kannalaBrandt.numSymmetric.value.intValue();
			app.kbNumAsymmetric = controlsModel.kannalaBrandt.numAsymmetric.value.intValue();
			app.zeroSkew = controlsModel.kannalaBrandt.skew.value;
		} else {
			JOptionPane.showMessageDialog(this, "Camera model not yet support: " + app.modeType);
			throw new RuntimeException("Not supported yet. " + app.modeType);
		}

		app.formatType = outputFormat;
		app.outputFilePath = textOutput.getText();
	}

	@Override
	public void calibrationParametersChanged( CalibrationPatterns type, Object config ) {
		int squareWidth = 40;
		int circle = squareWidth/2;

		if (type == CalibrationPatterns.ECOCHECK) {
			ConfigECoCheckMarkers c = (ConfigECoCheckMarkers)config;

			ECoCheckUtils utils = new ECoCheckUtils();
			utils.codec.setErrorCorrectionLevel(c.errorCorrectionLevel);
			c.convertToGridList(utils.markers);
			utils.fixate();
			ConfigECoCheckMarkers.MarkerShape shape = c.markerShapes.get(0);

			int markerWidth = squareWidth*(shape.numCols - 1);
			int markerHeight = squareWidth*(shape.numRows - 1);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			ECoCheckGenerator generator = new ECoCheckGenerator(utils);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render(0);
			renderingPanel.setImageUI(render.getImage());
			return;
		} else if (type == CalibrationPatterns.HAMMING_CHESSBOARD) {
			ConfigHammingChessboard c = (ConfigHammingChessboard)config;

			int markerWidth = squareWidth*c.numCols;
			int markerHeight = squareWidth*c.numRows;

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			HammingChessboardGenerator generator = new HammingChessboardGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			renderingPanel.setImageUI(render.getImage());
			return;
		} else if (type == CalibrationPatterns.HAMMING_GRID) {
			ConfigHammingGrid c = (ConfigHammingGrid)config;

			int markerWidth = (int)Math.round(squareWidth*c.getMarkerWidth()/c.squareSize);
			int markerHeight = (int)Math.round(squareWidth*c.getMarkerHeight()/c.squareSize);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			var generator = new HammingGridGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			renderingPanel.setImageUI(render.getImage());
			return;
		}

		final RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(20, 1);

		if (type == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			renderer.chessboard(c.numRows, c.numCols, squareWidth);
		} else if (type == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = squareWidth*c.shapeDistance/c.shapeSize;
			renderer.squareGrid(c.numRows, c.numCols, squareWidth, space);
		} else if (type == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleRegular(c.numRows, c.numCols, circle, space);
		} else if (type == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleHex(c.numRows, c.numCols, circle, space);
		}
		renderingPanel.setImageUI(renderer.getBufferred());
	}

	private FiducialRenderEngineGraphics2D configureRenderGraphics2D( int markerWidth, int markerHeight, int border ) {
		// Render the marker. Adjust marker size so that when the border is added it will match the paper size
		var render = new FiducialRenderEngineGraphics2D();
		render.configure(border, border, markerWidth, markerHeight);
		return render;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboOutputFormat) {
			outputFormat = (CameraCalibration.FormatType)comboOutputFormat.getSelectedItem();
		}
	}
}
