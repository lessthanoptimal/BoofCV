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

package boofcv.demonstrations.distort;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Panel which lets you select the shape and FOV for a cylindrical projection
 *
 * @author Peter Abeles
 */
public class CylinderPanel extends StandardAlgConfigPanel
	implements ChangeListener
{

	JSpinner selectVFOV;
	JSpinner selectWidth;
	JSpinner selectHeight;

	double vfov;
	int width,height;

	Listener listener;

	public CylinderPanel(int width , int height , double vfov ,
						 Listener listener ) {
		this.width = width;
		this.height = height;
		this.vfov = vfov;
		this.listener = listener;

		selectVFOV = new JSpinner(new SpinnerNumberModel(vfov, 5, 175, 5));
		selectVFOV.setMaximumSize(selectVFOV.getPreferredSize());
		selectVFOV.addChangeListener(this);

		selectWidth = new JSpinner(new SpinnerNumberModel(width, 100, 800, 10));
		selectWidth.setMaximumSize(selectWidth.getPreferredSize());
		selectWidth.addChangeListener(this);

		selectHeight = new JSpinner(new SpinnerNumberModel(height, 100, 800, 10));
		selectHeight.setMaximumSize(selectHeight.getPreferredSize());
		selectHeight.addChangeListener(this);

		addLabeled(selectWidth,  "Image Width: ");
		addLabeled(selectHeight, "Image Height: ");
		addLabeled(selectVFOV,    "Field-of-View: ");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( selectWidth == e.getSource() ) {
			width = ((Number) selectWidth.getValue()).intValue();
		} else if( selectHeight == e.getSource() ) {
			height = ((Number) selectHeight.getValue()).intValue();
		} else if( selectVFOV == e.getSource() ) {
			vfov =  ((Number) selectVFOV.getValue()).intValue();
		} else {
			return;
		}
		listener.updateCylinder(width,height, vfov);
	}

	public interface Listener {
		void updateCylinder(int width, int height, double vfov);
	}
}
