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

package boofcv.app.batch;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Batch processing of files control panel
 *
 * @author Peter Abeles
 */
public abstract class BatchControlPanel extends StandardAlgConfigPanel {
	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";

	protected JTextField textInputSource = new JTextField();
	public JButton bAction = new JButton("Start");

	public int textWidth = 200;
	public int textHeight = 30;

	protected JPanel createTextSelect( final JTextField field, final String message, boolean directory ) {
		JButton bOpen = new JButton(UIManager.getIcon("FileView.fileIcon"));
		bOpen.setPreferredSize(new Dimension(30, 30));
		bOpen.setMaximumSize(bOpen.getPreferredSize());
		bOpen.addActionListener(a -> {
			selectPath(field, message, directory);
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(field);
		panel.add(bOpen);
		return panel;
	}

	protected void selectPath( JTextField field, String message, boolean directory ) {
		File current = new File(field.getText());
		if (!current.exists())
			current = new File(".");

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(current);
		chooser.setDialogTitle(message);
		if (directory)
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		else
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	protected static JComponent createInputHelp() {
		var text = new JTextArea("If 'Input Source' is a file or directory then all image files in the directory are added." +
				" Otherwise 'glob' or 'regex' patterns can be used to search for all matches.\n" +
				"'glob:path/**/media*' will recursively search for all files starting with media\n" +
				"'regex:path/\\w+/media\\w+' will recursively search for all files starting with media");
		text.setLineWrap(true);
		return text;
	}

	protected abstract void handleStart();

	public interface Listener {
		void batchUpdate( String fileName );
	}
}
