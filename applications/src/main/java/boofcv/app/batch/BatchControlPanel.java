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

package boofcv.app.batch;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * @author Peter Abeles
 */
public abstract class BatchControlPanel extends StandardAlgConfigPanel {
	public static final String KEY_INPUT = "input";
	public static final String KEY_OUTPUT = "output";

	protected JTextField textInputDirectory = new JTextField();
	protected JTextField textOutputDirectory = new JTextField();
	protected JTextField textRegex = new JTextField();
	protected JCheckBox checkRecursive = new JCheckBox("Recursive");
	protected JCheckBox checkRename = new JCheckBox("Rename");
	public JButton bAction = new JButton("Start");

	int textWidth = 200;
	int textHeight = 30;

	public void addStandardControls(Preferences prefs) {

		textInputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
		textInputDirectory.setMaximumSize(textInputDirectory.getPreferredSize());
		textOutputDirectory.setPreferredSize(new Dimension(textWidth,textHeight));
		textOutputDirectory.setMaximumSize(textOutputDirectory.getPreferredSize());
		textRegex.setPreferredSize(new Dimension(textWidth+40,textHeight));
		textRegex.setMaximumSize(textRegex.getPreferredSize());
		textRegex.setText("([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)");
		checkRecursive.setSelected(false);

		textInputDirectory.setText(prefs.get(KEY_INPUT,""));
		textOutputDirectory.setText(prefs.get(KEY_OUTPUT,""));

		bAction.addActionListener(a-> handleStart());

		addLabeled(createTextSelect(textInputDirectory,"Input Directory",true),"Input");
		addLabeled(createTextSelect(textOutputDirectory,"Output Directory",true),"Output");
		addAlignLeft(checkRecursive);
		addAlignLeft(checkRename);
		addLabeled(textRegex,"Regex");
		addVerticalGlue();
		addAlignCenter(bAction);
	}

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
}
