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
 * Panel which lets you select the shape and FOV for a pinhole camera
 *
 * @author Peter Abeles
 */
public class PinholeSimplifiedPanel extends StandardAlgConfigPanel
	implements ChangeListener
{

	JSpinner selectFOV;
	JSpinner selectWidth;
	JSpinner selectHeight;

	double fov;
	int width,height;

	Listener listener;

	public PinholeSimplifiedPanel(int width , int height , double fov ,
								  Listener listener ) {
		this.width = width;
		this.height = height;
		this.fov = fov;
		this.listener = listener;

		selectFOV = new JSpinner(new SpinnerNumberModel(80, 5, 175, 5));
		selectFOV.setMaximumSize(selectFOV.getPreferredSize());
		selectFOV.addChangeListener(this);

		selectWidth = new JSpinner(new SpinnerNumberModel(400, 100, 800, 10));
		selectWidth.setMaximumSize(selectWidth.getPreferredSize());
		selectWidth.addChangeListener(this);

		selectHeight = new JSpinner(new SpinnerNumberModel(300, 100, 800, 10));
		selectHeight.setMaximumSize(selectHeight.getPreferredSize());
		selectHeight.addChangeListener(this);

		addLabeled(selectWidth,  "Image Width: ");
		addLabeled(selectHeight, "Image Height: ");
		addLabeled(selectFOV,    "Field-of-View: ");
	}

	public void addToFOV( int degrees ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			double value = fov + degrees;
			value = Math.max(5,value);
			value = Math.min(175,value);
			selectFOV.setValue(value);
		} else
			throw new RuntimeException("Invoke in GUI thread!");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( selectWidth == e.getSource() ) {
			width = ((Number) selectWidth.getValue()).intValue();
		} else if( selectHeight == e.getSource() ) {
			height = ((Number) selectHeight.getValue()).intValue();
		} else if( selectFOV == e.getSource() ) {
			fov =  ((Number) selectFOV.getValue()).intValue();
		} else {
			return;
		}
		listener.updatedPinholeModel(width,height,fov);

	}

	public interface Listener {
		void updatedPinholeModel( int width , int height , double fov );
	}
}
