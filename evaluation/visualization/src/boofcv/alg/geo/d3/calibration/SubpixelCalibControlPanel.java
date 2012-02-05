/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
public class SubpixelCalibControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener
{
	// toggle what is visible or not
	JCheckBox showPixel;
	JCheckBox showSubpixel;

	// allows the user to change the image zoom
	JSpinner selectZoom;

	// selects threshold to create binary image from
	JSpinner thresholdSpinner;

	Listener listener;
	
	boolean doShowPixel = true;
	boolean doShowSubpixel = true;

	int thresholdLevel = 120;
	
	public SubpixelCalibControlPanel( Listener listener ) {
		this.listener = listener;

		selectZoom = new JSpinner(new SpinnerNumberModel(1,1,100,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		addLabeled(selectZoom, "Zoom:",this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectZoom ) {
			double value = ((Number)selectZoom.getValue()).doubleValue();
			listener.changeScale(value);
		}
	}

	public boolean isShowPixel() {
		return doShowPixel;
	}

	public boolean isShowSubpixel() {
		return doShowSubpixel;
	}

	public int getThresholdLevel() {
		return thresholdLevel;
	}

	public static interface Listener
	{
		public void changeScale( double scale );
	}
}
