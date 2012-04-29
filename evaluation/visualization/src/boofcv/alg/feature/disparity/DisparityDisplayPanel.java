/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Controls GUI and settings for disparity calculation
 *
 * @author Peter Abeles
 */
public class DisparityDisplayPanel extends StandardAlgConfigPanel
		implements ChangeListener, ItemListener
{

	// selects which image to view
	JComboBox viewSelector;

	JSpinner disparitySpinner;
	JSpinner errorSpinner;
	JSpinner reverseSpinner;

	// which image to show
	int selectedView;

	// maximum disparity to calculate
	int maxDisparity = 100;
	// maximum allowed per pixel error
	int pixelError = 20;
	// reverse association tolerance
	int reverseTol = 2;

	// listener for changes in states
	Listener listener;

	public DisparityDisplayPanel() {
		viewSelector = new JComboBox();
		viewSelector.addItem("Disparity");
		viewSelector.addItem("Left");
		viewSelector.addItem("Right");
		viewSelector.addItemListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		disparitySpinner = new JSpinner(new SpinnerNumberModel(maxDisparity,0, 255, 5));
		disparitySpinner.addChangeListener(this);
		disparitySpinner.setMaximumSize(disparitySpinner.getPreferredSize());

		errorSpinner = new JSpinner(new SpinnerNumberModel(pixelError,-1, 50, 5));
		errorSpinner.addChangeListener(this);
		errorSpinner.setMaximumSize(errorSpinner.getPreferredSize());

		reverseSpinner = new JSpinner(new SpinnerNumberModel(reverseTol,-1, 20, 1));
		reverseSpinner.addChangeListener(this);
		reverseSpinner.setMaximumSize(reverseSpinner.getPreferredSize());

		addLabeled(viewSelector,"View ",this);
		addSeparator(100);
		addLabeled(disparitySpinner, "Max Disparity", this);
		addLabeled(errorSpinner, "Max Error", this);
		addLabeled(reverseSpinner, "Reverse", this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == reverseSpinner) {
			reverseTol = ((Number) reverseSpinner.getValue()).intValue();
		} else if( e.getSource() == disparitySpinner) {
			maxDisparity = ((Number) disparitySpinner.getValue()).intValue();
		} else if( e.getSource() == errorSpinner) {
			pixelError = ((Number) errorSpinner.getValue()).intValue();
		}
		listener.disparitySettingChange();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
			listener.disparityGuiChange();
		}
	}

	public void setListener(Listener listener ) {
		this.listener = listener;
	}

	public int getReverseTol() {
		return reverseTol;
	}

	public int getMaxDisparity() {

		return maxDisparity;
	}

	public int getPixelError() {
		return pixelError;
	}

	public int getSelectedView() {
		return selectedView;
	}

	public static interface Listener
	{
		public void disparitySettingChange();

		public void disparityGuiChange();
	}
}
