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

package boofcv.app.fiducials;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.drawing.FiducialImageGenerator;
import boofcv.app.BaseFiducialSquare;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * GUI for printing square binary fiducials
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class CreateSquareFiducialGui extends JPanel implements CreateSquareFiducialControlPanel.Listener {

	protected CreateSquareFiducialControlPanel controls;
	protected ImagePanel imagePanel = new ImagePanel();
	JFrame frame;

	protected FiducialImageEngine render = new FiducialImageEngine();
	protected FiducialImageGenerator generator;
	protected BufferedImage buffered;

	String defaultSaveName;

	protected CreateSquareFiducialGui( String defaultSaveName ) {
		super(new BorderLayout());
		this.defaultSaveName = defaultSaveName;
	}

	public void setupGui( CreateSquareFiducialControlPanel controls, String title ) {
		this.controls = controls;
		render.configure(20, 300);
		generator.setMarkerWidth(300);
		buffered = new BufferedImage(render.getGray().width, render.getGray().height, BufferedImage.TYPE_INT_RGB);

		imagePanel.setPreferredSize(new Dimension(400, 400));
		imagePanel.setScaling(ScaleOptions.DOWN);
		imagePanel.setCentering(true);
		imagePanel.setBackground(Color.GRAY);

		add(controls, BorderLayout.WEST);
		add(imagePanel, BorderLayout.CENTER);

		setPreferredSize(new Dimension(700, 500));
		frame = ShowImages.setupWindow(this, title, true);
		createMenuBar();

		renderPreview();
		frame.setVisible(true);
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave, KeyEvent.VK_S, KeyEvent.VK_S);
		menuSave.addActionListener(e -> saveFile(false));

		JMenuItem menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint, KeyEvent.VK_P, KeyEvent.VK_P);
		menuPrint.addActionListener(e -> saveFile(true));

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit, KeyEvent.VK_Q, KeyEvent.VK_Q);
		menuQuit.addActionListener(e -> System.exit(0));

		JMenuItem menuHelp = new JMenuItem("Help", KeyEvent.VK_H);
		menuHelp.addActionListener(e -> showHelp());

		menuFile.addSeparator();
		menuFile.add(menuSave);
		menuFile.add(menuPrint);
		menuFile.add(menuHelp);
		menuFile.add(menuQuit);
		menuBar.add(menuFile);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		JMenuItem menuCut = new JMenuItem(new DefaultEditorKit.CutAction());
		menuCut.setText("Cut");
		BoofSwingUtil.setMenuItemKeys(menuCut, KeyEvent.VK_T, KeyEvent.VK_X);
		JMenuItem menuCopy = new JMenuItem(new DefaultEditorKit.CopyAction());
		menuCopy.setText("Copy");
		BoofSwingUtil.setMenuItemKeys(menuCopy, KeyEvent.VK_C, KeyEvent.VK_C);
		JMenuItem menuPaste = new JMenuItem(new DefaultEditorKit.PasteAction());
		menuPaste.setText("Paste");
		BoofSwingUtil.setMenuItemKeys(menuPaste, KeyEvent.VK_P, KeyEvent.VK_V);

		editMenu.add(menuCut);
		editMenu.add(menuCopy);
		editMenu.add(menuPaste);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	protected void saveFile( boolean sendToPrinter, BaseFiducialSquare c ) {
		if (sendToPrinter) {
			if (controls.format.compareToIgnoreCase("pdf") != 0) {
				JOptionPane.showMessageDialog(this, "Must select PDF document type to print");
				return;
			}
		} else {
			File f = FileSystemView.getFileSystemView().getHomeDirectory();
			f = new File(f, defaultSaveName + "." + controls.format);

			f = BoofSwingUtil.fileChooser(null, this, false, f.getPath(), (n)-> FilenameUtils.getBaseName(n) + "." + controls.format);
			if (f == null) {
				return;
			}
			if (f.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Can't save to a directory!");
				return;
			}
			c.fileName = f.getAbsolutePath();
		}

		c.finishParsing();
		try {
			c.run();
		} catch (IOException e) {
			BoofSwingUtil.warningDialog(this, e);
		}
	}

	protected abstract void saveFile( boolean sendToPrinter );

	protected abstract void showHelp();

	protected abstract void renderPreview();

	@Override
	public void controlsUpdates() {
		renderPreview();
	}
}
