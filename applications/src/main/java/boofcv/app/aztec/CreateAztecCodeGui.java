/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.app.aztec;

import boofcv.alg.fiducial.aztec.AztecEncoder;
import boofcv.alg.fiducial.aztec.AztecGenerator;
import boofcv.app.CreateAztecCodeDocument;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * GUI for creating Aztec Codes
 *
 * @author Peter Abeles
 */
public class CreateAztecCodeGui extends JPanel implements CreateAztecCodeControlPanel.Listener {
	CreateAztecCodeControlPanel controls = new CreateAztecCodeControlPanel(this);
	ImagePanel imagePanel = new ImagePanel();

	JFrame frame;

	public CreateAztecCodeGui() {
		setLayout(new BorderLayout());

		imagePanel.setCentering(true);
		imagePanel.setScaling(ScaleOptions.DOWN);
		imagePanel.setBackground(Color.GRAY);

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imagePanel);

		setPreferredSize(new Dimension(700, 500));
		frame = ShowImages.setupWindow(this, "Aztec Code Document Creator", true);
		createMenuBar();

		renderPreview();
		frame.setVisible(true);
	}

	void createMenuBar() {
		var menuBar = new JMenuBar();

		var menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		var menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave, KeyEvent.VK_S, KeyEvent.VK_S);
		menuSave.addActionListener(e -> saveFile(false));

		var menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint, KeyEvent.VK_P, KeyEvent.VK_P);
		menuPrint.addActionListener(e -> saveFile(true));

		var menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);
		menuQuit.addActionListener(e -> System.exit(0));

		var menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(e -> showHelp());

		menuFile.addSeparator();
		menuFile.add(menuSave);
		menuFile.add(menuPrint);
		menuFile.add(menuHelp);
		menuFile.add(menuQuit);
		menuBar.add(menuFile);

		var editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		var menuCut = new JMenuItem(new DefaultEditorKit.CutAction());
		menuCut.setText("Cut");
		BoofSwingUtil.setMenuItemKeys(menuCut, KeyEvent.VK_T, KeyEvent.VK_X);
		var menuCopy = new JMenuItem(new DefaultEditorKit.CopyAction());
		menuCopy.setText("Copy");
		BoofSwingUtil.setMenuItemKeys(menuCopy, KeyEvent.VK_C, KeyEvent.VK_C);
		var menuPaste = new JMenuItem(new DefaultEditorKit.PasteAction());
		menuPaste.setText("Paste");
		BoofSwingUtil.setMenuItemKeys(menuPaste, KeyEvent.VK_P, KeyEvent.VK_V);

		editMenu.add(menuCut);
		editMenu.add(menuCopy);
		editMenu.add(menuPaste);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	private void showHelp() {
		JOptionPane.showMessageDialog(this, "Many more options and better documentation available through commandline");
	}

	private void saveFile( boolean sendToPrinter ) {
		// grab the focus and force what the user is editing to be saved

		File f;

		// see where the document is to be sent
		if (sendToPrinter) {
			if (controls.format.compareToIgnoreCase("pdf") != 0) {
				JOptionPane.showMessageDialog(this, "Must select PDF document type to print");
				return;
			}
			f = new File(""); // dummy to make the code below happy and less complex
		} else {
			f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f, "aztec." + controls.format);

			f = BoofSwingUtil.fileChooser(null, this, false, f.getPath(), null);
			if (f == null) {
				return;
			}

			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
		}

		var generator = new CreateAztecCodeDocument();

		// Make sure the file has the correct extension
		String outputFile = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(outputFile);
		if (ext.compareToIgnoreCase(controls.format) != 0) {
			outputFile = FilenameUtils.removeExtension(outputFile);
			outputFile += "." + controls.format;
		}

		generator.fileName = outputFile;
		generator.errorFraction = controls.errorFraction;
		generator.numLayers = controls.numLayers;
		generator.structure = controls.structure;
		generator.paperSize = controls.paperSize;
		generator.gridFill = controls.fillGrid;
		generator.drawGrid = controls.drawGrid;
		generator.hideInfo = controls.hideInfo;
		generator.messages = new ArrayList<>();
		generator.messages.add(controls.message);
		generator.unit = controls.documentUnits;
		generator.markerWidth = (float)controls.markerWidth;
		generator.sendToPrinter = sendToPrinter;

		try {
			generator.finishParsing();
			generator.run();
		} catch (IOException | RuntimeException e) {
			System.out.println("Exception!!!");
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	private void renderPreview() {
		AztecEncoder encoder = new AztecEncoder().setStructure(controls.structure);

		if (controls.errorFraction >= 0) {
			encoder.setEcc(controls.errorFraction);
		}
		if (controls.numLayers > 0) {
			encoder.setLayers(controls.numLayers);
		}

		encoder.addAutomatic(controls.message);
		GrayU8 preview = null;
		try {
			preview = AztecGenerator.renderImage(10, 0, encoder.fixate());
		} catch (RuntimeException e) {
			System.err.println("Render Failed! " + e.getClass().getSimpleName() + " " + e.getMessage());
//			e.printStackTrace();
		}

		if (preview == null) {
			BufferedImage output = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = output.createGraphics();
			g2.setColor(Color.RED);
			g2.fillRect(0, 0, output.getWidth(), output.getHeight());
			imagePanel.setImageRepaint(output);
		} else {
			BufferedImage output = new BufferedImage(preview.width, preview.height, BufferedImage.TYPE_INT_RGB);
			ConvertBufferedImage.convertTo(preview, output);
			imagePanel.setImageRepaint(output);
		}
	}

	@Override
	public void controlsUpdates() {
		new Thread(() -> renderPreview()).start();
	}
}
