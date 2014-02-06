/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel which allows the user to configure segmentation algorithms and select the visualization
 *
 * @author Peter Abeles
 */
public class SegmentConfigPanel extends StandardAlgConfigPanel implements ActionListener{



	JComboBox selectVisualize;

	VisualizeImageSegmentationApp owner;

	public SegmentConfigPanel(VisualizeImageSegmentationApp owner) {
		this.owner = owner;

		selectVisualize = new JComboBox(new String[]{"Color","Border","Regions"});
		selectVisualize.addActionListener(this);
		selectVisualize.setMaximumSize(selectVisualize.getPreferredSize());

		addLabeled(selectVisualize,"Visualize",this);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		owner.updateActiveDisplay(selectVisualize.getSelectedIndex());
	}

}
