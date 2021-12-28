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
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.calib.hammingchess.HammingChessboardGenerator;
import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.FiducialRenderEngineGraphics2D;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
import boofcv.gui.controls.CalibrationTargetPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import static boofcv.gui.StandardAlgConfigPanel.addAlignLeft;
import static boofcv.gui.StandardAlgConfigPanel.addLabeled;

@SuppressWarnings({"NullAway.Init"})
public class CreateCalibrationTargetGui extends JPanel
		implements CalibrationTargetPanel.Listener, ActionListener {

	JCheckBox checkSaveLandmarks;
	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));
	JComboBox<Unit> comboUnits = new JComboBox<>(Unit.values());

	CalibrationPatterns selectedType;
	Object selectedCalib;

	CalibrationTargetPanel controlsTarget = new CalibrationTargetPanel(this);
	ImagePanel renderingPanel = new ImagePanel();

	PaperSize paper = PaperSize.LETTER;
	Unit units = Unit.CENTIMETER;
	boolean saveLandmarks = false;

	JFrame frame;

	public CreateCalibrationTargetGui() {
		setLayout(new BorderLayout());

		// set reasonable defaults
		controlsTarget.configChessboard.shapeSize = 3;
		controlsTarget.configHammingChess.squareSize = 3;
		controlsTarget.configHammingGrid.squareSize = 3;
		controlsTarget.configECoCheck.markerShapes.get(0).squareSize = 3;
		controlsTarget.configSquare.shapeSize = 3;
		controlsTarget.configSquare.shapeDistance = 3*0.2;
		controlsTarget.configCircleHex = new ConfigGridDimen(20, 24, 1, 1.5);
		controlsTarget.configCircle = new ConfigGridDimen(17, 12, 1, 1.5);
		controlsTarget.changeTargetPanel();

		comboPaper.addActionListener(this);
		comboPaper.setSelectedItem(paper);
		comboPaper.setMaximumSize(comboPaper.getPreferredSize());

		comboUnits.addActionListener(this);
		comboUnits.setSelectedIndex(units.ordinal());
		comboUnits.setMaximumSize(comboUnits.getPreferredSize());

		checkSaveLandmarks = new JCheckBox("Save Landmarks", saveLandmarks);
		checkSaveLandmarks.addActionListener(this);

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		addAlignLeft(checkSaveLandmarks, controlsPanel);
		addLabeled(comboPaper, "Paper", null, controlsPanel);
		controlsPanel.add(controlsTarget);
		addLabeled(comboUnits, "Target Units", null, controlsPanel);
		controlsPanel.add(Box.createVerticalGlue());

		renderingPanel.setPreferredSize(new Dimension(400, 500));
		renderingPanel.setCentering(true);
		renderingPanel.setScaling(ScaleOptions.DOWN);
		renderingPanel.setBackground(Color.GRAY);

		add(BorderLayout.WEST, controlsPanel);
		add(BorderLayout.CENTER, renderingPanel);
		createMenuBar();

		// trigger an event which will cause the target to be rendered
		controlsTarget.updateParameters();

		frame = ShowImages.showWindow(this, "BoofCV Create Calibration Target", true);
	}

	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		JMenuItem menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave, KeyEvent.VK_S, KeyEvent.VK_S);
		menuSave.addActionListener(e -> saveFile(false));

		JMenuItem menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint, KeyEvent.VK_P, KeyEvent.VK_P);
		menuPrint.addActionListener(e -> saveFile(true));

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(e -> showHelp());

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);
		menuQuit.addActionListener(e -> System.exit(0));

		menu.addSeparator();
		menu.add(menuSave);
		menu.add(menuPrint);
		menu.add(menuHelp);
		menu.add(menuQuit);

		add(BorderLayout.NORTH, menuBar);
	}

	private void showHelp() {
		JOptionPane.showMessageDialog(this, "Many more options and better documentation available through commandline");
	}

	private void saveFile( boolean sendToPrinter ) {
		// grab the focus and force what the user is editing to be saved

		File f;

		// see where the document is to be sent
		if (sendToPrinter) {
			f = new File(""); // dummy to make the code below happy and less complex
		} else {
			String defaultName = defaultFileName() + ".pdf";
			f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f, defaultName);

			f = BoofSwingUtil.fileChooser(null, this, false, f.getPath(), path -> {
				File tmp = new File(path);
				return new File(tmp.getParent(), defaultName).getPath();
			});
			if (f == null) {
				return;
			}

			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
		}

		// use the same save function as the commandline tool
		var app = new CreateCalibrationTarget();
		app.fileName = new File(f.getParent(), FilenameUtils.getBaseName(f.getName())).getPath();
		app.sendToPrinter = sendToPrinter;
		app.unit = units;
		app.paperSize = paper;
		app.type = selectedType;
		app.saveLandmarks = saveLandmarks;

		switch (selectedType) {
			case CHESSBOARD -> {
				ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.shapeSize;
			}
			case ECOCHECK -> {
				ConfigECoCheckMarkers config = (ConfigECoCheckMarkers)selectedCalib;
				ConfigECoCheckMarkers.MarkerShape shape = config.markerShapes.get(0);
				app.rows = shape.numRows;
				app.columns = shape.numCols;
				app.shapeWidth = (float)shape.squareSize;
				app.numMarkers = config.firstTargetDuplicated;
				app.chessBitsError = config.errorCorrectionLevel;
				app.ecocheckChecksum = config.checksumBits;
			}
			case HAMMING_CHESSBOARD -> {
				ConfigHammingChessboard config = (ConfigHammingChessboard)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.squareSize;
				app.markerScale = (float)config.markerScale;
				app.encodingName = config.markers.dictionary.name();
				app.encodingOffset = config.markerOffset;
			}
			case HAMMING_GRID -> {
				ConfigHammingGrid config = (ConfigHammingGrid)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.squareSize;
				app.shapeSpace = (float)config.spaceToSquare;
				app.encodingName = config.markers.dictionary.name();
				app.encodingOffset = config.markerOffset;
			}
			case SQUARE_GRID -> {
				ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.shapeSize;
				app.shapeSpace = (float)config.shapeDistance;
			}
			case CIRCLE_GRID -> {
				ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.shapeSize;
				app.centerDistance = (float)config.shapeDistance;
			}
			case CIRCLE_HEXAGONAL -> {
				ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
				app.rows = config.numRows;
				app.columns = config.numCols;
				app.shapeWidth = (float)config.shapeSize;
				app.centerDistance = (float)config.shapeDistance;
			}
		}

		try {
			app.run();
		} catch (IOException e) {
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	@Override
	public void calibrationParametersChanged( CalibrationPatterns type, Object _config ) {
		this.selectedType = type;
		this.selectedCalib = _config;

		updatePreview();
	}

	private void updatePreview() {
		double paperWidth = paper.unit.convert(paper.width, units);
		double paperHeight = paper.unit.convert(paper.height, units);
		double unitsToPixel = 400.0/paperWidth;

		// TODO switch other calibration targets over to using the generic fiducial engine
		if (selectedType == CalibrationPatterns.ECOCHECK) {
			ConfigECoCheckMarkers c = (ConfigECoCheckMarkers)selectedCalib;

			ECoCheckUtils utils = new ECoCheckUtils();
			utils.setParametersFromConfig(c);
			utils.fixate();
			ConfigECoCheckMarkers.MarkerShape shape = c.markerShapes.get(0);

			double markerWidth = shape.squareSize*(shape.numCols - 1);
			double markerHeight = shape.squareSize*(shape.numRows - 1);

			// Render the marker. Adjust marker size so that when the border is added it will match the paper size
			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, unitsToPixel);

			ECoCheckGenerator generator = new ECoCheckGenerator(utils);
			generator.squareWidth = shape.squareSize*unitsToPixel;
			generator.setRender(render);
			generator.render(0);
			renderingPanel.setImageUI(render.getImage());
			return;
		} else if (selectedType == CalibrationPatterns.HAMMING_CHESSBOARD) {
			ConfigHammingChessboard c = (ConfigHammingChessboard)selectedCalib;

			double markerWidth = c.getMarkerWidth();
			double markerHeight = c.getMarkerHeight();

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, unitsToPixel);

			HammingChessboardGenerator generator = new HammingChessboardGenerator(c);
			generator.squareWidth = unitsToPixel;
			generator.setRender(render);
			generator.render();
			renderingPanel.setImageUI(render.getImage());
			return;
		} else if (selectedType == CalibrationPatterns.HAMMING_GRID) {
			ConfigHammingGrid c = (ConfigHammingGrid)selectedCalib;

			double markerWidth = c.getMarkerWidth();
			double markerHeight = c.getMarkerHeight();

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, unitsToPixel);

			var generator = new HammingGridGenerator(c);
			generator.squareWidth = unitsToPixel;
			generator.setRender(render);
			generator.render();
			renderingPanel.setImageUI(render.getImage());
			return;
		}

		final RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(-1, 400/paperWidth);
		renderer.setPaperSize(paperWidth, paperHeight);

		if (selectedType == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			renderer.chessboard(config.numRows, config.numCols, config.shapeSize);
		} else if (selectedType == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			renderer.squareGrid(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		} else if (selectedType == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			renderer.circleRegular(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		} else if (selectedType == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			renderer.circleHex(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		}

		renderingPanel.setImageUI(renderer.getBuffered());
	}

	private FiducialRenderEngineGraphics2D configureRenderGraphics2D( double markerWidth, double markerHeight, double unitsToPixel ) {
		double paperWidth = paper.unit.convert(paper.width, units);
		double paperHeight = paper.unit.convert(paper.height, units);

		double paperWidthPixels = (int)(paperWidth*unitsToPixel);
		double paperHeightPixels = (int)(paperHeight*unitsToPixel);

		markerWidth *= unitsToPixel;
		markerHeight *= unitsToPixel;

		// Center it in the page
		int borderX = (int)(Math.max(0, paperWidthPixels - markerWidth)/2);
		int borderY = (int)(Math.max(0, paperHeightPixels - markerHeight)/2);

		// Render the marker. Adjust marker size so that when the border is added it will match the paper size
		var render = new FiducialRenderEngineGraphics2D();
		render.configure(borderX, borderY,
				(int)(paperWidth*unitsToPixel) - 2*borderX, (int)(paperHeight*unitsToPixel) - 2*borderY);
		return render;
	}

	String defaultFileName() {
		if (selectedType == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			return "chessboard_" + config.numRows + "x" + config.numCols;
		} else if (selectedType == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			return "squaregrid_" + config.numRows + "x" + config.numCols;
		} else if (selectedType == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			return "circlegrid_" + config.numRows + "x" + config.numCols;
		} else if (selectedType == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen config = (ConfigGridDimen)selectedCalib;
			return "circlehex_" + config.numRows + "x" + config.numCols;
		} else if (selectedType == CalibrationPatterns.ECOCHECK) {
			ConfigECoCheckMarkers config = (ConfigECoCheckMarkers)selectedCalib;
			return "ecocheck_" + config.compactName();
		} else if (selectedType == CalibrationPatterns.HAMMING_CHESSBOARD) {
			ConfigHammingChessboard config = (ConfigHammingChessboard)selectedCalib;
			return "chessboard_" + config.numRows + "x" + config.numCols + "_" + config.markers.dictionary;
		} else if (selectedType == CalibrationPatterns.HAMMING_GRID) {
			ConfigHammingGrid config = (ConfigHammingGrid)selectedCalib;
			return "squaregrid_" + config.numRows + "x" + config.numCols + "_" + config.markers.dictionary;
		} else {
			return "calibration_target";
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboPaper) {
			paper = (PaperSize)comboPaper.getSelectedItem();
		} else if (e.getSource() == comboUnits) {
			units = Unit.values()[comboUnits.getSelectedIndex()];
		} else if (e.getSource() == checkSaveLandmarks) {
			saveLandmarks = checkSaveLandmarks.isSelected();
		}

		updatePreview();
	}
}
