/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A jpanel that keeps track of the process id it is supposed to be displaying information for.
 * Intended to contain the source code and output.
 *
 * Created by Jalal on 4/30/2017.
 */
public class ProcessTabPanel extends JPanel {

	private long processId;

	public JComboBox<String> options;
	public JButton bOpenInGitHub = new JButton("GitHub");

	public ProcessTabPanel(long processId) {
		setLayout(new BorderLayout());
		this.processId = processId;

		String[] optionsList = new String[]{"Source", "Output"};
		options = new JComboBox<>(optionsList);
		options.setBorder(new EmptyBorder(5,5,5,5));
		options.setMaximumSize(options.getPreferredSize());

		JPanel intermediate = new JPanel();
		intermediate.setLayout(new BoxLayout(intermediate,BoxLayout.X_AXIS));
		intermediate.add(options);
		intermediate.add(Box.createHorizontalGlue());
		if( Desktop.isDesktopSupported() ) {
			intermediate.add(bOpenInGitHub);
		}

		add(intermediate, BorderLayout.NORTH);
	}

	public void setProcessId(long processId) {
		this.processId = processId;
	}

	public long getProcessId() {
		return this.processId;
	}
}
