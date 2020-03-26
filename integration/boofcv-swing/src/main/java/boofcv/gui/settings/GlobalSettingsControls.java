/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.settings;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.dialogs.JSpringPanel;
import com.github.weisj.darklaf.LafManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Control panel
 *
 * @author Peter Abeles
 */
public class GlobalSettingsControls extends StandardAlgConfigPanel implements ActionListener  {

	GlobalDemoSettings settings = GlobalDemoSettings.SETTINGS.copy();

	JComboBox<String> comboThemes = combo(settings.theme.ordinal(), GlobalDemoSettings.ThemesUI.values());
	JComboBox<String> comboControl3D = combo(settings.controls3D.ordinal(), GlobalDemoSettings.Controls3D.values());

	JButton bSave = new JButton("Save");
	JButton bReset = new JButton("Reset");

	boolean changedTheme = false;

	JDialog dialog;
	boolean canceled = false;

	public GlobalSettingsControls() {
		bSave.addActionListener((e)-> handleSave());
		bReset.addActionListener((e)-> handleReset());

		addLabeled(comboThemes,"Themes");
		addLabeled(comboControl3D,"Control3D");
		addVerticalGlue();
		JPanel foo = JSpringPanel.createLockedSides(bReset,bSave,30);
		foo.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
		foo.setPreferredSize(new Dimension(0,40));
		foo.setMaximumSize(new Dimension(5000,40));
		add(foo);
		setPreferredSize(new Dimension(250,120));
	}

	private void handleSave() {
		GlobalDemoSettings.SETTINGS = settings;
		settings.save();
		dialog.setVisible(false);
	}

	private void handleReset() {
		// should reset everything. This is ugly
		GlobalDemoSettings.SETTINGS = new GlobalDemoSettings();
		GlobalDemoSettings.SETTINGS.save();
		handleCancel();
	}

	private void handleCancel() {
		canceled = true;
		dialog.setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboThemes ) {
			settings.theme = GlobalDemoSettings.ThemesUI.values()[comboThemes.getSelectedIndex()];
			changedTheme = true;
		} else if( e.getSource() == comboControl3D ) {
			settings.controls3D = GlobalDemoSettings.Controls3D.values()[comboControl3D.getSelectedIndex()];
		}
	}

	public void showDialog( JFrame owner , Component parent )
	{
		canceled = false;

		dialog = new JDialog(owner,"Demonstration Settings", Dialog.ModalityType.APPLICATION_MODAL);

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				handleCancel();
			}
		});
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		// should block at this point
		dialog.dispose();
		dialog = null;

		// See if it needs to enact the changes
		if( !canceled ) {
			if( changedTheme ) {
				settings.changeTheme();
				// Update the all windows with the new theme
				LafManager.updateLaf();
				// This warning is needed since not all changes from the previous LAF are reset and can result
				// in really messed up looking themes
				JOptionPane.showMessageDialog(parent,"Restart to ensure the theme renders correctly");
			}
		}
	}

	public static void main(String[] args) {
		GlobalSettingsControls controls = new GlobalSettingsControls();
		controls.showDialog(null,null);
	}
}
