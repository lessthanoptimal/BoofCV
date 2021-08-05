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
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.app.calib.CalibrationTargetPanel;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;
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

import static boofcv.app.CreateCalibrationTarget.saveChessboardBitsToPdf;
import static boofcv.gui.StandardAlgConfigPanel.addLabeled;

public class CreateCalibrationTargetGui extends JPanel
		implements CalibrationTargetPanel.Listener, ActionListener {

	JComboBox<PaperSize> comboPaper = new JComboBox<>(PaperSize.values().toArray(new PaperSize[0]));
	JComboBox<Unit> comboUnits = new JComboBox<>(Unit.values());

	CalibrationPatterns selectedType;
	ConfigGridDimen selectedCalib;

	CalibrationTargetPanel controlsTarget = new CalibrationTargetPanel(this);
	ImagePanel renderingPanel = new ImagePanel();

	PaperSize paper = PaperSize.LETTER;
	Unit units = Unit.CENTIMETER;

	JFrame frame;

	public CreateCalibrationTargetGui() {
		setLayout(new BorderLayout());

		// set reasonable defaults
		controlsTarget.configChessboard.shapeSize = 3;
		controlsTarget.configChessboardBits.shapeSize = 3;
		controlsTarget.configSquare.numRows = 5;
		controlsTarget.configSquare.numCols = 4;
		controlsTarget.configSquare.shapeSize = 3;
		controlsTarget.configSquare.shapeDistance = 2;
		controlsTarget.configCircleHex = new ConfigGridDimen(20, 24, 1, 1.5);
		controlsTarget.configCircle = new ConfigGridDimen(17, 12, 1, 1.5);
		controlsTarget.changeTargetPanel();

		comboPaper.addActionListener(this);
		comboPaper.setSelectedItem(paper);
		comboPaper.setMaximumSize(comboPaper.getPreferredSize());

		comboUnits.addActionListener(this);
		comboUnits.setSelectedIndex(units.ordinal());
		comboUnits.setMaximumSize(comboUnits.getPreferredSize());

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
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
			f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f, "calibration_target.pdf");

			f = BoofSwingUtil.fileChooser(null, this, false, f.getPath(), null);
			if (f == null) {
				return;
			}

			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
		}


		// Make sure the file has the correct extension
		String outputFile = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(outputFile);
		if (ext.compareToIgnoreCase("pdf") != 0) {
			outputFile = FilenameUtils.removeExtension(outputFile);
			outputFile += "." + "pdf";
		}

		try {
			switch (selectedType) {
				case CHESSBOARD -> {
					ConfigGridDimen config = selectedCalib;
					CreateCalibrationTargetGenerator generator = new CreateCalibrationTargetGenerator(outputFile, paper,
							config.numRows, config.numCols, units);
					generator.sendToPrinter = sendToPrinter;
					generator.chessboard((float)config.shapeSize);
				}

				case CHESSBOARD_BITS -> {
					// TODO add number of markers to config
					ConfigGridDimen config = selectedCalib;
					saveChessboardBitsToPdf(outputFile, paper, units, config.numRows,
							config.numCols, (float)config.shapeSize, 1);
				}

				case SQUARE_GRID -> {
					ConfigGridDimen config = selectedCalib;
					CreateCalibrationTargetGenerator generator = new CreateCalibrationTargetGenerator(outputFile, paper,
							config.numRows, config.numCols, units);
					generator.sendToPrinter = sendToPrinter;
					generator.squareGrid((float)config.shapeSize, (float)config.shapeDistance);
				}
				case CIRCLE_GRID -> {
					ConfigGridDimen config = selectedCalib;
					CreateCalibrationTargetGenerator generator = new CreateCalibrationTargetGenerator(outputFile, paper,
							config.numRows, config.numCols, units);
					generator.sendToPrinter = sendToPrinter;
					generator.circleGrid((float)config.shapeSize, (float)config.shapeDistance);
				}
				case CIRCLE_HEXAGONAL -> {
					ConfigGridDimen config = selectedCalib;
					CreateCalibrationTargetGenerator generator = new CreateCalibrationTargetGenerator(outputFile, paper,
							config.numRows, config.numCols, units);
					generator.sendToPrinter = sendToPrinter;
					generator.circleHexagonal((float)config.shapeSize, (float)config.shapeDistance);
				}
				default -> throw new RuntimeException("Unknown type " + selectedType);
			}
		} catch (IOException e) {
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	@Override
	public void calibrationParametersChanged( CalibrationPatterns type, ConfigGridDimen _config ) {
		this.selectedType = type;
		this.selectedCalib = _config;

		updatePreview();
	}

	private void updatePreview() {
		double paperWidth = paper.unit.convert(paper.width, units);
		double paperHeight = paper.unit.convert(paper.height, units);

		final RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(-1, 400/paperWidth);
		renderer.setPaperSize(paperWidth, paperHeight);

		if (selectedType == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen config = selectedCalib;
			renderer.chessboard(config.numRows, config.numCols, config.shapeSize);
		} else if (selectedType == CalibrationPatterns.CHESSBOARD_BITS) {
//			ConfigGridDimen config = selectedCalib;
//			renderer.chessboard(config.numRows, config.numCols, config.shapeSize);
			// TODO implement
		} else if (selectedType == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen config = selectedCalib;
			renderer.squareGrid(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		} else if (selectedType == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen config = selectedCalib;
			renderer.circleRegular(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		} else if (selectedType == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen config = selectedCalib;
			renderer.circleHex(config.numRows, config.numCols, config.shapeSize, config.shapeDistance);
		}

		renderingPanel.setImageUI(renderer.getBufferred());
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == comboPaper) {
			paper = (PaperSize)comboPaper.getSelectedItem();
		} else if (e.getSource() == comboUnits) {
			units = Unit.values()[comboUnits.getSelectedIndex()];
		}

		updatePreview();
	}
}
