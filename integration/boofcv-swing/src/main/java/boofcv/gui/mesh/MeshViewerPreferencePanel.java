/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.mesh;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Lets the user configure controls and provides help explaining how to control.
 *
 * @author Peter Abeles
 */
public class MeshViewerPreferencePanel extends StandardAlgConfigPanel {
	// Shows user interface controls
	JComboBox<String> comboControls;

	// Allows the user to hide the help. Good for screenshots
	JCheckBox checkHide = checkbox("Hide Help", true);

	// Help about controls
	JTextArea textArea = new JTextArea();

	// The owner
	MeshViewerPanel panel;

	/**
	 *
	 * @param panel The viewer that this is adjusting
	 */
	public MeshViewerPreferencePanel( MeshViewerPanel panel ) {
		this.panel = panel;

		comboControls = combo(0, new ArrayList<>(panel.controls.keySet()).toArray());

		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));

		setHelpText();

		addLabeled(comboControls, "Controls");
		addAlignLeft(checkHide);
		addAlignCenter(textArea);

		setPreferredSize(new Dimension(300, 400));
	}

	private void setHelpText() {
		String text = """
				h   Set view to home
				j   Cycle colorization
				k   Show depth image
				""";
		text += "\n" + panel.controls.get((String)comboControls.getSelectedItem()).getHelpText();

		textArea.setText(text);
	}

	@Override public void controlChanged( final Object source ) {
		if (source == comboControls) {
			var selected = (String)comboControls.getSelectedItem();
			panel.setActiveControl(selected);
			setHelpText();
		} else if (source == checkHide) {
			panel.helpButtonActive = checkHide.isSelected();
			panel.repaint();
		}
	}
}
