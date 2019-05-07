/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
 * @author Peter Abeles
 */
public abstract class BatchControlPanel extends StandardAlgConfigPanel {
	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";
	public static final String KEY_RECURSIVE = "recursive";

	protected JTextField textInputDirectory = new JTextField();
	protected JTextField textRegex = new JTextField();
	protected JCheckBox checkRecursive = new JCheckBox("Recursive");
	public JButton bAction = new JButton("Start");

	public int textWidth = 200;
	public int textHeight = 30;

	protected JPanel createTextSelect( final JTextField field , final String message , boolean directory ) {
		JButton bOpen = new JButton(UIManager.getIcon("FileView.fileIcon"));
		bOpen.setPreferredSize(new Dimension(30,30));
		bOpen.setMaximumSize(bOpen.getPreferredSize());
		bOpen.addActionListener(a->{
			selectPath(field,message,directory);
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.add(field);
		panel.add(bOpen);
		return panel;
	}

	protected void selectPath(JTextField field , String message , boolean directory) {
		File current = new File(field.getText());
		if( !current.exists() )
			current = new File(".");

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(current);
		chooser.setDialogTitle(message);
		if( directory )
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		else
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	protected abstract void handleStart();

	public interface Listener {
		void batchUpdate( String fileName );
	}

}
