/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.app.markers.CreateSquareMarkerControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * GUI for printing square binary fiducials
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinaryGui extends JPanel implements CreateSquareMarkerControlPanel.Listener {

	// TODO list with fiducials by ID
	// TODO Add and Remove buttons

	ControlPanel controls = new ControlPanel(this);
	ImagePanel imagePanel = new ImagePanel();
	JFrame frame;

	public CreateFiducialSquareBinaryGui() {
		super(new BorderLayout());

		imagePanel.setPreferredSize(new Dimension(400,400));
		imagePanel.setScaling(ScaleOptions.DOWN);

		add(controls,BorderLayout.WEST);
		add(imagePanel,BorderLayout.CENTER);

		setPreferredSize(new Dimension(700,500));
		frame = ShowImages.setupWindow(this,"Fiducial Square Binary",true);
		createMenuBar();

		renderPreview();
		frame.setVisible(true);
	}

	void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		JMenuItem menuSave = new JMenuItem("Save");
		BoofSwingUtil.setMenuItemKeys(menuSave,KeyEvent.VK_S,KeyEvent.VK_S);
		menuSave.addActionListener(e -> saveFile(false));

		JMenuItem menuPrint = new JMenuItem("Print...");
		BoofSwingUtil.setMenuItemKeys(menuPrint,KeyEvent.VK_P,KeyEvent.VK_P);
		menuPrint.addActionListener(e -> saveFile(true));

		JMenuItem menuQuit = new JMenuItem("Quit");
		BoofSwingUtil.setMenuItemKeys(menuQuit,KeyEvent.VK_Q,KeyEvent.VK_Q);
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
		menuCut.setText("Cut");BoofSwingUtil.setMenuItemKeys(menuCut,KeyEvent.VK_T,KeyEvent.VK_X);
		JMenuItem menuCopy = new JMenuItem(new DefaultEditorKit.CopyAction());
		menuCopy.setText("Copy");BoofSwingUtil.setMenuItemKeys(menuCopy,KeyEvent.VK_C,KeyEvent.VK_C);
		JMenuItem menuPaste = new JMenuItem(new DefaultEditorKit.PasteAction());
		menuPaste.setText("Paste");BoofSwingUtil.setMenuItemKeys(menuPaste,KeyEvent.VK_P,KeyEvent.VK_V);

		editMenu.add(menuCut);
		editMenu.add(menuCopy);
		editMenu.add(menuPaste);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	private void saveFile( boolean sendToPrinter ) {

	}

	private void showHelp() {

	}

	private void renderPreview() {

	}

	@Override
	public void controlsUpdates() {

	}

	class ControlPanel extends CreateSquareMarkerControlPanel {

		public ControlPanel(Listener listener) {
			super(listener);

			layoutComponents();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(CreateFiducialSquareBinaryGui::new);
	}
}
