/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

/**
 * Provides some common basic functionality for demonstrations
 *
 * @author Peter Abeles
 */
public abstract class DemonstrationBase extends JPanel {
	protected JMenuBar menuBar;
	JMenuItem menuFile, menuWebcam, menuQuit;
	final JFileChooser fc = new JFileChooser();

	public DemonstrationBase(List<String> exampleInputs) {
		setLayout(new BorderLayout());

		createMenuBar(exampleInputs);
	}

	public abstract void openFile(File file);

	private void createMenuBar(List<String> exampleInputs) {
		menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		ActionListener listener = createActionListener();

		menuFile = new JMenuItem("Open File", KeyEvent.VK_O);
		menuFile.addActionListener(listener);
		menuFile.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menuWebcam = new JMenuItem("Open Webcam", KeyEvent.VK_W);
		menuWebcam.addActionListener(listener);
		menuWebcam.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.addActionListener(listener);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		menu.add(menuFile);
		menu.add(menuWebcam);
		menu.addSeparator();
		menu.add(menuQuit);

		menu = new JMenu("Examples");
		menuBar.add(menu);

		for (final String path : exampleInputs) {
			JMenuItem menuItem = new JMenuItem(new File(path).getName());
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					openFile(new File(path));
				}
			});
			menu.add(menuItem);
		}

		add(BorderLayout.NORTH, menuBar);
	}

	private ActionListener createActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (menuFile == e.getSource()) {
					int returnVal = fc.showOpenDialog(DemonstrationBase.this);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						openFile(file);
					} else {
					}
				} else if (menuWebcam == e.getSource()) {

				} else if (menuQuit == e.getSource()) {
					System.exit(0);
				}
			}
		};
	}
}
